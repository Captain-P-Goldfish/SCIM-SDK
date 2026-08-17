package de.captaingoldfish.scim.sdk.client.builder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * Verifies the wire format of PATCH requests immediately before they are sent to the remote SCIM provider.
 * <p>
 * {@link PatchRequestOperation} instances should preserve the representation explicitly supplied by the
 * caller while they are being built. Optional normalization of pathless PATCH operations is applied only
 * immediately before transmission if enabled through {@link ScimClientConfig}.
 * </p>
 * <p>
 * Path-based operations must preserve the JSON type explicitly supplied by the caller regardless of whether
 * pathless PATCH normalization is enabled. Pathless operations containing an already valid object value must
 * likewise remain unchanged.
 * </p>
 * <p>
 * If pathless PATCH normalization is enabled, a singleton array containing a resource object is unwrapped
 * immediately before transmission. If normalization is disabled, the representation supplied by the caller is
 * transmitted unchanged.
 * </p>
 *
 * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
 */
public class PatchBuilderWireFormatTest
{

  private ScimHttpClient scimHttpClient;

  private PatchBuilder<User> patchBuilder;

  /**
   * Initializes the PATCH builder and mocked HTTP client with the requested pathless PATCH normalization
   * setting.
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH operations should be normalized before
   *          transmission
   */
  private void initialize(boolean normalizePathlessPatchOperations)
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .normalizePathlessPatchOperations(normalizePathlessPatchOperations)
                                                        .build();
    scimHttpClient = Mockito.spy(new ScimHttpClient(scimClientConfig));

    HttpResponse httpResponse = HttpResponse.builder()
                                            .httpStatusCode(HttpStatus.NO_CONTENT)
                                            .responseHeaders(Collections.singletonMap(HttpHeader.CONTENT_TYPE_HEADER,
                                                                                      HttpHeader.SCIM_CONTENT_TYPE))
                                            .build();

    Mockito.doReturn(httpResponse).when(scimHttpClient).sendRequest(Mockito.any(HttpUriRequest.class));

    patchBuilder = new PatchBuilder<>("http://localhost:8180/scim/v2", EndpointPaths.USERS, "user-id", User.class,
                                      scimHttpClient);
  }

  /**
   * Verifies that an explicitly supplied textual value remains a scalar when a path is present. The result must
   * be independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "preferredLanguage",
   *   "value": "de"
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based textual valueNode is sent unchanged")
  public void testPathBasedTextualValueNodeIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation().op(PatchOp.REPLACE).path("preferredLanguage").valueNode(TextNode.valueOf("de")).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isTextual(), value.toPrettyString());
    Assertions.assertEquals("de", value.textValue());
  }

  /**
   * Verifies that a single value supplied through the convenience {@code value} method is transmitted as a
   * scalar when a path is present. The result must be independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "preferredLanguage",
   *   "value": "de"
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based single value is sent as scalar")
  public void testPathBasedSingleValueIsSentAsScalar(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation().op(PatchOp.REPLACE).path("preferredLanguage").value("de").build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isTextual(), value.toPrettyString());
    Assertions.assertEquals("de", value.textValue());
  }

  /**
   * Numeric strings such as {@code "67890"} must retain their JSON string type and must not be converted into
   * numeric JSON values. The result must be independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "preferredLanguage",
   *   "value": "67890"
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based numeric string keeps its string type")
  public void testPathBasedNumericStringKeepsStringType(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation()
                .op(PatchOp.REPLACE)
                .path("preferredLanguage")
                .valueNode(TextNode.valueOf("67890"))
                .build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isTextual(), value.toPrettyString());
    Assertions.assertEquals("67890", value.textValue());
  }

  /**
   * Verifies that a numeric JSON value retains its type when a path is present. The result must be independent
   * of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "someNumber",
   *   "value": 42
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based numeric value is sent unchanged")
  public void testPathBasedNumericValueIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation().op(PatchOp.REPLACE).path("someNumber").valueNode(IntNode.valueOf(42)).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isIntegralNumber(), value.toPrettyString());
    Assertions.assertEquals(42, value.intValue());
  }

  /**
   * Verifies that a boolean JSON value retains its type when a path is present. The result must be independent
   * of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "active",
   *   "value": false
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based boolean value is sent unchanged")
  public void testPathBasedBooleanValueIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation().op(PatchOp.REPLACE).path("active").valueNode(BooleanNode.FALSE).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isBoolean(), value.toPrettyString());
    Assertions.assertFalse(value.booleanValue());
  }

  /**
   * Verifies that an object supplied for a path-based operation remains an object. The result must be
   * independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "name",
   *   "value": {
   *     "givenName": "Link"
   *   }
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based object is sent unchanged")
  public void testPathBasedObjectIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    ObjectNode name = JsonNodeFactory.instance.objectNode();
    name.put("givenName", "Link");

    patchBuilder.addOperation().op(PatchOp.REPLACE).path("name").valueNode(name).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isObject(), value.toPrettyString());
    Assertions.assertEquals("Link", value.get("givenName").textValue());
  }

  /**
   * Verifies that an explicitly supplied singleton array remains an array when a path is present.
   * <p>
   * A singleton array may represent a multi-valued SCIM attribute containing exactly one value. Its
   * representation must therefore not be changed based on the number of contained elements.
   * </p>
   *
   * <pre>{@code
   * {
   *   "op": "add",
   *   "path": "roles",
   *   "value": ["ADMIN"]
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based singleton scalar array is sent unchanged")
  public void testPathBasedSingletonScalarArrayIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    ArrayNode roles = JsonNodeFactory.instance.arrayNode();
    roles.add("ADMIN");

    patchBuilder.addOperation().op(PatchOp.ADD).path("roles").valueNode(roles).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isArray(), value.toPrettyString());
    Assertions.assertEquals(1, value.size());
    Assertions.assertEquals("ADMIN", value.get(0).textValue());
  }

  /**
   * Verifies that a singleton array created through the {@code valueNodes} convenience method remains an array
   * when a path is present. The result must be independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "add",
   *   "path": "roles",
   *   "value": ["ADMIN"]
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based singleton valueNodes array is sent unchanged")
  public void testPathBasedSingletonValueNodesArrayIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation()
                .op(PatchOp.ADD)
                .path("roles")
                .valueNodes(Collections.singletonList(TextNode.valueOf("ADMIN")))
                .build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isArray(), value.toPrettyString());
    Assertions.assertEquals(1, value.size());
    Assertions.assertEquals("ADMIN", value.get(0).textValue());
  }

  /**
   * Verifies that a singleton array containing an object remains an array when a path is present. The result
   * must be independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "add",
   *   "path": "emails",
   *   "value": [{
   *     "value": "link@hyrule.example",
   *     "type": "work"
   *   }]
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based singleton object array is sent unchanged")
  public void testPathBasedSingletonObjectArrayIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    ObjectNode email = JsonNodeFactory.instance.objectNode();
    email.put("value", "link@hyrule.example");
    email.put("type", "work");

    ArrayNode emails = JsonNodeFactory.instance.arrayNode();
    emails.add(email);

    patchBuilder.addOperation().op(PatchOp.ADD).path("emails").valueNode(emails).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isArray(), value.toPrettyString());
    Assertions.assertEquals(1, value.size());
    Assertions.assertTrue(value.get(0).isObject(), value.toPrettyString());
    Assertions.assertEquals("link@hyrule.example", value.get(0).get("value").textValue());
    Assertions.assertEquals("work", value.get(0).get("type").textValue());
  }

  /**
   * Verifies that an array containing multiple values remains unchanged when a path is present. The result must
   * be independent of pathless PATCH normalization.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "tags",
   *   "value": ["tag-a", "tag-b"]
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based multiple values are sent as array")
  public void testPathBasedMultipleValuesAreSentAsArray(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation().op(PatchOp.REPLACE).path("tags").values(Arrays.asList("tag-a", "tag-b")).build();

    JsonNode value = sendAndGetValue();

    Assertions.assertTrue(value.isArray(), value.toPrettyString());
    Assertions.assertEquals(2, value.size());
    Assertions.assertEquals("tag-a", value.get(0).textValue());
    Assertions.assertEquals("tag-b", value.get(1).textValue());
  }

  /**
   * Verifies that an already valid object supplied for a pathless operation is transmitted unchanged regardless
   * of whether pathless PATCH normalization is enabled.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "value": {
   *     "preferredLanguage": "de",
   *     "active": true
   *   }
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Pathless object is sent unchanged")
  public void testPathlessObjectIsSentUnchanged(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");
    attributes.put("active", true);

    patchBuilder.addOperation().op(PatchOp.REPLACE).valueNode(attributes).build();

    JsonNode operation = sendAndGetOperation();

    Assertions.assertFalse(operation.has("path"), operation.toPrettyString());

    JsonNode value = operation.get(AttributeNames.RFC7643.VALUE);
    Assertions.assertNotNull(value);
    Assertions.assertTrue(value.isObject(), operation.toPrettyString());
    Assertions.assertEquals("de", value.get("preferredLanguage").textValue());
    Assertions.assertTrue(value.get("active").booleanValue());
  }

  /**
   * Verifies that a singleton array containing a resource object remains untouched while the operation is being
   * built and is unwrapped immediately before transmission when pathless PATCH normalization is enabled.
   *
   * <pre>{@code
   * Built:
   * {
   *   "op": "replace",
   *   "value": [{
   *     "preferredLanguage": "de"
   *   }]
   * }
   *
   * Sent:
   * {
   *   "op": "replace",
   *   "value": {
   *     "preferredLanguage": "de"
   *   }
   * }
   * }</pre>
   */
  @Test
  @DisplayName("Pathless singleton object array is unwrapped when normalization is enabled")
  public void testPathlessSingletonObjectArrayIsUnwrappedWhenNormalizationIsEnabled()
  {
    initialize(true);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");

    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    values.add(attributes);

    patchBuilder.addOperation().op(PatchOp.REPLACE).valueNode(values).build();

    // Building the PATCH operation must not implicitly alter the supplied value.
    JsonNode builtValue = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(builtValue.isArray(), patchBuilder.getResource());
    Assertions.assertEquals(1, builtValue.size());
    Assertions.assertTrue(builtValue.get(0).isObject(), patchBuilder.getResource());

    JsonNode sentOperation = sendAndGetOperation();
    JsonNode sentValue = sentOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertFalse(sentOperation.has("path"), sentOperation.toPrettyString());
    Assertions.assertNotNull(sentValue);
    Assertions.assertTrue(sentValue.isObject(), sentOperation.toPrettyString());
    Assertions.assertEquals("de", sentValue.get("preferredLanguage").textValue());
  }

  /**
   * Verifies that a singleton array containing a resource object is transmitted unchanged when pathless PATCH
   * normalization is disabled.
   *
   * <pre>{@code
   * Built and sent:
   * {
   *   "op": "replace",
   *   "value": [{
   *     "preferredLanguage": "de"
   *   }]
   * }
   * }</pre>
   */
  @Test
  @DisplayName("Pathless singleton object array remains unchanged when normalization is disabled")
  public void testPathlessSingletonObjectArrayRemainsUnchangedWhenNormalizationIsDisabled()
  {
    initialize(false);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");

    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    values.add(attributes);

    patchBuilder.addOperation().op(PatchOp.REPLACE).valueNode(values).build();

    JsonNode builtValue = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(builtValue.isArray(), patchBuilder.getResource());
    Assertions.assertEquals(1, builtValue.size());
    Assertions.assertTrue(builtValue.get(0).isObject(), patchBuilder.getResource());

    JsonNode sentOperation = sendAndGetOperation();
    JsonNode sentValue = sentOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertFalse(sentOperation.has("path"), sentOperation.toPrettyString());
    Assertions.assertNotNull(sentValue);
    Assertions.assertTrue(sentValue.isArray(), sentOperation.toPrettyString());
    Assertions.assertEquals(1, sentValue.size());
    Assertions.assertTrue(sentValue.get(0).isObject(), sentOperation.toPrettyString());
    Assertions.assertEquals("de", sentValue.get(0).get("preferredLanguage").textValue());
  }

  /**
   * Verifies that a singleton object array created through the public {@code valueNodes} API is unwrapped
   * before transmission when pathless PATCH normalization is enabled.
   */
  @Test
  @DisplayName("Pathless singleton valueNodes object array is unwrapped when normalization is enabled")
  public void testPathlessSingletonValueNodesObjectArrayIsUnwrappedWhenNormalizationIsEnabled()
  {
    initialize(true);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");

    patchBuilder.addOperation().op(PatchOp.REPLACE).valueNodes(Collections.singletonList(attributes)).build();

    JsonNode builtValue = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(builtValue.isArray(), patchBuilder.getResource());

    JsonNode sentValue = sendAndGetValue();

    Assertions.assertTrue(sentValue.isObject(), sentValue.toPrettyString());
    Assertions.assertEquals("de", sentValue.get("preferredLanguage").textValue());
  }

  /**
   * Verifies that a singleton object array created through the public {@code valueNodes} API remains unchanged
   * during transmission when pathless PATCH normalization is disabled.
   */
  @Test
  @DisplayName("Pathless singleton valueNodes object array remains unchanged when normalization is disabled")
  public void testPathlessSingletonValueNodesObjectArrayRemainsUnchangedWhenNormalizationIsDisabled()
  {
    initialize(false);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");

    patchBuilder.addOperation().op(PatchOp.REPLACE).valueNodes(Collections.singletonList(attributes)).build();

    JsonNode builtValue = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(builtValue.isArray(), patchBuilder.getResource());

    JsonNode sentValue = sendAndGetValue();

    Assertions.assertTrue(sentValue.isArray(), sentValue.toPrettyString());
    Assertions.assertEquals(1, sentValue.size());
    Assertions.assertTrue(sentValue.get(0).isObject(), sentValue.toPrettyString());
    Assertions.assertEquals("de", sentValue.get(0).get("preferredLanguage").textValue());
  }

  /**
   * Verifies that the final state of an operation determines its wire representation.
   * <p>
   * If an operation receives a path after it was initially built without one, an explicitly supplied singleton
   * array must remain an array during transmission regardless of whether pathless PATCH normalization is
   * enabled.
   * </p>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Singleton array remains unchanged when path is assigned after build")
  public void testSingletonArrayRemainsUnchangedWhenPathIsAssignedAfterBuild(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    ArrayNode roles = JsonNodeFactory.instance.arrayNode();
    roles.add("ADMIN");

    PatchRequestOperation operation = PatchRequestOperation.builder().op(PatchOp.ADD).valueNode(roles).build();
    operation.setPath("roles");

    patchBuilder.setPatchResource(PatchOpRequest.builder().operations(Collections.singletonList(operation)).build());

    JsonNode sentOperation = sendAndGetOperation();
    JsonNode sentValue = sentOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertEquals("roles", sentOperation.get("path").textValue());
    Assertions.assertNotNull(sentValue);
    Assertions.assertTrue(sentValue.isArray(), sentOperation.toPrettyString());
    Assertions.assertEquals(1, sentValue.size());
    Assertions.assertEquals("ADMIN", sentValue.get(0).textValue());
  }

  /**
   * Verifies that normalization is applied individually according to the final path of each operation when
   * pathless PATCH normalization is enabled.
   */
  @Test
  @DisplayName("Each PATCH operation is normalized according to its final path when normalization is enabled")
  public void testEachPatchOperationIsNormalizedAccordingToItsFinalPathWhenNormalizationIsEnabled()
  {
    initialize(true);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");

    ArrayNode resourceValues = JsonNodeFactory.instance.arrayNode();
    resourceValues.add(attributes);

    ArrayNode roles = JsonNodeFactory.instance.arrayNode();
    roles.add("ADMIN");

    patchBuilder.addOperation()
                .op(PatchOp.REPLACE)
                .valueNode(resourceValues)
                .next()
                .op(PatchOp.ADD)
                .path("roles")
                .valueNode(roles)
                .build();

    JsonNode patchRequest = sendAndGetPatchRequest();
    JsonNode operations = patchRequest.get(AttributeNames.RFC7643.OPERATIONS);

    Assertions.assertNotNull(operations);
    Assertions.assertTrue(operations.isArray(), patchRequest.toPrettyString());
    Assertions.assertEquals(2, operations.size());

    JsonNode pathlessOperation = operations.get(0);
    JsonNode pathlessValue = pathlessOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertFalse(pathlessOperation.has("path"), pathlessOperation.toPrettyString());
    Assertions.assertTrue(pathlessValue.isObject(), pathlessOperation.toPrettyString());
    Assertions.assertEquals("de", pathlessValue.get("preferredLanguage").textValue());

    JsonNode pathBasedOperation = operations.get(1);
    JsonNode pathBasedValue = pathBasedOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertEquals("roles", pathBasedOperation.get("path").textValue());
    Assertions.assertTrue(pathBasedValue.isArray(), pathBasedOperation.toPrettyString());
    Assertions.assertEquals(1, pathBasedValue.size());
    Assertions.assertEquals("ADMIN", pathBasedValue.get(0).textValue());
  }

  /**
   * Verifies that operations retain their supplied representation during transmission when pathless PATCH
   * normalization is disabled.
   */
  @Test
  @DisplayName("PATCH operations remain unchanged when normalization is disabled")
  public void testPatchOperationsRemainUnchangedWhenNormalizationIsDisabled()
  {
    initialize(false);

    ObjectNode attributes = JsonNodeFactory.instance.objectNode();
    attributes.put("preferredLanguage", "de");

    ArrayNode resourceValues = JsonNodeFactory.instance.arrayNode();
    resourceValues.add(attributes);

    ArrayNode roles = JsonNodeFactory.instance.arrayNode();
    roles.add("ADMIN");

    patchBuilder.addOperation()
                .op(PatchOp.REPLACE)
                .valueNode(resourceValues)
                .next()
                .op(PatchOp.ADD)
                .path("roles")
                .valueNode(roles)
                .build();

    JsonNode patchRequest = sendAndGetPatchRequest();
    JsonNode operations = patchRequest.get(AttributeNames.RFC7643.OPERATIONS);

    Assertions.assertNotNull(operations);
    Assertions.assertTrue(operations.isArray(), patchRequest.toPrettyString());
    Assertions.assertEquals(2, operations.size());

    JsonNode pathlessOperation = operations.get(0);
    JsonNode pathlessValue = pathlessOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertFalse(pathlessOperation.has("path"), pathlessOperation.toPrettyString());
    Assertions.assertTrue(pathlessValue.isArray(), pathlessOperation.toPrettyString());
    Assertions.assertEquals(1, pathlessValue.size());
    Assertions.assertTrue(pathlessValue.get(0).isObject(), pathlessOperation.toPrettyString());
    Assertions.assertEquals("de", pathlessValue.get(0).get("preferredLanguage").textValue());

    JsonNode pathBasedOperation = operations.get(1);
    JsonNode pathBasedValue = pathBasedOperation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertEquals("roles", pathBasedOperation.get("path").textValue());
    Assertions.assertTrue(pathBasedValue.isArray(), pathBasedOperation.toPrettyString());
    Assertions.assertEquals(1, pathBasedValue.size());
    Assertions.assertEquals("ADMIN", pathBasedValue.get(0).textValue());
  }

  /**
   * Verifies that a remove operation with a path is transmitted without an artificial value attribute
   * regardless of whether pathless PATCH normalization is enabled.
   *
   * <pre>{@code
   * {
   *   "op": "remove",
   *   "path": "nickName"
   * }
   * }</pre>
   *
   * @param normalizePathlessPatchOperations whether pathless PATCH normalization is enabled
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Path-based remove operation is sent without value")
  public void testPathBasedRemoveOperationIsSentWithoutValue(boolean normalizePathlessPatchOperations)
  {
    initialize(normalizePathlessPatchOperations);

    patchBuilder.addOperation().op(PatchOp.REMOVE).path("nickName").build();

    JsonNode operation = sendAndGetOperation();

    Assertions.assertEquals("remove", operation.get("op").textValue());
    Assertions.assertEquals("nickName", operation.get("path").textValue());
    Assertions.assertFalse(operation.has(AttributeNames.RFC7643.VALUE), operation.toPrettyString());
  }

  /**
   * Sends the PATCH request through the regular public {@link PatchBuilder#sendRequest()} API and returns the
   * first serialized operation that would have been sent to the remote SCIM provider.
   */
  private JsonNode sendAndGetOperation()
  {
    JsonNode patchRequest = sendAndGetPatchRequest();
    JsonNode operations = patchRequest.get(AttributeNames.RFC7643.OPERATIONS);

    Assertions.assertNotNull(operations, patchRequest.toPrettyString());
    Assertions.assertTrue(operations.isArray(), patchRequest.toPrettyString());
    Assertions.assertFalse(operations.isEmpty(), patchRequest.toPrettyString());

    return operations.get(0);
  }

  /**
   * Sends the PATCH request and returns the value of its first operation.
   */
  private JsonNode sendAndGetValue()
  {
    JsonNode operation = sendAndGetOperation();
    JsonNode value = operation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertNotNull(value, operation.toPrettyString());
    return value;
  }

  /**
   * Sends the PATCH request and returns the complete JSON document that was passed to the HTTP client.
   */
  private JsonNode sendAndGetPatchRequest()
  {
    HttpUriRequest request = sendAndGetRequest();
    return JsonHelper.readJsonDocument(getRequestBody(request));
  }

  /**
   * Sends the request through the regular public API and captures the actual HTTP request immediately before it
   * would have been sent to the remote SCIM provider.
   */
  private HttpUriRequest sendAndGetRequest()
  {
    patchBuilder.sendRequest();

    ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
    Mockito.verify(scimHttpClient, Mockito.times(1)).sendRequest(requestCaptor.capture());

    return requestCaptor.getValue();
  }

  /**
   * Extracts the serialized PATCH body from the captured HTTP request.
   */
  private String getRequestBody(HttpUriRequest request)
  {
    Assertions.assertTrue(request instanceof HttpPatch, "Expected HttpPatch but got: " + request.getClass());

    HttpPatch httpPatch = (HttpPatch)request;
    Assertions.assertNotNull(httpPatch.getEntity());

    try
    {
      return EntityUtils.toString(httpPatch.getEntity(), StandardCharsets.UTF_8);
    }
    catch (IOException ex)
    {
      throw new IllegalStateException("Could not read PATCH request body", ex);
    }
  }

  /**
   * Returns the value of the first operation from an already serialized PATCH resource.
   */
  private JsonNode getSerializedValue(String patchRequestResource)
  {
    JsonNode patchRequest = JsonHelper.readJsonDocument(patchRequestResource);
    return patchRequest.get(AttributeNames.RFC7643.OPERATIONS).get(0).get(AttributeNames.RFC7643.VALUE);
  }
}
