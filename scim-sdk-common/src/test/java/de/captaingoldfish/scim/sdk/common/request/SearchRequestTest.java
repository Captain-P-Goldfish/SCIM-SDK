package de.captaingoldfish.scim.sdk.common.request;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 18:57 <br>
 * <br>
 */
public class SearchRequestTest
{

  /**
   * the searchRequest might be created by the {@link JsonHelper} and therefore simply needs a noArgsConstructor
   */
  @Test
  public void testCreateWithNoArgsConstructor()
  {
    SearchRequest searchRequest = Assertions.assertDoesNotThrow((ThrowingSupplier<SearchRequest>)SearchRequest::new);
    Assertions.assertEquals(1, searchRequest.size());
    Assertions.assertEquals(1, searchRequest.getSchemas().size());
    Assertions.assertEquals(SchemaUris.SEARCH_REQUEST_URI, searchRequest.getSchemas().iterator().next());
  }

  /**
   * test {@link SearchRequest} creation with builder
   */
  @Test
  public void testCreateWithEmptyBuilder()
  {
    SearchRequest searchRequest = Assertions.assertDoesNotThrow(() -> SearchRequest.builder().build());
    Assertions.assertEquals(1, searchRequest.size());
    Assertions.assertEquals(1, searchRequest.getSchemas().size());
    Assertions.assertEquals(SchemaUris.SEARCH_REQUEST_URI, searchRequest.getSchemas().iterator().next());
  }

  /**
   * tests that the values are correctly entered if the builder method is used
   */
  @Test
  public void testBuilderSettingMethods()
  {
    final long startIndex = 1;
    final int count = 1;
    final String filter = "filter";
    final String sortBy = "sortBy";
    final SortOrder sortOrder = SortOrder.ASCENDING;
    final String attributes = "attributes";
    final String excludedAttr = "excludedAttributes";

    SearchRequest searchRequest = Assertions.assertDoesNotThrow(() -> SearchRequest.builder()
                                                                                   .startIndex(startIndex)
                                                                                   .count(count)
                                                                                   .filter(filter)
                                                                                   .sortBy(sortBy)
                                                                                   .sortOrder(sortOrder)
                                                                                   .attributesString(attributes)
                                                                                   .excludedAttributesString(excludedAttr)
                                                                                   .build());

    Assertions.assertEquals(startIndex, searchRequest.getStartIndex().get());
    Assertions.assertEquals(count, searchRequest.getCount().get());
    Assertions.assertEquals(filter, searchRequest.getFilter().get());
    Assertions.assertEquals(sortBy, searchRequest.getSortBy().get());
    Assertions.assertEquals(sortOrder.name().toLowerCase(), searchRequest.getSortOrder().get());
    Assertions.assertEquals(attributes, searchRequest.getAttributes().get(0));
    Assertions.assertEquals(excludedAttr, searchRequest.getExcludedAttributes().get(0));
  }

  /**
   * will verify that the attributes are getting removed if null values are put into the setter methods
   */
  @Test
  public void testSetterMethodsWithNullValues()
  {
    final long startIndex = 1;
    final int count = 1;
    final String filter = "filter";
    final String sortBy = "sortBy";
    final SortOrder sortOrder = SortOrder.ASCENDING;
    final String attributes = "attributes";
    final String excludedAttr = "excludedAttributes";

    SearchRequest searchRequest = Assertions.assertDoesNotThrow(() -> SearchRequest.builder()
                                                                                   .startIndex(startIndex)
                                                                                   .count(count)
                                                                                   .filter(filter)
                                                                                   .sortBy(sortBy)
                                                                                   .sortOrder(sortOrder)
                                                                                   .attributesString(attributes)
                                                                                   .excludedAttributesString(excludedAttr)
                                                                                   .build());

    searchRequest.setStartIndex(null);
    searchRequest.setCount(null);
    searchRequest.setFilter(null);
    searchRequest.setSortBy(null);
    searchRequest.setSortOrder(null);
    searchRequest.setAttributes((String)null);
    searchRequest.setExcludedAttributes((String)null);

    Assertions.assertEquals(1, searchRequest.size());
    Assertions.assertFalse(searchRequest.getSchemas().isEmpty());
  }

  /**
   * will verify that the attributes are getting removed if null values are put into the setter methods
   */
  @Test
  public void testSetterThatCorrectKeyNamesAreUsed()
  {
    final long startIndex = 1;
    final int count = 1;
    final String filter = "filter";
    final String sortBy = "sortBy";
    final SortOrder sortOrder = SortOrder.ASCENDING;
    final String attributes = "attributes";
    final String excludedAttr = "excludedAttributes";

    SearchRequest searchRequest = Assertions.assertDoesNotThrow(() -> SearchRequest.builder()
                                                                                   .startIndex(startIndex)
                                                                                   .count(count)
                                                                                   .filter(filter)
                                                                                   .sortBy(sortBy)
                                                                                   .sortOrder(sortOrder)
                                                                                   .attributesString(attributes)
                                                                                   .excludedAttributesString(excludedAttr)
                                                                                   .build());

    searchRequest.remove("startIndex");
    searchRequest.remove("count");
    searchRequest.remove("filter");
    searchRequest.remove("sortBy");
    searchRequest.remove("sortOrder");
    searchRequest.remove("attributes");
    searchRequest.remove("excludedAttributes");

    Assertions.assertEquals(1, searchRequest.size());
    Assertions.assertFalse(searchRequest.getSchemas().isEmpty());
  }

  @DisplayName("Parameter 'attributesString' is correctly set when present")
  @Test
  public void testAttributesStringIsCorrectlySet()
  {
    final String attributesString = "userName,displayName";
    SearchRequest searchRequest = SearchRequest.builder().attributesString(attributesString).build();
    Assertions.assertEquals(2, searchRequest.size(), searchRequest.toPrettyString());

    ArrayNode expectedNode = new ArrayNode(JsonNodeFactory.instance);
    expectedNode.add("userName");
    expectedNode.add("displayName");
    Assertions.assertEquals(expectedNode, searchRequest.get(AttributeNames.RFC7643.ATTRIBUTES));
  }

  @DisplayName("Parameter 'attributes' is correctly set when present")
  @Test
  public void testAttributesIsCorrectlySet()
  {
    final List<String> attributesString = Arrays.asList("userName", "displayName");
    SearchRequest searchRequest = SearchRequest.builder().attributes(attributesString).build();
    Assertions.assertEquals(2, searchRequest.size(), searchRequest.toPrettyString());

    ArrayNode expectedNode = new ArrayNode(JsonNodeFactory.instance);
    expectedNode.add("userName");
    expectedNode.add("displayName");
    Assertions.assertEquals(expectedNode, searchRequest.get(AttributeNames.RFC7643.ATTRIBUTES));
  }

  @DisplayName("Parameter 'excludedAttributes' is correctly set when present")
  @Test
  public void testExcludedAttributesStringIsCorrectlySet()
  {
    final List<String> excludedAttributesString = Arrays.asList("userName", "displayName");
    SearchRequest searchRequest = SearchRequest.builder().excludedAttributes(excludedAttributesString).build();
    Assertions.assertEquals(2, searchRequest.size(), searchRequest.toPrettyString());

    ArrayNode expectedNode = new ArrayNode(JsonNodeFactory.instance);
    expectedNode.add("userName");
    expectedNode.add("displayName");
    Assertions.assertEquals(expectedNode, searchRequest.get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES));
  }

}
