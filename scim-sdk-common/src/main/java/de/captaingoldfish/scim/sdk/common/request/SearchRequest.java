package de.captaingoldfish.scim.sdk.common.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.AbstractSchemasHolder;
import de.captaingoldfish.scim.sdk.common.utils.RequestUtils;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 18:39 <br>
 * <br>
 * represents a search request that is used on .search requests
 */
public class SearchRequest extends AbstractSchemasHolder
{

  public SearchRequest()
  {
    this(null, null, null, null, null, null, null, null, null);
  }

  @Builder
  public SearchRequest(Long startIndex,
                       Integer count,
                       String filter,
                       String sortBy,
                       SortOrder sortOrder,
                       String attributesString,
                       List<String> attributes,
                       String excludedAttributesString,
                       List<String> excludedAttributes)
  {
    setSchemas(Collections.singletonList(SchemaUris.SEARCH_REQUEST_URI));
    setStartIndex(startIndex);
    setCount(count);
    setFilter(filter);
    setSortBy(sortBy);
    setSortOrder(sortOrder);
    if (StringUtils.isNotBlank(attributesString))
    {
      setAttributes(attributesString);
    }
    else
    {
      setAttributes(attributes);
    }
    if (StringUtils.isNotBlank(excludedAttributesString))
    {
      setExcludedAttributes(excludedAttributesString);
    }
    else
    {
      setExcludedAttributes(excludedAttributes);
    }
  }

  /**
   * An integer indicating the 1-based index of the first query result. See Section 3.4.2.4. OPTIONAL.
   */
  public Optional<Long> getStartIndex()
  {
    return getLongAttribute(AttributeNames.RFC7643.START_INDEX);
  }

  /**
   * An integer indicating the 1-based index of the first query result. See Section 3.4.2.4. OPTIONAL.
   */
  public void setStartIndex(Long startIndex)
  {
    setAttribute(AttributeNames.RFC7643.START_INDEX, startIndex);
  }

  /**
   * An integer indicating the desired maximum number of query results per page. See Section 3.4.2.4. OPTIONAL.
   */
  public Optional<Integer> getCount()
  {
    return getLongAttribute(AttributeNames.RFC7643.COUNT).map(Long::intValue);
  }

  /**
   * An integer indicating the desired maximum number of query results per page. See Section 3.4.2.4. OPTIONAL.
   */
  public void setCount(Integer count)
  {
    setAttribute(AttributeNames.RFC7643.COUNT, count == null ? null : Long.valueOf(count));
  }

  /**
   * The filter string used to request a subset of resources. The filter string MUST be a valid filter (Section
   * 3.4.2.2) expression. OPTIONAL.
   */
  public Optional<String> getFilter()
  {
    return getStringAttribute(AttributeNames.RFC7643.FILTER);
  }

  /**
   * The filter string used to request a subset of resources. The filter string MUST be a valid filter (Section
   * 3.4.2.2) expression. OPTIONAL.
   */
  public void setFilter(String filter)
  {
    setAttribute(AttributeNames.RFC7643.FILTER, filter);
  }

  /**
   * A string indicating the attribute whose value SHALL be used to order the returned responses. The "sortBy"
   * attribute MUST be in standard attribute notation (Section 3.10) form. See Section 3.4.2.3. OPTIONAL.
   */
  public Optional<String> getSortBy()
  {
    return getStringAttribute(AttributeNames.RFC7643.SORT_BY);
  }

  /**
   * A string indicating the attribute whose value SHALL be used to order the returned responses. The "sortBy"
   * attribute MUST be in standard attribute notation (Section 3.10) form. See Section 3.4.2.3. OPTIONAL.
   */
  public void setSortBy(String sortBy)
  {
    setAttribute(AttributeNames.RFC7643.SORT_BY, sortBy);
  }

  /**
   * A string indicating the order in which the "sortBy" parameter is applied. Allowed values are "ascending"
   * and "descending". See Section 3.4.2.3. OPTIONAL.
   */
  public Optional<String> getSortOrder()
  {
    return getStringAttribute(AttributeNames.RFC7643.SORT_ORDER);
  }

  /**
   * A string indicating the order in which the "sortBy" parameter is applied. Allowed values are "ascending"
   * and "descending". See Section 3.4.2.3. OPTIONAL.
   */
  public void setSortOrder(SortOrder sortOrder)
  {
    setAttribute(AttributeNames.RFC7643.SORT_ORDER,
                 Optional.ofNullable(sortOrder).map(order -> order.name().toLowerCase()).orElse(null));
  }

  /**
   * A multi-valued list of strings indicating the names of resource attributes to return in the response,
   * overriding the set of attributes that would be returned by default. Attribute names MUST be in standard
   * attribute notation (Section 3.10) form. See Section 3.9 for additional retrieval query parameters.
   * OPTIONAL.
   */
  public List<String> getAttributes()
  {
    JsonNode attributes = get(AttributeNames.RFC7643.ATTRIBUTES);
    if (attributes instanceof ArrayNode)
    {
      return getSimpleArrayAttribute(AttributeNames.RFC7643.ATTRIBUTES);
    }
    else if (attributes instanceof TextNode)
    {
      return Optional.ofNullable(attributes.textValue()).map(RequestUtils::getAttributeList).orElseGet(ArrayList::new);
    }
    else
    {
      return Collections.emptyList();
    }
  }

  /**
   * A multi-valued list of strings indicating the names of resource attributes to return in the response,
   * overriding the set of attributes that would be returned by default. Attribute names MUST be in standard
   * attribute notation (Section 3.10) form. See Section 3.9 for additional retrieval query parameters.
   * OPTIONAL.
   */
  public void setAttributes(String attributes)
  {
    List<String> attributeList = RequestUtils.getAttributeList(attributes);
    setAttributes(attributeList);
  }

  /**
   * A multi-valued list of strings indicating the names of resource attributes to return in the response,
   * overriding the set of attributes that would be returned by default. Attribute names MUST be in standard
   * attribute notation (Section 3.10) form. See Section 3.9 for additional retrieval query parameters.
   * OPTIONAL.
   */
  public void setAttributes(List<String> attributes)
  {
    setStringAttributeList(AttributeNames.RFC7643.ATTRIBUTES, attributes);
  }

  /**
   * A multi-valued list of strings indicating the names of resource attributes to be removed from the default
   * set of attributes to return. This parameter SHALL have no effect on attributes whose schema "returned"
   * setting is "always" (see Sections 2.2 and 7 of [RFC7643]). Attribute names MUST be in standard attribute
   * notation (Section 3.10) form. See Section 3.9 for additional retrieval query parameters. OPTIONAL.
   */
  public List<String> getExcludedAttributes()
  {
    JsonNode attributes = get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES);
    if (attributes instanceof ArrayNode)
    {
      return getSimpleArrayAttribute(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES);
    }
    else if (attributes instanceof TextNode)
    {
      return Optional.ofNullable(attributes.textValue()).map(RequestUtils::getAttributeList).orElseGet(ArrayList::new);
    }
    else
    {
      return Collections.emptyList();
    }
  }

  /**
   * A multi-valued list of strings indicating the names of resource attributes to be removed from the default
   * set of attributes to return. This parameter SHALL have no effect on attributes whose schema "returned"
   * setting is "always" (see Sections 2.2 and 7 of [RFC7643]). Attribute names MUST be in standard attribute
   * notation (Section 3.10) form. See Section 3.9 for additional retrieval query parameters. OPTIONAL.
   */
  public void setExcludedAttributes(String excludedAttributes)
  {
    List<String> excludedAttributeList = RequestUtils.getAttributeList(excludedAttributes);
    setExcludedAttributes(excludedAttributeList);
  }

  /**
   * A multi-valued list of strings indicating the names of resource attributes to be removed from the default
   * set of attributes to return. This parameter SHALL have no effect on attributes whose schema "returned"
   * setting is "always" (see Sections 2.2 and 7 of [RFC7643]). Attribute names MUST be in standard attribute
   * notation (Section 3.10) form. See Section 3.9 for additional retrieval query parameters. OPTIONAL.
   */
  public void setExcludedAttributes(List<String> excludedAttributes)
  {
    setStringAttributeList(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES, excludedAttributes);
  }

  /**
   * override lombok builder with public constructor
   */
  public static class SearchRequestBuilder
  {

    public SearchRequestBuilder()
    {}
  }
}
