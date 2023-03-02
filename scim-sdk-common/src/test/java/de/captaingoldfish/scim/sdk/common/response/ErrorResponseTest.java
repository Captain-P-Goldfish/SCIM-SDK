package de.captaingoldfish.scim.sdk.common.response;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotModifiedException;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


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
    JsonNode errorJson = JsonHelper.readJsonDocument(errorResponse.toString());
    Assertions.assertEquals(detail, JsonHelper.getSimpleAttribute(errorJson, AttributeNames.RFC7643.DETAIL).get());
    Assertions.assertEquals(ScimType.RFC7644.INVALID_FILTER,
                            JsonHelper.getSimpleAttribute(errorJson, AttributeNames.RFC7643.SCIM_TYPE).get());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST,
                            JsonHelper.getSimpleAttribute(errorJson, AttributeNames.RFC7643.STATUS, Integer.class)
                                      .get());

    Response response = errorResponse.buildResponse();
    Assertions.assertEquals(1, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertNull(response.getHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
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

  /**
   * verifies that an exception is thrown if the status is not present in the {@link ErrorResponse}
   */
  @Test
  public void testErrorOnEmptyStatus()
  {
    Assertions.assertThrows(InternalServerException.class, () -> new ErrorResponse((JsonNode)null));
  }

  /**
   * will verify that a conditional error response with an exception as
   * {@link de.captaingoldfish.scim.sdk.common.exceptions.NotModifiedException} will not cause any errors and
   * works correctly
   */
  @Test
  public void testErrorResponseWithEmptyBody()
  {
    NotModifiedException notModifiedException = new NotModifiedException();
    ErrorResponse errorResponse = new ErrorResponse(notModifiedException);

    Assertions.assertEquals(1, errorResponse.getHttpHeaders().size());
    Assertions.assertNull(errorResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            errorResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertEquals("", errorResponse.toString());
    Assertions.assertEquals("", errorResponse.toPrettyString());

    Response response = errorResponse.buildResponse();
    Assertions.assertEquals(1, response.getHeaders().size());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            response.getHeaders().get(HttpHeader.CONTENT_TYPE_HEADER).get(0));
    Assertions.assertNull(response.getHeaders().get(HttpHeader.LOCATION_HEADER));
    Assertions.assertEquals(HttpStatus.NOT_MODIFIED, response.getStatus());
    Assertions.assertEquals("", response.getEntity());
  }
}
