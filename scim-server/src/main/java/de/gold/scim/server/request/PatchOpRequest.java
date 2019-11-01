package de.gold.scim.server.request;

import java.util.Collections;
import java.util.List;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.constants.SchemaUris;
import de.gold.scim.server.resources.AbstractSchemasHolder;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 08:31 <br>
 * <br>
 * HTTP PATCH is an OPTIONAL server function that enables clients to update one or more attributes of a SCIM
 * resource using a sequence of operations to "add", "remove", or "replace" values.Clients may discover
 * service provider support for PATCH by querying the service provider configuration
 */
public class PatchOpRequest extends AbstractSchemasHolder
{

  public PatchOpRequest()
  {
    setSchemas(Collections.singletonList(SchemaUris.PATCH_OP));
  }

  @Builder
  public PatchOpRequest(List<PatchRequestOperation> operations)
  {
    this();
    setOperations(operations);
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public List<PatchRequestOperation> getOperations()
  {
    return getArrayAttribute(AttributeNames.RFC7643.OPERATIONS, PatchRequestOperation.class);
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public void setOperations(List<PatchRequestOperation> operations)
  {
    setAttribute(AttributeNames.RFC7643.OPERATIONS, operations);
  }
}
