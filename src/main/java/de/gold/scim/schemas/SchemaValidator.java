package de.gold.scim.schemas;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.ReferenceTypes;
import de.gold.scim.constants.enums.Returned;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.constants.enums.Uniqueness;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.exceptions.InternalServerException;
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
 * This class will validate documents against their meta-schemata. Normally the validation should be done with
 * the help of {@link ResourceType}-definitions that define a resource endpoint with the setup of their
 * resources. Meaning that a {@link ResourceType} knows the meta-schemata of a resource and its extensions
 * which is why the {@link ResourceType} is the base of validating a resource-document
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
   * this marker tells us if a new schema is validated. If this field is set to true the {@link #directionType}
   * and {@link #httpMethod} fields will be ignored during validation
   */
  private final boolean schemaValidation;

  /**
   * tells us if the schema is validated as request or as response
   *
   * @see DirectionType
   */
  private final DirectionType directionType;

  /**
   * tells us which request type the user has used. This is e.g. necessary for immutable types that are valid on
   * POST requests but invalid on PUT requests
   */
  private final HttpMethod httpMethod;

  /**
   * used to check if a reference-type of {@link ReferenceTypes#RESOURCE} is registered or not.
   */
  private final ResourceTypeFactory resourceTypeFactory;

  private SchemaValidator(ResourceTypeFactory resourceTypeFactory)
  {
    this.resourceTypeFactory = resourceTypeFactory;
    this.extensionSchema = false;
    this.directionType = null;
    this.httpMethod = null;
    this.schemaValidation = true;
  }

  private SchemaValidator(ResourceTypeFactory resourceTypeFactory, DirectionType directionType, HttpMethod httpMethod)
  {
    this.resourceTypeFactory = resourceTypeFactory;
    this.directionType = directionType;
    this.httpMethod = httpMethod;
    this.extensionSchema = false;
    this.schemaValidation = false;
  }

  private SchemaValidator(ResourceTypeFactory resourceTypeFactory,
                          DirectionType directionType,
                          HttpMethod httpMethod,
                          boolean extensionSchema)
  {
    this.resourceTypeFactory = resourceTypeFactory;
    this.directionType = directionType;
    this.httpMethod = httpMethod;
    this.extensionSchema = extensionSchema;
    this.schemaValidation = false;
  }

  /**
   * this method will validate a new schema declaration against a meta schema. In other validations it might
   * happen that specific attributes will be removed from the document because they do not belong into a request
   * or a response. This method will ignore the direction-validation and keeps these attributes
   *
   * @param metaSchema the meta schema that is used to validate the new schema
   * @param schemaDocument the new schema document that should be validated
   * @return the validated schema definition
   */
  static JsonNode validateSchemaDocument(ResourceTypeFactory resourceTypeFactory,
                                         Schema metaSchema,
                                         JsonNode schemaDocument)
  {
    SchemaValidator schemaValidator = new SchemaValidator(resourceTypeFactory);
    return schemaValidator.validateDocument(metaSchema, schemaDocument);
  }

  /**
   * will validate an outgoing document against its main schema and all its extensions. Attributes that are
   * unknown to the given schema or are meaningless or forbidden in responses due to their mutability or
   * returned value will be removed from the validated document. <br>
   * attributes that will be removed in the response validation are thos that are having a mutability of
   * {@link Mutability#WRITE_ONLY} or a returned value of {@link Returned#NEVER}. This will prevent the server
   * from accidentally returning passwords or equally sensitive information
   *
   * @param resourceType the resource type definition of the incoming document
   * @param document the document that should be validated
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   * @throws DocumentValidationException if the schema validation failed
   */
  public static JsonNode validateDocumentForResponse(ResourceTypeFactory resourceTypeFactory,
                                                     ResourceType resourceType,
                                                     JsonNode document)
    throws DocumentValidationException
  {
    ResourceType.ResourceSchema resourceSchema = resourceType.getResourceSchema(document);
    JsonNode validatedMainDocument = validateDocumentForResponse(resourceTypeFactory,
                                                                 resourceSchema.getMetaSchema(),
                                                                 document);
    for ( Schema schemaExtension : resourceSchema.getExtensions() )
    {
      Supplier<String> message = () -> "the extension '" + schemaExtension.getId() + "' is referenced in the '"
                                       + AttributeNames.SCHEMAS + "' attribute but is "
                                       + "not present within the document";
      JsonNode extension = Optional.ofNullable(document.get(schemaExtension.getId()))
                                   .orElseThrow(() -> new InternalServerException(message.get(), null,
                                                                                  ScimType.MISSING_EXTENSION));
      JsonNode extensionNode = validateExtensionForResponse(resourceTypeFactory, schemaExtension, extension);
      JsonHelper.addAttribute(validatedMainDocument, schemaExtension.getId(), extensionNode);
    }
    return validatedMainDocument;
  }

  /**
   * will validate an outgoing document against its main schema and all its extensions. Attributes that are
   * unknown to the given schema or are meaningless or forbidden in responses due to their mutability or
   * returned value will be removed from the validated document. <br>
   * attributes that will be removed in the response validation are thos that are having a mutability of
   * {@link Mutability#WRITE_ONLY} or a returned value of {@link Returned#NEVER}. This will prevent the server
   * from accidentally returning passwords or equally sensitive information
   *
   * @param metaSchema the json meta schema definition of the document
   * @param document the document to validate
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   */
  protected static JsonNode validateDocumentForResponse(ResourceTypeFactory resourceTypeFactory,
                                                        Schema metaSchema,
                                                        JsonNode document)
  {
    SchemaValidator schemaValidator = new SchemaValidator(resourceTypeFactory, DirectionType.RESPONSE, null);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * This method is explicitly for extension validation. Extensions within its main documents should not have
   * "schemas"-attribute itself since the relation of the schema-uri can be found in the "schemas"-attribute of
   * the main-document. So this method will trigger the schema validation but will ignore the validation of the
   * "schemas"-attrbiute
   *
   * @param metaSchema the json meta schema definition of the extension
   * @param document the extension to validate
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   */
  private static JsonNode validateExtensionForResponse(ResourceTypeFactory resourceTypeFactory,
                                                       Schema metaSchema,
                                                       JsonNode document)
  {
    SchemaValidator schemaValidator = new SchemaValidator(resourceTypeFactory, DirectionType.RESPONSE, null, true);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * will validate an incoming document against its main schema and all its extensions. Attributes that are
   * unknown to the given schema or are meaningless in requests due to their mutability value will be removed
   * from the validated document. <br>
   * attributes that will be removed in the request validation are those that are have a mutability of
   * {@link Mutability#READ_ONLY}. The client is not able to write these attributes and therefore the server
   * does not need to process them.
   *
   * @param resourceType the resource type definition of the incoming document
   * @param document the document that should be validated
   * @param httpMethod the request http method that is used to validate the request-document
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   * @throws DocumentValidationException if the schema validation failed
   */
  public static JsonNode validateDocumentForRequest(ResourceTypeFactory resourceTypeFactory,
                                                    ResourceType resourceType,
                                                    JsonNode document,
                                                    HttpMethod httpMethod)
    throws DocumentValidationException
  {
    ResourceType.ResourceSchema resourceSchema = resourceType.getResourceSchema(document);
    JsonNode validatedMainDocument = validateDocumentForRequest(resourceTypeFactory,
                                                                resourceSchema.getMetaSchema(),
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
      JsonNode extensionNode = validateExtensionForRequest(resourceTypeFactory, schemaExtension, extension, httpMethod);
      JsonHelper.addAttribute(validatedMainDocument, schemaExtension.getId(), extensionNode);
    }
    return validatedMainDocument;
  }

  /**
   * will validate an incoming document against its main schema and all its extensions. Attributes that are
   * unknown to the given schema or are meaningless in requests due to their mutability value will be removed
   * from the validated document. <br>
   * attributes that will be removed in the request validation are those that are have a mutability of
   * {@link Mutability#READ_ONLY}. The client is not able to write these attributes and therefore the server
   * does not need to process them.
   *
   * @param metaSchema the json meta schema definition of the document
   * @param document the document to validate
   * @param httpMethod tells us which request type the client has used. This is e.g. necessary for immutable
   *          types that are valid on POST requests but invalid on PUT requests
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   */
  protected static JsonNode validateDocumentForRequest(ResourceTypeFactory resourceTypeFactory,
                                                       Schema metaSchema,
                                                       JsonNode document,
                                                       HttpMethod httpMethod)
  {
    SchemaValidator schemaValidator = new SchemaValidator(resourceTypeFactory, DirectionType.REQUEST, httpMethod);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * This method is explicitly for extension validation. Extensions within its main documents should not have
   * "schemas"-attribute itself since the relation of the schema-uri can be found in the "schemas"-attribute of
   * the main-document. So this method will trigger the schema validation but will ignore the validation of the
   * "schemas"-attrbiute
   *
   * @param metaSchema the json meta schema definition of the document
   * @param document the extension to validate
   * @param httpMethod tells us which request type the client has used. This is e.g. necessary for immutable
   *          types that are valid on POST requests but invalid on PUT requests
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   */
  protected static JsonNode validateExtensionForRequest(ResourceTypeFactory resourceTypeFactory,
                                                        Schema metaSchema,
                                                        JsonNode document,
                                                        HttpMethod httpMethod)
  {
    SchemaValidator schemaValidator = new SchemaValidator(resourceTypeFactory, DirectionType.REQUEST, httpMethod, true);
    return schemaValidator.validateDocument(metaSchema, document);
  }

  /**
   * this method will validate the given document against the given meta schema and check if the document is
   * valid. Attributes that are unknown in the metaSchema but do exist in the document will be removed from the
   * document
   *
   * @param metaSchema the document description
   * @param document the document that should be built after the rules of the metaSchema
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   */
  private JsonNode validateDocument(Schema metaSchema, JsonNode document)
  {
    log.trace("validating metaSchema vs document");
    JsonNode schemasNode = null;
    if (!extensionSchema)
    {
      schemasNode = checkDocumentAndMetaSchemaRelationship(metaSchema, document);
    }
    JsonNode validatedDocument = validateAttributes(metaSchema.getAttributes(), document, null);
    if (validatedDocument == null)
    {
      throw getException("the validation returned an empty document please verify your validation parameters", null);
    }
    if (schemasNode != null)
    {
      JsonHelper.addAttribute(validatedDocument, AttributeNames.SCHEMAS, schemasNode);
    }
    return validatedDocument;
  }

  /**
   * will use the given meta attributes to validate each attribute in the document
   *
   * @param metaAttributes list of meta attributes that may or may not be present within the document
   * @param document the document to validate
   * @param parentAttribute this method is getting called recursively and this is the parent document that is
   *          given to the new {@link SchemaAttribute} object
   * @return the validated document that consists of {@link de.gold.scim.resources.ScimNode}s
   */
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

  /**
   * will check a single meta-attribute on the given document
   *
   * @param document the document to validate
   * @param schemaAttribute the single meta-attribute that will be validated against the given document
   * @return the attribute if present in the document an empty else
   */
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

  /**
   * validates attributes that are marked as multiValued attributes in the meta attribute
   *
   * @param document the document that holds the multiValued attribute
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute if present in the document or an empty else
   */
  private Optional<JsonNode> handleMultivaluedNodes(JsonNode document, SchemaAttribute schemaAttribute)
  {
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      // we will throw an exception if the primary counter exceeds 1
      AtomicInteger countPrimary = new AtomicInteger(0);
      return handleMultivaluedNode(document, schemaAttribute, (jsonNode, scimArrayNode) -> {
        countPrimary.set(checkForPrimary(jsonNode, schemaAttribute, countPrimary.get()));
        handleComplexNode(jsonNode, schemaAttribute).ifPresent(returnedAttribute -> {
          JsonHelper.addAttributeToArray(scimArrayNode, returnedAttribute);
        });
      });
    }
    else
    {
      return handleMultivaluedNode(document, schemaAttribute, (jsonNode, scimArrayNode) -> {
        handleSimpleNode(jsonNode, schemaAttribute).ifPresent(simpleNode -> {
          JsonHelper.addAttributeToArray(scimArrayNode, simpleNode);
        });
      });
    }
  }

  /**
   * checks if the given jsonNode contains a primary value and will throw an exception if the counter exceeds
   * more than 1 detected primary values
   *
   * @param jsonNode the multi valued complex type node that might contain a primary value
   * @param primaryCounter the current number of found primary values
   * @return the new calculated number of primary values
   */
  private int checkForPrimary(JsonNode jsonNode, SchemaAttribute schemaAttribute, int primaryCounter)
  {
    boolean isPrimary = JsonHelper.getSimpleAttribute(jsonNode, AttributeNames.PRIMARY).isPresent();
    int counter = primaryCounter + (isPrimary ? 1 : 0);
    if (counter > 1)
    {
      String errorMessage = "multiple primary values detected in attribute with name '"
                            + schemaAttribute.getScimNodeName() + "'";
      throw getException(errorMessage, null);
    }
    return counter;
  }

  /**
   * handles a json array with complex node types
   *
   * @param document the document that should be validated
   * @param schemaAttribute the meta information of the attribute
   * @param handleMultivaluedNode a consumer that handles either a simple multivalued node or a multivalued
   *          complex node
   * @return the attribute if present in the document or an empty else
   */
  private Optional<JsonNode> handleMultivaluedNode(JsonNode document,
                                                   SchemaAttribute schemaAttribute,
                                                   BiConsumer<JsonNode, ScimArrayNode> handleMultivaluedNode)
  {
    ArrayNode arrayNode;
    if (document.isArray())
    {
      arrayNode = (ArrayNode)document;
    }
    else
    {
      arrayNode = new ArrayNode(JsonNodeFactory.instance);
      arrayNode.add(document);
    }
    ScimArrayNode scimArrayNode = new ScimArrayNode(schemaAttribute);
    for ( JsonNode jsonNode : arrayNode )
    {
      checkForUniqueAttribute(schemaAttribute, scimArrayNode, jsonNode);
      handleMultivaluedNode.accept(jsonNode, scimArrayNode);
    }
    if (scimArrayNode.isEmpty())
    {
      return Optional.empty();
    }
    return Optional.of(scimArrayNode);
  }

  /**
   * handles a simple json node with a primitive value
   *
   * @param document the document that should be validated
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute if present in the document or an empty else
   */
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

  /**
   * handles a complex json node type. A complex node type has its own meta-attribute array and is itself a full
   * fleshed json document so this method will initiate a recursive call to
   * {@link #validateDocument(Schema, JsonNode)} to do its work
   *
   * @param document the document complex node to validate
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute if present in the document or an empty else
   */
  private Optional<JsonNode> handleComplexNode(JsonNode document, SchemaAttribute schemaAttribute)
  {
    validateIsRequired(document, schemaAttribute);
    List<SchemaAttribute> metaSubAttributes = schemaAttribute.getSubAttributes();
    return Optional.ofNullable(validateAttributes(metaSubAttributes, document, schemaAttribute));
  }

  /**
   * the handling of a simple json node with a primitive type
   *
   * @param simpleDocumentNode the simple value node that should be validated
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute as a {@link JsonNode} that implements the interface
   *         {@link de.gold.scim.resources.ScimNode} in its corresponding node type
   */
  private Optional<JsonNode> handleSimpleNode(JsonNode simpleDocumentNode, SchemaAttribute schemaAttribute)
  {
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
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isDouble);
        return Optional.of(new ScimDoubleNode(schemaAttribute, simpleDocumentNode.doubleValue()));
      case DATE_TIME:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isTextual);
        parseDateTime(simpleDocumentNode.textValue());
        return Optional.of(new ScimTextNode(schemaAttribute, simpleDocumentNode.textValue()));
      default:
        isNodeOfExpectedType(schemaAttribute, simpleDocumentNode, JsonNode::isTextual);
        validateValueNodeWithReferenceTypes(schemaAttribute, simpleDocumentNode);
        return Optional.of(new ScimTextNode(schemaAttribute, simpleDocumentNode.textValue()));
    }
  }

  /**
   * checks if the given json node is a required node and throws an exception if the required node is not
   * present within the document.
   *
   * @param document the document that should contain the attribute
   * @param schemaAttribute the meta information of the attribute
   */
  private void validateIsRequired(JsonNode document, SchemaAttribute schemaAttribute)
  {
    if (!schemaAttribute.isRequired() || schemaValidation)
    {
      return;
    }
    if (DirectionType.REQUEST.equals(directionType))
    {
      validateIsRequiredForRequest(document, schemaAttribute);
    }
    else
    {
      validateIsRequiredForResponse(document, schemaAttribute);
    }
  }

  /**
   * checks if the attribute is required in a request
   *
   * @param document the document that should contain the attribute
   * @param schemaAttribute the meta information of the attribute
   */
  private void validateIsRequiredForRequest(JsonNode document, SchemaAttribute schemaAttribute)
  {
    boolean isNodeNull = document == null || document.isNull();
    Supplier<String> errorMessage = () -> "the attribute '" + schemaAttribute.getScimNodeName() + "' is required on '"
                                          + httpMethod + "' request for its mutability is '"
                                          + schemaAttribute.getMutability() + "'!";
    if ((Mutability.READ_WRITE.equals(schemaAttribute.getMutability())
         || Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
        && isNodeNull)
    {
      throw getException(errorMessage.get(), null);
    }
    else if (Mutability.IMMUTABLE.equals(schemaAttribute.getMutability()) && HttpMethod.POST.equals(httpMethod)
             && isNodeNull)
    {
      throw getException(errorMessage.get(), null);
    }
  }

  /**
   * checks if the attribute is required in a response
   *
   * @param document the document that should contain the attribute
   * @param schemaAttribute the meta information of the attribute
   */
  private void validateIsRequiredForResponse(JsonNode document, SchemaAttribute schemaAttribute)
  {
    boolean isNodeNull = document == null || document.isNull();
    Supplier<String> errorMessage = () -> "the attribute '" + schemaAttribute.getScimNodeName()
                                          + "' is required on response for its mutability is '"
                                          + schemaAttribute.getMutability() + "'!";
    if (isNodeNull && !Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
    {
      throw getException(errorMessage.get(), null);
    }
  }

  /**
   * this method checks if the given array does already contain an equally jsonNode as the given one and throws
   * an exception if the uniqueness is not set to none
   *
   * @param schemaAttribute the attribute definition
   * @param scimArrayNode the scimArrayNode that should not contain any duplicate nodes
   * @param jsonNode the node that should not have any duplicates if the uniqueness has another value than none
   */
  private void checkForUniqueAttribute(SchemaAttribute schemaAttribute, ScimArrayNode scimArrayNode, JsonNode jsonNode)
  {
    if (!Uniqueness.NONE.equals(schemaAttribute.getUniqueness()))
    {
      for ( JsonNode complexNode : scimArrayNode )
      {
        if (complexNode.equals(jsonNode))
        {
          String errorMessage = "the array node with name '" + schemaAttribute.getScimNodeName()
                                + "' has a uniqueness of '" + schemaAttribute.getUniqueness() + "' but "
                                + "has at least one duplicate value: '" + complexNode.toString() + "'";
          throw getException(errorMessage, null);
        }
      }
    }
  }

  /**
   * validates if the given attribute should be removed from the request or response document
   *
   * @param documentNode the document node that is being validated
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute if it should not be remove or an empty if the attribute should be removed
   */
  private Optional<JsonNode> validateRequestBasedInformation(JsonNode documentNode, SchemaAttribute schemaAttribute)
  {
    if (DirectionType.REQUEST.equals(directionType))
    {
      return validateRequestBasedInformationForRequest(documentNode, schemaAttribute);
    }
    else
    {
      return validateRequestBasedInformationForResponse(documentNode, schemaAttribute);
    }
  }

  /**
   * validates if the given attribute should be removed from the request document
   *
   * @param documentNode the document node that is being validated
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute if it should not be remove or an empty if the attribute should be removed
   */
  private Optional<JsonNode> validateRequestBasedInformationForRequest(JsonNode documentNode,
                                                                       SchemaAttribute schemaAttribute)
  {
    if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability()))
    {
      log.debug("removed attribute '{}' from request since it has a mutability of {}",
                schemaAttribute.getScimNodeName(),
                schemaAttribute.getMutability());
      return Optional.empty();
    }
    return Optional.of(documentNode);
  }

  /**
   * validates if the given attribute should be removed from the response document
   *
   * @param documentNode the document node that is being validated
   * @param schemaAttribute the meta information of the attribute
   * @return the attribute if it should not be remove or an empty if the attribute should be removed
   */
  private Optional<JsonNode> validateRequestBasedInformationForResponse(JsonNode documentNode,
                                                                        SchemaAttribute schemaAttribute)
  {
    if (Mutability.WRITE_ONLY.equals(schemaAttribute.getMutability()))
    {
      log.warn("server tried to return the attribute '{}' that has a mutability value of: {}",
               schemaAttribute.getScimNodeName(),
               schemaAttribute.getMutability());
      return Optional.empty();
    }
    else if (Returned.NEVER.equals(schemaAttribute.getReturned()))
    {
      log.warn("server tried to return the attribute '{}' that has a returned value of: {}",
               schemaAttribute.getScimNodeName(),
               schemaAttribute.getReturned());
      return Optional.empty();
    }
    return Optional.of(documentNode);
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
      throw new DocumentValidationException("given value is not a valid dateTime: " + textValue, null, getHttpStatus(),
                                            null);
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
          isValidReferenceType = resourceTypeFactory.isResourceRegistered(valueNode.textValue());
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
  private JsonNode checkDocumentAndMetaSchemaRelationship(Schema metaSchema, JsonNode document)
  {
    final String metaSchemaId = metaSchema.getId();

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
    ScimArrayNode schemasNode = new ScimArrayNode(null);
    schemasNode.addAll(documentSchemas.stream().map(s -> new ScimTextNode(null, s)).collect(Collectors.toList()));
    return schemasNode;
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
    return new DocumentValidationException(errorMessage, cause, getHttpStatus(), null);
  }

  /**
   * @return the current http status for this validation
   */
  private Integer getHttpStatus()
  {
    return directionType == null ? HttpStatus.SC_INTERNAL_SERVER_ERROR : directionType.getHttpStatus();
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
