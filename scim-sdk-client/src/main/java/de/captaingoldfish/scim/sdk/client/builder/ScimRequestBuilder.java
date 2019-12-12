package de.captaingoldfish.scim.sdk.client.builder;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;


/**
 * author Pascal Knueppel <br>
 * created at: 07.12.2019 - 23:08 <br>
 * <br>
 * this class can be used to build any type of request for SCIM
 */
public class ScimRequestBuilder
{

  /**
   * must contain the baseUrl to the scim service
   */
  private final String baseUrl;

  /**
   * the configuration for the client that should be used
   */
  private final ScimClientConfig scimClientConfig;

  public ScimRequestBuilder(String baseUrl, ScimClientConfig scimClientConfig)
  {
    this.baseUrl = baseUrl.replaceFirst("/$", "");
    this.scimClientConfig = scimClientConfig;
  }

  /**
   * builds a create builder class based on the given type
   * 
   * @param type the type that should be created
   * @return a request builder for the given resource type
   */
  public <T extends ResourceNode> CreateBuilder<T> create(Class<T> type)
  {
    return new CreateBuilder<>(baseUrl, scimClientConfig, type);
  }

}
