package de.gold.scim.response;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpHeader;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.InvalidFilterException;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 21:25 <br>
 * <br>
 */
public class ErrorResponseTest
{


  /**
   * will verify a new created ErrorResponse is setup correctly
   */
  @Test
  public void testErrorResponseCreation()
  {
    final String detail = "something bad happened";
    InvalidFilterException invalidFilterException = new InvalidFilterException(detail, null);
    ErrorResponse errorResponse = new ErrorResponse(invalidFilterException);

    Assertions.assertEquals(1, errorResponse.getHttpHeaders().size());
    Assertions.assertNull(errorResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    JsonNode errorJson = JsonHelper.readJsonDocument(errorResponse.toJsonDocument());
    Assertions.assertEquals(detail, JsonHelper.getSimpleAttribute(errorJson, AttributeNames.RFC7643.DETAIL).get());
    Assertions.assertEquals(ScimType.RFC7644.INVALID_FILTER,
                            JsonHelper.getSimpleAttribute(errorJson, AttributeNames.RFC7643.SCIM_TYPE).get());
    Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST,
                            JsonHelper.getSimpleAttribute(errorJson, AttributeNames.RFC7643.STATUS, Integer.class)
                                      .get());

    Response response = errorResponse.buildResponse();
    Assertions.assertEquals(1, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertNull(response.getHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  /**
   * just for code coverage
   */
  @Test
  public void testEquals()
  {
    ErrorResponse errorResponse = new ErrorResponse(new BadRequestException("test", null, "test"));
    Assertions.assertEquals(errorResponse, errorResponse);
    Assertions.assertNotEquals(errorResponse, null);
    Assertions.assertNotEquals(errorResponse, 5);
  }
}
