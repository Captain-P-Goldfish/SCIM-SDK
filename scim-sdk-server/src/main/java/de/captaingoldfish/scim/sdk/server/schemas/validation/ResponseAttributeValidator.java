package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
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
   * @param requestDocument the request object of the client that is used to evaluate if an attribute with a
   *          returned-value of "request" or "default" should be returned if the attributes parameter is
   *          present.
   * @param attributesList the list of attributes within the "attributes"-parameter
   * @param excludedAttributesList the list of attributes within the "excludedAttributes"-parameter
   * @param referenceUrlSupplier accepts the name of a resource e.g. "User" or "Group" and additionally the
   *          resource id of the resource and it will return the fully qualified url of this resource
   * @return the validated json node or an empty if the attribute is not present or should be ignored
   * @throws AttributeValidationException if the client has send an invalid attribute that does not match its
   *           definition
   */
  public static Optional<JsonNode> validateAttribute(SchemaAttribute schemaAttribute,
                                                     JsonNode attribute,
                                                     JsonNode requestDocument,
                                                     List<SchemaAttribute> attributesList,
                                                     List<SchemaAttribute> excludedAttributesList,
                                                     BiFunction<String, String, String> referenceUrlSupplier)
  {
    ContextValidator requestContextValidator = getContextValidator(attributesList,
                                                                   requestDocument,
                                                                   excludedAttributesList,
                                                                   referenceUrlSupplier);
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
   * @param requestDocument the request object of the client that is used to evaluate if an attribute with a
   *          returned-value of "request" or "default" should be returned if the attributes parameter is
   *          present.
   * @param excludedAttributesList the list of attributes within the "excludedAttributes"-parameter
   * @param referenceUrlSupplier accepts the name of a resource e.g. "User" or "Group" and additionally the
   *          resource id of the resource and it will return the fully qualified url of this resource
   * @return the context validation for responses
   */
  private static ContextValidator getContextValidator(List<SchemaAttribute> attributesList,
                                                      JsonNode requestDocument,
                                                      List<SchemaAttribute> excludedAttributesList,
                                                      BiFunction<String, String, String> referenceUrlSupplier)
  {
    return (schemaAttribute, attribute) -> {
      final boolean validateNode = validateNode(schemaAttribute,
                                                attribute,
                                                requestDocument,
                                                attributesList,
                                                excludedAttributesList);
      if (validateNode && Type.COMPLEX.equals(schemaAttribute.getType()))
      {
        overrideEmptyReferenceNode(schemaAttribute, attribute, referenceUrlSupplier);
      }
      return validateNode;
    };
  }

  /**
   * validates an attribute and will decides if the attribute should be returned to the client or not
   * 
   * @param schemaAttribute the attributes definition
   * @param attribute the attribute to validate
   * @param requestDocument the request object of the client that is used to evaluate if an attribute with a
   *          returned-value of "request" or "default" should be returned if the attributes parameter is
   *          present.
   * @param attributesList the list of attributes within the "attributes"-parameter
   * @param excludedAttributesList the list of attributes within the "excludedAttributes"-parameter
   * @return true if the validation of this attribute should proceed, false else
   */
  private static boolean validateNode(SchemaAttribute schemaAttribute,
                                      JsonNode attribute,
                                      JsonNode requestDocument,
                                      List<SchemaAttribute> attributesList,
                                      List<SchemaAttribute> excludedAttributesList)
  {
    // read only attributes are not accepted on request so we will simply ignore this attribute
    if (Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability())
        || Returned.NEVER.equals(schemaAttribute.getReturned()))
    {
      if (attribute != null && !attribute.isNull())
      {
        log.debug("Removing attribute '{}' from document due to its definition of mutability '{}' and returned '{}'",
                  schemaAttribute.getScimNodeName(),
                  schemaAttribute.getMutability(),
                  schemaAttribute.getReturned());
      }
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
      final boolean removeAttribute = Returned.REQUEST.equals(schemaAttribute.getReturned())
                                      && !isAttributePresentInRequest(schemaAttribute, requestDocument);
      if (removeAttribute)
      {
        log.debug("Removing attribute '{}' from response. Returned value is '{}' and it was not present in the clients request",
                  schemaAttribute.getFullResourceName(),
                  Returned.REQUEST);
      }
      return !removeAttribute;
    }

    final boolean removeRequestOrDefaultAttribute = useAttributes
                                                    && !isAttributePresentInList(schemaAttribute, attributesList)
                                                    && !isAttributePresentInRequest(schemaAttribute, requestDocument);
    if (removeRequestOrDefaultAttribute)
    {
      log.debug("Removing attribute '{}' from response for its returned value is '{}' and its name is not in the list"
                + " of requested attributes",
                schemaAttribute.getFullResourceName(),
                schemaAttribute.getReturned());
      return false;
    }
    final boolean excludeAttribute = useExcludedAttributes
                                     && (isExcludedAttributePresentInList(schemaAttribute, excludedAttributesList)
                                         || Returned.REQUEST.equals(schemaAttribute.getReturned()));
    if (excludeAttribute)
    {
      log.debug("Removing attribute '{}' from response for it was excluded by the 'excludedAttributes'-parameter",
                schemaAttribute.getFullResourceName());
      return false;
    }
    return true;
  }

  /**
   * validates if the currently validated attribute was present in the request-document. If so the attribute
   * must be returned by a returned value of "request" or "default"
   * 
   * @param schemaAttribute the attributes definition
   * @param requestDocument the document that holds the attributes from the request
   * @return true if the attribute is present within the request, false else
   */
  private static boolean isAttributePresentInRequest(SchemaAttribute schemaAttribute, JsonNode requestDocument)
  {
    if (requestDocument == null)
    {
      return false;
    }
    JsonNode extensionNode = Optional.ofNullable(schemaAttribute.getResourceUri())
                                     .map(requestDocument::get)
                                     .orElse(null);
    final boolean isExtensionNode = extensionNode != null;

    JsonNode document = isExtensionNode ? extensionNode : requestDocument;
    return isAttributePresentInDocument(schemaAttribute, document);
  }

  /**
   * checks if the given current validated attribute is present within the given json complex node that is
   * either the original request document or an extension node
   * 
   * @param schemaAttribute the attributes definition
   * @param document the request document or an extension node
   * @return true if the attribute is present within the given complex json node
   */
  private static boolean isAttributePresentInDocument(SchemaAttribute schemaAttribute, JsonNode document)
  {
    String[] nameParts = schemaAttribute.getScimNodeName().split("\\.");
    JsonNode currentNode = document;
    boolean isPresent;
    for ( String namePart : nameParts )
    {
      currentNode = currentNode.get(namePart);
      isPresent = currentNode != null;
      if (!isPresent)
      {
        return false;
      }
    }
    return true;
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
   * @param attributes the attributes-parameter list
   * @return true if the given attribute is present within the list, false else
   */
  private static boolean isAttributePresentInList(SchemaAttribute schemaAttribute, List<SchemaAttribute> attributes)
  {
    for ( SchemaAttribute attribute : attributes )
    {
      boolean isPresentInList = attribute.getFullResourceName().startsWith(schemaAttribute.getFullResourceName())
                                || schemaAttribute.getFullResourceName().startsWith(attribute.getFullResourceName());
      if (isPresentInList)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * checks if the given schema attribute definition is present within the excludedAttributes list
   *
   * @param schemaAttribute the attribute to check for presence in the excludedAttributes list
   * @param excludedAttributes the excludedAttributes-parameter list
   * @return true if the given attribute is present within the list, false else
   */
  private static boolean isExcludedAttributePresentInList(SchemaAttribute schemaAttribute,
                                                          List<SchemaAttribute> excludedAttributes)
  {
    for ( SchemaAttribute attribute : excludedAttributes )
    {
      boolean isPresentInList = attribute.getFullResourceName().equals(schemaAttribute.getFullResourceName());
      if (isPresentInList)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * checks if the given complex node has a resource reference set and will add the fully qualified resource url
   * into the "$ref"-attribute if the values is not already set.
   * 
   * @param schemaAttribute the complex attributes definition
   * @param attribute the complex or multivalued complex attribute that might hold a reference to a specific
   *          registered resource
   * @param referenceUrlSupplier accepts the name of a resource e.g. "User" or "Group" and additionally the
   *          resource id of the resource and it will return the fully qualified url of this resource
   */
  private static void overrideEmptyReferenceNode(SchemaAttribute schemaAttribute,
                                                 JsonNode attribute,
                                                 BiFunction<String, String, String> referenceUrlSupplier)
  {
    final boolean hasReferenceSubAttribute = schemaAttribute.getSubAttributes()
                                                            .stream()
                                                            .anyMatch(attr -> attr.getName()
                                                                                  .equals(AttributeNames.RFC7643.REF)
                                                                              && attr.getType().equals(Type.REFERENCE)
                                                                              && attr.getReferenceTypes()
                                                                                     .contains(ReferenceTypes.RESOURCE));
    if (!hasReferenceSubAttribute)
    {
      return;
    }

    if (schemaAttribute.isMultiValued() && attribute.isArray())
    {
      for ( JsonNode complexNode : attribute )
      {
        overrideEmptyReferenceNodeInComplex(schemaAttribute, (ObjectNode)complexNode, referenceUrlSupplier);
      }
    }
    else if (attribute.isObject())
    {
      overrideEmptyReferenceNodeInComplex(schemaAttribute, (ObjectNode)attribute, referenceUrlSupplier);
    }
  }

  /**
   * overrides the $ref attribute within the given complex attribute if not already set and enough information
   * is available
   *
   * @param schemaAttribute the complex attributes definition
   * @param attribute the complex or multivalued complex attribute that might hold a reference to a specific
   *          registered resource
   * @param referenceUrlSupplier accepts the name of a resource e.g. "User" or "Group" and additionally the
   *          resource id of the resource and it will return the fully qualified url of this resource
   */
  private static void overrideEmptyReferenceNodeInComplex(SchemaAttribute schemaAttribute,
                                                          ObjectNode complexAttribute,
                                                          BiFunction<String, String, String> referenceUrlSupplier)
  {
    JsonNode refNode = complexAttribute.get(AttributeNames.RFC7643.REF);
    if (refNode != null && !refNode.isNull())
    {
      // reference node is already set
      return;
    }
    Optional<SchemaAttribute> valueAttribute = schemaAttribute.getSubAttributes().stream().filter(attr -> {
      return attr.getName().equals(AttributeNames.RFC7643.VALUE);
    }).findAny();
    Optional<SchemaAttribute> typeAttribute = schemaAttribute.getSubAttributes().stream().filter(attr -> {
      return attr.getName().equals(AttributeNames.RFC7643.TYPE);
    }).findAny();

    if (!valueAttribute.isPresent() || !typeAttribute.isPresent())
    {
      return;
    }
    String resourceId = Optional.ofNullable(complexAttribute.get(valueAttribute.get().getName()))
                                .map(JsonNode::textValue)
                                .orElse(null);
    String resourceName = Optional.ofNullable(complexAttribute.get(typeAttribute.get().getName()))
                                  .map(JsonNode::textValue)
                                  .orElse(null);

    if (StringUtils.isBlank(resourceId))
    {
      return;
    }
    if (StringUtils.isBlank(resourceName))
    {
      return;
    }

    String referenceUrl = referenceUrlSupplier.apply(resourceName, resourceId);
    if (referenceUrl == null)
    {
      return;
    }

    JsonNode newReferencenode = new ScimTextNode(schemaAttribute, referenceUrl);
    complexAttribute.set(AttributeNames.RFC7643.REF, newReferencenode);
  }
}
