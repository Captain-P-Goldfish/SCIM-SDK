package de.gold.scim.response;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;


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
    Assertions.assertEquals(1, deleteResponse.getHttpHeaders().size());
    Assertions.assertNull(deleteResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            deleteResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertTrue(deleteResponse.isEmpty());

    Response response = deleteResponse.buildResponse();
    Assertions.assertEquals(1, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertNull(response.getHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpStatus.SC_NO_CONTENT, deleteResponse.getHttpStatus());
    Assertions.assertNull(response.getEntity());
  }
}
