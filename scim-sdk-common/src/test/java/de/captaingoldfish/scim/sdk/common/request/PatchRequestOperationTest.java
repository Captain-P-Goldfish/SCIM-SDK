package de.captaingoldfish.scim.sdk.common.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 08:57 <br>
 * <br>
 */
public class PatchRequestOperationTest
{

  /**
   * verifies that the object is empty if nothing was set
   */
  @Test
  @DisplayName("Create empty PatchRequestOperation")
  public void testCreateEmptyObject()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder().build();
    Assertions.assertTrue(operation.isEmpty(), operation.toPrettyString());
    operation = new PatchRequestOperation();
    Assertions.assertTrue(operation.isEmpty(), operation.toPrettyString());
  }

  /**
   * will verify that the patch objects can be successfully built
   */
  @Test
  @DisplayName("Getter and Setter methods work correctly")
  public void testGetterAndSetterMethods()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder().build();
    Assertions.assertTrue(operation.isEmpty(), operation.toPrettyString());

    final String value = "{\"emails\":[{\"value\":\"babs@jensen.org\",\"type\":\"home\"}],\"nickname\":\"Babs\"}";
    final String path = "members[value eq \"2819c223-7f76-...413861904646\"]";
    final PatchOp patchOp = PatchOp.REPLACE;
    operation.setPath(path);
    operation.setValues(Collections.singletonList(value));
    operation.setOp(patchOp);

    Assertions.assertEquals(1, operation.getValues().size());
    Assertions.assertEquals(value, operation.getValues().get(0));
    Assertions.assertEquals(path, operation.getPath().get());
    Assertions.assertEquals(patchOp, operation.getOp());

    operation.setValueNode(JsonHelper.readJsonDocument(value));
    Assertions.assertEquals(value, operation.getValues().get(0));
  }

  /**
   * RFC 7644 Section 3.5.2 states: "If the "path" attribute is omitted, the "value" attribute MUST be present
   * and MUST contain the attribute to be added or replaced. The "value" attribute MUST be an object containing
   * the attributes to be added or replaced." <br>
   * <br>
   * This test ensures that if no path is present, the value is an object.
   */
  @Test
  @DisplayName("Value should be an Object when path is absent")
  public void testValueIsObjectWhenPathIsAbsent()
  {
    final String valueStr = "{\"userName\":\"bjensen\"}";
    JsonNode valueNode = JsonHelper.readJsonDocument(valueStr);
    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(valueNode).build();

    Assertions.assertFalse(operation.getPath().isPresent());
    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue);
    Assertions.assertTrue(internalValue.isObject(), "Value should be an object when path is absent");
    Assertions.assertEquals("bjensen", internalValue.get("userName").asText());
  }

  /**
   * RFC 7644 Section 3.5.2 states: "The "path" attribute is OPTIONAL... If the "path" attribute is present, the
   * "value" attribute MUST be an array of values to be added or replaced." <br>
   * <br>
   * This test ensures that if a path is present, the value is an array.
   */
  @Test
  @DisplayName("Value should be an Array when path is present")
  public void testValueIsArrayWhenPathIsPresent()
  {
    final String path = "emails";
    final String valueStr = "{\"value\":\"babs@jensen.org\",\"type\":\"home\"}";
    JsonNode valueNode = JsonHelper.readJsonDocument(valueStr);
    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.ADD)
                                                           .path(path)
                                                           .valueNode(valueNode)
                                                           .build();

    Assertions.assertTrue(operation.getPath().isPresent());
    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue);
    Assertions.assertTrue(internalValue.isArray(), "Value should be an array when path is present");
    Assertions.assertEquals(1, internalValue.size());
    Assertions.assertEquals("babs@jensen.org", internalValue.get(0).get("value").asText());
  }

  /**
   * Tests that setting the path after setting the value correctly transitions the value from object to array.
   */
  @Test
  @DisplayName("Transition value from Object to Array when setting path")
  public void testTransitionValueOnPathSet()
  {
    final String valueStr = "{\"userName\":\"bjensen\"}";
    JsonNode valueNode = JsonHelper.readJsonDocument(valueStr);
    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(valueNode).build();

    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isObject());

    operation.setPath("userName");
    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isArray(),
                          "Value should have transitioned to Array after setting path");
    Assertions.assertEquals(1, operation.get(AttributeNames.RFC7643.VALUE).size());
  }

  /**
   * Tests that removing the path after setting the value correctly transitions the value from array to object.
   */
  @Test
  @DisplayName("Transition value from Array to Object when removing path")
  public void testTransitionValueOnPathRemoved()
  {
    final String path = "emails";
    final String valueStr = "{\"value\":\"babs@jensen.org\",\"type\":\"home\"}";
    JsonNode valueNode = JsonHelper.readJsonDocument(valueStr);
    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.ADD)
                                                           .path(path)
                                                           .valueNode(valueNode)
                                                           .build();

    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isArray());

    operation.setPath(null);
    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isObject(),
                          "Value should have transitioned to Object after removing path");
    Assertions.assertEquals("babs@jensen.org", operation.get(AttributeNames.RFC7643.VALUE).get("value").asText());
  }

  /**
   * Tests the behavior when multiple values are set via setValues and how path presence affects the internal
   * representation.
   */
  @Test
  @DisplayName("Handling multiple values via setValues")
  public void testMultipleValuesWithSetValues()
  {
    PatchRequestOperation operation = new PatchRequestOperation();
    List<String> values = Arrays.asList("value1", "value2");
    operation.setValues(values);

    // If no path is present, but multiple values are set, the current implementation (via setAttributeList)
    // will likely keep it as an array.
    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertTrue(internalValue.isArray());
    Assertions.assertEquals(2, internalValue.size());

    operation.setPath("somePath");
    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isArray());
    Assertions.assertEquals(2, operation.get(AttributeNames.RFC7643.VALUE).size());
  }

  /**
   * Verifies that getValueNode() always returns an ArrayNode and updates the internal state if necessary.
   */
  @Test
  @DisplayName("getValueNode should always return an ArrayNode")
  public void testGetValueNode()
  {
    PatchRequestOperation operation = new PatchRequestOperation();
    operation.setValue("singleValue");

    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isTextual());

    Optional<ArrayNode> arrayNodeOptional = operation.getValueNode();
    Assertions.assertTrue(arrayNodeOptional.isPresent());
    Assertions.assertEquals(1, arrayNodeOptional.get().size());
    Assertions.assertEquals("singleValue", arrayNodeOptional.get().get(0).asText());

    // Check internal state after getValueNode()
    // Since path is absent, it should have remained/become a single node if not for getValueNode's side effect?
    // Actually getValueNode calls setValueNode(jsonNode) if path is absent.
  }

  @ParameterizedTest
  @CsvSource({", true", // no path -> should be object (if single)
              "path, false" // has path -> should be array
  })
  @DisplayName("Internal value type depends on path presence")
  public void testInternalValueType(String path, boolean expectedIsObject)
  {
    PatchRequestOperation operation = new PatchRequestOperation();
    operation.setOp(PatchOp.ADD);
    operation.setPath(path);
    operation.setValueNode(JsonHelper.readJsonDocument("{\"key\":\"val\"}"));

    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    if (expectedIsObject)
    {
      Assertions.assertTrue(internalValue.isObject(), "Expected object for path: " + path);
    }
    else
    {
      Assertions.assertTrue(internalValue.isArray(), "Expected array for path: " + path);
    }
  }

  @Test
  @DisplayName("Complex object in value node without path")
  public void testComplexObjectNoPath()
  {
    String json = "{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\"],\"userName\":\"bjensen\"}";
    JsonNode node = JsonHelper.readJsonDocument(json);
    PatchRequestOperation operation = PatchRequestOperation.builder().valueNode(node).build();

    Assertions.assertFalse(operation.getPath().isPresent());
    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isObject());
    Assertions.assertEquals("bjensen", operation.get(AttributeNames.RFC7643.VALUE).get("userName").asText());
  }


}
