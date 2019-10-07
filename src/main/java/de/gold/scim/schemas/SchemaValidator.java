package de.gold.scim.schemas;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.ReferenceTypes;
import de.gold.scim.constants.enums.Returned;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.exceptions.IncompatibleAttributeException;
import de.gold.scim.exceptions.InternalServerErrorException;
import de.gold.scim.exceptions.InvalidDateTimeRepresentationException;
import de.gold.scim.resources.ScimArrayNode;
import de.gold.scim.resources.ScimBooleanNode;
import de.gold.scim.resources.ScimDoubleNode;
import de.gold.scim.resources.ScimIntNode;
import de.gold.scim.resources.ScimObjectNode;
import de.gold.scim.resources.ScimTextNode;
import de.gold.scim.utils.HttpStatus;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.TimeUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 06.10.2019 - 00:18 <br>
 * <br>
 */
@Slf4j
public class SchemaValidator
{

  /**
   * tells us if the current validation is about an extension or the main document. In case of an extension we
   * will not validate the "schemas"-attribute because it is not expected within an extension
   */
  private final boolean extensionSchema;
  /**
   * tells us if the schema is validated as request or as response
   *
   * @see DirectionType
   */
  private DirectionType directionType;
  /**
   * tells us which request type the user has used. This is e.g. necessary for immutable types that are valid on
   * POST requests but invalid on PUT requests
   */
  private HttpMethod httpMethod;

  private SchemaValidator(DirectionType directionType, HttpMethod httpMethod)
  {
    this.directionType = directionType;
    this.httpMethod = httpMethod;
    this.extensionSchema = false;
  }

  private SchemaValidator(DirectionType directionType, HttpMethod httpMethod, boolean extensionSchema)
  {
    this.directionType = directionType;
    this.httpMethod = httpMethod;
    this.extensionSchema = extensionSchema;
  }

  /**
   * will validate an outgoing document against its main schema and all its extensions
   *
   * @param resourceType the resource type definition of the incoming document
   * @param document the document that should be validated
   * @return the validated document that might have several nodes removed if they were unknown by the
   *         resource-schema-definitions
   */
  public static JsonNode validateDocumentForResponse(ResourceType resourceType, JsonNode document)
  {
    ResourceType.ResourceSchema resourceSchema = resourceType.getResourceSchema(document);
    JsonNode validatedMainDocument = validateDocumentForResponse(resourceSchema.getMetaSchema().toJsonNode(), document);
    for ( Schema schemaExtension : resourceSchema.getExtensions() )
    {
      Supplier<String> message = () -> "the extension '" + schemaExtension.getId() + "' is referenced in the '"
                                       + AttributeNames.SCHEMAS + "' attribute but is "
                                       + "not present within the document";
      JsonNode extension = Optional.ofNullable(document.get(schemaExtension.getId()))
                                   .orElseThrow(() -> new InternalServerErrorException(message.get(), null,
                                                                                       ScimType.MISSING_EXTENSION));
      JsonNode extensionNode = validateExtensionForResponse(schemaExtension.toJsonNode(), extension);
      JsonHelper.addAttribute(validatedMainDocument, schemaExtension.getId(), extensionNode);
    }
    return validatedMainDocument;
  }

  /**
   * This method will build an instance of schema validator will then validate the document with the given
   * schema and returns a new document that conforms to the json meta schema definition.<br>
   * the validation direction of this method is {@link DirectionType#RESPONSE}
   *
   * @param metaSchema the json meta schema definition of the document
   * @param document the document to validate
   * @return the validated document that might have been reduced of some attributes
   */
  protected static JsonNode validateDocumentForResponse(JsonNode metaSchema, JsonNode document)
  {
    SchemaValidator schemaValidator = new SchemaValidator(DirectionType.RESPONSE, null);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * This method will build an instance of schema validator will then validate the extension with the given
   * schema and returns a new document that conforms to the json meta schema definition.<br>
   * the validation direction of this method is {@link DirectionType#RESPONSE}
   *
   * @param metaSchema the json meta schema definition of the extension
   * @param document the extension to validate
   * @return the validated extension that might have been reduced of some attributes
   */
  protected static JsonNode validateExtensionForResponse(JsonNode metaSchema, JsonNode document)
  {
    SchemaValidator schemaValidator = new SchemaValidator(DirectionType.RESPONSE, null, true);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * will validate an incoming document against its main schema and all its extensions
   *
   * @param resourceType the resource type definition of the incoming document
   * @param document the document that should be validated
   * @param httpMethod the request http method that is used to validate the request-document
   * @return the validated document that might have several nodes removed if they were unknown by the
   *         resource-schema-definitions
   */
  public static JsonNode validateDocumentForRequest(ResourceType resourceType, JsonNode document, HttpMethod httpMethod)
  {
    ResourceType.ResourceSchema resourceSchema = resourceType.getResourceSchema(document);
    JsonNode validatedMainDocument = validateDocumentForRequest(resourceSchema.getMetaSchema().toJsonNode(),
                                                                document,
                                                                httpMethod);
    for ( Schema schemaExtension : resourceSchema.getExtensions() )
    {
      Supplier<String> message = () -> "the extension '" + schemaExtension.getId() + "' is referenced in the '"
                                       + AttributeNames.SCHEMAS + "' attribute but is "
                                       + "not present within the document";
      JsonNode extension = Optional.ofNullable(document.get(schemaExtension.getId()))
                                   .orElseThrow(() -> new BadRequestException(message.get(), null,
                                                                              ScimType.MISSING_EXTENSION));
      JsonNode extensionNode = validateExtensionForRequest(schemaExtension.toJsonNode(), extension, httpMethod);
      JsonHelper.addAttribute(validatedMainDocument, schemaExtension.getId(), extensionNode);
    }
    return validatedMainDocument;
  }

  /**
   * This method will build an instance of schema validator will then validate the document with the given
   * schema and returns a new document that conforms to the json meta schema definition <br>
   * the validation direction of this method is {@link DirectionType#REQUEST}
   *
   * @param metaSchema the json meta schema definition of the document
   * @param document the document to validate
   * @param httpMethod tells us which request type the client has used. This is e.g. necessary for immutable
   *          types that are valid on POST requests but invalid on PUT requests
   * @return the validated document that might have been reduced of some attributes
   */
  protected static JsonNode validateDocumentForRequest(JsonNode metaSchema, JsonNode document, HttpMethod httpMethod)
  {
    SchemaValidator schemaValidator = new SchemaValidator(DirectionType.REQUEST, httpMethod);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * This method will build an instance of schema validator will then validate the given extension with the given
   * schema and returns a new document that conforms to the json meta schema definition <br>
   * the validation direction of this method is {@link DirectionType#REQUEST}
   *
   * @param metaSchema the json meta schema definition of the document
   * @param document the extension to validate
   * @param httpMethod tells us which request type the client has used. This is e.g. necessary for immutable
   *          types that are valid on POST requests but invalid on PUT requests
   * @return the validated extension that might have been reduced of some attributes
   */
  protected static JsonNode validateExtensionForRequest(JsonNode metaSchema, JsonNode document, HttpMethod httpMethod)
  {
    SchemaValidator schemaValidator = new SchemaValidator(DirectionType.REQUEST, httpMethod, true);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * this method will validate the given document against the given meta schema and check if the document is
   * valid
   *
   * @param metaSchema the document description
   * @param document the document that should be built after the rules of the metaSchema
   * @throws DocumentValidationException if the schema validation failed
   */
  private JsonNode validateDocument(JsonNode metaSchema, JsonNode document) throws DocumentValidationException
  {
    log.trace("validating metaSchema vs document");
    if (!extensionSchema)
    {
      checkDocumentAndMetaSchemaRelationship(metaSchema, document);
    }
    JsonNode attributes = getAttributes(metaSchema);
    try
    {
      List<SchemaAttribute> schemaAttributeList = new ArrayList<>();
      for ( JsonNode metaAttribute : attributes )
      {
        schemaAttributeList.add(new SchemaAttribute(metaAttribute));
      }
      return validateAttributes(schemaAttributeList, document, null);
    }
    catch (IncompatibleAttributeException ex)
    {
      throw DocumentValidationException.builder()
                                       .status(directionType.getHttpStatus())
                                       .message(ex.getMessage())
                                       .cause(ex)
                                       .build();
    }
  }

  private JsonNode validateAttributes(List<SchemaAttribute> metaAttributes,
                                      JsonNode document,
                                      SchemaAttribute parentAttribute)
  {
    JsonNode scimNode = new ScimObjectNode(parentAttribute);
    for ( SchemaAttribute metaAttribute : metaAttributes )
    {
      checkMetaAttributeOnDocument(document, metaAttribute).ifPresent(childNode -> {
        if (!(childNode.isArray() && childNode.isEmpty()))
        {
          JsonHelper.addAttribute(scimNode, metaAttribute.getName(), childNode);
        }
      });
    }
    if (scimNode.isEmpty())
    {
      return null;
    }
    return scimNode;
  }

  private Optional<JsonNode> checkMetaAttributeOnDocument(JsonNode document, SchemaAttribute schemaAttribute)
  {
    JsonNode documentNode = document.get(schemaAttribute.getName());
    validateIsRequired(documentNode, schemaAttribute);
    if (documentNode == null)
    {
      return Optional.empty();
    }
    if (schemaAttribute.isMultiValued())
    {
      return handleMultivaluedNodes(documentNode, schemaAttribute);
    }
    else
    {
      return handleNode(documentNode, schemaAttribute);
    }
  }

  private Optional<JsonNode> handleMultivaluedNodes(JsonNode document, SchemaAttribute schemaAttribute)
  {
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      return handleMultivaluedComplexNode(document, schemaAttribute);
    }
    else
    {
      return handleSimpleMultivaluedNode(document, schemaAttribute);
    }
  }

  private Optional<JsonNode> handleSimpleMultivaluedNode(JsonNode document, SchemaAttribute schemaAttribute)
  {
    JsonNode multiValuedAttribute = document.get(schemaAttribute.getName());
    validateIsRequired(multiValuedAttribute, schemaAttribute);
    if (multiValuedAttribute == null)
    {
      return Optional.empty();
    }
    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    for ( JsonNode jsonNode : multiValuedAttribute )
    {
      handleSimpleNode(jsonNode, schemaAttribute).ifPresent(simpleNode -> {
        JsonHelper.addAttribute(scimArrayNode, schemaAttribute.getName(), simpleNode);
      });
    }
    if (scimArrayNode.isEmpty())
    {
      return Optional.empty();
    }
    return Optional.of(scimArrayNode);
  }

  private Optional<JsonNode> handleMultivaluedComplexNode(JsonNode document, SchemaAttribute schemaAttribute)
  {
    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    for ( JsonNode complexAttribute : document )
    {
      handleComplexNode(complexAttribute, schemaAttribute).ifPresent(returnedAttribute -> {
        JsonHelper.addAttributeToArray(scimArrayNode, returnedAttribute);
      });
    }
    if (scimArrayNode.isEmpty())
    {
      return Optional.empty();
    }
    return Optional.of(scimArrayNode);
  }

  private Optional<JsonNode> handleNode(JsonNode document, SchemaAttribute schemaAttribute)
  {
    Optional<JsonNode> jsonNode;
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      jsonNode = handleComplexNode(document, schemaAttribute);
    }
    else
    {
      jsonNode = handleSimpleNode(document, schemaAttribute);
    }
    if (jsonNode.isPresent())
    {
      return validateRequestBasedInformation(jsonNode.get(), schemaAttribute);
    }
    else
    {
      return Optional.empty();
    }
  }

  private Optional<JsonNode> validateRequestBasedInformation(JsonNode jsonNode, SchemaAttribute schemaAttribute)
  {
    if (DirectionType.REQUEST.equals(directionType))
    {
      return validateRequestBasedInformationForRequest(jsonNode, schemaAttribute);
    }
    else
    {
      return validateRequestBasedInformationForResponse(jsonNode, schemaAttribute);
    }
  }

  private Optional<JsonNode> validateRequestBasedInformationForRequest(JsonNode jsonNode,
                                                                       SchemaAttribute schemaAttribute)
  {
    if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability()))
    {
      return Optional.empty();
    }
    return Optional.of(jsonNode);
  }

  private Optional<JsonNode> validateRequestBasedInformationForResponse(JsonNode jsonNode,
                                                                        SchemaAttribute schemaAttribute)
  {
    if (Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
    {
      return Optional.empty();
    }
    else if (Returned.NEVER.equals(schemaAttribute.getReturned()))
    {
      return Optional.empty();
    }
    return Optional.of(jsonNode);
  }

  private Optional<JsonNode> handleComplexNode(JsonNode documentComplexNode, SchemaAttribute schemaAttribute)
  {
    validateIsRequired(documentComplexNode, schemaAttribute);
    if (documentComplexNode == null)
    {
      return Optional.empty();
    }
    List<SchemaAttribute> metaSubAttributes = schemaAttribute.getSubAttributes();
    return Optional.ofNullable(validateAttributes(metaSubAttributes, documentComplexNode, schemaAttribute));
  }

  private void validateIsRequired(JsonNode jsonNode, SchemaAttribute schemaAttribute)
  {
    if (!schemaAttribute.isRequired())
    {
      return;
    }
    if (DirectionType.REQUEST.equals(directionType))
    {
      validateIsRequiredForRequest(jsonNode, schemaAttribute);
    }
    else
    {
      validateIsRequiredForResponse(jsonNode, schemaAttribute);
    }
  }

  private void validateIsRequiredForRequest(JsonNode jsonNode, SchemaAttribute schemaAttribute)
  {
    boolean isNodeNull = jsonNode == null || jsonNode.isNull();
    Supplier<String> errorMessage = () -> "the attribute '" + schemaAttribute.getScimNodeName() + "' is required on '"
                                          + httpMethod + "' request for its mutability is '"
                                          + schemaAttribute.getMutability() + "'!";
    if ((Mutability.READ_WRITE.equals(schemaAttribute.getMutability())
         || Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
        && isNodeNull)
    {
      throw getException(errorMessage.get(), null);
    }
    else if (Mutability.IMMUTABLE.equals(schemaAttribute.getMutability()) && httpMethod.equals(HttpMethod.POST)
             && isNodeNull)
    {
      throw getException(errorMessage.get(), null);
    }
  }

  private void validateIsRequiredForResponse(JsonNode jsonNode, SchemaAttribute schemaAttribute)
  {
    boolean isNodeNull = jsonNode == null || jsonNode.isNull();
    Supplier<String> errorMessage = () -> "the attribute '" + schemaAttribute.getScimNodeName()
                                          + "' is required on response for its mutability is '"
                                          + schemaAttribute.getMutability() + "'!";
    if (isNodeNull && !Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
    {
      throw getException(errorMessage.get(), null);
    }
  }

  private Optional<JsonNode> handleSimpleNode(JsonNode simpleDocumentNode, SchemaAttribute schemaAttribute)
  {
    if (simpleDocumentNode == null)
    {
      // node is not present in the document so everything's fine
      log.trace("attribute {} not present in document: required = {}",
                schemaAttribute.getName(),
                schemaAttribute.isRequired());
      return Optional.empty();
    }
    checkCanonicalValues(schemaAttribute, simpleDocumentNode);
    Type type = schemaAttribute.getType();
    switch (type)
    {
      case STRING:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isTextual);
        return Optional.of(new ScimTextNode(schemaAttribute, simpleDocumentNode.textValue()));
      case BOOLEAN:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isBoolean);
        return Optional.of(new ScimBooleanNode(schemaAttribute, simpleDocumentNode.booleanValue()));
      case INTEGER:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isInt);
        return Optional.of(new ScimIntNode(schemaAttribute, simpleDocumentNode.intValue()));
      case DECIMAL:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isFloat);
        return Optional.of(new ScimDoubleNode(schemaAttribute, simpleDocumentNode.doubleValue()));
      case DATE_TIME:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isTextual);
        parseDateTime(simpleDocumentNode.textValue());
        return Optional.of(new ScimTextNode(schemaAttribute, simpleDocumentNode.textValue()));
      case REFERENCE:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isTextual);
        validateValueNodeWithReferenceTypes(schemaAttribute, simpleDocumentNode);
        return Optional.of(new ScimTextNode(schemaAttribute, simpleDocumentNode.textValue()));
      default:
        throw DocumentValidationException.builder()
                                         .message("value node '" + schemaAttribute.getName() + "' is of unknown type. "
                                                  + "Expected type is: " + type.getValue())
                                         .status(directionType.getHttpStatus())
                                         .build();
    }
  }

  /**
   * will verify that the current value node does define one of the canonical values of the attribute definition
   * if some are defined
   *
   * @param attributeDefinition the attribute definition from the meta schema
   * @param valueNode the value that matches to this definition
   */
  private void checkCanonicalValues(SchemaAttribute attributeDefinition, JsonNode valueNode)
  {
    if (attributeDefinition.getCanonicalValues().isEmpty())
    {
      // all values are valid
      return;
    }
    final String value = valueNode.textValue();
    if (!attributeDefinition.getCanonicalValues().contains(value))
    {
      final String errorMessage = "attribute with name '" + attributeDefinition.getName()
                                  + "' does not have one of the " + "canonicalValues: '"
                                  + attributeDefinition.getCanonicalValues() + "' actual value is: '" + value + "'";
      throw getException(errorMessage, null);
    }
  }

  /**
   * checks if the given node is of the expected type
   *
   * @param attributeDefinition the meta attribute definition
   * @param valueNode the current value node that should be checked
   * @param isOfType the check that will validate if the node has the expected type
   */
  private void isNodeOfExpectedType(SchemaAttribute attributeDefinition,
                                    JsonNode valueNode,
                                    Function<JsonNode, Boolean> isOfType)
  {
    Type type = attributeDefinition.getType();
    final String errorMessage = "value of field with name '" + attributeDefinition.getName() + "' is not of " + "type '"
                                + type.getValue() + "' but of type: "
                                + StringUtils.lowerCase(valueNode.getNodeType().toString());
    checkAttributeValidity(isOfType.apply(valueNode), errorMessage);
  }

  /**
   * checks if the expression is valid and throws an exception if not
   *
   * @param aBoolean the value of the expression to be checked
   * @param errorMessage the error message to display if the expression is false
   */
  private void checkAttributeValidity(Boolean aBoolean, String errorMessage)
  {
    if (!aBoolean)
    {
      throw getException(errorMessage, null);
    }
  }

  /**
   * tries to parse the given text as a xsd:datetime representation as defined in RFC7643 chapter 2.3.5
   */
  private void parseDateTime(String textValue)
  {
    try
    {
      TimeUtils.parseDateTime(textValue);
    }
    catch (InvalidDateTimeRepresentationException ex)
    {
      throw DocumentValidationException.builder()
                                       .status(directionType.getHttpStatus())
                                       .message("given value is not a valid dateTime: " + textValue)
                                       .cause(ex)
                                       .build();
    }
  }

  /**
   * validates a simple value node against the valid resource types defined in the meta schema
   *
   * @param attributeDefinition the meta attribute definition
   * @param valueNode the value node
   */
  private void validateValueNodeWithReferenceTypes(SchemaAttribute attributeDefinition, JsonNode valueNode)
  {
    boolean isValidReferenceType = false;
    for ( ReferenceTypes referenceType : attributeDefinition.getReferenceTypes() )
    {
      switch (referenceType)
      {
        case RESOURCE:
          // TODO check if the referenced resource type was already registered
          isValidReferenceType = true;
          break;
        case URI:
          isValidReferenceType = parseUri(valueNode.textValue());
          break;
        default:
          isValidReferenceType = true;
      }
      if (isValidReferenceType)
      {
        break;
      }
    }
    checkAttributeValidity(isValidReferenceType,
                           "given value is not a valid reference type: " + valueNode.textValue()
                                                 + ": was expected to be of one of the following types: "
                                                 + attributeDefinition.getReferenceTypes());
  }

  /**
   * tries to parse the given text into a URI
   */
  private boolean parseUri(String textValue)
  {
    try
    {
      new URI(textValue);
      return true;
    }
    catch (URISyntaxException ex)
    {
      log.debug(ex.getMessage());
      return false;
    }
  }

  /**
   * this method will verify that the meta schema is the correct schema to validate the document. This is done
   * by comparing the "id"-attribute of the metaSchema with the "schemas"-attribute of the document
   *
   * @param metaSchema the meta schema that should be used to validate the document
   * @param document the document that should be validated
   */
  private void checkDocumentAndMetaSchemaRelationship(JsonNode metaSchema, JsonNode document)
  {
    final String idAttribute = AttributeNames.ID;
    final String metaSchemaWithouidMessage = "meta schema does not have an '" + idAttribute + "'-attribute";
    String metaSchemaId = JsonHelper.getSimpleAttribute(metaSchema, idAttribute)
                                    .orElseThrow(() -> getException(metaSchemaWithouidMessage, null));

    final String schemasAttribute = AttributeNames.SCHEMAS;
    final String documentNoSchemasMessage = "document does not have a '" + schemasAttribute + "'-attribute";
    List<String> documentSchemas = JsonHelper.getSimpleAttributeArray(document, schemasAttribute)
                                             .orElseThrow(() -> getException(documentNoSchemasMessage, null));
    if (!documentSchemas.contains(metaSchemaId))
    {
      final String errorMessage = "document can not be validated against schema with id '" + metaSchemaId
                                  + "' for id is missing in the '" + schemasAttribute + "'-list: " + documentSchemas;
      throw getException(errorMessage, null);
    }
    log.trace("meta schema with id {} does apply to document with schemas '{}'", metaSchemaId, documentSchemas);
  }

  /**
   * any meta schema must have an attributes array that holds the field descriptions for the document
   *
   * @param metaSchema the meta schema that must have attributes
   * @return the attributes node
   */
  private JsonNode getAttributes(JsonNode metaSchema)
  {
    final String attributeName = AttributeNames.ATTRIBUTES;
    final String errorMessage = "meta schema is missing attribute '" + attributeName + "'";
    return JsonHelper.getArrayAttribute(metaSchema, attributeName)
                     .orElseThrow(() -> InternalServerErrorException.builder().message(errorMessage).build());
  }

  /**
   * any meta complex attribute must have an subAttributes array that holds the field descriptions for the
   * complex attribute
   *
   * @param metaSchema the meta complex attribute that must have subAttributes
   * @return the subAttributes node
   */
  private ArrayNode getSubAttributes(JsonNode metaSchema)
  {
    final String attributeName = AttributeNames.SUB_ATTRIBUTES;
    final String errorMessage = "meta complex attribute is missing attribute '" + attributeName + "'";
    return JsonHelper.getArrayAttribute(metaSchema, attributeName)
                     .orElseThrow(() -> InternalServerErrorException.builder().message(errorMessage).build());
  }

  /**
   * builds an exception
   *
   * @param errorMessage the error message of the exception
   * @param cause the cause of this exception, may be null
   * @return a document validation exception
   */
  private DocumentValidationException getException(String errorMessage, Exception cause)
  {
    return new DocumentValidationException(errorMessage, cause, directionType.getHttpStatus(), null);
  }

  /**
   * tells us which request type the user has used. This is e.g. necessary for immutable types that are valid on
   * POST requests but invalid on PUT requests
   */
  public enum HttpMethod
  {
    POST, PUT, PATCH
  }

  /**
   * the direction type is used for validation. It tells us how a schema should be validated because there are
   * some differences. The meta-attribute for example is a required attribute for the response and therefore has
   * a readOnly mutability. In order to validate those attribute correctly we need to know if we validate the
   * schema as a request or as a response
   */
  protected enum DirectionType
  {
    REQUEST(HttpStatus.SC_BAD_REQUEST), RESPONSE(HttpStatus.SC_INTERNAL_SERVER_ERROR);

    /**
     * should be interna l server error if the response validation fails and a bad request if the request
     * validation fails
     */
    @Getter(AccessLevel.PRIVATE)
    private int httpStatus;

    DirectionType(int httpStatus)
    {
      this.httpStatus = httpStatus;
    }
  }
}
