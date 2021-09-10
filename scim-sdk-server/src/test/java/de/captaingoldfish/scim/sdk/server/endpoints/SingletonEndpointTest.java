package de.captaingoldfish.scim.sdk.server.endpoints;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserSingletonHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import lombok.extern.slf4j.Slf4j;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knüppel
 */
@Slf4j
public class SingletonEndpointTest
{

  private static final String BASE_URI = "https://localhost/scim/v2";

  /**
   * the resource endpoint under test
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * used to configure the resource endpoint
   */
  private ResourceType userResourceType;

  /**
   * a mockito spy to verify the class that have been made on this instance
   */
  private UserHandlerImpl userHandler;

  /**
   * the http header map that is validated on a request
   */
  private Map<String, String> httpHeaders = new HashMap<>();

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().build();
    userHandler = Mockito.spy(new UserSingletonHandlerImpl());
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    userResourceType = resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));
    // this is the basic configuration on this test
    userResourceType.getFeatures().setSingletonEndpoint(true);
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    User user = User.builder()
                    .id("1")
                    .userName("goldfish")
                    .meta(Meta.builder().created(Instant.now()).lastModified(Instant.now()).build())
                    .build();
    userHandler.getInMemoryMap().put(user.getId().get(), user);
  }

  /**
   * this test will create 500 users and will then send a filter request with get that verifies that the request
   * is correctly processed
   */
  @Test
  public void testGetSingletonUser()
  {
    final String url = BASE_URI + EndpointPaths.USERS;
    ScimResponse scimResponse = resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;

    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    Mockito.verify(userHandler, Mockito.times(0)).createResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(1))
           .getResource(Mockito.isNull(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.notNull());
    Mockito.verify(userHandler, Mockito.times(0)).updateResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0)).deleteResource(Mockito.any(), Mockito.any());
    Mockito.verify(userHandler, Mockito.times(0))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any());
  }
}
