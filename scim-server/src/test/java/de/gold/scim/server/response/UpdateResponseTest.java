package de.gold.scim.server.response;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.server.constants.HttpHeader;
import de.gold.scim.server.constants.HttpStatus;
import de.gold.scim.server.utils.FileReferences;
import de.gold.scim.server.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 19.10.2019 - 00:28 <br>
 * <br>
 */
public class UpdateResponseTest implements FileReferences
{


  /**
   * will verify that a {@link UpdateResponse} can successfully be created and that the values are correctly
   * returned
   */
  @Test
  public void testCreateGetResponse()
  {
    JsonNode userResource = JsonHelper.loadJsonDocument(USER_RESOURCE);
    final String location = "https://localhost/scim/v2/Users/123456789";
    UpdateResponse updateResponse = Assertions.assertDoesNotThrow(() -> new UpdateResponse(userResource, location));
    Assertions.assertEquals(2, updateResponse.getHttpHeaders().size());
    Assertions.assertEquals(location, updateResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            updateResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertEquals(userResource, JsonHelper.readJsonDocument(updateResponse.toString()));
    Assertions.assertEquals(userResource, updateResponse);

    Response response = updateResponse.buildResponse();
    Assertions.assertEquals(2, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertEquals(location, response.getHeaders().get(HttpHeader.LOCATION_HEADER).get(0));
    Assertions.assertEquals(userResource, JsonHelper.readJsonDocument((String)response.getEntity()));
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
  }
}
