package de.gold.scim.server.response;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.constants.HttpStatus;
import de.gold.scim.server.constants.SchemaUris;
import de.gold.scim.server.resources.base.ScimObjectNode;
import de.gold.scim.server.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 22:04 <br>
 * <br>
 * represents a list response
 */
public class ListResponse extends ScimResponse
{

  public ListResponse(String resourceJsonRepresentation)
  {
    super(JsonHelper.readJsonDocument(resourceJsonRepresentation));
    // JsonNode responseNode = JsonHelper.readJsonDocument(resourceJsonRepresentation);
    // setTotalResults(JsonHelper.getSimpleAttribute(responseNode, AttributeNames.RFC7643.TOTAL_RESULTS,
    // Long.class)
    // .orElse(null));
    // this.itemsPerPage = JsonHelper.getSimpleAttribute(responseNode,
    // AttributeNames.RFC7643.ITEMS_PER_PAGE,
    // Integer.class)
    // .orElse(null);
    // this.startIndex = JsonHelper.getSimpleAttribute(responseNode, AttributeNames.RFC7643.START_INDEX,
    // Long.class)
    // .orElse(null);
    // this.listedResources = new ArrayList<>();
    // JsonHelper.getArrayAttribute(responseNode, AttributeNames.RFC7643.RESOURCES).ifPresent(resourceArray -> {
    // resourceArray.forEach(listedResources::add);
    // });
  }

  public ListResponse(List<JsonNode> listedResources, Long totalResults, Integer itemsPerPage, Long startIndex)
  {
    super(null);
    setSchemasAttribute();
    setTotalResults(totalResults);
    setItemsPerPage(itemsPerPage);
    setStartIndex(startIndex);
    setListedResources(listedResources);
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
  public List<ScimObjectNode> getListedResources()
  {
    return getArrayAttribute(AttributeNames.RFC7643.RESOURCES, ScimObjectNode.class);
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
