package de.captaingoldfish.scim.sdk.client;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.builder.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.constants.ResponseType;
import de.captaingoldfish.scim.sdk.client.response.ScimServerResponse;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SecurityConstants;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
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
public class ScimRequestBuilderBasicSpringbootTest extends AbstractSpringBootWebTest
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
  @Test
  public void testBuildCreateRequest()
  {
    User user = User.builder().userName("goldfish").name(Name.builder().givenName("goldfish").build()).build();
    ScimServerResponse<User> response = scimRequestBuilder.create(User.class)
                                                          .setEndpoint(EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertEquals(CreateResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(ResponseType.CREATE, response.getResponseType());
    Assertions.assertEquals(HttpStatus.CREATED, response.getHttpStatus());
    Assertions.assertNotNull(response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));

    Assertions.assertTrue(response.getResource().isPresent());
    User returnedUser = response.getResource().get();
    Assertions.assertEquals("goldfish", returnedUser.getUserName().get());
    Assertions.assertEquals(returnedUser.getMeta().get().getVersion().get().getEntityTag(),
                            response.getHttpHeaders().get(HttpHeader.E_TAG_HEADER));
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
    ScimServerResponse<User> response = scimRequestBuilder.create(User.class)
                                                          .setEndpoint(EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertFalse(response.getScimResponse().isPresent());
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
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
    ScimServerResponse<User> response = scimRequestBuilder.create(User.class)
                                                          .setEndpoint(EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertEquals(ErrorResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getHttpStatus());
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
    ScimServerResponse<User> response = scimRequestBuilder.create(User.class)
                                                          .setEndpoint(EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertEquals(ErrorResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getHttpStatus());
    Assertions.assertEquals("you are not authorized to access the 'CREATE' endpoint on resource type 'User'",
                            response.getErrorResponse().get().getDetail());
  }

  /**
   * verifies that an error response is correctly parsed
   */
  @Test
  public void testBuildCreateRequestWithErrorResponse()
  {
    User user = User.builder().userName("goldfish").build();
    user.setSchemas(Collections.singleton(SchemaUris.GROUP_URI)); // this will cause an error for wrong schema uri

    ScimServerResponse<User> response = scimRequestBuilder.create(User.class)
                                                          .setEndpoint(EndpointPaths.USERS)
                                                          .setResource(user)
                                                          .sendRequest();
    Assertions.assertEquals(ErrorResponse.class, response.getScimResponse().get().getClass());
    Assertions.assertEquals(ResponseType.ERROR, response.getResponseType());
    Assertions.assertTrue(response.getErrorResponse().isPresent());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    Assertions.assertEquals("main resource schema 'urn:ietf:params:scim:schemas:core:2.0:User' is not present in "
                            + "resource. Main schema is: urn:ietf:params:scim:schemas:core:2.0:User",
                            response.getErrorResponse().get().getDetail());
    Assertions.assertFalse(response.getResource().isPresent());
  }

}
