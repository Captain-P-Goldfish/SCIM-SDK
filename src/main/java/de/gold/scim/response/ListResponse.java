package de.gold.scim.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.utils.JsonHelper;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 17.10.2019 - 22:04 <br>
 * <br>
 * represents a list response
 */
public class ListResponse extends ScimResponse
{

  /**
   * this is the actual node that represents this response
   */
  private JsonNode listResponseNode;

  /**
   * the resources that have been extracted
   */
  @Getter
  private List<JsonNode> listedResources;

  /**
   * Non-negative integer. Specifies the total number of results matching the client query, e.g., 1000
   */
  @Getter
  private Integer totalResults;

  /**
   * Non-negative integer. Specifies the number of query results returned in a query response page, e.g., 10.
   */
  @Getter
  private Integer itemsPerPage;

  /**
   * The 1-based index of the first result in the current set of query results, e.g., 1.
   */
  @Getter
  private Integer startIndex;

  public ListResponse(String resourceJsonRepresentation)
  {
    this.listResponseNode = JsonHelper.readJsonDocument(resourceJsonRepresentation);
    this.totalResults = JsonHelper.getSimpleAttribute(listResponseNode,
                                                      AttributeNames.RFC7643.TOTAL_RESULTS,
                                                      Integer.class)
                                  .orElse(null);
    this.itemsPerPage = JsonHelper.getSimpleAttribute(listResponseNode,
                                                      AttributeNames.RFC7643.ITEMS_PER_PAGE,
                                                      Integer.class)
                                  .orElse(null);
    this.startIndex = JsonHelper.getSimpleAttribute(listResponseNode, AttributeNames.RFC7643.START_INDEX, Integer.class)
                                .orElse(null);
    this.listedResources = new ArrayList<>();
    JsonHelper.getArrayAttribute(listResponseNode, AttributeNames.RFC7643.RESOURCES).ifPresent(resourceArray -> {
      resourceArray.forEach(listedResources::add);
    });
  }

  public ListResponse(List<JsonNode> listedResources, Integer totalResults, Integer itemsPerPage, Integer startIndex)
  {
    super();
    this.listResponseNode = new ObjectNode(JsonNodeFactory.instance);
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
    JsonHelper.addAttribute(this.listResponseNode, AttributeNames.RFC7643.SCHEMAS, schemas);
  }

  /**
   * @see #totalResults
   */
  public void setTotalResults(Integer totalResults)
  {
    this.totalResults = totalResults;
    if (totalResults != null)
    {
      JsonHelper.addAttribute(this.listResponseNode, AttributeNames.RFC7643.TOTAL_RESULTS, new IntNode(totalResults));
    }
  }

  /**
   * @see #itemsPerPage
   */
  public void setItemsPerPage(Integer itemsPerPage)
  {
    this.itemsPerPage = itemsPerPage;
    if (itemsPerPage != null)
    {
      JsonHelper.addAttribute(this.listResponseNode, AttributeNames.RFC7643.ITEMS_PER_PAGE, new IntNode(itemsPerPage));
    }
  }

  /**
   * @see #startIndex
   */
  public void setStartIndex(Integer startIndex)
  {
    this.startIndex = startIndex;
    if (startIndex != null)
    {
      JsonHelper.addAttribute(this.listResponseNode, AttributeNames.RFC7643.START_INDEX, new IntNode(startIndex));
    }
  }

  /**
   * @see #listedResources
   */
  public void setListedResources(List<JsonNode> listedResources)
  {
    this.listedResources = listedResources == null ? new ArrayList<>() : listedResources;
    ArrayNode resources = new ArrayNode(JsonNodeFactory.instance);
    this.listedResources.forEach(resources::add);
    JsonHelper.addAttribute(this.listResponseNode, AttributeNames.RFC7643.RESOURCES, resources);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return HttpStatus.SC_OK;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    return listResponseNode.toString();
  }
}
