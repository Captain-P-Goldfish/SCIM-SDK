package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.utils.SchemaAttributeBuilder;
import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 10.04.2021
 */
public class SimpleMultivaluedAttributeValidatorTest
{

  /**
   * tests the following structure
   * 
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   * 
   * <pre>
   *    {
   *      "array": ["hello", "world", "foo", "bar"]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithStringTypesOnly()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"),
                                   new TextNode("world"),
                                   new TextNode("foo"),
                                   new TextNode("bar")));
    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   * 
   * <pre>
   *    {
   *      "type": "dateTime",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   * 
   * <pre>
   *    {
   *      "array": ["2019-09-29T24:00:00", "2019-09-29T24:00:00.0000000-14:00",
   *                "2019-09-29T24:00:00.0000000Z", "2019-09-29T24:00:00Z"]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithDateTimeTypesOnly()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.DATE_TIME)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("2019-09-29T24:00:00"),
                                   new TextNode("2019-09-29T24:00:00.0000000-14:00"),
                                   new TextNode("2019-09-29T24:00:00.0000000Z"),
                                   new TextNode("2019-09-29T24:00:00Z")));
    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "integer",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [1, 2, 3]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithIntegerTypesOnly()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.INTEGER)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new IntNode(1), new IntNode(2), new IntNode(3)));
    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimIntNode.class));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "decimal",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [1, 2.5, 3, 1.8]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithDecimalTypesOnly()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.DECIMAL)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new IntNode(1), new DoubleNode(2.5), new IntNode(3), new DoubleNode(1.8)));
    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimDoubleNode.class));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "boolean",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [true, false, false, true]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithBooleanTypesOnly()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.BOOLEAN)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(BooleanNode.getTrue(),
                                   BooleanNode.getFalse(),
                                   BooleanNode.getFalse(),
                                   BooleanNode.getTrue()));
    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimBooleanNode.class));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["hello", 1, 3, true]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithMixedTypesOnStringAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"), new IntNode(1), new IntNode(3), BooleanNode.getTrue()));
    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Found unsupported value in multivalued attribute '%s'", arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
      AttributeValidationException cause = (AttributeValidationException)ex.getCause();
      Assertions.assertEquals(schemaAttribute, cause.getSchemaAttribute());
      String causeErrorMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(arrayNode.get(1).getNodeType().toString()),
                                               arrayNode.get(1));
      Assertions.assertEquals(causeErrorMessage, cause.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "dateTime",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["2019-09-29T24:00:00Z", "hello", 1]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithMixedTypesOnDateTimeAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.DATE_TIME)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("2019-09-29T24:00:00Z"), new TextNode("hello"), new IntNode(1)));
    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Found unsupported value in multivalued attribute '%s'", arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
      AttributeValidationException cause = (AttributeValidationException)ex.getCause();
      Assertions.assertEquals(schemaAttribute, cause.getSchemaAttribute());
      String causeErrorMessage = String.format("Given value is not a valid dateTime '%s'",
                                               arrayNode.get(1).textValue());
      Assertions.assertEquals(causeErrorMessage, cause.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "integer",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["hello", 1, 3, true]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithMixedTypesOnIntegerAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.INTEGER)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"), new IntNode(1), new IntNode(3), BooleanNode.getTrue()));
    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Found unsupported value in multivalued attribute '%s'", arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
      AttributeValidationException cause = (AttributeValidationException)ex.getCause();
      Assertions.assertEquals(schemaAttribute, cause.getSchemaAttribute());
      String causeErrorMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(arrayNode.get(0).getNodeType().toString()),
                                               arrayNode.get(0));
      Assertions.assertEquals(causeErrorMessage, cause.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "boolean",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [true, false, "hello", 1, 3]
   *    }
   * </pre>
   */
  @Test
  public void testValidateWithMixedTypesOnBooleanAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.BOOLEAN)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(BooleanNode.getTrue(),
                                   BooleanNode.getFalse(),
                                   new TextNode("hello"),
                                   new IntNode(1),
                                   new IntNode(3)));
    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Found unsupported value in multivalued attribute '%s'", arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
      AttributeValidationException cause = (AttributeValidationException)ex.getCause();
      Assertions.assertEquals(schemaAttribute, cause.getSchemaAttribute());
      String causeErrorMessage = String.format("Value of field '%s' is not of type '%s' but of type '%s' with value '%s'",
                                               schemaAttribute.getFullResourceName(),
                                               schemaAttribute.getType().getValue(),
                                               StringUtils.lowerCase(arrayNode.get(2).getNodeType().toString()),
                                               arrayNode.get(2));
      Assertions.assertEquals(causeErrorMessage, cause.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [{"bad": "no"}, {"bad": "objects"}, {"bad": "allowed"}]
   *    }
   * </pre>
   */
  @Test
  public void testValidateObjectTypesOnStringAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode bad1 = new ObjectNode(JsonNodeFactory.instance);
    bad1.set("bad", new TextNode("no"));
    ObjectNode bad2 = new ObjectNode(JsonNodeFactory.instance);
    bad2.set("bad", new TextNode("objects"));
    ObjectNode bad3 = new ObjectNode(JsonNodeFactory.instance);
    bad3.set("bad", new TextNode("allowed"));

    arrayNode.addAll(Arrays.asList(bad1, bad2, bad3));
    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      String errorMessage = String.format("Attribute '%s' is expected to hold only simple values but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["no", "objects", {"bad": "allowed"}]
   *    }
   * </pre>
   */
  @Test
  public void testValidateOneObjectTypeOnStringAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode bad = new ObjectNode(JsonNodeFactory.instance);
    bad.set("bad", new TextNode("allowed"));

    arrayNode.addAll(Arrays.asList(new TextNode("no"), new TextNode("objects"), bad));
    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      String errorMessage = String.format("Attribute '%s' is expected to hold only simple values but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": {"bad": "not allowed"}
   *    }
   * </pre>
   */
  @Test
  public void testValidateObjectTypeOnMultivaluedStringAttribute()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .multivalued(true)
                                                            .build();
    ObjectNode bad = new ObjectNode(JsonNodeFactory.instance);
    bad.set("bad", new TextNode("not allowed"));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, bad);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      String errorMessage = String.format("Attribute '%s' is expected to be an array but is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          bad);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": "simple value"
   *    }
   * </pre>
   */
  @Test
  public void testValidateSimpleStringAttributeAsMultivalued()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .multivalued(true)
                                                            .build();
    TextNode attribute = new TextNode("simple value");

    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, attribute);
    });
    Assertions.assertEquals(1, scimArrayNode.size());
    JsonNode parsedNode = scimArrayNode.get(0);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
    Assertions.assertEquals(attribute.textValue(), parsedNode.textValue());
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "integer",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": 1
   *    }
   * </pre>
   */
  @Test
  public void testValidateSimpleIntegerAttributeAsMultivalued()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.INTEGER)
                                                            .multivalued(true)
                                                            .build();
    IntNode attribute = new IntNode(1);

    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, attribute);
    });
    Assertions.assertEquals(1, scimArrayNode.size());
    JsonNode parsedNode = scimArrayNode.get(0);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimIntNode.class));
    Assertions.assertEquals(attribute.textValue(), parsedNode.textValue());
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "boolean",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": true
   *    }
   * </pre>
   */
  @Test
  public void testValidateSimpleBooleanAttributeAsMultivalued()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.BOOLEAN)
                                                            .multivalued(true)
                                                            .build();
    BooleanNode attribute = BooleanNode.getTrue();

    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, attribute);
    });
    Assertions.assertEquals(1, scimArrayNode.size());
    JsonNode parsedNode = scimArrayNode.get(0);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimBooleanNode.class));
    Assertions.assertEquals(attribute.textValue(), parsedNode.textValue());
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "decimal",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": 1.9
   *    }
   * </pre>
   */
  @Test
  public void testValidateSimpleDecimalAttributeAsMultivalued()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.DECIMAL)
                                                            .multivalued(true)
                                                            .build();
    DoubleNode attribute = new DoubleNode(1.9);

    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, attribute);
    });
    Assertions.assertEquals(1, scimArrayNode.size());
    JsonNode parsedNode = scimArrayNode.get(0);
    MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimDoubleNode.class));
    Assertions.assertEquals(attribute.textValue(), parsedNode.textValue());
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "canonicalValues": ["hello", "world"],
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["hello", "hello", "world"]
   *    }
   * </pre>
   */
  @Test
  public void testValidateStringTypesWithCanonicalValues()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .canonicalValues("hello", "world")
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"), new TextNode("hello"), new TextNode("world")));

    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      MatcherAssert.assertThat(parsedNode.getClass(), Matchers.typeCompatibleWith(ScimTextNode.class));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "canonicalValues": ["hello", "world"],
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["hello", "hello", "goldfish"]
   *    }
   * </pre>
   */
  @Test
  public void testValidateStringTypesWithIllegalCanonicalValue()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .canonicalValues("hello", "world")
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"), new TextNode("hello"), new TextNode("goldfish")));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Attribute with name '%s' does not match one of its canonicalValues "
                                          + "'%s' actual value is '%s'",
                                          schemaAttribute.getFullResourceName(),
                                          schemaAttribute.getCanonicalValues(),
                                          arrayNode.get(2).textValue());
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "any",
   *      "multiValued": true,
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["hello", 1, 2.5, false, "2019-09-29T24:00:00Z"]
   *    }
   * </pre>
   */
  @SneakyThrows
  @Test
  public void testValidateAnyType()
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.ANY)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"),
                                   new LongNode(1),
                                   new DoubleNode(2.5),
                                   BooleanNode.getFalse(),
                                   new TextNode("2019-09-29T24:00:00Z")));

    ScimArrayNode scimArrayNode = (ScimArrayNode)Assertions.assertDoesNotThrow(() -> {
      return SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
    });
    Assertions.assertEquals(arrayNode.size(), scimArrayNode.size());
    for ( int i = 0 ; i < arrayNode.size() ; i++ )
    {
      JsonNode node = arrayNode.get(i);
      JsonNode parsedNode = scimArrayNode.get(i);
      final String classPackage = "de.captaingoldfish.scim.sdk.common.resources.base.";
      MatcherAssert.assertThat(parsedNode.getClass(),
                               Matchers.typeCompatibleWith(Class.forName(classPackage + "Scim"
                                                                         + node.getClass().getSimpleName())));
      Assertions.assertEquals(node.textValue(), parsedNode.textValue());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "string",
   *      "multiValued": true,
   *      "uniqueness": "server|global"
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["hello", "world", "world"]
   *    }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testValidateStringTypesUnique(Uniqueness uniqueness)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.STRING)
                                                            .uniqueness(uniqueness)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("hello"), new TextNode("world"), new TextNode("world")));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                          schemaAttribute.getUniqueness().getValue(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "integer",
   *      "multiValued": true,
   *      "uniqueness": "server|global"
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [1, 1, 5]
   *    }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testValidateIntegerTypesUnique(Uniqueness uniqueness)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.INTEGER)
                                                            .uniqueness(uniqueness)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new IntNode(1), new IntNode(1), new IntNode(5)));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                          schemaAttribute.getUniqueness().getValue(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "decimal",
   *      "multiValued": true,
   *      "uniqueness": "server|global"
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [1.3, 1.3]
   *    }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testValidateDecimalTypesUnique(Uniqueness uniqueness)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.DECIMAL)
                                                            .uniqueness(uniqueness)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new DoubleNode(1.3), new DoubleNode(1.3)));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                          schemaAttribute.getUniqueness().getValue(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "boolean",
   *      "multiValued": true,
   *      "uniqueness": "server|global"
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": [true, true]
   *    }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testValidateBooleanTypesUnique(Uniqueness uniqueness)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.BOOLEAN)
                                                            .uniqueness(uniqueness)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(BooleanNode.getTrue(), BooleanNode.getTrue()));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                          schemaAttribute.getUniqueness().getValue(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * tests the following structure
   *
   * <pre>
   *    {
   *      "type": "dateTime",
   *      "multiValued": true,
   *      "uniqueness": "server|global"
   *      ...
   *    }
   * </pre>
   *
   * <pre>
   *    {
   *      "array": ["2019-10-18T14:51:11+02:00", "2019-10-18T14:51:11+02:00"]
   *    }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = {"SERVER", "GLOBAL"})
  public void testValidateDateTypesUnique(Uniqueness uniqueness)
  {
    SchemaAttribute schemaAttribute = SchemaAttributeBuilder.builder()
                                                            .name("id")
                                                            .type(Type.DATE_TIME)
                                                            .uniqueness(uniqueness)
                                                            .multivalued(true)
                                                            .build();
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.addAll(Arrays.asList(new TextNode("2019-10-18T14:51:11+02:00"),
                                   new TextNode("2019-10-18T14:51:11+02:00")));

    try
    {
      SimpleMultivaluedAttributeValidator.parseNodeType(schemaAttribute, arrayNode);
      Assertions.fail("this point must not be reached");
    }
    catch (AttributeValidationException ex)
    {
      Assertions.assertEquals(schemaAttribute, ex.getSchemaAttribute());
      String errorMessage = String.format("Array with uniqueness '%s' contains duplicate values '%s'",
                                          schemaAttribute.getUniqueness().getValue(),
                                          arrayNode);
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }
}
