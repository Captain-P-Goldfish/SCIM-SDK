package de.gold.scim.response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.SchemaUris;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 21:25 <br>
 * <br>
 */
public class ErrorResponseTest
{


  /**
   * will verify a new created error response with the noArgsConstructor holds only its schemas attribute
   */
  @Test
  public void testEmptyErrorResponseCreation()
  {
    ErrorResponse errorResponse = new ErrorResponse();
    Assertions.assertEquals(1, errorResponse.size());
    Assertions.assertEquals(1, errorResponse.getSchemas().size());
    Assertions.assertEquals(SchemaUris.ERROR_URI, errorResponse.getSchemas().get(0));
  }
}
