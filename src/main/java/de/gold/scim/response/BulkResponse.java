package de.gold.scim.response;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.resources.AbstractSchemasHolder;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 20:34 <br>
 * <br>
 * represents a simple bulk response
 */
@NoArgsConstructor
public class BulkResponse extends ScimResponse
{

  /**
   * this is the actual response object
   */
  private BulkResponseObject response = new BulkResponseObject();

  /**
   * the http status that should be set with this response
   */
  private int httpStatus;

  @Builder
  public BulkResponse(List<BulkResponseOperation> bulkResponseOperation, Integer httpStatus)
  {
    setBulkResponseOperations(bulkResponseOperation);
    this.httpStatus = httpStatus == null ? HttpStatus.SC_OK : httpStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return httpStatus;
  }

  /**
   * @return the actual response object as {@link JsonNode}
   */
  public BulkResponseObject getAsJsonNode()
  {
    return response;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    return response.toString();
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public List<BulkResponseOperation> getBulkResponseOperations()
  {
    return response.getBulkResponseOperations();
  }

  /**
   * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
   * resource endpoint. REQUIRED.
   */
  public void setBulkResponseOperations(List<BulkResponseOperation> bulkRequestOperations)
  {
    response.setBulkResponseOperations(bulkRequestOperations);
  }

  /**
   * @return the list of schemas witin this resource
   */
  public List<String> getSchemas()
  {
    return response.getSchemas();
  }

  /**
   * delegated method to {@link JsonNode}
   */
  public JsonNode get(String fieldName)
  {
    return response.get(fieldName);
  }

  /**
   * a proxy class that shall solve the problem that the class {@link BulkResponse} must inherit from
   * {@link ScimResponse} instead of {@link AbstractSchemasHolder}
   */
  private static class BulkResponseObject extends AbstractSchemasHolder
  {

    public BulkResponseObject()
    {
      setSchemas(Collections.singletonList(SchemaUris.BULK_RESPONSE_URI));
    }

    /**
     * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
     * resource endpoint. REQUIRED.
     */
    public List<BulkResponseOperation> getBulkResponseOperations()
    {
      List<BulkResponseOperation> operations = getArrayAttribute(AttributeNames.RFC7643.OPERATIONS,
                                                                 BulkResponseOperation.class);
      if (operations.isEmpty())
      {
        setAttribute(AttributeNames.RFC7643.OPERATIONS, operations);
      }
      return operations;
    }

    /**
     * Defines operations within a bulk job. Each operation corresponds to a single HTTP request against a
     * resource endpoint. REQUIRED.
     */
    public void setBulkResponseOperations(List<BulkResponseOperation> bulkRequestOperations)
    {
      setAttribute(AttributeNames.RFC7643.OPERATIONS, bulkRequestOperations);
    }

  }
}
