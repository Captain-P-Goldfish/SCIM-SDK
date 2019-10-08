package de.gold.scim.schemas;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.Returned;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.constants.enums.Uniqueness;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.resources.ScimNode;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
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
   * defines the schema - document pairs that should be validated
   */
  private static Stream<Arguments> getSchemaValidations()
  {
    return Stream.of(Arguments.of("check user schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON)),
                     Arguments.of("check enterprise user schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON)),
                     Arguments.of("check group schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON)),
                     Arguments.of("check user-resourceType schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON)),
                     Arguments.of("check group-resourceType schema definition",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON),
                                  JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON)),
                     Arguments.of("check enterprise-user validation",
                                  JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
                                  JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE)));
  }


  /**
   * will produce a number of timestamp arguments for testing date parsing on scim documents
   */
  private static Stream<Arguments> getTimeStampArguments()
  {
    return Stream.of(Arguments.of(OffsetDateTime.now().withNano(0).toString()),
                     Arguments.of(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(-10))
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_DATE_TIME)),
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
  public static void validateJsonNodeIsScimNode(JsonNode validatedDocument)
  {
    Assertions.assertTrue(validatedDocument instanceof ScimNode);
    ScimNode scimNode = (ScimNode)validatedDocument;
    log.trace(scimNode.getScimNodeName());
    if (validatedDocument.isArray() || validatedDocument.isObject())
    {
      for ( JsonNode jsonNode : validatedDocument )
      {
        validateJsonNodeIsScimNode(jsonNode);
      }
    }
  }

  /**
   * validates the schemata from the classpath
   *
   * @param testName the name of the test
   * @param metaSchema the meta schema that describes the given json document
   * @param jsonDocument the json document that is validated against the schema
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("getSchemaValidations")
  public void testSchemaValidationForUserResourceSchema(String testName, JsonNode metaSchema, JsonNode jsonDocument)
  {
    log.trace(testName);
    JsonNode jsonNode = SchemaValidator.validateDocumentForResponse(metaSchema, jsonDocument);
    Assertions.assertTrue(JsonHelper.getArrayAttribute(jsonNode, AttributeNames.SCHEMAS).isPresent(),
                          "the schemas attribute must not be removed from the document");
    ArrayNode documentSchemas = JsonHelper.getArrayAttribute(jsonDocument, AttributeNames.SCHEMAS).get();
    ArrayNode jsonNodeSchemas = JsonHelper.getArrayAttribute(jsonNode, AttributeNames.SCHEMAS).get();
    Assertions.assertEquals(documentSchemas, jsonNodeSchemas);
  }

  /**
   * checks that the validation will fail if a required attribute is missing
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.SCHEMAS, AttributeNames.NAME, AttributeNames.SCHEMA, AttributeNames.ENDPOINT})
  public void testValidationFailsOnMissingRequiredAttribute(String attributeName)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    JsonHelper.removeAttribute(userSchema, attributeName);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(metaSchema, userSchema));
  }

  /**
   * checks that the validation will fail if a required attribute is missing within a sub-attribute
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.NAME, AttributeNames.TYPE, AttributeNames.MULTI_VALUED})
  public void testValidationFailsOnMissingRequiredSubAttribute(String attributeName)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNode attributes = JsonHelper.getArrayAttribute(userSchema, AttributeNames.ATTRIBUTES).get();
    JsonNode firstAttribute = attributes.get(0);
    JsonHelper.removeAttribute(firstAttribute, attributeName);

    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(metaSchema, userSchema));
  }

  /**
   * checks that the validation will fail if a canonical value has a typo
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.MUTABILITY, AttributeNames.TYPE, AttributeNames.RETURNED,
                          AttributeNames.UNIQUENESS})
  public void testValidationFailsOnTypoInCanonicalValue(String attributeName)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNode attributes = JsonHelper.getArrayAttribute(userSchema, AttributeNames.ATTRIBUTES).get();
    JsonNode firstAttribute = attributes.get(0);
    JsonHelper.writeValue(firstAttribute, attributeName, "unknown_value");
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(metaSchema, userSchema));
  }

  /**
   * shows that the validation will fail if a field is an array but is expected by the schema as a simple value
   */
  @Test
  public void testValidationFailsIfNodeIsArrayInsteadOfSimple()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    JsonNodeFactory factory = new JsonNodeFactory(false);
    ArrayNode arrayNode = new ArrayNode(factory);
    arrayNode.add("bla");
    JsonHelper.replaceNode(userSchema, AttributeNames.ID, arrayNode);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(metaSchema, userSchema));
  }

  /**
   * shows that the validation will fail if a field is an array but is expected by the schema as a simple value
   */
  @Test
  public void testValidationFailsIfNodeIsOfDifferentType()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);

    IntNode idNode = new IntNode(new Random().nextInt());
    JsonHelper.replaceNode(userSchema, AttributeNames.ID, idNode);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(metaSchema, userSchema));
  }

  /**
   * this test will show that the validation will fail if a value marked as reference of type uri is not of type
   * uri
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.SCHEMA, AttributeNames.ENDPOINT})
  public void testValidationFailsIfUriReferenceIsNotAUri(String attributeName)
  {
    JsonNode resourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    JsonHelper.writeValue(userResourceTypeSchema, attributeName, "oh happy day");
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(resourceTypeSchema,
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

    Assertions.assertDoesNotThrow(() -> SchemaValidator.validateDocumentForResponse(resourceTypeSchema,
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

    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateDocumentForResponse(resourceTypeSchema,
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
    String dateTimeTypeString = getAttributeString(createdAttributeName,
                                                   Type.DATE_TIME,
                                                   false,
                                                   true,
                                                   false,
                                                   Mutability.READ_WRITE,
                                                   Returned.DEFAULT,
                                                   Uniqueness.NONE);
    JsonNode metaAttributes = JsonHelper.getArrayAttribute(metaSchema, AttributeNames.ATTRIBUTES).get();
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
    JsonNode resourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode userResourceTypeSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    final String helloWorldKey = "helloWorld";
    JsonHelper.addAttribute(userResourceTypeSchema, helloWorldKey, new TextNode("hello world"));

    JsonNode validatedSchema = SchemaValidator.validateDocumentForResponse(resourceTypeSchema, userResourceTypeSchema);
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(validatedSchema, helloWorldKey).isPresent());
    ArrayNode schemaExtensions = JsonHelper.getArrayAttribute(validatedSchema, AttributeNames.SCHEMA_EXTENSIONS)
                                           .orElseThrow(() -> new IllegalStateException("the document does not contain "
                                                                                        + "an endpoint  attribute"));
    ObjectNode schemaExtensionAttribute = (ObjectNode)schemaExtensions.get(0);
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(schemaExtensionAttribute, helloWorldKey).isPresent());

    Assertions.assertFalse(JsonHelper.getObjectAttribute(validatedSchema, AttributeNames.META).isPresent(),
                           "meta attribute must be removed from validated request-document");
  }

  /**
   * this test will verify that the validated nodes created by the {@link SchemaValidator} are all implementing
   * the interface {@link de.gold.scim.resources.ScimNode}
   */
  @ParameterizedTest
  @CsvSource({ClassPathReferences.META_SCHEMA_JSON + "," + ClassPathReferences.USER_SCHEMA_JSON,
              ClassPathReferences.META_RESOURCE_TYPES_JSON + "," + ClassPathReferences.USER_RESOURCE_TYPE_JSON,
              ClassPathReferences.META_RESOURCE_TYPES_JSON + "," + ClassPathReferences.GROUP_RESOURCE_TYPE_JSON,
              ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE,
              ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE_ENTERPRISE,
              ClassPathReferences.GROUP_SCHEMA_JSON + "," + GROUP_RESOURCE})
  public void testThatAllValidatedNodesAreScimNodes(String metaSchemaLocation, String documentLocation)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(metaSchemaLocation);
    JsonNode userSchema = JsonHelper.loadJsonDocument(documentLocation);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return SchemaValidator.validateDocumentForResponse(metaSchema, userSchema);
    });
    Assertions.assertNotNull(validatedDocument);
    validateJsonNodeIsScimNode(validatedDocument);
  }

  /**
   * this test will check that on response validation an exception is thrown if a required attribute is missing
   */
  @ParameterizedTest
  @CsvSource({ClassPathReferences.USER_SCHEMA_JSON + "," + USER_RESOURCE_ENTERPRISE,
              ClassPathReferences.GROUP_SCHEMA_JSON + "," + GROUP_RESOURCE})
  public void testValidationFailsForMissingIdOnResponse(String metaSchemaLocation, String documentLocation)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(metaSchemaLocation);
    JsonNode resourceSchema = JsonHelper.loadJsonDocument(documentLocation);

    JsonHelper.removeAttribute(resourceSchema, AttributeNames.ID);
    Assertions.assertThrows(DocumentValidationException.class, () -> {
      SchemaValidator.validateDocumentForResponse(metaSchema, resourceSchema);
    });
  }

  /**
   * this test will verify that unknown attributes are removed from the validated document
   */
  @Test
  public void testRemoveEnterpriseExtensionFromValidatedDocument()
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE_ENTERPRISE);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return SchemaValidator.validateDocumentForRequest(metaSchema, userSchema, SchemaValidator.HttpMethod.POST);
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
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return SchemaValidator.validateDocumentForResponse(metaSchema, userSchema);
    });
    // since the document was only validated against the user-schema and not the enterprise-user-extension schema
    // the extension attribute should not be present in the result
    Assertions.assertNull(validatedDocument.get(AttributeNames.PASSWORD));
  }

  /**
   * this test will verify read only attributes are simply removed from the request if they are not required
   */
  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT"})
  public void testRemoveNonRequiredReadOnlyAttributesFromRequest(SchemaValidator.HttpMethod httpMethod)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(USER_RESOURCE);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return SchemaValidator.validateDocumentForRequest(metaSchema, userSchema, httpMethod);
    });
    // since the document was only validated against the user-schema and not the enterprise-user-extension schema
    // the extension attribute should not be present in the result
    Assertions.assertNull(validatedDocument.get(AttributeNames.ID));
    Assertions.assertNull(validatedDocument.get(AttributeNames.DISPLAY));
    Assertions.assertNull(validatedDocument.get(AttributeNames.GROUPS));
  }

  @Test
  public void testDuplicateSimpleAttribute()
  {
    Assertions.fail("This test must check that a document does not have the same attribute twice");
  }

  @Test
  public void testDuplicateValueOnUniqueMultivaluedAttribute()
  {
    Assertions.fail("This test must check that a document does not have an attribute with the same value twice if the"
                    + " unique value is set to server or global");
  }

  private String getAttributeString(String name,
                                    Type type,
                                    boolean multiValued,
                                    boolean required,
                                    boolean caseExact,
                                    Mutability mutability,
                                    Returned returned,
                                    Uniqueness uniqueness)
  {
    // @formatter:off
    return "{" +
           "   \"name\": \"" + name + "\",\n" +
           "   \"type\": \"" + type.getValue() + "\",\n" +
           "   \"multiValued\": " + multiValued+ ",\n" +
           "   \"description\": \"some description\",\n" +
           "   \"required\": " + required + ",\n" +
           "   \"caseExact\": " + caseExact + ",\n" +
           "   \"mutability\": \"" + mutability.getValue() + "\",\n" +
           "   \"returned\": \"" + returned.getValue() + "\",\n" +
           "   \"uniqueness\": \"" + uniqueness.getValue() + "\"\n" +
           "}";
    // @formatter:on
  }
}
