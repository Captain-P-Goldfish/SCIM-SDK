package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Returned;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.ValidationContext;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 06.10.2019 - 18:09 <br>
 * <br>
 */
@Slf4j
public class SchemaValidatorTest implements FileReferences
{

  /**
   * a basic service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * a url supplier that may be used in special cases during schema validation
   */
  private Supplier<String> baseUrlSupplier = () -> "http://localhost:8080/scim/v2";


  /**
   * the factory that builds and holds all registered resource-types
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * creates a endpoint reference url to a specific resource that was registered within the
   * {@link #resourceTypeFactory}
   */
  private BiFunction<String, String, String> referenceUrlSupplier = (resourceId, resourceName) -> {
    String endpoint = resourceTypeFactory.getResourceTypeByName(resourceName)
                                         .map(ResourceType::getEndpoint)
                                         .orElse("/" + resourceName);
    return String.format("http://localhost:8080/scim/v2%s/%s", endpoint, resourceId);
  };

  /**
   * defines the schema - document pairs that should be validated
   */
  private static Stream<Arguments> getSchemaValidations()
  {
    return Stream.of(Arguments.of("check user schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON)),
                     Arguments.of("check enterprise user schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON)),
                     Arguments.of("check group schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON)),
                     Arguments.of("check user-resourceType schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON)),
                     Arguments.of("check group-resourceType schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON)),
                     Arguments.of("check enterprise-user validation",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(USER_RESOURCE)));
  }

  /**
   * will produce a number of timestamp arguments for testing date parsing on scim documents
   */
  private static Stream<Arguments> getTimeStampArguments()
  {
    return Stream.of(Arguments.of(OffsetDateTime.now().withNano(0).format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(14))
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(-14))
                                         .withHour(0)
                                         .withMinute(0)
                                         .withSecond(0)
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(-10))
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now().toString()),
                     Arguments.of(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now()
                                               .withSecond(0)
                                               .withNano(0)
                                               .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now()
                                               .atOffset(ZoneOffset.ofHours(3))
                                               .withHour(0)
                                               .withMinute(0)
                                               .withSecond(0)
                                               .withNano(0)
                                               .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now()
                                               .withHour(0)
                                               .withMinute(0)
                                               .withSecond(0)
                                               .withNano(0)
                                               .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of("2019-09-29T24:00:00"),
                     Arguments.of("2019-09-29T24:00:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000"),
                     Arguments.of("2019-09-29T24:00:00Z"),
                     Arguments.of("2019-09-29T24:00:00.0000000Z"),
                     Arguments.of("2019-09-29T24:00:00.0000000-10:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000+10:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000-14:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000+14:00"));
  }

  /**
   * calls itself recursively to verify that all existing nodes are of type {@link ScimNode}
   *
   * @param validatedDocument the node to verify that it is a {@link ScimNode}
   */
  public static void validateJsonNodeIsScimNode(String fieldName, JsonNode validatedDocument)
  {
    if (AttributeNames.RFC7643.SCHEMAS.equals(fieldName))
    {
      return;
    }
    Assertions.assertTrue(validatedDocument instanceof ScimNode,
                          validatedDocument.getClass() + ": " + validatedDocument);
    ScimNode scimNode = (ScimNode)validatedDocument;
    log.trace(scimNode.getScimNodeName());
    if (validatedDocument.isArray() || validatedDocument.isObject())
    {
      validatedDocument.properties().forEach(stringJsonNodeEntry -> {
        validateJsonNodeIsScimNode(stringJsonNodeEntry.getKey(), stringJsonNodeEntry.getValue());
      });
    }
  }

  @BeforeEach
  public void initialize()
  {
    this.serviceProvider = new ServiceProvider();
    this.resourceTypeFactory = new ResourceTypeFactory();
  }

  /**
   * validates the schemata from the classpath
   *
   * @param testName the name of the test
   * @param metaSchemaNode the meta schema that describes the given json document
   * @param jsonDocument the json document that is validated against the schema
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("getSchemaValidations")
  public void testSchemaValidations(String testName, JsonNode metaSchemaNode, JsonNode jsonDocument)
  {
    log.trace(testName);
    Schema metaSchema = new Schema(metaSchemaNode);
    JsonNode jsonNode = MetaSchemaValidator.getInstance().validateDocument(metaSchema, jsonDocument);
    Assertions.assertTrue(JsonHelper.getArrayAttribute(jsonNode, AttributeNames.RFC7643.SCHEMAS).isPresent(),
                          "the schemas attribute must not be removed from the document");
    ArrayNode documentSchemas = JsonHelper.getArrayAttribute(jsonDocument, AttributeNames.RFC7643.SCHEMAS).get();
    ArrayNode jsonNodeSchemas = JsonHelper.getArrayAttribute(jsonNode, AttributeNames.RFC7643.SCHEMAS).get();
    Assertions.assertEquals(documentSchemas, jsonNodeSchemas);
  }

  /**
   * checks that the validation will fail if a required attribute is missing
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.SCHEMAS, AttributeNames.RFC7643.NAME, AttributeNames.RFC7643.SCHEMA,
                          AttributeNames.RFC7643.ENDPOINT})
  public void testValidationFailsOnMissingRequiredAttribute(String attributeName)
  {
    Schema resourceTypeSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    JsonHelper.removeAttribute(userResourceTypeSchema, attributeName);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> MetaSchemaValidator.getInstance()
                                                     .validateDocument(resourceTypeSchema, userResourceTypeSchema));
  }

  /**
   * checks that the validation will fail if a required attribute is missing within a sub-attribute
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.NAME, AttributeNames.RFC7643.TYPE,
                          AttributeNames.RFC7643.MULTI_VALUED})
  public void testValidationFailsOnMissingRequiredSubAttribute(String attributeName)
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNode attributes = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonNode firstAttribute = attributes.get(0);
    JsonHelper.removeAttribute(firstAttribute, attributeName);

    Assertions.assertThrows(DocumentValidationException.class,
                            () -> MetaSchemaValidator.getInstance().validateDocument(resourceMetaSchema, userSchema));
  }

  /**
   * checks that the validation will fail if a canonical value has a typo
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.MUTABILITY, AttributeNames.RFC7643.TYPE,
                          AttributeNames.RFC7643.RETURNED, AttributeNames.RFC7643.UNIQUENESS})
  public void testValidationFailsOnTypoInCanonicalValue(String attributeName)
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNode attributes = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonNode firstAttribute = attributes.get(0);
    JsonHelper.writeValue(firstAttribute, attributeName, "unknown_value");
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> MetaSchemaValidator.getInstance().validateDocument(resourceMetaSchema, userSchema));
  }

  /**
   * checks that the validation accepts JSON null for optional attribute
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.DESCRIPTION})
  public void testValidationAcceptsNullForOptionalAttribute(String attributeName)
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonHelper.replaceNode(userSchema, attributeName, JsonNodeFactory.instance.nullNode());
    Assertions.assertDoesNotThrow(() -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null,
                                                                    null, referenceUrlSupplier).validateDocument(
                                                                                                                 resourceMetaSchema,
                                                                                                                 userSchema));
    Assertions.assertDoesNotThrow(() -> new RequestSchemaValidator(serviceProvider, ScimObjectNode.class,
                                                                   HttpMethod.POST).validateDocument(resourceMetaSchema,
                                                                                                     userSchema));
  }

  /**
   * checks that the validation accepts JSON null for optional sub-attribute
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.DESCRIPTION, // string
                          AttributeNames.RFC7643.REQUIRED, // boolean
                          AttributeNames.RFC7643.CANONICAL_VALUES, // string, multiple
                          AttributeNames.RFC7643.MUTABILITY, // canonical string
                          AttributeNames.RFC7643.SUB_ATTRIBUTES, // complex, multiple
                          AttributeNames.Custom.MULTIPLE_OF, // decimal
                          AttributeNames.Custom.NOT_BEFORE, // dateTime
  })
  public void testValidationAcceptsNullForOptionalSubAttribute(String attributeName)
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNode attributes = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonNode firstAttribute = attributes.get(0);
    JsonHelper.replaceNode(firstAttribute, attributeName, JsonNodeFactory.instance.nullNode());
    Assertions.assertDoesNotThrow(() -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null,
                                                                    null, referenceUrlSupplier).validateDocument(
                                                                                                                 resourceMetaSchema,
                                                                                                                 userSchema));
    Assertions.assertDoesNotThrow(() -> new RequestSchemaValidator(serviceProvider, ScimObjectNode.class,
                                                                   HttpMethod.POST).validateDocument(resourceMetaSchema,
                                                                                                     userSchema));
  }

  /**
   * shows that the validation will fail if a field is an array with several elements but is expected by the
   * schema as a simple value
   */
  @Test
  public void testValidationFailsIfNodeIsArrayInsteadOfSimple()
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNodeFactory factory = new JsonNodeFactory(false);
    ArrayNode arrayNode = new ArrayNode(factory);
    arrayNode.add("bla");
    arrayNode.add("bla2");
    JsonHelper.replaceNode(userSchema, AttributeNames.RFC7643.ID, arrayNode);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                                              referenceUrlSupplier).validateDocument(resourceMetaSchema,
                                                                                                     userSchema));
  }

  /**
   * shows that the validation will fail if a field is an array but is expected by the schema as a simple value
   */
  @Test
  public void testValidationFailsIfNodeIsOfDifferentType()
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    IntNode idNode = new IntNode(new Random().nextInt());
    JsonHelper.replaceNode(userSchema, AttributeNames.RFC7643.ID, idNode);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                                              referenceUrlSupplier).validateDocument(resourceMetaSchema,
                                                                                                     userSchema));
  }

  /**
   * this test will show that the validation will fail if a value marked as reference of type uri is not of type
   * uri
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.SCHEMA, AttributeNames.RFC7643.ENDPOINT})
  public void testValidationFailsIfUriReferenceIsNotAUri(String attributeName)
  {
    Schema resourceTypeSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    JsonHelper.writeValue(userResourceTypeSchema, attributeName, "oh happy day");
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                                              referenceUrlSupplier).validateDocument(resourceTypeSchema,
                                                                                                     userResourceTypeSchema));
  }

  /**
   * this test will show that the validation of timestamps is successfully executed up to RFC7643 chapter 2.3.5
   */
  @ParameterizedTest
  @MethodSource("getTimeStampArguments")
  public void testValidationWithTimestampFields(String dateTime)
  {
    JsonNode resourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    addTimestampToMetaSchemaAndDocument(dateTime, resourceTypeSchema, userResourceTypeSchema);

    Schema metaSchema = new Schema(resourceTypeSchema);
    Assertions.assertDoesNotThrow(() -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null,
                                                                    null, referenceUrlSupplier).validateDocument(
                                                                                                                 metaSchema,
                                                                                                                 userResourceTypeSchema));
  }

  /**
   * this test will verify that the validation fails if timestamps are used that do not fit the xsd:datetime
   * definition
   */
  @ParameterizedTest
  @ValueSource(strings = {"hello world", "123456", "2019-12-24", "2019-12-24 13:54:28"})
  public void testValidationFailsForInvalidTimestamps(String dateTime)
  {
    JsonNode resourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    addTimestampToMetaSchemaAndDocument(dateTime, resourceTypeSchema, userResourceTypeSchema);

    Schema metaSchema = new Schema(resourceTypeSchema);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                                              referenceUrlSupplier).validateDocument(metaSchema,
                                                                                                     userResourceTypeSchema));
  }

  /**
   * this method takes a dateTime value, a meta schema and a document and will add a timestamp definition to the
   * meta-schema, and the timestamp value to the document for validation
   *
   * @param dateTime the date time value that should be added to the document
   * @param metaSchema the meta schema that will need a datetime definition
   * @param document the document that must hold the datetime value
   */
  private void addTimestampToMetaSchemaAndDocument(String dateTime, JsonNode metaSchema, JsonNode document)
  {
    final String createdAttributeName = "created";
    String dateTimeTypeString = TestHelper.getAttributeString(createdAttributeName,
                                                              Type.DATE_TIME,
                                                              false,
                                                              true,
                                                              false,
                                                              Mutability.READ_WRITE,
                                                              Returned.DEFAULT,
                                                              Uniqueness.NONE);
    JsonNode metaAttributes = JsonHelper.getArrayAttribute(metaSchema, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonNode createMetaAttribute = JsonHelper.readJsonDocument(dateTimeTypeString);
    JsonHelper.addAttributeToArray(metaAttributes, createMetaAttribute);
    TextNode textNode = new TextNode(dateTime);
    JsonHelper.addAttribute(document, createdAttributeName, textNode);
  }

  /**
   * this test will show that the validation will also remove attributes that are not defined by the schema
   */
  @Test
  public void testRemoveUnknownAttributes()
  {
    Schema resourceTypeSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON));
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    final String helloWorldKey = "helloWorld";
    JsonHelper.addAttribute(userResourceTypeSchema, helloWorldKey, new TextNode("hello world"));

    JsonNode validatedSchema = new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                                           referenceUrlSupplier).validateDocument(resourceTypeSchema,
                                                                                                  userResourceTypeSchema);
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(validatedSchema, helloWorldKey).isPresent());
    ArrayNode schemaExtensions = JsonHelper.getArrayAttribute(validatedSchema, AttributeNames.RFC7643.SCHEMA_EXTENSIONS)
                                           .orElseThrow(() -> new IllegalStateException("the document does not contain "
                                                                                        + "an endpoint  attribute"));
    ObjectNode schemaExtensionAttribute = (ObjectNode)schemaExtensions.get(0);
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(schemaExtensionAttribute, helloWorldKey).isPresent());

    Assertions.assertFalse(JsonHelper.getObjectAttribute(validatedSchema, AttributeNames.RFC7643.META).isPresent(),
                           "meta attribute must be removed from validated request-document");
  }

  /**
   * this test will verify that the validated nodes created by the {@link SchemaValidator} are all implementing
   * the interface {@link ScimNode}
   */
  @ParameterizedTest
  @CsvSource({ClassPathReferences.META_RESOURCE_SCHEMA_JSON + "," + ClassPathReferences.USER_SCHEMA_JSON,
              ClassPathReferences.META_RESOURCE_TYPES_JSON + "," + ClassPathReferences.USER_RESOURCE_TYPE_JSON,
              ClassPathReferences.META_RESOURCE_TYPES_JSON + "," + ClassPathReferences.GROUP_RESOURCE_TYPE_JSON,
              ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE,
              ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE_ENTERPRISE,
              ClassPathReferences.GROUP_SCHEMA_JSON + "," + GROUP_RESOURCE})
  public void testThatAllValidatedNodesAreScimNodes(String metaSchemaLocation, String documentLocation)
  {
    Schema resourceMetaSchema = new Schema(JsonHelper.loadJsonDocument(metaSchemaLocation));
    JsonNode userSchema = JsonHelper.loadJsonDocument(documentLocation);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                         referenceUrlSupplier).validateDocument(resourceMetaSchema, userSchema);
    });
    Assertions.assertNotNull(validatedDocument);
    validateJsonNodeIsScimNode(null, validatedDocument);
  }

  /**
   * this test will check that on response validation an exception is thrown if a required attribute is missing
   */
  @ParameterizedTest
  @CsvSource({ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE_ENTERPRISE,
              ClassPathReferences.GROUP_SCHEMA_JSON + "," + GROUP_RESOURCE})
  public void testValidationFailsForMissingIdOnResponse(String metaSchemaLocation, String documentLocation)
  {
    Schema metaSchema = new Schema(JsonHelper.loadJsonDocument(metaSchemaLocation));
    JsonNode resourceSchema = JsonHelper.loadJsonDocument(documentLocation);

    JsonHelper.removeAttribute(resourceSchema, AttributeNames.RFC7643.ID);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                  referenceUrlSupplier).validateDocument(metaSchema, resourceSchema);
    });
  }

  /**
   * this test will check that on response validation NO exception is thrown if a required attribute is missing
   * but the {@link ServiceProvider#isIgnoreRequiredExtensionsOnResponse()} is set to true
   */
  @ParameterizedTest
  @CsvSource({ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE_ENTERPRISE,
              ClassPathReferences.GROUP_SCHEMA_JSON + "," + GROUP_RESOURCE})
  public void testValidationDoesNotFailOnMissingIdOnResponse(String metaSchemaLocation, String documentLocation)
  {
    Schema metaSchema = new Schema(JsonHelper.loadJsonDocument(metaSchemaLocation));
    JsonNode resourceSchema = JsonHelper.loadJsonDocument(documentLocation);

    serviceProvider.setIgnoreRequiredAttributesOnResponse(true);

    JsonHelper.removeAttribute(resourceSchema, AttributeNames.RFC7643.ID);
    Assertions.assertDoesNotThrow(() -> {
      new ResponseSchemaValidator(serviceProvider, ScimObjectNode.class, null, null, null,
                                  referenceUrlSupplier).validateDocument(metaSchema, resourceSchema);
    });
  }

  /**
   * This test is explicitly for checking compatibility with microsoft Azure AD. Azure AD sends schema extension
   * references in the schema attribute even if the extension is not present within the document. So this test
   * verifies that the schema validation will not fail if the extension is explicitly set but not present within
   * a request
   */
  @Test
  public void testIgnoreReferencedButMissingExtensionsOnRequest()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUser);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.addSchema(SchemaUris.ENTERPRISE_USER_URI);

    Assertions.assertNull(user.get(SchemaUris.ENTERPRISE_USER_URI));
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new RequestResourceValidator(serviceProvider, userResourceType, HttpMethod.POST).validateDocument(user);
    });
    User validatedUser = JsonHelper.readJsonDocument(validatedDocument.toString(), User.class);
    Assertions.assertFalse(validatedUser.getEnterpriseUser().isPresent());
    Assertions.assertEquals(1, validatedUser.getSchemas().size());
    Assertions.assertEquals(SchemaUris.USER_URI, validatedUser.getSchemas().iterator().next());
  }

  /**
   * this test will verify that unknown attributes are removed from the validated document
   */
  @Test
  public void testRemoveEnterpriseExtensionFromValidatedDocument()
  {
    Schema userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(userSchema,
                                                                                                       enterpriseUser);
    });
    // since the document was only validated against the user-schema and not the enterprise-user-extension schema
    // the extension attribute should not be present in the result
    Assertions.assertNull(validatedDocument.get(SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * this test will verify never returned attributes are simply removed from responses
   */
  @Test
  public void testRemoveNeverReturnedAttributesFromResponse()
  {
    Schema userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseSchemaValidator(serviceProvider, User.class, null, null, null,
                                         referenceUrlSupplier).validateDocument(userSchema, user);
    });
    // since the document was only validated against the user-schema and not the enterprise-user-extension schema
    // the extension attribute should not be present in the result
    Assertions.assertNull(validatedDocument.get(AttributeNames.RFC7643.PASSWORD));
  }


  /**
   * this test will verify that reference attributes are added on response automatically if enough information
   * is present
   */
  @Test
  public void testAddReferenceValueAutomatically()
  {
    resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));
    resourceTypeFactory.registerResourceType(new GroupHandlerImpl(),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON),
                                             JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));

    Schema groupSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON));
    String userId = UUID.randomUUID().toString();
    String groupId = UUID.randomUUID().toString();
    String groupResourceString = readResourceFile(GROUP_RESOURCE_TWO_MEMBERS,
                                                  s -> s.replace("${userId}", userId).replace("${groupId}", groupId));
    Group originalGroup = JsonHelper.readJsonDocument(groupResourceString, Group.class);
    originalGroup.setId(UUID.randomUUID().toString());

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseSchemaValidator(serviceProvider, Group.class, null, null, null,
                                         referenceUrlSupplier).validateDocument(groupSchema, originalGroup);
    });
    Group group = JsonHelper.copyResourceToObject(validatedDocument, Group.class);
    String expectedUserUrl = baseUrlSupplier.get() + EndpointPaths.USERS + "/" + userId;
    String expectedGroupUrl = baseUrlSupplier.get() + EndpointPaths.GROUPS + "/" + groupId;
    Member userMember = group.getMembers()
                             .stream()
                             .filter(member -> member.getType().get().equals(ResourceTypeNames.USER))
                             .findAny()
                             .get();
    Member groupMember = group.getMembers()
                              .stream()
                              .filter(member -> member.getType().get().equals(ResourceTypeNames.GROUPS))
                              .findAny()
                              .get();
    Assertions.assertEquals(expectedUserUrl, userMember.getRef().get());
    Assertions.assertEquals(expectedGroupUrl, groupMember.getRef().get());
  }

  /**
   * this test will verify read only attributes are simply removed from the request if they are not required
   */
  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT"})
  public void testRemoveNonRequiredReadOnlyAttributesFromRequest(HttpMethod httpMethod)
  {
    Schema metaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class, httpMethod).validateDocument(metaSchema,
                                                                                                  userSchema);
    });
    // since the document was only validated against the user-schema and not the enterprise-user-extension schema
    // the extension attribute should not be present in the result
    Assertions.assertNull(validatedDocument.get(AttributeNames.RFC7643.ID));
    Assertions.assertNull(validatedDocument.get(AttributeNames.RFC7643.DISPLAY));
    Assertions.assertNull(validatedDocument.get(AttributeNames.RFC7643.GROUPS));
  }

  /**
   * will test that simple arrays will also be handled successfully
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "value1"})
  public void testValidationWithSimpleArrayNode(String value)
  {
    value = StringUtils.stripToNull(value);
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final String attributeName = "simpleArray";
    JsonNode uniqueArray = JsonHelper.readJsonDocument(TestHelper.getAttributeString(attributeName,
                                                                                     Type.STRING,
                                                                                     true,
                                                                                     true,
                                                                                     true,
                                                                                     Mutability.READ_WRITE,
                                                                                     Returned.ALWAYS,
                                                                                     Uniqueness.NONE));
    JsonNode attributes = JsonHelper.getArrayAttribute(metaSchemaNode, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonHelper.addAttributeToArray(attributes, uniqueArray);

    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    Optional.ofNullable(value).ifPresent(arrayNode::add);
    JsonHelper.addAttribute(userSchema, attributeName, arrayNode);
    Schema metaSchema = new Schema(metaSchemaNode);
    Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema,
                                                                                                       userSchema);
    });
  }

  /**
   * this test will verify that the validation fails if an array with a uniqueness of server or global has
   * duplicate values
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testDuplicateValueOnUniqueMultivaluedAttribute(Uniqueness uniqueness)
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final String attributeName = "uniqueArray";
    JsonNode uniqueArray = JsonHelper.readJsonDocument(TestHelper.getAttributeString(attributeName,
                                                                                     Type.STRING,
                                                                                     true,
                                                                                     true,
                                                                                     true,
                                                                                     Mutability.READ_WRITE,
                                                                                     Returned.ALWAYS,
                                                                                     uniqueness));
    JsonNode attributes = JsonHelper.getArrayAttribute(metaSchemaNode, AttributeNames.RFC7643.ATTRIBUTES).get();
    JsonHelper.addAttributeToArray(attributes, uniqueArray);

    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    // add the same value twice
    arrayNode.add(attributeName);
    arrayNode.add(attributeName);
    JsonHelper.addAttribute(userSchema, attributeName, arrayNode);

    Schema metaSchema = new Schema(metaSchemaNode);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema, userSchema);
    });
  }

  /**
   * this test will verify that the validation fails if an complex array with a uniqueness of server or global
   * has duplicate values<br>
   * the test will change the uniqueness of the emails-attribute and will then add a duplicate entry to the
   * emails-attribute
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testDuplicateValueOnUniqueComplexMultivaluedAttribute(Uniqueness uniqueness)
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(metaSchemaNode,
                                       AttributeNames.RFC7643.EMAILS,
                                       null,
                                       null,
                                       null,
                                       uniqueness,
                                       null,
                                       null,
                                       null,
                                       null);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    // @formatter:off
    final String email = "{" +
                         "    \"value\": \"goldfish@germany.de\",\n" +
                         "    \"type\": \"work\"" +
                         "}";
    // @formatter:on
    JsonNode emailNode = JsonHelper.readJsonDocument(email);
    ArrayNode emailArray = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.EMAILS).get();
    emailArray.add(emailNode);
    emailArray.add(emailNode);
    Schema metaSchema = new Schema(metaSchemaNode);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema, userSchema);
    });
  }

  /**
   * This test will make sure that an exception is thrown if a multivalued complex type contains several primary
   * attributes
   */
  @Test
  public void testFailOnSeveralPrimaryMultivaluedComplexTypeValues()
  {
    Schema metaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    // @formatter:off
    final String email = "{" +
                         "    \"value\": \"goldfish@germany.de\",\n" +
                         "    \"type\": \"work\",\n" +
                         "    \"primary\": true" +
                         "}";
    // @formatter:on
    JsonNode emailNode = JsonHelper.readJsonDocument(email);
    ArrayNode emailArray = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.EMAILS).get();
    emailArray.add(emailNode);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema, userSchema);
    });
  }

  /**
   * This test will make sure that no exception is thrown if a multivalued complex type contains several primary
   * attributes where just a single node is set to primary true
   */
  @Test
  public void testSucceedOnSeveralPrimaryMultivaluedComplexTypeValuesSetButJustOneSetToTrue()
  {
    Schema metaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    // @formatter:off
    final String email = "{" +
                         "    \"value\": \"goldfish@germany.de\",\n" +
                         "    \"type\": \"work\",\n" +
                         "    \"primary\": false" +
                         "}";
    // @formatter:on
    JsonNode emailNode = JsonHelper.readJsonDocument(email);
    ArrayNode emailArray = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.EMAILS).get();
    emailArray.add(emailNode);
    Assertions.assertDoesNotThrow(() -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema, userSchema);
    });
  }

  /**
   * will test that an attribute with returned-value never will not be returned from the server
   */
  @Test
  public void testReturnValueWithReturnedValueNever()
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(metaSchemaNode,
                                       AttributeNames.RFC7643.EXTERNAL_ID,
                                       null,
                                       null,
                                       Returned.NEVER,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);

    Schema metaSchema = new Schema(metaSchemaNode);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseSchemaValidator(serviceProvider, User.class, null, null, null,
                                         referenceUrlSupplier).validateDocument(metaSchema, userSchema);
    });
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(validatedDocument, AttributeNames.RFC7643.EXTERNAL_ID)
                                     .isPresent());
  }

  /**
   * will test that the validation does not fail if an erroneous uri was put into the schemas attribute and that
   * the correct uri will be eventually found within the validated resource
   */
  @Test
  public void testDocumentDoesNotContainMetaSchemaId()
  {
    Schema metaSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    ArrayNode schemas = JsonHelper.getArrayAttribute(userSchema, AttributeNames.RFC7643.SCHEMAS).get();
    schemas.removeAll();
    schemas.add("urn:some:unknown:id:reference");
    User validatedUser = (User)Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema,
                                                                                                       userSchema);
    });
    Assertions.assertEquals(1, validatedUser.getSchemas().size());
    MatcherAssert.assertThat(validatedUser.getSchemas(), Matchers.hasItem(metaSchema.getNonNullId()));
  }

  /**
   * tests that an exception is thrown if a required immutable or a writeOnly attribute is not present on a
   * creation request
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "WRITE_ONLY"})
  public void testMissingRequiredAttributesOnCreationRequest(Mutability mutability)
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(metaSchemaNode,
                                       AttributeNames.RFC7643.USER_NAME,
                                       null,
                                       mutability,
                                       Returned.NEVER,
                                       null,
                                       null,
                                       true,
                                       null,
                                       null);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);
    JsonHelper.removeAttribute(user, AttributeNames.RFC7643.USER_NAME);

    Schema userSchema = new Schema(metaSchemaNode);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(userSchema, user);
    });
  }

  /**
   * will verify that an exception is thrown if an attribute is of another type as declared in the schema. This
   * explicit test changes the nickname into an integer type but the attribute in the document will send a
   * string-nickname
   */
  @Test
  public void testValidationWithIncorrectAttributeType()
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(metaSchemaNode,
                                       AttributeNames.RFC7643.NICK_NAME,
                                       Type.INTEGER,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setNickName("goldfish");
    Schema userSchema = new Schema(metaSchemaNode);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(userSchema, user);
    });
  }

  /**
   * will verify that the validation of an integer attribute type works successfully
   */
  @Test
  public void testValidationWithIntAttribute()
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(metaSchemaNode,
                                       AttributeNames.RFC7643.NICK_NAME,
                                       Type.INTEGER,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    JsonHelper.addAttribute(userSchema, AttributeNames.RFC7643.NICK_NAME, new IntNode(Integer.MAX_VALUE));

    Schema metaSchema = new Schema(metaSchemaNode);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(metaSchema,
                                                                                                       userSchema);
    });
    Assertions.assertEquals(Integer.MAX_VALUE,
                            JsonHelper.getSimpleAttribute(validatedDocument,
                                                          AttributeNames.RFC7643.NICK_NAME,
                                                          Integer.class)
                                      .get());
  }

  /**
   * will verify that the validation of a decimal (double) attribute type works successfully
   */
  @Test
  public void testValidationWithDecimalAttribute()
  {
    JsonNode metaSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(metaSchemaNode,
                                       AttributeNames.RFC7643.NICK_NAME,
                                       Type.DECIMAL,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);
    JsonHelper.addAttribute(user, AttributeNames.RFC7643.NICK_NAME, new DoubleNode(Double.MAX_VALUE));
    Schema userSchema = new Schema(metaSchemaNode);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(userSchema,
                                                                                                       user);
    });
    Assertions.assertEquals(Double.MAX_VALUE,
                            JsonHelper.getSimpleAttribute(validatedDocument,
                                                          AttributeNames.RFC7643.NICK_NAME,
                                                          Double.class)
                                      .get());
  }

  /**
   * this test will verify that a required missing attribute will cause an exception if it has been set to a
   * jsonNull value
   */
  @Test
  public void testValidationWithJsonNullValue()
  {
    Schema userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);
    JsonHelper.addAttribute(user, AttributeNames.RFC7643.USER_NAME, NullNode.instance);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new RequestSchemaValidator(serviceProvider, User.class, HttpMethod.POST).validateDocument(userSchema, user);
    });
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      new ResponseSchemaValidator(serviceProvider, User.class, null, null, null,
                                  referenceUrlSupplier).validateDocument(userSchema, user);
    });
  }

  /**
   * this test will verify that a required missing attribute will NOT cause an exception if it has been set to a
   * jsonNull value if the service-provider ignores required attributes on response
   */
  @Test
  public void testValidationWithJsonNullValueAndRequiredIgnored()
  {
    Schema userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);
    JsonHelper.addAttribute(user, AttributeNames.RFC7643.USER_NAME, NullNode.instance);

    serviceProvider.setIgnoreRequiredAttributesOnResponse(true);

    Assertions.assertDoesNotThrow(() -> {
      new ResponseSchemaValidator(serviceProvider, User.class, null, null, null,
                                  referenceUrlSupplier).validateDocument(userSchema, user);
    });
  }

  /**
   * this test will verify that a {@link ReferenceTypes#RESOURCE} referenceType is successfully verified if the
   * given resourceType name is registered in the {@link ResourceTypeFactory}
   */
  @Test
  public void testResourceReferenceIsUsedAndResourceWasRegistered()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    resourceTypeFactory.registerResourceType(null, userResourceType, userResourceSchema, enterpriseUserExtension);

    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    JsonNode enterpriseUser = JsonHelper.getObjectAttribute(userSchema, SchemaUris.ENTERPRISE_USER_URI).get();

    Schema enterpriseSchema = new Schema(enterpriseUserExtension);
    Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, User.class,
                                        HttpMethod.POST).validateDocument(new ScimObjectNode(),
                                                                          enterpriseSchema,
                                                                          enterpriseUser);
    });
  }

  /**
   * will check that an exception is thrown if a required extension is missing
   */
  @Test
  public void testBadRequestOnMissingRequiredExtension()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode schemaExtensions = JsonHelper.getArrayAttribute(userResourceType, AttributeNames.RFC7643.SCHEMA_EXTENSIONS)
                                          .get();
    // sets the enterprise user extension to required
    for ( JsonNode schemaExtension : schemaExtensions )
    {
      JsonHelper.addAttribute(schemaExtension, AttributeNames.RFC7643.REQUIRED, BooleanNode.valueOf(true));
    }
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                     HttpMethod.POST);
    Assertions.assertDoesNotThrow(() -> requestResourceValidator.validateDocument(user));

    String errorMessage = "Required extension 'urn:ietf:params:scim:schemas:extension:enterprise:2.0:User' is missing";
    Assertions.assertTrue(requestResourceValidator.getValidationContext().hasErrors());
    MatcherAssert.assertThat(requestResourceValidator.getValidationContext().getErrors(),
                             Matchers.containsInAnyOrder(errorMessage));
  }

  /**
   * this test will have an "always" returned attribute that is not present in the response because it is not a
   * required attribute. For this reason the validation will not throw an exception but print a log message.
   * This test is for code coverage and checking for NullPointerExceptions or similar
   */
  @ParameterizedTest
  @ValueSource(strings = {"ALWAYS", "REQUEST", "DEFAULT"})
  public void testNonPresentReturnedAttribute(Returned returned)
  {
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       AttributeNames.RFC7643.NICK_NAME,
                                       null,
                                       null,
                                       returned,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);

    Schema userSchema = new Schema(userSchemaNode);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      String attributeName = AttributeNames.RFC7643.NICK_NAME;
      List<SchemaAttribute> attributes = Collections.singletonList(userSchema.getSchemaAttribute(attributeName));
      return new ResponseSchemaValidator(serviceProvider, User.class, attributes, null, null,
                                         referenceUrlSupplier).validateDocument(userSchema, user);
    });

    JsonNode nickName = validatedDocument.get(AttributeNames.RFC7643.NICK_NAME);
    Assertions.assertNull(nickName);
    JsonNode id = validatedDocument.get(AttributeNames.RFC7643.ID);
    Assertions.assertNotNull(id);
    JsonNode userName = validatedDocument.get(AttributeNames.RFC7643.USER_NAME);
    Assertions.assertEquals(user.get(AttributeNames.RFC7643.USER_NAME), userName);
    JsonNode externalId = validatedDocument.get(AttributeNames.RFC7643.EXTERNAL_ID);
    Assertions.assertNull(externalId);
    JsonNode description = validatedDocument.get(AttributeNames.RFC7643.DESCRIPTION);
    Assertions.assertNull(description);
    JsonNode display = validatedDocument.get(AttributeNames.RFC7643.DISPLAY);
    Assertions.assertNull(display);
    JsonNode phoneNumbers = validatedDocument.get(AttributeNames.RFC7643.PHONE_NUMBERS);
    Assertions.assertNull(phoneNumbers);
    JsonNode emails = validatedDocument.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNull(emails);
    JsonNode groups = validatedDocument.get(AttributeNames.RFC7643.GROUPS);
    Assertions.assertNull(groups);
    JsonNode roles = validatedDocument.get(AttributeNames.RFC7643.ROLES);
    Assertions.assertNull(roles);
  }

  /**
   * this test will verify that attributes whos returned value is {@link Returned#REQUEST} will not be returned
   * until they have been requested explicitly
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.USER_NAME, AttributeNames.RFC7643.NAME,
                          AttributeNames.RFC7643.DISPLAY_NAME, AttributeNames.RFC7643.EMAILS,
                          AttributeNames.RFC7643.PHONE_NUMBERS,
                          AttributeNames.RFC7643.PHONE_NUMBERS + "." + AttributeNames.RFC7643.VALUE,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.GIVEN_NAME})
  public void testDoNotReturnRequestAttributes(String attributeName)
  {
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE);

    Schema userSchema = new Schema(userSchemaNode);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseSchemaValidator(serviceProvider, User.class, null, null, null,
                                         referenceUrlSupplier).validateDocument(userSchema, user);
    });
    String[] attributeNameParts = attributeName.split("\\.");
    if (attributeNameParts.length == 1)
    {
      Assertions.assertNull(validatedDocument.get(attributeName));
    }
    else
    {
      JsonNode complexAttribute = validatedDocument.get(attributeNameParts[0]);
      Assertions.assertNotNull(complexAttribute);
      Assertions.assertNull(complexAttribute.get(attributeNameParts[1]));
    }
  }

  /**
   * same as {@link #testRequestAttributeIsReturnedAfterPutPostOrPatchRequest} but this time the check is for a
   * schema extension and not the main resource
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.EMPLOYEE_NUMBER, AttributeNames.RFC7643.COST_CENTER,
                          AttributeNames.RFC7643.ORGANIZATION, AttributeNames.RFC7643.DIVISION,
                          AttributeNames.RFC7643.DEPARTMENT, AttributeNames.RFC7643.MANAGER})
  public void testRequestAttributeIsReturnedAfterPutPostOrPatchRequestForExtension(String attributeName)
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(enterpriseUserExtension,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    TestHelper.addMetaToDocument(user);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                           referenceUrlSupplier).validateDocument(user);
    });
    JsonNode enterpriseUser = validatedDocument.get(SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertNotNull(enterpriseUser);
    JsonNode attribute = enterpriseUser.get(attributeName);
    Assertions.assertNull(attribute);
  }

  /**
   * this test shall verify that the attributes from an extension are not removed if they have a returned value
   * of request and they are present in the attributes parameter
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.EMPLOYEE_NUMBER, AttributeNames.RFC7643.COST_CENTER,
                          AttributeNames.RFC7643.ORGANIZATION, AttributeNames.RFC7643.DIVISION,
                          AttributeNames.RFC7643.DEPARTMENT, AttributeNames.RFC7643.MANAGER})
  public void testKeepRequestAttributesInResponseOnExtension(String attributeName)
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(enterpriseUserExtension,
                                       attributeName,
                                       null,
                                       null,
                                       Returned.REQUEST,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode user = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    TestHelper.addMetaToDocument(user);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      List<SchemaAttribute> attributes = getSchemaAttributes(resourceType, attributeName);
      return new ResponseResourceValidator(serviceProvider, resourceType, attributes, null, null,
                                           referenceUrlSupplier).validateDocument(user);
    });
    JsonNode enterpriseUser = validatedDocument.get(SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertNotNull(enterpriseUser);
    JsonNode attribute = enterpriseUser.get(attributeName);
    Assertions.assertNotNull(attribute);
  }

  /**
   * this test will provoke that all attributes from the enterprise user extension are removed from the response
   * which causes the extension itself to be removed. In such a case the enterprise schema uri must also be
   * removed from the schemas-attribute of the main document
   */
  @Test
  public void testRemoveAttributesFromResponseOnExtension()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    Arrays.asList(AttributeNames.RFC7643.EMPLOYEE_NUMBER,
                  AttributeNames.RFC7643.COST_CENTER,
                  AttributeNames.RFC7643.ORGANIZATION,
                  AttributeNames.RFC7643.DIVISION,
                  AttributeNames.RFC7643.DEPARTMENT,
                  AttributeNames.RFC7643.MANAGER)
          .forEach(attributeName -> {

            TestHelper.modifyAttributeMetaData(enterpriseUserExtension,
                                               attributeName,
                                               null,
                                               null,
                                               Returned.REQUEST,
                                               null,
                                               null,
                                               null,
                                               null,
                                               null);
          });
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    TestHelper.addMetaToDocument(userSchema);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                           referenceUrlSupplier).validateDocument(userSchema);
    });
    JsonNode enterpriseUser = validatedDocument.get(SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertNull(enterpriseUser);
    List<String> schemas = JsonHelper.getSimpleAttributeArray(validatedDocument, AttributeNames.RFC7643.SCHEMAS).get();
    MatcherAssert.assertThat(schemas, Matchers.not(Matchers.hasItem(SchemaUris.ENTERPRISE_USER_URI)));
  }

  /**
   * Verifies that excluded attributes are removed from the resource
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.RFC7643.DISPLAY_NAME, AttributeNames.RFC7643.EXTERNAL_ID,
                          AttributeNames.RFC7643.EMAILS,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.GIVEN_NAME,
                          AttributeNames.RFC7643.NAME + "." + AttributeNames.RFC7643.MIDDLE_NAME})
  public void testExcludedAttributes(String excludedAttributeName)
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    TestHelper.addMetaToDocument(userSchema);
    List<SchemaAttribute> excludedAttributes = getSchemaAttributes(resourceType, excludedAttributeName);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseResourceValidator(serviceProvider, resourceType, null, excludedAttributes, null,
                                           referenceUrlSupplier).validateDocument(userSchema);
    });
    for ( SchemaAttribute schemaAttribute : excludedAttributes )
    {
      String[] attributeNameParts = schemaAttribute.getScimNodeName().split("\\.");
      if (attributeNameParts.length == 1)
      {
        Assertions.assertNull(validatedDocument.get(schemaAttribute.getName()), validatedDocument.toString());
      }
      else
      {
        JsonNode complexAttribute = validatedDocument.get(attributeNameParts[0]);
        Assertions.assertNotNull(complexAttribute, schemaAttribute.toString());
        Assertions.assertNull(complexAttribute.get(attributeNameParts[1]), schemaAttribute.toString());
      }
    }
  }

  /**
   * Verifies that a required attribute can be excluded from the resource if the returned-type is default
   */
  @DisplayName("Required excluded attribute is removed from resource if returned=default")
  @Test
  public void testRequiredExcludedAttributeIsRemovedIfReturnedIsDefault()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    User userResource = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    TestHelper.addMetaToDocument(userResource);
    List<SchemaAttribute> excludedAttributes = getSchemaAttributes(resourceType, AttributeNames.RFC7643.USER_NAME);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseResourceValidator(serviceProvider, resourceType, null, excludedAttributes, null,
                                           referenceUrlSupplier).validateDocument(userResource);
    });
    JsonNode userNameNode = validatedDocument.get(AttributeNames.RFC7643.USER_NAME);
    Assertions.assertNull(userNameNode);
  }

  /**
   * Verifies that complex attributes are removed if mentioned on first level e.g. "name"
   */
  @Test
  public void testExcludedNameAttribute()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    TestHelper.addMetaToDocument(userSchema);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      List<SchemaAttribute> excludedAttributes = getSchemaAttributes(resourceType, AttributeNames.RFC7643.NAME);
      return new ResponseResourceValidator(serviceProvider, resourceType, null, excludedAttributes, null,
                                           referenceUrlSupplier).validateDocument(userSchema);
    });
    Assertions.assertNull(validatedDocument.get(AttributeNames.RFC7643.NAME));
  }

  private List<SchemaAttribute> getSchemaAttributes(ResourceType resourceType, String attributeName)
  {
    return resourceType.getAllSchemas()
                       .stream()
                       .flatMap(schema -> schema.getAttributes().stream())
                       .filter(schemaAttribute -> schemaAttribute.getScimNodeName().equals(attributeName))
                       .collect(Collectors.toList());
  }

  /**
   * Verifies that the meta attribute is not present if not requested on response
   */
  @Test
  public void testAttributesParameterSetAndMetaNotPresent()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);
    TestHelper.addMetaToDocument(userSchema);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      List<SchemaAttribute> attributes = getSchemaAttributes(resourceType, AttributeNames.RFC7643.NAME);
      return new ResponseResourceValidator(serviceProvider, resourceType, attributes, null, null,
                                           referenceUrlSupplier).validateDocument(userSchema);
    });
    Assertions.assertNotNull(validatedDocument.get(AttributeNames.RFC7643.NAME));
    JsonNode meta = validatedDocument.get(AttributeNames.RFC7643.META);
    Assertions.assertNull(meta);
  }

  /**
   * Verifies that excluded attributes are removed from extensions
   */
  @Test
  public void testExcludedAttributesOnExtension()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    List<SchemaAttribute> excludedAttributes = Stream.of(AttributeNames.RFC7643.EMPLOYEE_NUMBER,
                                                         AttributeNames.RFC7643.COST_CENTER,
                                                         AttributeNames.RFC7643.ORGANIZATION,
                                                         AttributeNames.RFC7643.DIVISION,
                                                         AttributeNames.RFC7643.DEPARTMENT,
                                                         AttributeNames.RFC7643.MANAGER)
                                                     .flatMap(s -> getSchemaAttributes(resourceType, s).stream())
                                                     .collect(Collectors.toList());
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);
    TestHelper.addMetaToDocument(userSchema);
    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new ResponseResourceValidator(serviceProvider, resourceType, null, excludedAttributes, null,
                                           referenceUrlSupplier).validateDocument(userSchema);
    });
    JsonNode enterpriseUser = validatedDocument.get(SchemaUris.ENTERPRISE_USER_URI);
    Assertions.assertNull(enterpriseUser);
    List<String> schemas = JsonHelper.getSimpleAttributeArray(validatedDocument, AttributeNames.RFC7643.SCHEMAS).get();
    MatcherAssert.assertThat(schemas, Matchers.not(Matchers.hasItem(SchemaUris.ENTERPRISE_USER_URI)));
  }

  /**
   * this test will verify that a {@link DocumentValidationException} is thrown if an object attribute is set as
   * an array attribute in the document that is validated
   */
  @Test
  public void testGotArrayInsteadOfObject()
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    List<Name> nameList = Arrays.asList(Name.builder().familyName("norris").build(),
                                        Name.builder().familyName("goldfish").build());
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    nameList.forEach(arrayNode::add);
    user.set(AttributeNames.RFC7643.NAME, arrayNode);
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .location("/Users")
                    .build();
    user.setMeta(meta);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    Assertions.assertNotNull(resourceType);
    try
    {
      new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                    referenceUrlSupplier).validateDocument(user);
      Assertions.fail("the schema validation must fail. The name attribute is not of type object!");
    }
    catch (DocumentValidationException ex)
    {
      String errorMessage = "Attribute 'urn:ietf:params:scim:schemas:core:2.0:User:name' must be of type object "
                            + "but is '[{\"familyName\":\"norris\"},{\"familyName\":\"goldfish\"}]'";
      MatcherAssert.assertThat(ex.getDetail(), Matchers.containsString(errorMessage));
    }
  }

  /**
   * this test will verify that a multivalued complex type is automatically converted into an array if sent as
   * an object type
   */
  @Test
  public void testGotObjectInsteadOfArray()
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .location("/Users")
                    .build();
    user.set(AttributeNames.RFC7643.EMAILS, Email.builder().value("abc@abc.de").build());
    user.setMeta(meta);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    Assertions.assertNotNull(resourceType);
    User validatedUser = (User)new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                                             referenceUrlSupplier).validateDocument(user);

    JsonNode emailsNode = validatedUser.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emailsNode);
    Assertions.assertTrue(emailsNode.isArray());
    Assertions.assertEquals(1, emailsNode.size());
  }

  /**
   * this test will verify that a {@link DocumentValidationException} is thrown if an int attribute is set as a
   * string attribute in the document that is validated
   */
  @Test
  public void testGotIntegerInsteadOfString()
  {
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    User userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .location("/Users")
                    .build();
    userSchema.setMeta(meta);
    userSchema.set(AttributeNames.RFC7643.USER_NAME, new IntNode(5));
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    Assertions.assertNotNull(resourceType);
    try
    {
      new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                    referenceUrlSupplier).validateDocument(userSchema);
      Assertions.fail("the schema validation must fail. The userName attribute is not of type string!");
    }
    catch (DocumentValidationException ex)
    {
      String errorMessage = "Value of attribute 'urn:ietf:params:scim:schemas:core:2.0:User:userName' is not of type "
                            + "'string' but of type 'number' with value '5'";
      MatcherAssert.assertThat(ex.getDetail(), Matchers.containsString(errorMessage));
    }
  }

  /**
   * this test will verify that a field that is defined as multivalued attribute can be set as simple single
   * valued attribute and is then converted automatically into a one-size array.<br>
   * <br>
   *
   * <pre>
   *   "strings": "123456"
   * </pre>
   *
   * should become
   *
   * <pre>
   *   "strings": ["123456"]
   * </pre>
   */
  @Test
  public void testSimpleSingleValueAttributeAsMultiValuedAttribute()
  {
    JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_VALIDATION_SCHEMA);
    JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         allTypesResourceTypeJson,
                                                                         allTypesValidationSchema,
                                                                         enterpriseUserValidationSchema);
    SchemaAttribute stringArrayAttribute = resourceType.getMainSchema().getSchemaAttribute("stringArray");
    stringArrayAttribute.setMinItems(null); // do not require a minimum array size default is set to 2 here

    AllTypes allTypes = new AllTypes(true);
    // see method AllTypes#setStringArray
    allTypes.set("stringArray", new TextNode("123456"));

    successfulValidationForRequest(resourceType, allTypes);
  }

  /**
   * this test will verify that the attribute validation is executed correctly
   */
  @TestFactory
  public List<DynamicTest> testSchemaValidatorWithAttributeValidation()
  {
    JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_VALIDATION_SCHEMA);
    JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         allTypesResourceTypeJson,
                                                                         allTypesValidationSchema,
                                                                         enterpriseUserValidationSchema);

    List<DynamicTest> dynamicTests = new ArrayList<>();

    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple string validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setString("12345");
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setString("0123456789");
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple string validation fails (too short)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setString("1");
      String errorMessage = "The 'STRING'-attribute 'string' with value '1' must have a minimum length of '5' "
                            + "characters but is '1' characters long";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple string validation fails (too long)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setString("01234567890");
      String errorMessage = "The 'STRING'-attribute 'string' with value '01234567890' must not be longer than "
                            + "'10' characters but is '11' characters long";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple string validation fails (pattern mismatch)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setString("abcdefg");
      String errorMessage = "The 'STRING'-attribute 'string' with value 'abcdefg' must match the regular expression "
                            + "of '[0-9]+'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple number validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setNumber(2L);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setNumber(10L);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setNumber(6L);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple number validation fails (value too low)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setNumber(1L);
      String errorMessage = "The 'INTEGER'-attribute 'number' with value '1' must have at least a value of '2'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple number validation fails (value too high)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setNumber(11L);
      String errorMessage = "The 'INTEGER'-attribute 'number' with value '11' must not be greater than '10'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple number validation fails (not multipleOf)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setNumber(7L);
      String errorMessage = "The 'INTEGER'-attribute 'number' with value '7' must be a multiple of '2.0'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple decimal validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDecimal(11.34);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setDecimal(147.42);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setDecimal(28.35);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple decimal validation fails (too low)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDecimal(0.0);
      String errorMessage = "The 'DECIMAL'-attribute 'decimal' with value '0.0' must have at least a value of '10.8'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple decimal validation fails (too high)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDecimal(153.09);
      String errorMessage = "The 'DECIMAL'-attribute 'decimal' with value '153.09' must not be greater than '150.9'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple decimal validation fails (not multipleOf)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDecimal(150.9);
      String errorMessage = "The 'DECIMAL'-attribute 'decimal' with value '150.9' must be a multiple of '5.67'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple date validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDate(Instant.parse("2018-11-01T00:00:00Z"));
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setDate(Instant.parse("2020-12-01T00:00:00Z"));
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple date validation fails (before)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDate(Instant.parse("2018-11-01T00:00:00Z").minusSeconds(1));
      String errorMessage = "The 'DATE_TIME'-attribute 'date' with value '2018-10-31T23:59:59Z' must not "
                            + "be before '2018-11-01T00:00:00Z'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("simple date validation fails (after)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setDate(Instant.parse("2020-12-01T00:00:00Z").plusSeconds(1));
      String errorMessage = "The 'DATE_TIME'-attribute 'date' with value '2020-12-01T00:00:01Z' must not be "
                            + "after '2020-12-01T00:00:00Z'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("string array validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setStringArray(Arrays.asList("123456", "1234567"));
      successfulValidationForResponse(resourceType, allTypes);
      successfulValidationForRequest(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("string array validation fails (not enough items)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setStringArray(Arrays.asList("123456"));
      String errorMessage = "The 'ARRAY'-attribute 'stringArray' with value '[\"123456\"]' must have at least '2' "
                            + "items but only '1' items are present";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("string array validation fails (too many items)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setStringArray(Arrays.asList("123456", "12345", "1234567", "0123456", "013654", "987654"));
      String errorMessage = "The 'ARRAY'-attribute 'stringArray' with value '[\"123456\",\"12345\",\"1234567\","
                            + "\"0123456\",\"013654\",\"987654\"]' must not have more than '5' items. Items found '6'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("string array validation fails (string too short)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      allTypes.setStringArray(Arrays.asList("123", "12345", "1234567", "0123456", "013654", "987654"));

      final String errorMessage1 = "Found unsupported value in multivalued attribute "
                                   + "'[\"123\",\"12345\",\"1234567\",\"0123456\",\"013654\",\"987654\"]'";
      final String errorMessage2 = "The 'STRING'-attribute 'stringArray' with value '123' must have a minimum length "
                                   + "of '5' characters but is '3' characters long";
      String[] errorMessages = new String[]{errorMessage1, errorMessage2};
      failValidationForRequest(resourceType, allTypes, errorMessages);
      failValidationForResponse(resourceType, allTypes, errorMessage2);
    }));
    /* *********************************************************************************************************/
    /* *********************************************************************************************************/
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setString("12345");
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      complex.setString("0123456789");
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string validation fails (too short)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setString("1");
      String errorMessage = "The 'STRING'-attribute 'complex.string' with value '1' must have a minimum length "
                            + "of '5' characters but is '1' characters long";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string validation fails (too long)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setString("01234567890");
      String errorMessage = "The 'STRING'-attribute 'complex.string' with value '01234567890' must not be longer "
                            + "than '10' characters but is '11' characters long";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string validation fails (pattern mismatch)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setString("abcdefg");
      String errorMessage = "The 'STRING'-attribute 'complex.string' with value 'abcdefg' must match the "
                            + "regular expression of '[0-9]+'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex number validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setNumber(2L);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      complex.setNumber(10L);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      complex.setNumber(6L);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex number validation fails (value too low)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setNumber(1L);
      String errorMessage = "The 'INTEGER'-attribute 'complex.number' with value '1' must have at least a value "
                            + "of '2'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex number validation fails (value too high)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setNumber(11L);
      String errorMessage = "The 'INTEGER'-attribute 'complex.number' with value '11' must not be greater than '10'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex number validation fails (not multipleOf)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setNumber(7L);
      String errorMessage = "The 'INTEGER'-attribute 'complex.number' with value '7' must be a multiple of '2.0'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex decimal validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDecimal(11.34);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      complex.setDecimal(147.42);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      complex.setDecimal(28.35);
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex decimal validation fails (too low)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDecimal(0.0);
      String errorMessage = "The 'DECIMAL'-attribute 'complex.decimal' with value '0.0' must have at least a value "
                            + "of '10.8'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex decimal validation fails (too high)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDecimal(153.09);
      String errorMessage = "The 'DECIMAL'-attribute 'complex.decimal' with value '153.09' must not be greater "
                            + "than '150.9'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex decimal validation fails (not multipleOf)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDecimal(150.9);
      String errorMessage = "The 'DECIMAL'-attribute 'complex.decimal' with value '150.9' must be a multiple of '5.67'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex date validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDate(Instant.parse("2018-11-01T00:00:00Z"));
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      complex.setDate(Instant.parse("2020-12-01T00:00:00Z"));
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex date validation fails (before)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDate(Instant.parse("2018-11-01T00:00:00Z").minusSeconds(1));
      String errorMessage = "The 'DATE_TIME'-attribute 'complex.date' with value '2018-10-31T23:59:59Z' must not "
                            + "be before '2018-11-01T00:00:00Z'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex date validation fails (after)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setDate(Instant.parse("2020-12-01T00:00:00Z").plusSeconds(1));
      String errorMessage = "The 'DATE_TIME'-attribute 'complex.date' with value '2020-12-01T00:00:01Z' must not "
                            + "be after '2020-12-01T00:00:00Z'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string array validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setStringArray(Arrays.asList("123456", "1234567"));
      successfulValidationForResponse(resourceType, allTypes);
      successfulValidationForRequest(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string array validation fails (not enough items)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setStringArray(Arrays.asList("123456"));
      String errorMessage = "The 'ARRAY'-attribute 'complex.stringArray' with value '[\"123456\"]' must have at "
                            + "least '2' items but only '1' items are present";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string array validation fails (too many items)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setStringArray(Arrays.asList("123456", "12345", "1234567", "0123456", "013654", "987654"));
      String errorMessage = "The 'ARRAY'-attribute 'complex.stringArray' with value '[\"123456\",\"12345\","
                            + "\"1234567\",\"0123456\",\"013654\",\"987654\"]' must not have more than '5' items. "
                            + "Items found '6'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("complex string array validation fails (string too short)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setStringArray(Arrays.asList("123", "12345", "1234567", "0123456", "013654", "987654"));
      final String errorMessage1 = "Found unsupported value in multivalued attribute "
                                   + "'[\"123\",\"12345\",\"1234567\",\"0123456\",\"013654\",\"987654\"]'";
      final String errorMessage2 = "The 'STRING'-attribute 'complex.stringArray' with value '123' must have a minimum "
                                   + "length of '5' characters but is '3' characters long";
      String[] errorMessages = new String[]{errorMessage1, errorMessage2};
      failValidationForRequest(resourceType, allTypes, errorMessages);
      failValidationForResponse(resourceType, allTypes, errorMessage2);
    }));
    /* *********************************************************************************************************/
    /* *********************************************************************************************************/
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("multiComplex string validation succeeds", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      complex.setString("12345");
      allTypes.setMultiComplex(Arrays.asList(complex, complex));
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
      allTypes.setMultiComplex(Arrays.asList(complex, complex, complex));
      successfulValidationForRequest(resourceType, allTypes);
      successfulValidationForResponse(resourceType, allTypes);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("multiComplex validation fails (not enough items)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      complex.setString("123456");
      allTypes.setMultiComplex(Arrays.asList(complex));
      String errorMessage = "The 'ARRAY'-attribute 'multiComplex' with value '[{\"string\":\"123456\"}]' must "
                            + "have at least '2' items but only '1' items are present";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    /* *********************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("multiComplex validation fails (too many items)", () -> {
      AllTypes allTypes = buildAllTypesForValidation();
      AllTypes complex = new AllTypes();
      complex.setString("123456");
      allTypes.setMultiComplex(Arrays.asList(complex, complex, complex, complex));
      String errorMessage = "The 'ARRAY'-attribute 'multiComplex' with value '[{\"string\":\"123456\"},"
                            + "{\"string\":\"123456\"},{\"string\":\"123456\"},{\"string\":\"123456\"}]' must not "
                            + "have more than '3' items. Items found '4'";
      failValidationForRequest(resourceType, allTypes, errorMessage);
      failValidationForResponse(resourceType, allTypes, errorMessage);
    }));
    return dynamicTests;
  }

  @DisplayName("multiComplex string validation fails (too long)")
  @Test
  public void testMulticomplexStringValidationFailsForTooLong()
  {
    JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_VALIDATION_SCHEMA);
    JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         allTypesResourceTypeJson,
                                                                         allTypesValidationSchema,
                                                                         enterpriseUserValidationSchema);

    AllTypes allTypes = buildAllTypesForValidation();
    AllTypes complex = new AllTypes();
    allTypes.setMultiComplex(Arrays.asList(complex, complex));
    complex.setString("01234567890");

    final String errorMessage1 = "Found unsupported value in multivalued complex attribute "
                                 + "'[{\"string\":\"01234567890\"},{\"string\":\"01234567890\"}]'";
    final String errorMessage2 = "The 'STRING'-attribute 'multiComplex.string' with value '01234567890' must not be "
                                 + "longer than '10' characters but is '11' characters long";

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                     HttpMethod.POST);
    requestResourceValidator.validateDocument(allTypes);

    ValidationContext validationContext = requestResourceValidator.getValidationContext();
    Assertions.assertTrue(validationContext.hasErrors());

    Assertions.assertEquals(2,
                            validationContext.getFieldErrors().size(),
                            validationContext.getFieldErrors().toString());

    SchemaAttribute multiComplexString = resourceType.getSchemaAttribute("multiComplex.string").get();
    SchemaAttribute multiComplex = multiComplexString.getParent();

    {
      List<String> fieldErrorMessages = validationContext.getFieldErrors().get(multiComplex.getScimNodeName());
      Assertions.assertEquals(1, fieldErrorMessages.size());
      Assertions.assertEquals(errorMessage1, fieldErrorMessages.get(0));
    }
    {
      List<String> fieldErrorMessages = validationContext.getFieldErrors().get(multiComplexString.getScimNodeName());
      Assertions.assertEquals(1, fieldErrorMessages.size());
      Assertions.assertEquals(errorMessage2, fieldErrorMessages.get(0));
    }
  }

  /**
   * verifies that no exception occurs if the given document is validated for request
   */
  private void successfulValidationForRequest(ResourceType resourceType, AllTypes allTypes)
  {
    Assertions.assertDoesNotThrow(() -> new RequestResourceValidator(serviceProvider, resourceType,
                                                                     HttpMethod.POST).validateDocument(allTypes));
  }

  /**
   * verifies that no exception occurs if the given document is validated for response
   */
  private void successfulValidationForResponse(ResourceType resourceType, AllTypes allTypes)
  {
    Assertions.assertDoesNotThrow(() -> new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                                                      referenceUrlSupplier).validateDocument(allTypes));
  }

  /**
   * verifies that an exception occurs if the given document is validated for response
   */
  private void failValidationForResponse(ResourceType resourceType, AllTypes allTypes, String errorMessage)
  {
    try
    {
      new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                    referenceUrlSupplier).validateDocument(allTypes);
      Assertions.fail("this point must not be reached");
    }
    catch (DocumentValidationException ex)
    {
      Assertions.assertEquals(errorMessage, ex.getDetail());
      Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }
  }

  /**
   * verifies that an exception occurs if the given document is validated for request
   */
  private void failValidationForRequest(ResourceType resourceType, AllTypes allTypes, String... errorMessages)
  {
    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                     HttpMethod.POST);
    requestResourceValidator.validateDocument(allTypes);

    ValidationContext validationContext = requestResourceValidator.getValidationContext();
    Assertions.assertTrue(validationContext.hasErrors());

    Assertions.assertEquals(1,
                            validationContext.getFieldErrors().size(),
                            validationContext.getFieldErrors().toString());
    List<String> fieldErrorMessages = validationContext.getFieldErrors()
                                                       .get(validationContext.getFieldErrors()
                                                                             .keySet()
                                                                             .iterator()
                                                                             .next());
    Assertions.assertEquals(errorMessages.length, fieldErrorMessages.size(), fieldErrorMessages.toString());
    MatcherAssert.assertThat(fieldErrorMessages,
                             Matchers.containsInAnyOrder(Arrays.stream(errorMessages)
                                                               .map(Matchers::equalTo)
                                                               .collect(Collectors.toList())));
  }

  /**
   * builds a standard all types instance that will pass validation with its required default attributes
   */
  private AllTypes buildAllTypesForValidation()
  {
    AllTypes allTypes = new AllTypes(true);
    allTypes.setMeta(Meta.builder()
                         .resourceType("AllTypes")
                         .created(Instant.now())
                         .lastModified(Instant.now())
                         .location("http://localhost")
                         .build());
    return allTypes;
  }

  /**
   * this test will assert that an exception is thrown if the client or developer sets a multi valued complex as
   * a simple array type.
   */
  @Test
  public void testFailIfMultiValuedComplexIsMalformed()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);

    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         userResourceType,
                                                                         userResourceSchema,
                                                                         enterpriseUserExtension);
    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(Instant.now())
                    .lastModified(Instant.now())
                    .location("http://localhost")
                    .build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    ArrayNode email = new ArrayNode(JsonNodeFactory.instance);
    email.add("hello world");
    user.set(AttributeNames.RFC7643.EMAILS, email);


    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(serviceProvider, resourceType,
                                                                                     HttpMethod.POST);
    Assertions.assertDoesNotThrow(() -> requestResourceValidator.validateDocument(user));

    Assertions.assertTrue(requestResourceValidator.getValidationContext().hasErrors());
    MatcherAssert.assertThat(requestResourceValidator.getValidationContext().getErrors(), Matchers.empty());
    Assertions.assertEquals(1, requestResourceValidator.getValidationContext().getFieldErrors().size());
    List<String> fieldErrors = requestResourceValidator.getValidationContext()
                                                       .getFieldErrors()
                                                       .get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(fieldErrors);
    Assertions.assertEquals(1, fieldErrors.size());
    String expectedErrorMessage = "Attribute 'urn:ietf:params:scim:schemas:core:2.0:User:emails' is expected to "
                                  + "hold only complex attributes but is '[\"hello world\"]'";
    MatcherAssert.assertThat(fieldErrors, Matchers.hasItem(expectedErrorMessage));
  }

  /**
   * verifies that the document does not throw any exceptions anymore if the response-document evaluates to an
   * empty document. RFC7644 Tells us that a body SHOULD be returned but it must not
   */
  @Test
  public void testValidateReceivedDocumentToEmptyDocumentForResponse()
  {
    JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_VALIDATION_SCHEMA);
    JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

    ResourceType resourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                         allTypesResourceTypeJson,
                                                                         allTypesValidationSchema,
                                                                         enterpriseUserValidationSchema);

    AllTypes allTypes = buildAllTypesForValidation();
    Assertions.assertNull(new ResponseResourceValidator(serviceProvider, resourceType, null, null, null,
                                                        referenceUrlSupplier).validateDocument(allTypes));
  }

  /**
   * verifies that a patch operation is accepting a json boolean on schema validation
   */
  @Test
  public void testPatchOpValidationWithBoolean()
  {
    Schema patchSchema = resourceTypeFactory.getSchemaFactory().getMetaSchema(SchemaUris.PATCH_OP);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("bool")
                                                                                .valueNode(BooleanNode.getTrue())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, ScimObjectNode.class,
                                        HttpMethod.PATCH).validateDocument(patchSchema, patchOpRequest);
    });
  }

  /**
   * verifies that a patch operation is accepting a json integer on schema validation
   */
  @Test
  public void testPatchOpValidationWithInteger()
  {
    Schema patchSchema = resourceTypeFactory.getSchemaFactory().getMetaSchema(SchemaUris.PATCH_OP);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("bool")
                                                                                .valueNode(new IntNode(6))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Assertions.assertDoesNotThrow(() -> {
      return new RequestSchemaValidator(serviceProvider, ScimObjectNode.class,
                                        HttpMethod.PATCH).validateDocument(patchSchema, patchOpRequest);
    });
  }

  @DisplayName("Test validation with default values on schema-request-validation")
  @Nested
  public class SchemaRequestValidationDefaultValueTests
  {

    private ResourceType allTypesResourceType;

    private SchemaAttribute testAttribute;

    private SchemaAttribute testSubAttribute;

    @BeforeEach
    public void initialize()
    {
      serviceProvider.setUseDefaultValuesOnRequest(true);
      serviceProvider.setUseDefaultValuesOnResponse(false);

      JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
      JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
      JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

      allTypesResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                      allTypesResourceTypeJson,
                                                                      allTypesValidationSchema,
                                                                      enterpriseUserValidationSchema);
      testAttribute = allTypesResourceType.getSchemaAttribute("string").get();
      testSubAttribute = allTypesResourceType.getSchemaAttribute("complex.string").get();
    }

    /**
     * verifies that if a string-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value is successfully assigned on required empty field")
    @Test
    public void testAssignRequiredStringDefaultValue()
    {
      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");
      testAttribute.setRequired(true);

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(testAttribute.getDefaultValue(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a string-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value is successfully assigned on empty field")
    @Test
    public void testAssignStringDefaultValue()
    {
      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(testAttribute.getDefaultValue(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * shows that the default-value is not set if the serviceProvider does not support it
     */
    @DisplayName("STRING-type default value is not set if not supported by ServiceProvider")
    @Test
    public void testAssignStringDefaultValueWithFeatureDisabled()
    {
      serviceProvider.setUseDefaultValuesOnRequest(false);

      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(1, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
    }

    /**
     * verifies that if a string-default value is not overriding a field that is present
     */
    @DisplayName("STRING-type default value does not override present value")
    @Test
    public void testAssignStringDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setString("hello");
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("hello", resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a reference-default value is assigned to an attribute that this value is set if the field
     * is left empty
     */
    @DisplayName("REFERENCE-type default value is successfully assigned on empty field")
    @Test
    public void testAssignReferenceDefaultValue()
    {
      testAttribute.setType(Type.REFERENCE);
      testAttribute.setReferenceTypes(Arrays.asList(ReferenceTypes.EXTERNAL));
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("world", resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a reference-default value is not overriding a field that is present
     */
    @DisplayName("REFERENCE-type default value does not override present value")
    @Test
    public void testAssignReferenceDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.REFERENCE);
      testAttribute.setReferenceTypes(Arrays.asList(ReferenceTypes.EXTERNAL));
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setString("hello");
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(allTypes.getString().get(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a datetime-default value is assigned to an attribute that this value is set if the field
     * is left empty
     */
    @DisplayName("DATETIME-type default value is successfully assigned on empty field")
    @Test
    public void testAssignDateTimeDefaultValue()
    {
      testAttribute.setType(Type.DATE_TIME);
      Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
      testAttribute.setDefaultValue(now.toString());

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(testAttribute.getDefaultValue(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a datetime-default value is not overriding a field that is present
     */
    @DisplayName("DATETIME-type default value does not override present value")
    @Test
    public void testAssignDatetimeDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.DATE_TIME);
      Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
      testAttribute.setDefaultValue(now.toString());

      Instant later = Instant.now().plusSeconds(50).truncatedTo(ChronoUnit.MILLIS);

      AllTypes allTypes = new AllTypes();
      allTypes.setString(later.toString());
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(later.toString(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a boolean-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("BOOLEAN-type default value is successfully assigned on empty field")
    @Test
    public void testAssignBooleanDefaultValue()
    {
      testAttribute.setType(Type.BOOLEAN);
      testAttribute.setDefaultValue(Boolean.TRUE.toString());

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(Boolean.parseBoolean(testAttribute.getDefaultValue()),
                              resource.get(testAttribute.getName()).booleanValue());
    }

    /**
     * verifies that if a boolean-default value is not overriding a field that is present
     */
    @DisplayName("BOOLEAN-type default value does not override present value")
    @Test
    public void testAssignBooleanDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.BOOLEAN);
      testAttribute.setDefaultValue("false");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", BooleanNode.TRUE);
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertTrue(resource.get(testAttribute.getName()).booleanValue());
    }

    /**
     * verifies that if a integer-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("INTEGER-type default value is successfully assigned on empty field")
    @Test
    public void testAssignIntegerDefaultValue()
    {
      testAttribute.setType(Type.INTEGER);
      testAttribute.setDefaultValue("55");

      AllTypes allTypes = new AllTypes();
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(55, resource.get(testAttribute.getName()).intValue());
    }

    /**
     * verifies that if a integer-default value is not overriding a field that is present
     */
    @DisplayName("INTEGER-type default value does not override present value")
    @Test
    public void testAssignIntegerDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.INTEGER);
      testAttribute.setDefaultValue("55");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", new IntNode(1));
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(1, resource.get(testAttribute.getName()).intValue());
    }

    /**
     * verifies that if a decimal-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("DECIMAL-type default value is successfully assigned on empty field")
    @Test
    public void testAssignDecimalDefaultValue()
    {
      testAttribute.setType(Type.DECIMAL);
      testAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", new DoubleNode(55.559));
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(55.559, resource.get(testAttribute.getName()).doubleValue());
    }

    /**
     * verifies that if a decimal-default value is not overriding a field that is present
     */
    @DisplayName("DECIMAL-type default value does not override present value")
    @Test
    public void testAssignDecimalDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.DECIMAL);
      testAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", new DoubleNode(88.79));
      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(88.79, resource.get(testAttribute.getName()).doubleValue());
    }


    /**
     * verifies that if a decimal-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value is successfully assigned on empty-complex sub-attribute")
    @Test
    public void testAssignStringDefaultValueOnSubAttribute()
    {
      testSubAttribute.setType(Type.STRING);
      testSubAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();

      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);

      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);
      log.warn(resource.toPrettyString());
      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("55.559", resource.get("complex").get(testSubAttribute.getName()).textValue());
    }

    /**
     * verifies that if a decimal-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value does not override present-complex sub-attribute")
    @Test
    public void testAssignStringDefaultValueOnPresentSubAttribute()
    {
      testSubAttribute.setType(Type.STRING);
      testSubAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");

      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setString("hello-world");

      ObjectNode resource = new RequestResourceValidator(serviceProvider, allTypesResourceType,
                                                         HttpMethod.POST).validateDocument(allTypes);

      log.warn(resource.toPrettyString());
      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("hello-world", resource.get("complex").get(testSubAttribute.getName()).textValue());
    }
  }

  @DisplayName("Test validation with default values on schema-response-validation")
  @Nested
  public class SchemaResponseValidationDefaultValueTests
  {

    private ResourceType allTypesResourceType;

    private SchemaAttribute testAttribute;

    private SchemaAttribute testSubAttribute;

    @BeforeEach
    public void initialize()
    {
      serviceProvider.setUseDefaultValuesOnRequest(false);
      serviceProvider.setUseDefaultValuesOnResponse(true);

      JsonNode allTypesResourceTypeJson = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
      JsonNode allTypesValidationSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
      JsonNode enterpriseUserValidationSchema = JsonHelper.loadJsonDocument(ENTERPRISE_USER_VALIDATION_SCHEMA);

      allTypesResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(false),
                                                                      allTypesResourceTypeJson,
                                                                      allTypesValidationSchema,
                                                                      enterpriseUserValidationSchema);
      testAttribute = allTypesResourceType.getSchemaAttribute("string").get();
      testSubAttribute = allTypesResourceType.getSchemaAttribute("complex.string").get();
    }

    /**
     * verifies that if a string-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("default value is successfully assigned on response on required-attribute")
    @Test
    public void testAssignDefaultValueToRequiredAttribute()
    {
      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");
      testAttribute.setRequired(true);

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(testAttribute.getDefaultValue(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a string-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value is successfully assigned on empty field")
    @Test
    public void testAssignStringDefaultValue()
    {
      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(testAttribute.getDefaultValue(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * shows that the default-value is not set if the serviceProvider does not support it
     */
    @DisplayName("STRING-type default value is not set if not supported by ServiceProvider")
    @Test
    public void testAssignStringDefaultValueWithFeatureDisabled()
    {
      serviceProvider.setUseDefaultValuesOnResponse(false);

      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(2, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
    }

    /**
     * verifies that if a string-default value is not overriding a field that is present
     */
    @DisplayName("STRING-type default value does not override present value")
    @Test
    public void testAssignStringDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.STRING);
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setString("hello");
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("hello", resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a reference-default value is assigned to an attribute that this value is set if the field
     * is left empty
     */
    @DisplayName("REFERENCE-type default value is successfully assigned on empty field")
    @Test
    public void testAssignReferenceDefaultValue()
    {
      testAttribute.setType(Type.REFERENCE);
      testAttribute.setReferenceTypes(Arrays.asList(ReferenceTypes.EXTERNAL));
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("world", resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a reference-default value is not overriding a field that is present
     */
    @DisplayName("REFERENCE-type default value does not override present value")
    @Test
    public void testAssignReferenceDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.REFERENCE);
      testAttribute.setReferenceTypes(Arrays.asList(ReferenceTypes.EXTERNAL));
      testAttribute.setDefaultValue("world");

      AllTypes allTypes = new AllTypes();
      allTypes.setString("hello");
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(allTypes.getString().get(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a datetime-default value is assigned to an attribute that this value is set if the field
     * is left empty
     */
    @DisplayName("DATETIME-type default value is successfully assigned on empty field")
    @Test
    public void testAssignDateTimeDefaultValue()
    {
      testAttribute.setType(Type.DATE_TIME);
      Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
      testAttribute.setDefaultValue(now.toString());

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(testAttribute.getDefaultValue(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a datetime-default value is not overriding a field that is present
     */
    @DisplayName("DATETIME-type default value does not override present value")
    @Test
    public void testAssignDatetimeDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.DATE_TIME);
      Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
      testAttribute.setDefaultValue(now.toString());

      Instant later = Instant.now().plusSeconds(50).truncatedTo(ChronoUnit.MILLIS);

      AllTypes allTypes = new AllTypes();
      allTypes.setString(later.toString());
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(later.toString(), resource.get(testAttribute.getName()).textValue());
    }

    /**
     * verifies that if a boolean-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("BOOLEAN-type default value is successfully assigned on empty field")
    @Test
    public void testAssignBooleanDefaultValue()
    {
      testAttribute.setType(Type.BOOLEAN);
      testAttribute.setDefaultValue(Boolean.TRUE.toString());

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(Boolean.parseBoolean(testAttribute.getDefaultValue()),
                              resource.get(testAttribute.getName()).booleanValue());
    }

    /**
     * verifies that if a boolean-default value is not overriding a field that is present
     */
    @DisplayName("BOOLEAN-type default value does not override present value")
    @Test
    public void testAssignBooleanDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.BOOLEAN);
      testAttribute.setDefaultValue("false");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", BooleanNode.TRUE);
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertTrue(resource.get(testAttribute.getName()).booleanValue());
    }

    /**
     * verifies that if a integer-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("INTEGER-type default value is successfully assigned on empty field")
    @Test
    public void testAssignIntegerDefaultValue()
    {
      testAttribute.setType(Type.INTEGER);
      testAttribute.setDefaultValue("55");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(55, resource.get(testAttribute.getName()).intValue());
    }

    /**
     * verifies that if a integer-default value is not overriding a field that is present
     */
    @DisplayName("INTEGER-type default value does not override present value")
    @Test
    public void testAssignIntegerDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.INTEGER);
      testAttribute.setDefaultValue("55");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", new IntNode(1));
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(1, resource.get(testAttribute.getName()).intValue());
    }

    /**
     * verifies that if a decimal-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("DECIMAL-type default value is successfully assigned on empty field")
    @Test
    public void testAssignDecimalDefaultValue()
    {
      testAttribute.setType(Type.DECIMAL);
      testAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", new DoubleNode(55.559));
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(55.559, resource.get(testAttribute.getName()).doubleValue());
    }

    /**
     * verifies that if a decimal-default value is not overriding a field that is present
     */
    @DisplayName("DECIMAL-type default value does not override present value")
    @Test
    public void testAssignDecimalDefaultValueDoesNotOverride()
    {
      testAttribute.setType(Type.DECIMAL);
      testAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.set("string", new DoubleNode(88.79));
      allTypes.setId("1");
      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals(88.79, resource.get(testAttribute.getName()).doubleValue());
    }

    /**
     * verifies that if a decimal-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value is successfully assigned on empty-complex sub-attribute")
    @Test
    public void testAssignStringDefaultValueOnSubAttribute()
    {
      testSubAttribute.setType(Type.STRING);
      testSubAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");

      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);

      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("55.559", resource.get("complex").get(testSubAttribute.getName()).textValue());
    }

    /**
     * verifies that if a decimal-default value is assigned to an attribute that this value is set if the field is
     * left empty
     */
    @DisplayName("STRING-type default value does not override present-complex sub-attribute")
    @Test
    public void testAssignStringDefaultValueOnPresentSubAttribute()
    {
      testSubAttribute.setType(Type.STRING);
      testSubAttribute.setDefaultValue("55.559");

      AllTypes allTypes = new AllTypes();
      allTypes.setId("1");

      AllTypes complex = new AllTypes();
      allTypes.setComplex(complex);
      complex.setString("hello-world");

      ObjectNode resource = new ResponseResourceValidator(serviceProvider, allTypesResourceType, null, null, null,
                                                          referenceUrlSupplier).validateDocument(allTypes);

      Assertions.assertEquals(3, resource.size());
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.ID));
      Assertions.assertNotNull(resource.get(AttributeNames.RFC7643.SCHEMAS));
      Assertions.assertEquals("hello-world", resource.get("complex").get(testSubAttribute.getName()).textValue());
    }
  }
}
