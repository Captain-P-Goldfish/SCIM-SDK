package de.gold.scim.request;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.constants.SchemaUris;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 09:32 <br>
 * <br>
 */
public class PatchOpRequestTest
{

  /**
   * will verify that the schemas attribute is always added to an empty instance
   */
  @Test
  public void testCreateEmptyObject()
  {
    PatchOpRequest patchOpRequest = new PatchOpRequest();
    Assertions.assertEquals(1, patchOpRequest.size());
    Assertions.assertEquals(1, patchOpRequest.getSchemas().size());
    Assertions.assertEquals(SchemaUris.PATCH_OP, patchOpRequest.getSchemas().get(0));

    patchOpRequest = PatchOpRequest.builder().build();
    Assertions.assertEquals(1, patchOpRequest.size());
    Assertions.assertEquals(1, patchOpRequest.getSchemas().size());
    Assertions.assertEquals(SchemaUris.PATCH_OP, patchOpRequest.getSchemas().get(0));
  }

  /**
   * verifies that getter and setter methods are working correctly
   */
  @Test
  public void testGetterAndSetter()
  {
    PatchRequestOperation operation = PatchRequestOperation.builder().build();
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();
    Assertions.assertEquals(2, patchOpRequest.size());
    Assertions.assertEquals(1, patchOpRequest.getOperations().size());
    Assertions.assertTrue(patchOpRequest.getOperations().get(0).isEmpty(),
                          patchOpRequest.getOperations().get(0).toPrettyString());
  }
}
