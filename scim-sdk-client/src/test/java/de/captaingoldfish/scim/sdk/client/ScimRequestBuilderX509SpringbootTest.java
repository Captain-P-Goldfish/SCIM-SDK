package de.captaingoldfish.scim.sdk.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.builder.BulkBuilder;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SecurityConstants;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.BulkResponse;
import de.captaingoldfish.scim.sdk.common.response.BulkResponseOperation;
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
    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    User returnedUser = response.getResource();
    Assertions.assertEquals("goldfish", returnedUser.getUserName().get());
    Assertions.assertEquals(returnedUser.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @Test
  public void testBuildCreateRequestWithErrorResponse()
  {
    User user = User.builder().userName("goldfish").build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
    Assertions.assertEquals("main resource schema 'urn:ietf:params:scim:schemas:core:2.0:User' is not present in "
                            + "resource. Main schema is: urn:ietf:params:scim:schemas:core:2.0:User",
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

    User user = User.builder().userName("goldfish").build();
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

    User user = User.builder().userName("goldfish").build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNotNull(response.getErrorResponse());
    Assertions.assertEquals("you are not authorized to access the 'CREATE' endpoint on resource type 'User'",
                            response.getErrorResponse().getDetail().get());
  }

  /**
   * verifies that the bulk request builder can be used as expected
   */
  @Test
  public void testBulkRequest()
  {
    final String bulkId = UUID.randomUUID().toString();
    BulkBuilder builder = scimRequestBuilder.bulk();
    ServerResponse<BulkResponse> response = builder.bulkRequestOperation(EndpointPaths.USERS)
                                                   .data(User.builder().userName("goldfish").build())
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

    log.warn(bulkResponse.toPrettyString());
  }

}
