package de.gold.scim.schemas;

import static de.gold.scim.schemas.SchemaValidator.HttpMethod.PATCH;
import static de.gold.scim.schemas.SchemaValidator.HttpMethod.POST;
import static de.gold.scim.schemas.SchemaValidator.HttpMethod.PUT;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.Returned;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.constants.enums.Uniqueness;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 20:14 <br>
 * <br>
 */
@Slf4j
public class SchemaValidatorTest
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
                                  JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON)));
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
   * these arguments are used for a test that will verify that attributes are getting removed under specific
   * circumstances in the request and the response
   */
  private static Stream<Arguments> getAttributeDefinitionArguments()
  {
    return Stream.of(Arguments.of(Mutability.WRITE_ONLY, Returned.DEFAULT, null, null),
                     Arguments.of(Mutability.READ_ONLY, Returned.DEFAULT, null, null),
                     Arguments.of(Mutability.READ_WRITE, Returned.NEVER, null, null),
                     Arguments.of(Mutability.IMMUTABLE, Returned.DEFAULT, PUT, null),
                     Arguments.of(Mutability.IMMUTABLE, Returned.DEFAULT, PATCH, null),
                     Arguments.of(Mutability.READ_ONLY, Returned.DEFAULT, POST, null),
                     Arguments.of(Mutability.READ_WRITE, Returned.DEFAULT, null, Arrays.asList("id")),
                     Arguments.of(Mutability.READ_WRITE, Returned.REQUEST, null, Arrays.asList("id")));
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
    SchemaValidator.validateSchemaForResponse(metaSchema, jsonDocument);
  }

  /**
   * checks that the validation will fail if a required attribute is missing
   */
  @ParameterizedTest
  @ValueSource(strings = {AttributeNames.SCHEMAS, AttributeNames.ID, AttributeNames.NAME, AttributeNames.ATTRIBUTES})
  public void testValidationFailsOnMissingRequiredAttribute(String attributeName)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    JsonHelper.removeAttribute(userSchema, attributeName);
    Assertions.assertThrows(DocumentValidationException.class,
                            () -> SchemaValidator.validateSchemaForResponse(metaSchema, userSchema));
  }

  /**
   * checks that the validation will fail if a required attribute is missing
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
                            () -> SchemaValidator.validateSchemaForResponse(metaSchema, userSchema));
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
                            () -> SchemaValidator.validateSchemaForResponse(metaSchema, userSchema));
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
                            () -> SchemaValidator.validateSchemaForResponse(metaSchema, userSchema));
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
                            () -> SchemaValidator.validateSchemaForResponse(metaSchema, userSchema));
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
                            () -> SchemaValidator.validateSchemaForResponse(resourceTypeSchema,
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

    Assertions.assertDoesNotThrow(() -> SchemaValidator.validateSchemaForResponse(resourceTypeSchema,
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
                            () -> SchemaValidator.validateSchemaForResponse(resourceTypeSchema,
                                                                            userResourceTypeSchema));
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

    JsonNode validatedSchema = SchemaValidator.validateSchemaForResponse(resourceTypeSchema, userResourceTypeSchema);
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
   * this test will show that {@link Mutability#IMMUTABLE} attributes will be removed from PUT-requests
   */
  @ParameterizedTest
  @MethodSource("getAttributeDefinitionArguments")
  public void testRemoveAttributesOnValidation(Mutability mutability,
                                               Returned returned,
                                               SchemaValidator.HttpMethod httpMethod,
                                               List<String> requiredAttributes)
  {
    JsonNode metaSchema = JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_TYPES_JSON);
    JsonNode document = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);

    final String attributeName = "newAttribute";
    String attributeDefinition = getAttributeString(attributeName,
                                                    Type.STRING,
                                                    false,
                                                    false,
                                                    false,
                                                    mutability,
                                                    returned,
                                                    Uniqueness.NONE);
    JsonNode metaAttributes = JsonHelper.getArrayAttribute(metaSchema, AttributeNames.ATTRIBUTES).get();
    JsonNode createMetaAttribute = JsonHelper.readJsonDocument(attributeDefinition);
    JsonHelper.addAttributeToArray(metaAttributes, createMetaAttribute);
    TextNode textNode = new TextNode("some value");
    JsonHelper.addAttribute(document, attributeName, textNode);

    JsonNode validatedNode;
    if (httpMethod == null)
    {
      validatedNode = SchemaValidator.validateSchemaForResponse(metaSchema, document);
    }
    else
    {
      validatedNode = SchemaValidator.validateSchemaForRequest(metaSchema, document, httpMethod);
    }
    Assertions.assertFalse(JsonHelper.getSimpleAttribute(validatedNode, attributeName).isPresent(),
                           "attribute '" + attributeName + "' must not be present in the validated document");
    if (requiredAttributes != null)
    {
      Assertions.fail("TODO attribute '" + attributeName + "' must not be present if not required");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  public void testValidationForImmutableValues(SchemaValidator.HttpMethod httpMethod)
  {

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
