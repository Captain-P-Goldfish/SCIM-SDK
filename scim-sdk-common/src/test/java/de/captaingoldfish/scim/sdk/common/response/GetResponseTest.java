package de.captaingoldfish.scim.sdk.common.response;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 22:53 <br>
 * <br>
 */
public class GetResponseTest
{

  /**
   * will verify that a {@link GetResponse} can successfully be created and that the values are correctly
   * returned
   */
  @Test
  public void testCreateGetResponse()
  {
    JsonNode userResource = User.builder()
                                .id(UUID.randomUUID().toString())
                                .userName(UUID.randomUUID().toString())
                                .build();
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
