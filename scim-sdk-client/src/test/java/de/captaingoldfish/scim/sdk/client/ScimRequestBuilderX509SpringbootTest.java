package de.captaingoldfish.scim.sdk.client;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.builder.BulkBuilder;
import de.captaingoldfish.scim.sdk.client.builder.PatchBuilder;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.TestSingletonHandler;
import de.captaingoldfish.scim.sdk.client.setup.scim.resources.ScimTestSingleton;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SecurityConstants;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 10:50 <br>
 * <br>
 */
@Slf4j
@ActiveProfiles(SecurityConstants.X509_PROFILE)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {SpringBootInitializer.class})
public class ScimRequestBuilderX509SpringbootTest extends AbstractSpringBootWebTest
{

  /**
   * the request builder that is under test
   */
  private ScimRequestBuilder scimRequestBuilder;

  /**
   * initializes the request builder
   */
  @BeforeEach
  public void init()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(5)
                                                        .requestTimeout(5)
                                                        .socketTimeout(5)
                                                        .clientAuth(getClientAuthKeystore())
                                                        .truststore(getTruststore())
                                                        // hostname verifier disabled for tests
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getRequestUrl(TestController.SCIM_ENDPOINT_PATH), scimClientConfig);
  }

  /**
   * verifies that a create request can be successfully built and send to the scim service provider
   */
  @Test
  public void testBuildCreateRequest()
  {
    final String username = UUID.randomUUID().toString();
    User user = User.builder().userName(username).name(Name.builder().givenName("goldfish").build()).build();
    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    User returnedUser = response.getResource();
    Assertions.assertEquals(username, returnedUser.getUserName().get());
    Assertions.assertEquals(returnedUser.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @Test
  public void testBuildCreateRequestWithErrorResponse()
  {
    // the missing username will cause an error for missing required attribute
    User user = User.builder().nickName("goldfish").build();

    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
    Assertions.assertEquals("Required 'READ_WRITE' attribute "
                            + "'urn:ietf:params:scim:schemas:core:2.0:User:userName' is missing",
                            response.getErrorResponse().getDetail().get());
  }

  /**
   * verifies that the authorization fails if the required x509 authorization is missing
   */
  @Test
  public void testNoAuthorizationUsed()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(5)
                                                        .requestTimeout(5)
                                                        .socketTimeout(5)
                                                        .truststore(getTruststore())
                                                        // hostname verifier disabled for tests
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getRequestUrl(TestController.SCIM_ENDPOINT_PATH), scimClientConfig);

    User user = User.builder().userName(UUID.randomUUID().toString()).build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that an unauthorized user is not getting access
   */
  @Test
  public void testUnauthorizedUser()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(5)
                                                        .requestTimeout(5)
                                                        .socketTimeout(5)
                                                        .clientAuth(getUnauthorizedClientAuthKeystore())
                                                        .truststore(getTruststore())
                                                        // hostname verifier disabled for tests
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getRequestUrl(TestController.SCIM_ENDPOINT_PATH), scimClientConfig);

    User user = User.builder().userName(UUID.randomUUID().toString()).build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
    Assertions.assertEquals("You are not authorized to access the 'CREATE' endpoint on resource type 'User'",
                            response.getErrorResponse().getDetail().get());
  }

  /**
   * verifies that the bulk request builder can be used as expected
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBulkRequest(boolean withFullyQualifiedUrl)
  {
    final String bulkId = UUID.randomUUID().toString();
    BulkBuilder builder;
    if (withFullyQualifiedUrl)
    {
      builder = scimRequestBuilder.bulk(getRequestUrl(TestController.SCIM_ENDPOINT_PATH + EndpointPaths.BULK));
    }
    else
    {
      builder = scimRequestBuilder.bulk();
    }
    final String username = UUID.randomUUID().toString();
    ServerResponse<BulkResponse> response = builder.bulkRequestOperation(EndpointPaths.USERS)
                                                   .data(User.builder().userName(username).build())
                                                   .method(HttpMethod.POST)
                                                   .bulkId(bulkId)
                                                   .next()
                                                   .bulkRequestOperation(EndpointPaths.GROUPS)
                                                   .method(HttpMethod.POST)
                                                   .bulkId(UUID.randomUUID().toString())
                                                   .data(Group.builder()
                                                              .displayName("admin")
                                                              .members(Arrays.asList(Member.builder()
                                                                                           .value("bulkId:" + bulkId)
                                                                                           .type(ResourceTypeNames.USER)
                                                                                           .build()))
                                                              .build())
                                                   .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertNotNull(response.getResource());
    BulkResponse bulkResponse = response.getResource();
    Assertions.assertEquals(2, bulkResponse.getBulkResponseOperations().size());

    BulkResponseOperation createUserOperation = bulkResponse.getBulkResponseOperations().get(0);
    Assertions.assertEquals(HttpStatus.CREATED, createUserOperation.getStatus());
    Assertions.assertEquals(bulkId, createUserOperation.getBulkId().get());

    BulkResponseOperation createGroupOperation = bulkResponse.getBulkResponseOperations().get(1);
    Assertions.assertEquals(HttpStatus.CREATED, createGroupOperation.getStatus());
    Assertions.assertTrue(createGroupOperation.getBulkId().isPresent());
  }

  /**
   * tests that patch requests are correctly send to the server
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testPatchRequest(boolean withFullyQualifiedUrl)
  {
    final String emailValue = "happy.day@scim-sdk.de";
    final String emailType = "fun";
    final boolean emailPrimary = true;
    final String givenName = "Link";
    final String locale = "JAP";

    // get the id of an existing user
    ServerResponse<ListResponse<User>> listResponse = scimRequestBuilder.list(User.class, EndpointPaths.USERS)
                                                                        .count(1)
                                                                        .startIndex(withFullyQualifiedUrl ? 1 : 2)
                                                                        .get()
                                                                        .sendRequest();
    User randomUser = listResponse.getResource().getListedResources().get(0);

    User addingResource = User.builder()
                              .emails(Collections.singletonList(Email.builder()
                                                                     .value(emailValue)
                                                                     .type(emailType)
                                                                     .primary(emailPrimary)
                                                                     .build()))
                              .build();

    PatchBuilder<User> patchBuilder;
    if (withFullyQualifiedUrl)
    {
      patchBuilder = scimRequestBuilder.patch(getRequestUrl(TestController.SCIM_ENDPOINT_PATH + EndpointPaths.USERS
                                                            + "/" + randomUser.getId().get()),
                                              User.class);
    }
    else
    {
      patchBuilder = scimRequestBuilder.patch(User.class, EndpointPaths.USERS, randomUser.getId().get());
    }
    ServerResponse<User> response = patchBuilder.addOperation()
                                                .path("name.givenname")
                                                .op(PatchOp.ADD)
                                                .value(givenName)
                                                .next()
                                                .path("locale")
                                                .op(PatchOp.REPLACE)
                                                .value(locale)
                                                .next()
                                                .op(PatchOp.ADD)
                                                .valueNode(addingResource)
                                                .build()
                                                .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertNotNull(response.getResource());
    User patchedUser = response.getResource();
    Assertions.assertEquals(givenName, patchedUser.getName().flatMap(Name::getGivenName).orElse(null));
    Assertions.assertEquals(locale, patchedUser.getLocale().orElse(null));
    Assertions.assertEquals(1, patchedUser.getEmails().size());
    Assertions.assertEquals(emailValue, patchedUser.getEmails().get(0).getValue().orElse(null));
    Assertions.assertEquals(emailType, patchedUser.getEmails().get(0).getType().orElse(null));
    Assertions.assertEquals(emailPrimary, patchedUser.getEmails().get(0).isPrimary());
  }

  /**
   * tests that patch requests are correctly send to the server for singleton resources
   */
  @Test
  public void testPatchRequestForSingleton()
  {
    TestSingletonHandler.scimTestSingleton = ScimTestSingleton.builder()
                                                              .singletonAttribute("blubb")
                                                              .meta(Meta.builder()
                                                                        .created(Instant.now())
                                                                        .lastModified(Instant.now())
                                                                        .build())
                                                              .build();

    final String updatedValue = "hello world";
    PatchBuilder<ScimTestSingleton> patchBuilder = scimRequestBuilder.patch(ScimTestSingleton.class,
                                                                            TestSingletonHandler.ENDPOINT);
    ServerResponse<ScimTestSingleton> response = patchBuilder.addOperation()
                                                             .path("singletonAttribute")
                                                             .op(PatchOp.ADD)
                                                             .value(updatedValue)
                                                             .build()
                                                             .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertNotNull(response.getResource());
    ScimTestSingleton patchedResource = response.getResource();
    Assertions.assertEquals(updatedValue, patchedResource.getSingletonAttribute().orElse(null));
  }

}
