package de.gold.scim.server.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.common.constants.HttpStatus;
import de.gold.scim.common.constants.ScimType;
import de.gold.scim.common.exceptions.ScimException;
import de.gold.scim.common.resources.base.ScimArrayNode;
import de.gold.scim.common.schemas.SchemaAttribute;
import de.gold.scim.server.schemas.ResourceType;
import de.gold.scim.server.utils.RequestUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 09:10 <br>
 * <br>
 * abstract class that provides basic methods for patching
 */
public abstract class AbstractPatch
{

  /**
   * this resource type is used to get the attribute definitions of the values from the patch operations
   */
  protected final ResourceType resourceType;

  public AbstractPatch(ResourceType resourceType)
  {
    this.resourceType = resourceType;
  }

  /**
   * tries to resolve that attribute path and gets the schema of the attribute
   *
   * @param key the attribute name of which the definition should be extracted
   * @return the schema attribute or an exception if the definition does not exist for the resourceType
   */
  protected SchemaAttribute getSchemaAttribute(String key)
  {
    try
    {
      return RequestUtils.getSchemaAttributeByAttributeName(resourceType, key);
    }
    catch (ScimException ex)
    {
      ex.setScimType(ScimType.RFC7644.INVALID_PATH);
      ex.setStatus(HttpStatus.BAD_REQUEST);
      throw ex;
    }
  }

  /**
   * gets or creates a new attribute from or into the given parent node based on the given attribute name
   *
   * @param parentNode the parent node that might already hold the specified node or will get a node in the type
   *          of the attributes definition
   * @param attributeName the attribute name
   * @return the child object from the parentNode
   */
  protected JsonNode getAttributeFromObject(JsonNode parentNode, String attributeName)
  {
    SchemaAttribute schemaAttribute = getSchemaAttribute(attributeName);
    JsonNode child = parentNode.get(schemaAttribute.getName());
    if (child == null)
    {
      if (schemaAttribute.isMultiValued())
      {
        child = createNewMultiValuedNode(parentNode, schemaAttribute);
      }
    }
    return child;
  }

  /**
   * creates a new multi valued or multi valued complex attribute and returns the inner deepest created node
   *
   * @param parentNode the parent node to which the nodes will be added
   * @param schemaAttribute the attribute definition
   * @return the new created node
   */
  private JsonNode createNewMultiValuedNode(JsonNode parentNode, SchemaAttribute schemaAttribute)
  {

    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    if (parentNode.isArray())
    {
      ((ArrayNode)parentNode).add(scimArrayNode);
    }
    else
    {
      ((ObjectNode)parentNode).set(schemaAttribute.getName(), scimArrayNode);
    }
    return scimArrayNode;
  }
}
