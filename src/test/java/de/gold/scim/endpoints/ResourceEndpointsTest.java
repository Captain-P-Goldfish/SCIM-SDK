package de.gold.scim.endpoints;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.response.ScimResponse;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:54 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointsTest implements FileReferences
{

  private ResourceEndpoints resourceEndpoints;

  @BeforeEach
  public void initialize()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    EndpointDefinition userEndpoint = new EndpointDefinition(userResourceType, userResourceSchema,
                                                             Collections.singletonList(enterpriseUserExtension),
                                                             new TestResourceHandlerImpl());
    resourceEndpoints = new ResourceEndpoints(userEndpoint);
  }

  @Test
  public void testCreateResource()
  {
    ScimResponse scimResponse = resourceEndpoints.createResource("/Users", readResourceFile(USER_RESOURCE));
    Assertions.fail("validate the response");
  }

  private static class TestResourceHandlerImpl implements ResourceHandler<JsonNode>
  {

    @Override
    public JsonNode createResource(JsonNode resource)
    {
      JsonHelper.addAttribute(resource, AttributeNames.ID, new TextNode(UUID.randomUUID().toString()));
      return resource;
    }

    @Override
    public JsonNode readResource(String id)
    {
      return null;
    }

    @Override
    public JsonNode listResources()
    {
      return null;
    }

    @Override
    public JsonNode updateResource(JsonNode resource, String id)
    {
      return resource;
    }

    @Override
    public JsonNode deleteResource(String id)
    {
      return null;
    }
  }
}
