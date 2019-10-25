package de.gold.scim.request;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 25.10.2019 - 22:55 <br>
 * <br>
 */
public class BulkRequestOperationTest implements FileReferences
{


  /**
   * tests that the getter and setter methods are working correctly
   */
  @Test
  public void testGetAndSetValues()
  {
    final String method = "POST";
    final String bulkId = UUID.randomUUID().toString();
    final String path = EndpointPaths.USERS;
    final String data = readResourceFile(USER_RESOURCE).replaceAll("\\s", "");

    BulkRequestOperation operations = BulkRequestOperation.builder()
                                                          .method(method)
                                                          .bulkId(bulkId)
                                                          .path(path)
                                                          .data(data)
                                                          .build();
    Assertions.assertEquals(method, operations.getMethod());
    Assertions.assertEquals(bulkId, operations.getBulkId().get());
    Assertions.assertEquals(path, operations.getPath());
    Assertions.assertEquals(data, operations.getData());

    Assertions.assertEquals(method, operations.get("method").textValue());
    Assertions.assertEquals(bulkId, operations.get("bulkId").textValue());
    Assertions.assertEquals(path, operations.get("path").textValue());
    Assertions.assertEquals(data, operations.get("data").toString());
    Assertions.assertEquals(JsonHelper.readJsonDocument(data), operations.get("data"));
  }

  /**
   * verifies that exceptions are thrown if required attributes are missing if getter are called
   */
  @Test
  public void testThrowExceptionsOnRequiredMissingAttributes()
  {
    BulkRequestOperation operations = new BulkRequestOperation();
    Assertions.assertThrows(BadRequestException.class, operations::getMethod);
    Assertions.assertThrows(BadRequestException.class, operations::getPath);
    Assertions.assertThrows(BadRequestException.class, operations::getData);
  }
}
