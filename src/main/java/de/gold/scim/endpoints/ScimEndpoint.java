package de.gold.scim.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.gold.scim.response.ScimResponse;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.SchemaValidator;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:28 <br>
 * <br>
 */
@Slf4j
public final class ScimEndpoint
{

  private List<ResourceType> resourceType;

  public ScimEndpoint(ResourceType resourceType, ResourceType... resourceTypeArray)
  {
    this.resourceType = new ArrayList<>();
    this.resourceType.add(resourceType);
    if (resourceTypeArray != null)
    {
      this.resourceType.addAll(Arrays.asList(resourceTypeArray));
    }
  }

  public ScimResponse createResource(String resourceDocument)
  {

    SchemaValidator.validateSchemaForRequest(null,
                                             JsonHelper.readJsonDocument(resourceDocument),
                                             SchemaValidator.HttpMethod.POST);

    SchemaValidator.validateSchemaForResponse(null, JsonHelper.readJsonDocument(resourceDocument));
    return null;
  }

  public ScimResponse getResource(String id)
  {
    return null;
  }

  public ScimResponse updateResource(String id, String resourceDocument)
  {
    return null;
  }

  public ScimResponse deleteResource(String id)
  {
    return null;
  }
}
