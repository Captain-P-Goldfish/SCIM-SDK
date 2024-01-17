package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 12.04.2021
 */
@Slf4j
class MultivaluedComplexAttributeValidator
{

  /**
   * validates a multivalued complex attribute by validating all complex attributes within its array
   *
   * @param schemaAttribute the multivalued complex attributes definition
   * @param attribute the multivalued complex attribute
   * @param contextValidator the context validation that must also be executed for any element of the attribute
   *          array
   * @return the validated attribute or null if the validated attribute has no children
   * @throws AttributeValidationException if the multivalued complex attribute or one of its elements do not
   *           match its attribute definition
   */
  public static ArrayNode parseNodeTypeAndValidate(SchemaAttribute schemaAttribute,
                                                   JsonNode attribute,
                                                   ContextValidator contextValidator)
  {
    log.trace("Validating multivalued complex attribute '{}'", schemaAttribute.getScimNodeName());
    if (attribute == null || attribute.isNull())
    {
      return null;
    }
    ArrayNode arrayNode = toArrayNode(attribute).orElseThrow(() -> {
      String errorMessage = String.format("Attribute '%s' is expected to be an array but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          attribute);
      return new AttributeValidationException(schemaAttribute, errorMessage);
    });

    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    if (arrayNode.size() > 0)
    {
      List<JsonNode> uniqueValueList = new ArrayList<>();
      boolean primaryValueFound = false;
      for ( JsonNode jsonNode : arrayNode )
      {
        jsonNode = checkIsObject(jsonNode, schemaAttribute, attribute);
        primaryValueFound = checkForDuplicatePrimary(primaryValueFound, jsonNode, schemaAttribute, attribute);
        checkForUniqueness(uniqueValueList, jsonNode, schemaAttribute, attribute);

        try
        {
          JsonNode parsedComplexNode = ComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                          jsonNode,
                                                                                          contextValidator);
          if (parsedComplexNode != null)
          {
            scimArrayNode.add(parsedComplexNode);
          }
        }
        catch (AttributeValidationException ex)
        {
          String errorMessage = String.format("Found unsupported value in multivalued complex attribute '%s'",
                                              arrayNode);
          throw new AttributeValidationException(schemaAttribute, errorMessage, ex);
        }
      }
    }
    return scimArrayNode.isEmpty() ? null : scimArrayNode;
  }

  /**
   * verifies that no duplicate values are present within the multivalued complex type if the uniqueness has
   * another values than NONE
   *
   * @param uniqueValueList the list of elements within the multi valued complex type over which the loop has
   *          iterated so far
   * @param jsonNode the current complex attribute that is checked for being a duplicate of another element
   * @param schemaAttribute the multi valued complex types schema definition
   * @param multivaluedComplexParent the multivalued complex attribute (only used for exception)
   * @throws AttributeValidationException if the multivalued complex type defines uniqueness and a duplicate
   *           value is discovered
   */
  private static void checkForUniqueness(List<JsonNode> uniqueValueList,
                                         JsonNode jsonNode,
                                         SchemaAttribute schemaAttribute,
                                         JsonNode multivaluedComplexParent)
  {
    if (!Uniqueness.NONE.equals(schemaAttribute.getUniqueness()))
    {
      if (uniqueValueList.contains(jsonNode))
      {
        String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                            schemaAttribute.getUniqueness().getValue(),
                                            multivaluedComplexParent);
        throw new AttributeValidationException(schemaAttribute, errorMessage);
      }
      uniqueValueList.add(jsonNode);
    }
  }

  /**
   * simply verifies that the given element of the multi complex parent attribute is an object
   *
   * @param multivaluedComplexParentElement the element of the multivalued complex attribute
   * @param schemaAttribute the multi valued complex attributes definition (only used for exception)
   * @param multivaluedComplexParent the multivalued complex attribute (only used for exception)
   */
  private static JsonNode checkIsObject(JsonNode multivaluedComplexParentElement,
                                        SchemaAttribute schemaAttribute,
                                        JsonNode multivaluedComplexParent)
  {
    if (!multivaluedComplexParentElement.isObject())
    {
      JsonNode parsedNode = null;
      try
      {
        parsedNode = JsonHelper.readJsonDocument(multivaluedComplexParentElement.asText());
        if (parsedNode != null && parsedNode.isObject())
        {
          return parsedNode;
        }
      }
      catch (Exception ex)
      {
        // do nothing
      }
      String errorMessage = String.format("Attribute '%s' is expected to hold only complex attributes but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          multivaluedComplexParent);
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
    return multivaluedComplexParentElement;
  }

  /**
   * checks if the current given json node is a primary value and if the parameter {@code primaryValueFound} is
   * already true an exception will be thrown
   *
   * @param primaryValueFound if there was already a primary object in the multivaluedComplexParent
   * @param jsonNode the node to check for a primary value
   * @param schemaAttribute the attributes definition (only used for exception)
   * @param multivaluedComplexParent the multi valued complex attribute (only used for exception)
   * @return true if the current given jsonNode is the primary value, false else
   * @throws AttributeValidationException if the current jsonNode is a primary value and another primary value
   *           has been found before
   */
  private static boolean checkForDuplicatePrimary(boolean primaryValueFound,
                                                  JsonNode jsonNode,
                                                  SchemaAttribute schemaAttribute,
                                                  JsonNode multivaluedComplexParent)
  {
    boolean isPrimary = hasPrimaryValue(jsonNode);
    if (isPrimary && primaryValueFound)
    {
      String errorMessage = String.format("Attribute '%s' has at least two primary values but only one primary "
                                          + "is allowed '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          multivaluedComplexParent);
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
    return isPrimary;
  }

  /**
   * gets the value of the optional primary field in the given complex node
   *
   * @param jsonNode the complex node that might hold a primary value
   * @return true if the primary value is present and set to true
   */
  private static boolean hasPrimaryValue(JsonNode jsonNode)
  {
    JsonNode primary = jsonNode.get(AttributeNames.RFC7643.PRIMARY);
    if (primary == null)
    {
      return false;
    }
    return primary.booleanValue();
  }

  /**
   * parses an incoming attribute to an array. If the attribute is a complex attribute and not of type array
   * this method will add this single complex attribute into an array and return this array
   *
   * @param attribute the attribute that should either be an array or a complex attribute
   * @return an array or an empty if the attribute was neither an array nor a complex attribute
   */
  private static Optional<ArrayNode> toArrayNode(JsonNode attribute)
  {
    if (attribute.isObject())
    {
      ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
      arrayNode.add(attribute);
      return Optional.of(arrayNode);
    }

    if (attribute.isArray())
    {
      return Optional.of((ArrayNode)attribute);
    }
    return Optional.empty();
  }

}
