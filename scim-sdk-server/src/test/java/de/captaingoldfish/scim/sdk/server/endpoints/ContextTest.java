package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;


/**
 * @author Pascal Knueppel
 * @since 20.06.2021
 */
public class ContextTest
{

  /**
   * a simple basic uri used in these tests
   */
  private static final String BASE_URI = "https://localhost/scim/v2";

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

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
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
  }

  /**
   * This test will verify that the {@link Context#getResourceReferenceUrl(String)} and
   * {@link Context#getCrossResourceReferenceUrl(String, String)} will return the correct results.
   */
  @Test
  public void testGetResourceReferenceUrlOnGetOfUserHandlerImpl()
  {
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<Context> contextVerifier = context -> {
      String otherId = UUID.randomUUID().toString();
      Assertions.assertEquals(String.format("%s%s/%s", BASE_URI, EndpointPaths.USERS, otherId),
                              context.getResourceReferenceUrl(otherId));
      Assertions.assertEquals(String.format("%s%s/%s", BASE_URI, EndpointPaths.GROUPS, otherId),
                              context.getCrossResourceReferenceUrl("Group", otherId).get());
      wasCalled.set(true);
    };

    UserHandlerImpl userHandler = new UserHandlerImpl(contextVerifier);
    ResourceEndpoint resourceEndpoint = new ResourceEndpoint(serviceProvider);
    resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));
    resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(new GroupHandlerImpl()));

    final String id = UUID.randomUUID().toString();
    userHandler.getInMemoryMap().put(id, User.builder().id(id).userName("goldfish").build());

    final String url = String.format("%s%s/%s", BASE_URI, EndpointPaths.USERS, id);
    resourceEndpoint.handleRequest(url, HttpMethod.GET, null, httpHeaders, new Context(null));

    Assertions.assertTrue(wasCalled.get());
  }

  /*
   * I am not testing the other endpoints here because this is already reliably tested by other existing tests
   */
}
