package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimLongNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;


/**
 * @author Pascal Knueppel
 * @since 10.04.2021
 */
class ValidationSelector
{

  /**
   * decides how an attribute must be validated, meaning that it checks if the attribute is a simple attribute,
   * a simple multivalued attribute, a complex attribute or a multivalued complex attribute and lets the
   * attribute be validated by the corresponding implementation
   *
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to be validated
   * @param contextValidator the validation context must validate the attribute different by one of the
   *          following contexts: [REQUEST, RESPONSE, META_VALIDATION]
   * @return the json node if validated successfully and an empty if the attribute should be ignored
   * @throws AttributeValidationException if the attribute does not match its definition
   */
  public static Optional<JsonNode> validateNode(SchemaAttribute schemaAttribute,
                                                JsonNode attribute,
                                                ContextValidator contextValidator)
  {
    final JsonNode effectiveAttribute = contextValidator.handleDefaultValue(schemaAttribute, attribute);
    final boolean isContextValidation = contextValidator.validateContext(schemaAttribute, effectiveAttribute);
    if (!isContextValidation || effectiveAttribute == null)
    {
      return Optional.empty();
    }

    boolean isAnyNode = Type.ANY.equals(schemaAttribute.getType());
    if (isAnyNode)
    {
      return Optional.of(handleAnyAttribute(schemaAttribute, effectiveAttribute));
    }

    boolean isComplexType = Type.COMPLEX.equals(schemaAttribute.getType());
    if (schemaAttribute.isMultiValued())
    {
      if (isComplexType)
      {
        ArrayNode validatedAttribute = MultivaluedComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                                     effectiveAttribute,
                                                                                                     contextValidator);
        CustomAttributeValidator.validateArrayNode(schemaAttribute, validatedAttribute);
        return Optional.ofNullable(validatedAttribute);
      }
      else
      {
        ArrayNode validatedAttribute = SimpleMultivaluedAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                                    effectiveAttribute);
        CustomAttributeValidator.validateArrayNode(schemaAttribute, validatedAttribute);
        return Optional.ofNullable(validatedAttribute);
      }
    }
    else
    {
      if (isComplexType)
      {
        JsonNode validatedAttribute = ComplexAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                         effectiveAttribute,
                                                                                         contextValidator);
        return Optional.ofNullable(validatedAttribute);
      }
      else
      {
        JsonNode validatedAttribute = SimpleAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                                        effectiveAttribute);
        CustomAttributeValidator.validateSimpleNode(schemaAttribute, validatedAttribute);
        return Optional.of(validatedAttribute);
      }
    }
  }

  /**
   * handles an any attribute. Since we cannot forecast what type of attribute we will get we will simply accept
   * anything. So any nodes must define canonicalTypes or specific sub-attributes
   *
   * @param schemaAttribute the attribute definition of the any node
   * @param attribute the attribute to validate
   * @return the attribute or an empty
   */
  private static JsonNode handleAnyAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    if (schemaAttribute.isMultiValued() && !attribute.isArray())
    {
      ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
      scimArrayNode.add(attribute);
      return scimArrayNode;
    }

    if (attribute.isArray() || attribute.isObject())
    {
      return attribute;
    }

    return getSimpleAnyAttribute(schemaAttribute, attribute);
  }

  /**
   * converts simple any attributes in the corresponding attribute representation. This is not done for array or
   * object type attribute
   *
   * @param schemaAttribute the any types attribute definition
   * @param attribute the simple attribute
   * @return the {@link de.captaingoldfish.scim.sdk.common.resources.base.ScimNode} representation of the
   *         attribute
   */
  private static JsonNode getSimpleAnyAttribute(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    if (attribute.isTextual())
    {
      return new ScimTextNode(schemaAttribute, attribute.textValue());
    }
    if (attribute.isBoolean())
    {
      return new ScimBooleanNode(schemaAttribute, attribute.booleanValue());
    }
    if (attribute.isDouble() || attribute.isFloat())
    {
      return new ScimDoubleNode(schemaAttribute, attribute.doubleValue());
    }
    if (attribute.isNumber() || attribute.isLong())
    {
      return new ScimLongNode(schemaAttribute, attribute.longValue());
    }
    return NullNode.getInstance();
  }


}
