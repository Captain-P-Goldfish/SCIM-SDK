package de.captaingoldfish.scim.sdk.common.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 22:04 <br>
 * <br>
 * represents a list response
 */
public class ListResponse<T extends ScimObjectNode> extends ScimResponse
{

  /**
   * the generic type of this class
   */
  private final Class<T> type;

  public ListResponse()
  {
    this.type = getGenericType();
  }

  public ListResponse(Class<T> type)
  {
    this.type = type;
  }

  public ListResponse(String resourceJsonRepresentation)
  {
    super(JsonHelper.readJsonDocument(resourceJsonRepresentation));
    this.type = getGenericType();
  }

  public ListResponse(List<JsonNode> listedResources, Long totalResults, Integer itemsPerPage, Long startIndex)
  {
    super(null);
    setSchemasAttribute();
    setTotalResults(totalResults);
    setItemsPerPage(itemsPerPage);
    setStartIndex(startIndex);
    setListedResources(listedResources);
    this.type = getGenericType();
  }

  /**
   * tries to get the generic type of this response class
   */
  private Class<T> getGenericType()
  {
    Type type = getClass().getGenericSuperclass();
    if (type instanceof ParameterizedType)
    {
      ParameterizedType parameterizedType = (ParameterizedType)type;
      return (Class<T>)parameterizedType.getActualTypeArguments()[0];
    }
    else
    {
      return (Class<T>)ScimObjectNode.class;
    }
  }

  /**
   * sets the schemas attribute into the listed response
   */
  private void setSchemasAttribute()
  {
    ArrayNode schemas = new ArrayNode(JsonNodeFactory.instance);
    schemas.add(SchemaUris.LIST_RESPONSE_URI);
    JsonHelper.addAttribute(this, AttributeNames.RFC7643.SCHEMAS, schemas);
  }

  /**
   * Non-negative integer. Specifies the total number of results matching the client query, e.g., 1000
   */
  public long getTotalResults()
  {
    return getLongAttribute(AttributeNames.RFC7643.TOTAL_RESULTS).orElse(0L);
  }

  /**
   * Non-negative integer. Specifies the total number of results matching the client query, e.g., 1000
   */
  public void setTotalResults(Long totalResults)
  {
    setAttribute(AttributeNames.RFC7643.TOTAL_RESULTS, totalResults);
  }

  /**
   * Non-negative integer. Specifies the total number of results matching the client query, e.g., 1000
   */
  public int getItemsPerPage()
  {
    return getIntegerAttribute(AttributeNames.RFC7643.ITEMS_PER_PAGE).orElse(0);
  }

  /**
   * Non-negative integer. Specifies the number of query results returned in a query response page, e.g., 10.
   */
  public void setItemsPerPage(Integer itemsPerPage)
  {
    setAttribute(AttributeNames.RFC7643.ITEMS_PER_PAGE, itemsPerPage);
  }

  /**
   * The 1-based index of the first result in the current set of query results, e.g., 1.
   */
  public long getStartIndex()
  {
    return getLongAttribute(AttributeNames.RFC7643.START_INDEX).orElse(1L);
  }

  /**
   * The 1-based index of the first result in the current set of query results, e.g., 1.
   */
  public void setStartIndex(Long startIndex)
  {
    setAttribute(AttributeNames.RFC7643.START_INDEX, startIndex);
  }

  /**
   * the resources that have been extracted
   */
  public List<T> getListedResources()
  {
    return getArrayAttribute(AttributeNames.RFC7643.RESOURCES, type);
  }

  /**
   * the resources that have been extracted
   */
  public void setListedResources(List<JsonNode> listedResources)
  {
    setAttribute(AttributeNames.RFC7643.RESOURCES, listedResources);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.OK;
  }
}
