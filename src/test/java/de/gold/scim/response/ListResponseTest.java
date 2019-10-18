package de.gold.scim.response;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
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
    final int totalResults = 10;
    final int itemsPerPage = 2;
    final int startIndex = 1;
    List<JsonNode> resourceNodes = Arrays.asList(JsonHelper.loadJsonDocument(USER_RESOURCE),
                                                 JsonHelper.loadJsonDocument(USER_RESOURCE));
    ListResponse listResponse = new ListResponse(resourceNodes, totalResults, itemsPerPage, startIndex);

    Assertions.assertEquals(startIndex, listResponse.getStartIndex());
    Assertions.assertEquals(itemsPerPage, listResponse.getItemsPerPage());
    Assertions.assertEquals(totalResults, listResponse.getTotalResults());
    Assertions.assertEquals(1, listResponse.getHttpHeaders().size());
    Assertions.assertNull(listResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            listResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    JsonNode listJson = JsonHelper.readJsonDocument(listResponse.toJsonDocument());
    Assertions.assertEquals(totalResults,
                            JsonHelper.getSimpleAttribute(listJson, AttributeNames.RFC7643.TOTAL_RESULTS, Integer.class)
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
                            JsonHelper.getSimpleAttribute(listJson, AttributeNames.RFC7643.START_INDEX, Integer.class)
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
    Assertions.assertEquals(HttpStatus.SC_OK, response.getStatus());
    Assertions.assertEquals(listJson, JsonHelper.readJsonDocument((String)response.getEntity()));
    log.debug(listJson.toPrettyString());

    ListResponse clientListResponse = new ListResponse(listJson.toString());
    Assertions.assertEquals(listJson.toString(), clientListResponse.toJsonDocument());
  }
}
