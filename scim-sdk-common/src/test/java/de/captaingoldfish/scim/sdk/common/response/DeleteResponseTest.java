package de.captaingoldfish.scim.sdk.common.response;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;


/**
 * author Pascal Knueppel <br>
 * created at: 19.10.2019 - 00:29 <br>
 * <br>
 */
public class DeleteResponseTest
{


  /**
   * will verify that a {@link DeleteResponse} can successfully be created and that the values are correctly
   * returned
   */
  @Test
  public void testCreateDeleteResponse()
  {
    DeleteResponse deleteResponse = Assertions.assertDoesNotThrow(DeleteResponse::new);
    Assertions.assertEquals(0, deleteResponse.getHttpHeaders().size());
    Assertions.assertNull(deleteResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertTrue(deleteResponse.isEmpty());

    Response response = deleteResponse.buildResponse();
    Assertions.assertEquals(0, response.getHeaders().size());
    Assertions.assertNull(response.getHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    Assertions.assertNull(response.getEntity());
  }
}
