package de.gold.scim.response;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.exceptions.InvalidSchemaException;
import de.gold.scim.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 22:36 <br>
 * <br>
 */
public class BulkResponseOperationTest implements FileReferences
{

  /**
   * tests that the getter and setter methods are working correctly
   */
  @Test
  public void testGetAndSetValues()
  {
    final String method = "POST";
    final String bulkId = UUID.randomUUID().toString();
    final String version = UUID.randomUUID().toString();
    final String location = EndpointPaths.USERS + "/" + UUID.randomUUID().toString();
    final Integer status = HttpStatus.SC_OK;
    final ErrorResponse response = new ErrorResponse(new InvalidSchemaException("invalid syntax", null,
                                                                                HttpStatus.SC_BAD_REQUEST,
                                                                                ScimType.RFC7644.INVALID_SYNTAX));

    BulkResponseOperation operations = BulkResponseOperation.builder()
                                                            .method(method)
                                                            .bulkId(bulkId)
                                                            .version(version)
                                                            .location(location)
                                                            .status(status)
                                                            .response(response)
                                                            .build();
    Assertions.assertEquals(method, operations.getMethod());
    Assertions.assertEquals(bulkId, operations.getBulkId().get());
    Assertions.assertEquals(version, operations.getVersion().get());
    Assertions.assertEquals(location, operations.getLocation().get());
    Assertions.assertEquals(status, operations.getStatus());
    Assertions.assertEquals(response, operations.getResponse().get());

    Assertions.assertEquals(method, operations.get("method").textValue());
    Assertions.assertEquals(bulkId, operations.get("bulkId").textValue());
    Assertions.assertEquals(version, operations.get("version").textValue());
    Assertions.assertEquals(location, operations.get("location").textValue());
    Assertions.assertEquals(status, operations.get("status").intValue());
    Assertions.assertEquals(response, new ErrorResponse(operations.get("response")));
  }

  /**
   * verifies that exceptions are thrown if required attributes are missing if getter are called
   */
  @Test
  public void testThrowExceptionsOnRequiredMissingAttributes()
  {
    BulkResponseOperation operations = new BulkResponseOperation();
    Assertions.assertThrows(InternalServerException.class, operations::getMethod);
    Assertions.assertThrows(InternalServerException.class, operations::getStatus);
  }
}
