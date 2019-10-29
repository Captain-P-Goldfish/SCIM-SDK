package de.gold.scim.response;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 22:53 <br>
 * <br>
 */
public class GetResponseTest implements FileReferences
{

  /**
   * will verify that a {@link GetResponse} can successfully be created and that the values are correctly
   * returned
   */
  @Test
  public void testCreateGetResponse()
  {
    JsonNode userResource = JsonHelper.loadJsonDocument(USER_RESOURCE);
    final String location = "https://localhost/scim/v2/Users/123456789";
    GetResponse getResponse = Assertions.assertDoesNotThrow(() -> new GetResponse(userResource, location));
    Assertions.assertEquals(2, getResponse.getHttpHeaders().size());
    Assertions.assertEquals(location, getResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            getResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertEquals(userResource, JsonHelper.readJsonDocument(getResponse.toString()));

    Response response = getResponse.buildResponse();
    Assertions.assertEquals(2, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertEquals(location, response.getHeaders().get(HttpHeader.LOCATION_HEADER).get(0));
    Assertions.assertEquals(userResource, JsonHelper.readJsonDocument((String)response.getEntity()));
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
  }
}
