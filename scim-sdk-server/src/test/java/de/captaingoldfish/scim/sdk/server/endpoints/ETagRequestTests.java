package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;


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
    userHandler = Mockito.spy(new UserCustomHandler(false));
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(userHandler);

    ServiceProvider serviceProvider = ServiceProvider.builder().build();
    this.resourceEndpointHandler = new ResourceEndpointHandler(serviceProvider, userEndpoint);
  }

  private Context getContext(String id, HttpMethod httpMethod)
  {
    Context context = new Context(null);
    context.setResourceReferenceUrl(s -> getBaseUrlSupplier().get() + "/Users/" + s);
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    context.setUriInfos(UriInfos.getRequestUrlInfos(resourceEndpointHandler.getResourceTypeFactory(),
                                                    getBaseUrlSupplier().get() + "/Users" + Optional.ofNullable(id)
                                                                                                    .map(s -> "/" + s)
                                                                                                    .orElse(""),
                                                    httpMethod,
                                                    httpHeaders,
                                                    false));
    return context;
  }

  /**
   * will verify that ETags are set correctly if not set manually
   */
  @Test
  public void testETagIsSetCorrectly()
  {
    resourceEndpointHandler.getServiceProvider().getPatchConfig().setSupported(true);
    resourceEndpointHandler.getServiceProvider().getETagConfig().setSupported(true);
    ResourceType userResourceType = resourceEndpointHandler.getResourceTypeByName(ResourceTypeNames.USER).get();
    userResourceType.getFeatures().getETagFeature().setEnabled(true);
    User createdUser;
    {
      User user = User.builder().userName("goldfish").nickName("goldfish").build();
      ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                         user.toString(),
                                                                         getBaseUrlSupplier(),
                                                                         getContext(null, HttpMethod.POST));
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      createdUser = JsonHelper.copyResourceToObject(scimResponse, User.class);
      Assertions.assertTrue(createdUser.getMeta().isPresent());
      Assertions.assertTrue(createdUser.getMeta().get().getVersion().isPresent());
      Assertions.assertTrue(createdUser.getMeta().get().getVersion().get().isWeak());
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.USERS,
                                                                      createdUser.getId().get(),
                                                                      null,
                                                                      null,
                                                                      getBaseUrlSupplier(),
                                                                      getContext(createdUser.getId().get(),
                                                                                 HttpMethod.GET));
      User retrievedUser = JsonHelper.copyResourceToObject(scimResponse, User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                        SearchRequest.builder().build(),
                                                                        getBaseUrlSupplier(),
                                                                        getContext(".search", HttpMethod.POST));
      ListResponse listResponse = JsonHelper.copyResourceToObject(scimResponse, ListResponse.class);
      User retrievedUser = JsonHelper.copyResourceToObject((JsonNode)listResponse.getListedResources().get(0),
                                                           User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }

    {
      ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         createdUser.toString(),
                                                                         getBaseUrlSupplier(),
                                                                         getContext(createdUser.getId().get(),
                                                                                    HttpMethod.PUT));
      User retrievedUser = JsonHelper.copyResourceToObject(scimResponse, User.class);
      Assertions.assertEquals(createdUser, retrievedUser);
    }

    {
      User user = User.builder().nickName("goldfish").build();
      List<PatchRequestOperation> operations = new ArrayList<>();
      operations.add(PatchRequestOperation.builder().op(PatchOp.REPLACE).valueNode(user).build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      ScimResponse scimResponse = resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                                        createdUser.getId().get(),
                                                                        patchOpRequest.toString(),
                                                                        null,
                                                                        null,
                                                                        getBaseUrlSupplier(),
                                                                        getContext(createdUser.getId().get(),
                                                                                   HttpMethod.PATCH));
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

  /**
   * will verify that the version attribute sent from a client will be kept
   */
  public static class UserCustomHandler extends UserHandlerImpl
  {

    public UserCustomHandler(boolean returnETags)
    {
      super(returnETags);
    }

    /**
     * will verify that the version attribute sent by the client is kept from the request and given to the update
     * method
     */
    @Override
    public User updateResource(User resource, Context context)
    {
      Assertions.assertTrue(resource.getMeta().flatMap(Meta::getVersion).isPresent());
      return super.updateResource(resource, context);
    }
  }

}
