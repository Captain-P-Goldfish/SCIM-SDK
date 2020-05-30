package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.Collections;
import java.util.function.Supplier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;


/**
 * <br>
 * <br>
 * created at: 30.05.2020
 *
 * @author Pascal Knüppel
 */
public class ETagRequestTests
{

  /**
   * the resource endpoints implementation that will handle any request
   */
  private ResourceEndpointHandler resourceEndpointHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private UserHandlerImpl userHandler;


  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    userHandler = Mockito.spy(new UserHandlerImpl(false));
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(userHandler);

    ServiceProvider serviceProvider = ServiceProvider.builder().build();
    this.resourceEndpointHandler = new ResourceEndpointHandler(serviceProvider, userEndpoint);
  }

  /**
   * will verify that ETags are set correctly if not set manually
   */
  @Test
  public void testETagIsSetCorrectly()
  {
    resourceEndpointHandler.getServiceProvider().getETagConfig().setSupported(true);

    User createdUser;
    {
      User user = User.builder().userName("goldfish").build();
      ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                         user.toString(),
                                                                         getBaseUrlSupplier(),
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      createdUser = JsonHelper.copyResourceToObject(scimResponse, User.class);
      Assertions.assertTrue(createdUser.getMeta().isPresent());
      Assertions.assertTrue(createdUser.getMeta().get().getVersion().isPresent());
      Assertions.assertTrue(createdUser.getMeta().get().getVersion().get().isWeak());
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.USERS,
                                                                      createdUser.getId().get(),
                                                                      Collections.emptyMap(),
                                                                      getBaseUrlSupplier());
      User retrievedUser = JsonHelper.copyResourceToObject(scimResponse, User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                        SearchRequest.builder().build(),
                                                                        getBaseUrlSupplier(),
                                                                        null);
      ListResponse listResponse = JsonHelper.copyResourceToObject(scimResponse, ListResponse.class);
      User retrievedUser = JsonHelper.copyResourceToObject((JsonNode)listResponse.getListedResources().get(0),
                                                           User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                        SearchRequest.builder().build(),
                                                                        getBaseUrlSupplier(),
                                                                        null);
      ListResponse listResponse = JsonHelper.copyResourceToObject(scimResponse, ListResponse.class);
      User retrievedUser = JsonHelper.copyResourceToObject((JsonNode)listResponse.getListedResources().get(0),
                                                           User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         createdUser.toString(),
                                                                         Collections.emptyMap(),
                                                                         getBaseUrlSupplier(),
                                                                         null);
      User retrievedUser = JsonHelper.copyResourceToObject(scimResponse, User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }
  }

  /**
   * the base uri supplier that is given to the endpoint implementations
   */
  private Supplier<String> getBaseUrlSupplier()
  {
    return () -> "https://goldfish.de/scim/v2";
  }

}
