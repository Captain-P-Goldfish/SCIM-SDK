package de.captaingoldfish.scim.sdk.server.patch;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;


/**
 * Regression tests for PATCH replace operations targeting a sub-attribute of a multi-valued complex attribute
 * through a value filter.
 * <p>
 * Scalar PATCH values containing whitespace must retain their complete value while the
 * {@link PatchRequestOperation} is built and when the operation is subsequently applied to the resource.
 * </p>
 */
public class PatchFilteredSubAttributeReplaceRegressionTest
{

  private ResourceEndpoint resourceEndpoint;

  /**
   * Initializes a resource endpoint with PATCH support.
   */
  @BeforeEach
  public void initialize()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .patchConfig(PatchConfig.builder().supported(true).build())
                                                     .build();
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
  }

  /**
   * Verifies that building a path-based PATCH operation does not modify a multi-word scalar value.
   * <p>
   * This specifically covers the construction path used by {@link PatchRequestOperation#builder()}. The
   * presence of a path must not cause the textual value to be parsed, wrapped or otherwise modified.
   * </p>
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "addresses[type eq \"work\"].streetAddress",
   *   "value": "7070 Phoebe Hollow"
   * }
   * }</pre>
   */
  @Test
  @DisplayName("Path-based multi-word scalar remains unchanged while operation is built")
  public void testPathBasedMultiWordScalarRemainsUnchangedWhileOperationIsBuilt()
  {
    final String newStreetAddress = "7070 Phoebe Hollow";

    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.REPLACE)
                                                           .path("addresses[type eq \"work\"].streetAddress")
                                                           .valueNode(TextNode.valueOf(newStreetAddress))
                                                           .build();

    JsonNode value = operation.get(AttributeNames.RFC7643.VALUE);

    Assertions.assertNotNull(value, operation.toPrettyString());
    Assertions.assertTrue(value.isTextual(), operation.toPrettyString());
    Assertions.assertEquals(newStreetAddress, value.textValue(), operation.toPrettyString());
  }

  /**
   * Verifies that replacing a simple sub-attribute of a filtered multi-valued complex attribute preserves the
   * complete textual value, including all whitespace.
   *
   * <pre>{@code
   * {
   *   "op": "replace",
   *   "path": "addresses[type eq \"work\"].streetAddress",
   *   "value": "7070 Phoebe Hollow"
   * }
   * }</pre>
   * <p>
   * Only the address selected by the filter must be modified. Other attributes of the selected address and all
   * non-matching addresses must remain unchanged.
   * </p>
   *
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/972">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/972</a>
   */
  @Test
  @DisplayName("Filtered replace preserves complete multi-word sub-attribute value")
  public void testFilteredReplacePreservesCompleteMultiWordSubAttributeValue()
  {
    final String resourceId = "12345289-86cf-21c5-a6dd-de6daebd4eae";
    final String newStreetAddress = "7070 Phoebe Hollow";

    UserHandlerImpl userHandler = new UserHandlerImpl(false);
    resourceEndpoint.registerEndpoint(new UserEndpointDefinition(userHandler));

    Address workAddress = Address.builder().type("work").streetAddress("910 Broadway").locality("New York").build();

    Address homeAddress = Address.builder().type("home").streetAddress("1 Home Lane").build();

    User user = User.builder()
                    .id(resourceId)
                    .userName("John Smith")
                    .addresses(Arrays.asList(workAddress, homeAddress))
                    .build();

    userHandler.getInMemoryMap().put(resourceId, user);

    PatchRequestOperation operation = PatchRequestOperation.builder()
                                                           .op(PatchOp.REPLACE)
                                                           .path("addresses[type eq \"work\"].streetAddress")
                                                           .valueNode(TextNode.valueOf(newStreetAddress))
                                                           .build();

    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();

    PatchRequestHandler<User> patchRequestHandler = new PatchRequestHandler<>(resourceId, userHandler,
                                                                              resourceEndpoint.getPatchWorkarounds(),
                                                                              new Context(null));

    User patchedUser = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertTrue(patchRequestHandler.isResourceChanged(), patchedUser.toPrettyString());
    Assertions.assertEquals(2, patchedUser.getAddresses().size(), patchedUser.toPrettyString());

    Address patchedWorkAddress = patchedUser.getAddresses()
                                            .stream()
                                            .filter(address -> "work".equals(address.getType().orElse(null)))
                                            .findFirst()
                                            .orElseThrow(AssertionError::new);

    Assertions.assertEquals(newStreetAddress,
                            patchedWorkAddress.getStreetAddress().orElse(null),
                            patchedUser.toPrettyString());
    Assertions.assertEquals("New York", patchedWorkAddress.getLocality().orElse(null), patchedUser.toPrettyString());

    Address patchedHomeAddress = patchedUser.getAddresses()
                                            .stream()
                                            .filter(address -> "home".equals(address.getType().orElse(null)))
                                            .findFirst()
                                            .orElseThrow(AssertionError::new);

    Assertions.assertEquals("1 Home Lane",
                            patchedHomeAddress.getStreetAddress().orElse(null),
                            patchedUser.toPrettyString());
  }
}
