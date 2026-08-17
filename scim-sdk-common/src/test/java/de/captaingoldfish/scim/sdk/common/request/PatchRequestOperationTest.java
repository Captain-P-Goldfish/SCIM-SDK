package de.captaingoldfish.scim.sdk.common.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 08:57 <br>
 * <br>
 */
public class PatchRequestOperationTest
{

  /**
   * Verifies that both supported construction variants create an empty operation.
   *
   * <pre>{@code
   * PatchRequestOperation.builder().build() -> {}
   * new PatchRequestOperation()             -> {}
   * }</pre>
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
   * Verifies the basic setters and their corresponding getters with a replace of the single-valued complex
   * {@code name} attribute.
   *
   * <pre>{@code
   * Result:
   * {
   *   "op": "replace",
   *   "path": "name",
   *   "value": {"formatted":"Babs Jensen","familyName":"Jensen","givenName":"Babs"}
   * }
   * }</pre>
   */
  @Test
  @DisplayName("Getter and Setter methods work correctly")
  public void testGetterAndSetterMethods()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder().build();
    Assertions.assertTrue(operation.isEmpty(), operation.toPrettyString());

    final Name value = Name.builder().formatted("Babs Jensen").familyName("Jensen").givenName("Babs").build();
    final String path = "name";
    final PatchOp patchOp = PatchOp.REPLACE;
    operation.setPath(path);
    operation.setValueNode(value);
    operation.setOp(patchOp);

    Assertions.assertEquals(value, operation.getValue().get());
    Assertions.assertEquals(path, operation.getPath().get());
    Assertions.assertEquals(patchOp, operation.getOp());
  }

  /**
   * Verifies the RFC 7644 pathless-add representation: the supplied resource attributes remain a JSON object.
   *
   * <pre>{@code
   * Input valueNode:  User{schemas=["urn:ietf:params:scim:schemas:core:2.0:User"], userName="bjensen"}
   * Serialized PATCH: {"op":"add","value":{"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
   *                    "userName":"bjensen"}}
   * }</pre>
   *
   * In particular, {@code value} must be neither a JSON string nor an array.
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/scim-for-keycloak/issues/172">scim-for-keycloak issue
   *      #172</a>
   */
  @Test
  @DisplayName("Value should be an Object when path is absent")
  public void testValueIsObjectWhenPathIsAbsent()
  {
    User valueNode = User.builder().userName("bjensen").build();
    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(valueNode).build();

    Assertions.assertFalse(operation.getPath().isPresent());
    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue);
    Assertions.assertTrue(internalValue.isObject(), "Value should be an object when path is absent");
    Assertions.assertEquals("bjensen", internalValue.get("userName").asText());
  }

  /**
   * Verifies that adding a single complex value through {@code valueNode(ObjectNode)} does not wrap that value
   * in a singleton array merely because a path exists.
   *
   * <pre>{@code
   * Input:  path="name", valueNode={"formatted":"Babs Jensen","familyName":"Jensen"}
   * Output: {"op":"replace","path":"name","value":{"formatted":"Babs Jensen","familyName":"Jensen"}}
   * }</pre>
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("Object value should retain its type when path is present")
  public void testObjectValueRetainsTypeWhenPathIsPresent()
  {
    final String path = "name";
    Name valueNode = Name.builder().formatted("Babs Jensen").familyName("Jensen").build();
    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.REPLACE)
                                                           .path(path)
                                                           .valueNode(valueNode)
                                                           .build();

    Assertions.assertTrue(operation.getPath().isPresent());
    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue);
    Assertions.assertTrue(internalValue.isObject(), "Value should retain its object type");
    Assertions.assertEquals("Babs Jensen", internalValue.get("formatted").asText());
  }

  /**
   * Verifies that {@link PatchRequestOperation#setValues(List)} represents multiple strings as an array and
   * preserves the array for a custom multi-valued string attribute.
   *
   * <pre>{@code
   * {"op":"replace","path":"urn:example:params:scim:schemas:extension:Custom:2.0:tags",
   *  "value":["tag-a","tag-b"]}
   * }</pre>
   */
  @Test
  @DisplayName("Handling multiple values via setValues")
  public void testMultipleValuesWithSetValues()
  {
    PatchRequestOperation operation = new PatchRequestOperation();
    operation.setOp(PatchOp.REPLACE);
    operation.setPath("urn:example:params:scim:schemas:extension:Custom:2.0:tags");
    List<String> values = Arrays.asList("tag-a", "tag-b");
    operation.setValues(values);

    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertTrue(internalValue.isArray());
    Assertions.assertEquals(2, internalValue.size());

  }

  /**
   * Verifies the distinction between the stored wire value and the normalized server-processing view returned
   * by {@link PatchRequestOperation#getValueNode()}.
   *
   * <pre>{@code
   * Operation:               {"op":"replace","path":"preferredLanguage","value":"de"}
   * getValueNode() result:   ["de"]
   * Serialized value after:  "de"
   * }</pre>
   *
   * Calling the getter must not rewrite the serialized scalar into an array.
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("getValueNode should always return an ArrayNode")
  public void testGetValueNode()
  {
    PatchRequestOperation operation = new PatchRequestOperation();
    operation.setOp(PatchOp.REPLACE);
    operation.setPath("preferredLanguage");
    operation.setValue(TextNode.valueOf("de"));

    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isTextual());

    Optional<ArrayNode> arrayNodeOptional = operation.getValueNode();
    Assertions.assertTrue(arrayNodeOptional.isPresent());
    Assertions.assertEquals(1, arrayNodeOptional.get().size());
    Assertions.assertEquals("de", arrayNodeOptional.get().get(0).asText());
    Assertions.assertTrue(operation.get(AttributeNames.RFC7643.VALUE).isTextual());
  }

  /**
   * Verifies that a JSON-encoded object string is materialized in the operation and that modifications through
   * the node returned by {@link PatchRequestOperation#getValueNode()} update the serialized operation.
   *
   * <pre>{@code
   * Before getter: {"op":"replace","path":"manager","value":"{\"value\":\"bulkId:2\"}"}
   * After getter:  {"op":"replace","path":"manager","value":{"value":"bulkId:2"}}
   * After modify:  {"op":"replace","path":"manager","value":{"value":"resolved-id"}}
   * }</pre>
   * 
   * This mutable-node contract is used by the bulkId resolver to replace references in place.
   */
  @Test
  @DisplayName("Structured string values are materialized as mutable operation nodes")
  public void testStructuredStringValueIsMaterializedAndMutable()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.REPLACE)
                                                           .path("manager")
                                                           .value("{\"value\":\"bulkId:2\"}")
                                                           .build();

    ArrayNode getterValue = operation.getValueNode().get();
    ObjectNode manager = (ObjectNode)getterValue.get(0);
    Assertions.assertSame(manager, operation.get(AttributeNames.RFC7643.VALUE));

    manager.put(AttributeNames.RFC7643.VALUE, "resolved-id");
    Assertions.assertEquals("resolved-id",
                            operation.get(AttributeNames.RFC7643.VALUE).get(AttributeNames.RFC7643.VALUE).textValue());
  }

  /**
   * Verifies that path-based {@code valueNode(...)} operations preserve every explicitly supplied JSON node
   * type.
   *
   * <pre>{@code
   * preferredLanguage -> "value":"67890"  // remains a string, not the number 67890
   * custom:level      -> "value":42
   * active            -> "value":true
   * custom:tags       -> "value":["one","two"]
   * }</pre>
   *
   * This covers both the singleton-array bug and the numeric-string conversion regression.
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("Path-based valueNode preserves scalar and array JSON types")
  public void testPathBasedValueNodePreservesJsonType()
  {
    ArrayNode tags = new ArrayNode(JsonNodeFactory.instance);
    tags.add("one");
    tags.add("two");
    JsonNode[] values = {TextNode.valueOf("67890"), IntNode.valueOf(42), BooleanNode.TRUE, tags};
    String[] paths = {"preferredLanguage", "urn:example:params:scim:schemas:extension:Custom:2.0:level", "active",
                      "urn:example:params:scim:schemas:extension:Custom:2.0:tags"};

    for ( int i = 0 ; i < values.length ; i++ )
    {
      PatchRequestOperation operation = PatchRequestOperation.builder()
                                                             .op(PatchOp.REPLACE)
                                                             .path(paths[i])
                                                             .valueNode(values[i])
                                                             .build();
      operation.getValueNode();
      Assertions.assertEquals(values[i], operation.get(AttributeNames.RFC7643.VALUE));
    }
  }

  /**
   * Verifies that a singleton {@link ArrayNode} supplied via {@code valueNode(...)} for a multivalued attribute
   * is preserved as an array on the wire and not unwrapped into a scalar merely because the path is set after
   * the value in the builder.
   *
   * <pre>{@code
   * Input:  path="roles", valueNode=["ADMIN"]
   * Output: {"op":"add","path":"roles","value":["ADMIN"]}
   * }</pre>
   *
   * Regression test for the over-broad singleton-array unwrap that also stripped singleton arrays from
   * multivalued attributes.
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("Singleton array value for a multivalued attribute is preserved as an array")
  public void testSingletonArrayForMultivaluedAttributeIsPreserved()
  {
    ArrayNode roles = new ArrayNode(JsonNodeFactory.instance);
    roles.add("ADMIN");

    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.ADD)
                                                           .path("roles")
                                                           .valueNode(roles)
                                                           .build();

    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue, operation.toString());
    Assertions.assertTrue(internalValue.isArray(), "multivalued attribute value must remain an array: " + operation);
    Assertions.assertEquals(1, internalValue.size());
    Assertions.assertEquals("ADMIN", internalValue.get(0).asText());

    JsonNode serialized = JsonHelper.readJsonDocument(operation.toString());
    Assertions.assertTrue(serialized.get(AttributeNames.RFC7643.VALUE).isArray(), operation.toString());
    Assertions.assertEquals("ADMIN", serialized.get(AttributeNames.RFC7643.VALUE).get(0).asText());
  }

  /**
   * Verifies that a singleton list supplied via {@code values(...)} is preserved as an array on the wire and
   * not collapsed into a scalar. A single value supplied as a list is structurally distinct from a single
   * string supplied via {@code value(...)}/{@code valueNode(...)}, and the builder must not erase that
   * distinction.
   *
   * <pre>{@code
   * Input:  path="roles", values=["ADMIN"]
   * Output: {"op":"add","path":"roles","value":["ADMIN"]}
   * }</pre>
   *
   * Contrast with {@link #testSingletonArrayForMultivaluedAttributeIsPreserved()} which uses
   * {@code valueNode(ArrayNode)} and with {@link #testValueSerializationIsOrderIndependent()} which uses
   * {@code setValue(String)} for a scalar. Regression test for the 1.34.0 singleton-list collapse that made
   * {@code values(["ADMIN"])} and {@code value("ADMIN")} indistinguishable on the wire.
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("Singleton list via values(...) is preserved as an array on the wire")
  public void testSingletonListViaValuesIsPreservedAsArray()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.ADD)
                                                           .path("roles")
                                                           .values(Collections.singletonList("ADMIN"))
                                                           .build();

    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue, operation.toString());
    Assertions.assertTrue(internalValue.isArray(),
                          "a singleton list supplied via values(...) must remain an array: " + operation);
    Assertions.assertEquals(1, internalValue.size());
    Assertions.assertEquals("ADMIN", internalValue.get(0).asText());

    JsonNode serialized = JsonHelper.readJsonDocument(operation.toString());
    Assertions.assertTrue(serialized.get(AttributeNames.RFC7643.VALUE).isArray(), operation.toString());
    Assertions.assertEquals("ADMIN", serialized.get(AttributeNames.RFC7643.VALUE).get(0).asText());
  }

  /**
   * Verifies that a singleton array of complex objects supplied via {@code valueNode(...)} for a multivalued
   * complex attribute (e.g. {@code emails}) is preserved as an array on the wire and not unwrapped into a
   * single object merely because the path is set after the value in the builder.
   *
   * <pre>{@code
   * Input:  path="emails", valueNode=[{"value":"x@y.z","type":"work"}]
   * Output: {"op":"add","path":"emails","value":[{"value":"x@y.z","type":"work"}]}
   * }</pre>
   *
   * Regression test for the over-broad singleton-array unwrap that also collapsed singleton complex arrays
   * (e.g. {@code [{...}]} into {@code {...}}) for multivalued complex attributes.
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("Singleton array of complex objects for a multivalued complex attribute is preserved as an array")
  public void testSingletonComplexArrayForMultivaluedComplexAttributeIsPreserved()
  {
    ObjectNode email = new ObjectNode(JsonNodeFactory.instance);
    email.put("value", "x@y.z");
    email.put("type", "work");
    ArrayNode emails = new ArrayNode(JsonNodeFactory.instance);
    emails.add(email);

    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.ADD)
                                                           .path("emails")
                                                           .valueNode(emails)
                                                           .build();

    JsonNode internalValue = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(internalValue, operation.toString());
    Assertions.assertTrue(internalValue.isArray(),
                          "multivalued complex attribute value must remain an array: " + operation);
    Assertions.assertEquals(1, internalValue.size());
    Assertions.assertTrue(internalValue.get(0).isObject());
    Assertions.assertEquals("x@y.z", internalValue.get(0).get("value").asText());

    JsonNode serialized = JsonHelper.readJsonDocument(operation.toString());
    Assertions.assertTrue(serialized.get(AttributeNames.RFC7643.VALUE).isArray(), operation.toString());
    Assertions.assertTrue(serialized.get(AttributeNames.RFC7643.VALUE).get(0).isObject());
    Assertions.assertEquals("x@y.z", serialized.get(AttributeNames.RFC7643.VALUE).get(0).get("value").asText());
  }

  /**
   * Verifies that {@code valueNode(...)}/{@code setValue}/{@code setValues} serialize identically regardless of
   * whether {@code setPath} is called before or after the value setter. The setters store the supplied form
   * verbatim and do not branch on the path, so the order no longer affects the result.
   *
   * <pre>{@code
   * path-based scalar (preferredLanguage):  "value":"de"       in both orders
   * path-based multivalued (roles):          "value":["ADMIN"]  in both orders
   * }</pre>
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
   */
  @Test
  @DisplayName("value serialization is independent of path-before-value vs value-before-path order")
  public void testValueSerializationIsOrderIndependent()
  {
    // path-based scalar: setValue before setPath vs setPath before setValue
    PatchRequestOperation valueFirst = new PatchRequestOperation();
    valueFirst.setOp(PatchOp.REPLACE);
    valueFirst.setValue("de");
    valueFirst.setPath("preferredLanguage");

    PatchRequestOperation pathFirst = new PatchRequestOperation();
    pathFirst.setOp(PatchOp.REPLACE);
    pathFirst.setPath("preferredLanguage");
    pathFirst.setValue("de");

    Assertions.assertEquals("de", valueFirst.get(AttributeNames.RFC7643.VALUE).textValue(), valueFirst.toString());
    Assertions.assertEquals(JsonHelper.readJsonDocument(valueFirst.toString()),
                            JsonHelper.readJsonDocument(pathFirst.toString()),
                            "order must not affect path-based scalar");

    // path-based multivalued array: valueNode before setPath vs setPath before valueNode
    ArrayNode roles = new ArrayNode(JsonNodeFactory.instance);
    roles.add("ADMIN");
    PatchRequestOperation arrayValueFirst = new PatchRequestOperation();
    arrayValueFirst.setOp(PatchOp.ADD);
    arrayValueFirst.setValueNode(roles);
    arrayValueFirst.setPath("roles");

    PatchRequestOperation arrayPathFirst = new PatchRequestOperation();
    arrayPathFirst.setOp(PatchOp.ADD);
    arrayPathFirst.setPath("roles");
    arrayPathFirst.setValueNode(roles);

    Assertions.assertTrue(arrayValueFirst.get(AttributeNames.RFC7643.VALUE).isArray(), arrayValueFirst.toString());
    Assertions.assertEquals(JsonHelper.readJsonDocument(arrayValueFirst.toString()),
                            JsonHelper.readJsonDocument(arrayPathFirst.toString()),
                            "order must not affect path-based multivalued array");
  }

}
