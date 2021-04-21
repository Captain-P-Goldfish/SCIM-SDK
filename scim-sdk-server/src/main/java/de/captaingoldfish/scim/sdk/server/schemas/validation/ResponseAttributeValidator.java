package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 19.04.2021
 */
@Slf4j
class ResponseAttributeValidator
{

  /**
   * validates a schema attribute in the context of a server-response. There are a lot of things that must be
   * checked:<br>
   * An attribute definition looks like this:<br>
   * 
   * <pre>
   *  {
   *    "name": "myAttribute",
   *     "type": "string",
   *     "description": "my attribute description.",
   *     "mutability": "readWrite",
   *     "returned": "default",
   *     "uniqueness": "none",
   *     "multiValued": false,
   *     "required": false,
   *     "caseExact": false
   *   }
   * </pre>
   * 
   * The important values that must be validated specifically in a response context are: <br>
   * 
   * <pre>
   *   {
   *     "mutability": "readWrite",
   *     "returned": "default"
   *     "required": false
   *   }
   * </pre>
   *
   * Additionally the <b>attributes</b> or <b>excludedAttributes</b> parameter has an effect of some of the
   * values of the <b>"returned"</b> attribute.<br>
   * <br>
   * Now we will list the different value combinations and what they will mean as a result for this contexts
   * validation:
   * <ol>
   * <li>
   * 
   * <pre>
   *      {
   *        "mutability": "writeOnly",
   *        "returned": "never"
   *      }
   *   
   *      if an attribute has either one these values the attribute must not be returned to the client even if the
   *      attribute is required. These two values must always stand together but the evaluation for this is not done 
   *      here.
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *      {
   *        "required": true
   *      }
   *   
   *      <b>attribute is null:</b><br>
   *         an internal exception will be thrown<br>
   *      <b>attribute is not null:</b><br>
   *         everything is okay
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     {
   *       "returned": "always"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is returned
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"attributes"</b>-parameter is empty:
   *     {
   *       "returned": "default"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is returned
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"attributes"</b>-parameter is not empty and contains the name of the current attribute:
   *     {
   *       "returned": "default"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is returned
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"attributes"</b>-parameter is not empty and does not contain the name of the current attribute:
   *     {
   *       "returned": "default"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is ignored
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"attributes"</b>-parameter is empty:
   *     {
   *       "returned": "request"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is ignored
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"attributes"</b>-parameter is not empty and contains the name of the current attribute:
   *     {
   *       "returned": "request"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is returned
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"attributes"</b>-parameter is not empty and does not contain the name of the current attribute:
   *     {
   *       "returned": "request"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is ignored
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"excludedAttributes"</b>-parameter is not empty and contains the name of the current attribute:
   *     {
   *       "returned": "default"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is ignored
   * </pre>
   * 
   * </li>
   * <li>
   * 
   * <pre>
   *     <b>"excludedAttributes"</b>-parameter is not empty and contains the name of the current attribute:
   *     {
   *       "returned": "request"
   *     }
   *     <b>attribute is null:</b><br>
   *        attribute is ignored<br>
   *     <b>attribute is not null:</b><br>
   *        attribute is ignored
   * </pre>
   * 
   * </li>
   * </ol>
   *
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to validate
   * @return the validated json node or an empty if the attribute is not present or should be ignored
   * @throws AttributeValidationException if the client has send an invalid attribute that does not match its
   *           definition
   */
  public static Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute,
                                                     JsonNode attribute,
                                                     List<SchemaAttribute> attributesList,
                                                     List<SchemaAttribute> excludedAttributesList)
  {
    ContextValidator requestContextValidator = getContextValidator(attributesList, excludedAttributesList);
    Optional<JsonNode> validatedNode = ValidationSelector.validateNode(schemaAttribute,
                                                                       attribute,
                                                                       requestContextValidator);
    // checking once more for required is necessary for complex attributes and multivalued complex attributes
    // that have been evaluated to an empty.
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      try
      {
        validateRequiredAttribute(schemaAttribute, !validatedNode.isPresent());
      }
      catch (AttributeValidationException ex)
      {
        String errorMessage = String.format("The required attribute '%s' was evaluated to an empty during "
                                            + "schema validation but the attribute is required '%s'",
                                            schemaAttribute.getFullResourceName(),
                                            attribute);
        throw new AttributeValidationException(schemaAttribute, errorMessage, ex);
      }
    }
    return validatedNode;
  }

  /**
   * the validation that checks if an attribute must be removed from the response document
   *
   * @param attributesList the list of attributes within the "attributes"-parameter
   * @param excludedAttributesList the list of attributes within the "excludedAttributes"-parameter
   * @return the context validation for responses
   */
  private static ContextValidator getContextValidator(List<SchemaAttribute> attributesList,
                                                      List<SchemaAttribute> excludedAttributesList)
  {
    return (schemaAttribute, attribute) -> {

      // read only attributes are not accepted on request so we will simply ignore this attribute
      if (Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability())
          || Returned.NEVER.equals(schemaAttribute.getReturned()))
      {
        return false;
      }
      final boolean isNodeNull = attribute == null || attribute.isNull();
      validateRequiredAttribute(schemaAttribute, isNodeNull);

      if (isNodeNull)
      {
        return false;
      }

      if (Returned.ALWAYS.equals(schemaAttribute.getReturned()))
      {
        return true;
      }

      // the following two booleans are mutually exclusive meaning that only one of these boolean can evaluate to
      // true. Both can be false but the previous evaluation must ensure that only one list is present.
      final boolean useAttributes = attributesList != null && !attributesList.isEmpty();
      final boolean useExcludedAttributes = excludedAttributesList != null && !excludedAttributesList.isEmpty();

      if (!useAttributes && !useExcludedAttributes)
      {
        final boolean removeAttribute = Returned.REQUEST.equals(schemaAttribute.getReturned());
        if (removeAttribute)
        {
          log.trace("Removing attribute '{}' from response. Returned value is '{}' and it was not present in the request",
                    schemaAttribute.getFullResourceName(),
                    Returned.REQUEST);
        }
        return !removeAttribute;
      }

      final boolean removeRequestOrDefaultAttribute = useAttributes
                                                      && !isAttributePresentInList(schemaAttribute, attributesList);
      if (removeRequestOrDefaultAttribute)
      {
        log.trace("Removing attribute '{}' from response for its returned value is '{}' and its name is not in the list"
                  + " of requested attributes",
                  schemaAttribute.getFullResourceName(),
                  schemaAttribute.getReturned());
      }
      final boolean excludeAttribute = useExcludedAttributes
                                       && isAttributePresentInList(schemaAttribute, excludedAttributesList);
      if (excludeAttribute)
      {
        log.trace("Removing attribute '{}' from response for it was excluded by the 'excludedAttributes'-parameter",
                  schemaAttribute.getFullResourceName());
      }
      return !removeRequestOrDefaultAttribute && !excludeAttribute;
    };
  }

  /**
   * validates if the attribute is required and present
   *
   * @param schemaAttribute the attributes definition
   * @param isNodeNull if the attribute is null or not
   */
  private static void validateRequiredAttribute(SchemaAttribute schemaAttribute, boolean isNodeNull)
  {
    if (!schemaAttribute.isRequired())
    {
      return;
    }
    if (isNodeNull && !Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability())
        && !Returned.NEVER.equals(schemaAttribute.getReturned()))
    {
      String errorMessage = String.format("Required '%s' attribute '%s' is missing",
                                          schemaAttribute.getMutability(),
                                          schemaAttribute.getFullResourceName());
      throw new AttributeValidationException(schemaAttribute, errorMessage);
    }
  }

  /**
   * checks if the given schema attribute definition is present within the attributes list
   *
   * @param schemaAttribute the attribute to check for presence in the attributes list
   * @param attributes the attribute list that might also be the excludedAttributes list
   * @return true if the given attribute is present within the list, false else
   */
  private static boolean isAttributePresentInList(SchemaAttribute schemaAttribute, List<SchemaAttribute> attributes)
  {
    return attributes.stream()
                     .map(SchemaAttribute::getFullResourceName)
                     .anyMatch(param -> StringUtils.equals(schemaAttribute.getFullResourceName(), param));
  }
}
