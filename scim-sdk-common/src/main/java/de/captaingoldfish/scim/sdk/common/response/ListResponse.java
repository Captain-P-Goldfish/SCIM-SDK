package de.captaingoldfish.scim.sdk.common.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

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
    this(listedResources, totalResults, itemsPerPage, startIndex, null, null);
  }

  /**
   * Constructs a list response with optional cursor-based pagination attributes. Passing {@code null} for
   * {@code startIndex} omits it from the JSON representation, as expected by RFC 9865 cursor-paginated
   * responses. Passing {@code null} for {@code nextCursor} or {@code previousCursor} omits the corresponding
   * attribute.
   *
   * @param listedResources the resources to include in the page
   * @param totalResults the total number of results matching the query, or {@code null} if unknown
   * @param itemsPerPage the number of results returned in this page
   * @param startIndex the 1-based start index for index-based pagination, or {@code null} for cursor-only mode
   * @param nextCursor the {@code nextCursor} token, or {@code null} to omit
   * @param previousCursor the {@code previousCursor} token, or {@code null} to omit
   * @see <a href="https://www.rfc-editor.org/rfc/rfc9865.html">RFC 9865</a>
   */
  public ListResponse(List<JsonNode> listedResources,
                      Long totalResults,
                      Integer itemsPerPage,
                      Long startIndex,
                      String nextCursor,
                      String previousCursor)
  {
    super(null);
    setSchemasAttribute();
    setTotalResults(totalResults);
    setItemsPerPage(itemsPerPage);
    setStartIndex(startIndex);
    setNextCursor(nextCursor);
    setPreviousCursor(previousCursor);
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
   * A cursor value string that MAY be used in a subsequent request to obtain the next page of results. Service
   * providers supporting cursor-based pagination MUST include {@code nextCursor} in all paged query responses
   * except when returning the last page. See RFC 9865.
   */
  public Optional<String> getNextCursor()
  {
    return getStringAttribute(AttributeNames.RFC7643.NEXT_CURSOR);
  }

  /**
   * A cursor value string that MAY be used in a subsequent request to obtain the next page of results. Passing
   * {@code null} omits the attribute from the JSON representation. See RFC 9865.
   */
  public void setNextCursor(String nextCursor)
  {
    setAttribute(AttributeNames.RFC7643.NEXT_CURSOR, nextCursor);
  }

  /**
   * A cursor value string that MAY be used in a subsequent request to obtain the previous page of results.
   * Returning {@code previousCursor} is OPTIONAL. {@code previousCursor} MUST NOT be returned with the first
   * page. See RFC 9865.
   */
  public Optional<String> getPreviousCursor()
  {
    return getStringAttribute(AttributeNames.RFC7643.PREVIOUS_CURSOR);
  }

  /**
   * A cursor value string that MAY be used in a subsequent request to obtain the previous page of results.
   * Passing {@code null} omits the attribute from the JSON representation. See RFC 9865.
   */
  public void setPreviousCursor(String previousCursor)
  {
    setAttribute(AttributeNames.RFC7643.PREVIOUS_CURSOR, previousCursor);
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
