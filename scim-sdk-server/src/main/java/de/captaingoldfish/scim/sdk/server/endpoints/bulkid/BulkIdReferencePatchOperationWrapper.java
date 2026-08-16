package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import lombok.Getter;


/**
 * A bulkId reference wrapper for a scalar PATCH value. Scalar values are exposed by
 * {@link PatchRequestOperation#getValueNode()} through a temporary array view, so replacing an element in
 * that view would not update the operation itself. This wrapper writes the resolved value directly into the
 * operation while preserving its scalar JSON representation.
 *
 * @author Pascal Knueppel
 * @since 16.08.2026
 */
public class BulkIdReferencePatchOperationWrapper implements BulkIdReferenceWrapper
{

  private final PatchRequestOperation operation;

  @Getter
  private final String bulkId;

  public BulkIdReferencePatchOperationWrapper(PatchRequestOperation operation, JsonNode valueNode)
  {
    this.operation = operation;
    this.bulkId = valueNode.textValue().replaceFirst(String.format("^%s:", AttributeNames.RFC7643.BULK_ID), "");
  }

  @Override
  public void replaceValueNode(String newValue)
  {
    operation.setValue(TextNode.valueOf(newValue));
  }
}
