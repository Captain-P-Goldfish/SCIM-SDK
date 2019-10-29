package de.gold.scim.request;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.enums.PatchOp;
import de.gold.scim.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 08:57 <br>
 * <br>
 */
public class PatchRequestOperationTest
{

  /**
   * verifies that the object is empty if nothing was set
   */
  @Test
  public void testCreateEmptyObject()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder().build();
    Assertions.assertTrue(operation.isEmpty());
    operation = new PatchRequestOperation();
    Assertions.assertTrue(operation.isEmpty());
  }

  /**
   * will verify that the patch objects can be successfully built
   */
  @Test
  public void testGetterAndSetterMethods()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder().build();
    Assertions.assertTrue(operation.isEmpty());

    final String value = "{\"emails\":[{\"value\":\"babs@jensen.org\",\"type\":\"home\"}],\"nickname\":\"Babs\"}";
    final String path = "members[value eq \"2819c223-7f76-...413861904646\"]";
    final PatchOp patchOp = PatchOp.REPLACE;
    operation.setPath(path);
    operation.setValue(Collections.singletonList(value));
    operation.setOp(patchOp);

    Assertions.assertEquals(1, operation.getValue().size());
    Assertions.assertEquals(value, operation.getValue().get(0));
    Assertions.assertEquals(path, operation.getPath().get());
    Assertions.assertEquals(patchOp, operation.getOp());

    operation.setValueNode(JsonHelper.readJsonDocument(value));
    Assertions.assertEquals(value, operation.getValue().get(0));
  }
}
