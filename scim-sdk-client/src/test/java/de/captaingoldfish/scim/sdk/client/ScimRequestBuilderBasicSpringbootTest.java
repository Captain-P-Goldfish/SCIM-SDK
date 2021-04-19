package de.captaingoldfish.scim.sdk.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.FileReferences;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SecurityConstants;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 10:50 <br>
 * <br>
 */
@Slf4j
@ActiveProfiles(SecurityConstants.BASIC_PROFILE)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {SpringBootInitializer.class})
public class ScimRequestBuilderBasicSpringbootTest extends AbstractSpringBootWebTest implements FileReferences
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
                                                        .truststore(getTruststore())
                                                        // hostname verifier disabled for tests
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .basic(SecurityConstants.AUTHORIZED_USERNAME,
                                                               SecurityConstants.PASSWORD)
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getRequestUrl(TestController.SCIM_ENDPOINT_PATH), scimClientConfig);
  }

  /**
   * verifies that a create request can be successfully built and send to the scim service provider
   */
  @TestFactory
  public List<DynamicTest> testBuildCreateRequest()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(sendAuthorizationRequest(true));
    dynamicTests.add(sendAuthorizationRequest(false));
    return dynamicTests;
  }

  /**
   * sends a request to the server either with basic authentication or without
   */
  private DynamicTest sendAuthorizationRequest(boolean withBasicAuth)
  {
    final String descriptionString = (withBasicAuth ? "enabled" : "disabled");
    return DynamicTest.dynamicTest("basic auth: " + descriptionString, () -> {
      AtomicBoolean wasConsumerExecuted = new AtomicBoolean(false);
      if (withBasicAuth)
      {
        scimRequestBuilder.getScimClientConfig()
                          .setBasicAuth(BasicAuth.builder()
                                                 .username(SecurityConstants.AUTHORIZED_USERNAME)
                                                 .password(SecurityConstants.PASSWORD)
                                                 .build());
        AbstractSpringBootWebTest.headerValidator = headers -> {
          Assertions.assertTrue(headers.containsKey(HttpHeader.AUTHORIZATION.toLowerCase()));
          Assertions.assertFalse(headers.containsKey("cookie"));
          wasConsumerExecuted.set(true);
        };
      }
      else
      {
        scimRequestBuilder.getScimClientConfig().setBasicAuth(null);
        AbstractSpringBootWebTest.headerValidator = headers -> {
          Assertions.assertFalse(headers.containsKey(HttpHeader.AUTHORIZATION.toLowerCase()));
          Assertions.assertTrue(headers.containsKey("cookie"));
          wasConsumerExecuted.set(true);
        };
      }
      User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
      ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                        .setResource(user)
                                                        .sendRequest();
      Assertions.assertTrue(wasConsumerExecuted.get());
      Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
      Assertions.assertTrue(response.isSuccess());
      Assertions.assertNotNull(response.getResource());
      Assertions.assertNull(response.getErrorResponse());
      Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

      User returnedUser = response.getResource();
      Assertions.assertEquals("goldfish", returnedUser.getUserName().get());
      Assertions.assertEquals(returnedUser.getMeta().get().getVersion().get().getEntityTag(),
                              response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
    });
  }

  /**
   * verifies that the authorization on the mocked server is used so that the other tests are working correctly
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

    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that the authorization on the mocked server is used so that the other tests are working correctly
   */
  @Test
  public void testWrongPassword()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(5)
                                                        .requestTimeout(5)
                                                        .socketTimeout(5)
                                                        .truststore(getTruststore())
                                                        // hostname verifier disabled for tests
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .basic(SecurityConstants.AUTHORIZED_USERNAME, "wrong-password")
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getRequestUrl(TestController.SCIM_ENDPOINT_PATH), scimClientConfig);

    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
    ServerResponse<User> response = scimRequestBuilder.create(User.class, EndpointPaths.USERS)
                                                      .setResource(user)
                                                      .sendRequest();
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
    Assertions.assertFalse(response.isSuccess());
    Assertions.assertNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
  }

  /**
   * verifies that the user with an insufficient authorization is getting rejected
   */
  @Test
  public void testForbidden()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .connectTimeout(5)
                                                        .requestTimeout(5)
                                                        .socketTimeout(5)
                                                        .truststore(getTruststore())
                                                        // hostname verifier disabled for tests
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .basic(SecurityConstants.UNAUTHORIZED_USERNAME,
                                                               SecurityConstants.PASSWORD)
                                                        .build();
    scimRequestBuilder = new ScimRequestBuilder(getRequestUrl(TestController.SCIM_ENDPOINT_PATH), scimClientConfig);

    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
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
   * 
   */
  @Test
  public void testFailedResponse()
  {
    final String badResponse = readResourceFile(BROKEN_LISTRESPONSE);

    TestController.responseSupplier = () -> badResponse;
    ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(User.class, EndpointPaths.USERS)
                                                                    .get()
                                                                    .sendRequest();
    Assertions.assertFalse(response.isSuccess());
  }
}
