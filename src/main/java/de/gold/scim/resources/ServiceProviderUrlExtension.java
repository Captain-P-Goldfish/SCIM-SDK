package de.gold.scim.resources;

import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.exceptions.InvalidConfigException;
import de.gold.scim.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 13:07 <br>
 * <br>
 * represents an extension to the {@link ServiceProvider} that can be used to add a global base url that
 * should be used to access the resources
 */
public class ServiceProviderUrlExtension extends ScimObjectNode
{

  @Builder
  private ServiceProviderUrlExtension(String baseUrl)
  {
    super(null);
    setBaseUrl(baseUrl);
  }

  /**
   * a base url that may be used as a fallback if no other ways to build the location urls is provided
   */
  public String getBaseUrl()
  {
    return getStringAttribute(AttributeNames.Custom.BASE_URL).orElseThrow(() -> {
      return new InvalidConfigException("The base URL is not present");
    });
  }

  /**
   * a base url that may be used as a fallback if no other ways to build the location urls is provided
   */
  public void setBaseUrl(String baseUrl)
  {
    setAttribute(AttributeNames.Custom.BASE_URL, Optional.ofNullable(baseUrl).orElseThrow(() -> {
      return new InvalidConfigException("The base URL must not be empty");
    }));
  }
}
