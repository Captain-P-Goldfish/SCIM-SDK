package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * Verifies the wire format of patch requests built through the public {@link PatchBuilder} API. Path-based
 * single values must be serialized as scalars and not as singleton arrays in order to stay RFC 7644
 * compliant.
 *
 * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/968">SCIM-SDK issue #968</a>
 */
public class PatchBuilderWireFormatTest
{

  private PatchBuilder<User> createPatchBuilder()
  {
    ScimHttpClient scimHttpClient = new ScimHttpClient(ScimClientConfig.builder().build());
    return new PatchBuilder<>("http://localhost:8180/scim/v2", EndpointPaths.USERS, "user-id", User.class,
                              scimHttpClient);
  }

  /**
   * <pre>{@code
   * {"op":"replace","path":"preferredLanguage","value":"de"}
   * }</pre>
   */
  @Test
  @DisplayName("Path-based scalar valueNode is serialized as a scalar")
  public void testPathBasedScalarValueNodeIsSerializedAsScalar()
  {
    PatchBuilder<User> patchBuilder = createPatchBuilder();
    patchBuilder.addOperation().op(PatchOp.REPLACE).path("preferredLanguage").valueNode(TextNode.valueOf("de")).build();

    JsonNode valueNode = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(valueNode.isTextual(), patchBuilder.getResource());
    Assertions.assertEquals("de", valueNode.textValue());
  }

  /**
   * Numeric strings such as {@code "67890"} must keep their JSON string type and must not be converted into
   * numbers during serialization or value access.
   *
   * <pre>{@code
   * {"op":"replace","path":"preferredLanguage","value":"67890"}
   * }</pre>
   */
  @Test
  @DisplayName("Numeric string values keep their string type")
  public void testNumericStringValueKeepsStringType()
  {
    PatchBuilder<User> patchBuilder = createPatchBuilder();
    patchBuilder.addOperation()
                .op(PatchOp.REPLACE)
                .path("preferredLanguage")
                .valueNode(TextNode.valueOf("67890"))
                .build();

    JsonNode valueNode = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(valueNode.isTextual(), patchBuilder.getResource());
    Assertions.assertEquals("67890", valueNode.textValue());
  }

  /**
   * Multiple values must remain an array since multi-valued attributes require the array representation.
   *
   * <pre>{@code
   * {"op":"replace","path":"tags","value":["tag-a","tag-b"]}
   * }</pre>
   */
  @Test
  @DisplayName("Multiple values are serialized as an array")
  public void testMultipleValuesAreSerializedAsArray()
  {
    PatchBuilder<User> patchBuilder = createPatchBuilder();
    patchBuilder.addOperation().op(PatchOp.REPLACE).path("tags").values(Arrays.asList("tag-a", "tag-b")).build();

    JsonNode valueNode = getSerializedValue(patchBuilder.getResource());
    Assertions.assertTrue(valueNode.isArray(), patchBuilder.getResource());
    Assertions.assertEquals(2, valueNode.size());
  }

  private JsonNode getSerializedValue(String patchRequestResource)
  {
    JsonNode patchRequest = JsonHelper.readJsonDocument(patchRequestResource);
    return patchRequest.get("Operations").get(0).get("value");
  }
}
