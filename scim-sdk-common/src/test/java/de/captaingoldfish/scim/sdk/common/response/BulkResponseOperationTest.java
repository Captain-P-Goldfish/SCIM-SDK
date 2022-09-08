package de.captaingoldfish.scim.sdk.common.response;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidSchemaException;
import de.captaingoldfish.scim.sdk.common.utils.FileReferences;


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
    final HttpMethod method = HttpMethod.POST;
    final String bulkId = UUID.randomUUID().toString();
    final String version = UUID.randomUUID().toString();
    final String location = EndpointPaths.USERS + "/" + UUID.randomUUID();
    final Integer status = HttpStatus.OK;
    final ErrorResponse response = new ErrorResponse(new InvalidSchemaException("invalid syntax", null,
                                                                                HttpStatus.BAD_REQUEST,
                                                                                ScimType.RFC7644.INVALID_SYNTAX));

    ETag eTag = ETag.builder().tag(version).build();
    BulkResponseOperation operations = BulkResponseOperation.builder()
                                                            .method(method)
                                                            .bulkId(bulkId)
                                                            .version(eTag)
                                                            .location(location)
                                                            .status(status)
                                                            .response(response)
                                                            .build();
    Assertions.assertEquals(method, operations.getMethod());
    Assertions.assertEquals(bulkId, operations.getBulkId().get());
    Assertions.assertEquals(eTag, operations.getVersion().get());
    Assertions.assertEquals(location, operations.getLocation().get());
    Assertions.assertEquals(status, operations.getStatus());
    Assertions.assertEquals(response, operations.getResponse(ErrorResponse.class).get());

    Assertions.assertEquals(method.name(), operations.get("method").textValue());
    Assertions.assertEquals(bulkId, operations.get("bulkId").textValue());
    Assertions.assertEquals(eTag.getEntityTag(), operations.get("version").textValue());
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
