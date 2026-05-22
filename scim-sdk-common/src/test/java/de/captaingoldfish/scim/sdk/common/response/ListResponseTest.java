package de.captaingoldfish.scim.sdk.common.response;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.utils.FileReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 19.10.2019 - 01:15 <br>
 * <br>
 */
@Slf4j
public class ListResponseTest implements FileReferences
{


  /**
   * will verify a new created ListResponse is setup correctly
   */
  @Test
  public void testListResponseCreation()
  {
    final long totalResults = 10;
    final int itemsPerPage = 2;
    final long startIndex = 1;
    List<JsonNode> resourceNodes = Arrays.asList(JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE),
                                                 JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE));
    ListResponse listResponse = new ListResponse(resourceNodes, totalResults, itemsPerPage, startIndex);

    Assertions.assertEquals(startIndex, listResponse.getStartIndex());
    Assertions.assertEquals(itemsPerPage, listResponse.getItemsPerPage());
    Assertions.assertEquals(totalResults, listResponse.getTotalResults());
    Assertions.assertEquals(1, listResponse.getHttpHeaders().size());
    Assertions.assertNull(listResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            listResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    JsonNode listJson = JsonHelper.readJsonDocument(listResponse.toString());
    Assertions.assertEquals(totalResults,
                            JsonHelper.getSimpleAttribute(listJson, AttributeNames.RFC7643.TOTAL_RESULTS, Long.class)
                                      .get());
    Assertions.assertTrue(JsonHelper.getSimpleAttributeArray(listJson, AttributeNames.RFC7643.SCHEMAS).isPresent());
    List<String> schemas = JsonHelper.getSimpleAttributeArray(listJson, AttributeNames.RFC7643.SCHEMAS).get();
    Assertions.assertEquals(1, schemas.size());
    Assertions.assertEquals(1, schemas.size());
    Assertions.assertEquals(SchemaUris.LIST_RESPONSE_URI, schemas.get(0));
    Assertions.assertEquals(itemsPerPage,
                            JsonHelper.getSimpleAttribute(listJson,
                                                          AttributeNames.RFC7643.ITEMS_PER_PAGE,
                                                          Integer.class)
                                      .get());
    Assertions.assertEquals(startIndex,
                            JsonHelper.getSimpleAttribute(listJson, AttributeNames.RFC7643.START_INDEX, Long.class)
                                      .get());
    ArrayNode resources = JsonHelper.getArrayAttribute(listJson, AttributeNames.RFC7643.RESOURCES).get();
    Assertions.assertEquals(2, resources.size());
    for ( JsonNode resource : resources )
    {
      Assertions.assertEquals(resource, resourceNodes.get(0));
    }

    Response response = listResponse.buildResponse();
    Assertions.assertEquals(1, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertNull(response.getHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpStatus.OK, response.getStatus());
    Assertions.assertEquals(listJson, JsonHelper.readJsonDocument((String)response.getEntity()));
    log.debug(listJson.toString());

    ListResponse clientListResponse = new ListResponse(listJson.toString());
    Assertions.assertEquals(listJson.toString(), clientListResponse.toString());

    // TODO
  }

  /**
   * RFC 9865 cursor-mode response: startIndex MUST be omitted, nextCursor and previousCursor are exposed via
   * the getters and serialized into the JSON document.
   */
  @Test
  public void testCursorListResponseOmitsStartIndex()
  {
    final long totalResults = 100;
    final int itemsPerPage = 10;
    final String nextCursor = "VZUTiyhEQJ94IR";
    final String previousCursor = "ze7L30kMiiLX6x";
    List<JsonNode> resourceNodes = Arrays.asList(JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE));

    ListResponse listResponse = new ListResponse(resourceNodes, totalResults, itemsPerPage, null, nextCursor,
                                                 previousCursor);

    Assertions.assertEquals(totalResults, listResponse.getTotalResults());
    Assertions.assertEquals(itemsPerPage, listResponse.getItemsPerPage());
    Assertions.assertEquals(nextCursor, listResponse.getNextCursor().orElse(null));
    Assertions.assertEquals(previousCursor, listResponse.getPreviousCursor().orElse(null));
    Assertions.assertFalse(listResponse.has(AttributeNames.RFC7643.START_INDEX), listResponse.toPrettyString());
    Assertions.assertTrue(listResponse.has(AttributeNames.RFC7643.NEXT_CURSOR));
    Assertions.assertTrue(listResponse.has(AttributeNames.RFC7643.PREVIOUS_CURSOR));
  }

  /**
   * RFC 9865 §6: previousCursor MUST NOT be returned with the first page. When constructed with a {@code null}
   * previousCursor, the attribute must not appear in the JSON document.
   */
  @Test
  public void testFirstPageCursorResponseOmitsPreviousCursor()
  {
    final String nextCursor = "VZUTiyhEQJ94IR";
    List<JsonNode> resourceNodes = Arrays.asList(JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE));

    ListResponse listResponse = new ListResponse(resourceNodes, 100L, 10, null, nextCursor, null);

    Assertions.assertTrue(listResponse.getNextCursor().isPresent());
    Assertions.assertFalse(listResponse.getPreviousCursor().isPresent());
    Assertions.assertTrue(listResponse.has(AttributeNames.RFC7643.NEXT_CURSOR));
    Assertions.assertFalse(listResponse.has(AttributeNames.RFC7643.PREVIOUS_CURSOR));
  }

  /**
   * Last page cursor response: nextCursor MUST NOT be returned. When constructed with a {@code null}
   * nextCursor, the attribute must not appear in the JSON document.
   */
  @Test
  public void testLastPageCursorResponseOmitsNextCursor()
  {
    final String previousCursor = "ze7L30kMiiLX6x";
    List<JsonNode> resourceNodes = Arrays.asList(JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE));

    ListResponse listResponse = new ListResponse(resourceNodes, 100L, 10, null, null, previousCursor);

    Assertions.assertFalse(listResponse.getNextCursor().isPresent());
    Assertions.assertTrue(listResponse.getPreviousCursor().isPresent());
    Assertions.assertFalse(listResponse.has(AttributeNames.RFC7643.NEXT_CURSOR));
    Assertions.assertTrue(listResponse.has(AttributeNames.RFC7643.PREVIOUS_CURSOR));
  }

  /**
   * RFC 9865 permits index-paged responses to also carry a {@code nextCursor}. Verifies the combined response
   * shape: startIndex remains present and the cursor attributes are alongside it.
   */
  @Test
  public void testIndexResponseCanCarryNextCursor()
  {
    final String nextCursor = "VZUTiyhEQJ94IR";
    List<JsonNode> resourceNodes = Arrays.asList(JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE));

    ListResponse listResponse = new ListResponse(resourceNodes, 100L, 10, 1L, nextCursor, null);

    Assertions.assertEquals(1L, listResponse.getStartIndex());
    Assertions.assertEquals(nextCursor, listResponse.getNextCursor().orElse(null));
    Assertions.assertTrue(listResponse.has(AttributeNames.RFC7643.START_INDEX));
    Assertions.assertTrue(listResponse.has(AttributeNames.RFC7643.NEXT_CURSOR));
  }

  /**
   * The 4-arg constructor is the pre-RFC-9865 backwards-compatible form: it must continue to emit
   * {@code startIndex} and omit cursor attributes.
   */
  @Test
  public void testLegacyConstructorOmitsCursorAttributes()
  {
    ListResponse listResponse = new ListResponse(Arrays.asList(JsonHelper.loadJsonDocument(FileReferences.USER_RESOURCE)),
                                                 100L, 10, 1L);

    Assertions.assertEquals(1L, listResponse.getStartIndex());
    Assertions.assertFalse(listResponse.getNextCursor().isPresent());
    Assertions.assertFalse(listResponse.getPreviousCursor().isPresent());
    Assertions.assertFalse(listResponse.has(AttributeNames.RFC7643.NEXT_CURSOR));
    Assertions.assertFalse(listResponse.has(AttributeNames.RFC7643.PREVIOUS_CURSOR));
  }
}
