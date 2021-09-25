package de.captaingoldfish.scim.sdk.server.patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;


/**
 * @author Pascal Knueppel
 * @since 07.06.2021
 */
public class MsAzurePatchRemoveWorkaroundHandlerTest
{

  /**
   * verifies that the path is correctly handled in simple cases
   */
  @Test
  public void testFixPath()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\"}"));

    final String expectedPath = "members[value eq \"123456\"]";

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(expectedPath, pathResult);
  }

  /**
   * verifies that the path is correctly handled if several value objects are present
   */
  @Test
  public void testFixPathWithSeveralValueObjects()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\"}", "{\"value\": \"654321\"}"));

    final String expectedPath = "members[value eq \"123456\" or value eq \"654321\"]";

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(expectedPath, pathResult);
  }

  /**
   * verifies that the workaround does not change the path on REPLACE and ADD operations
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixPathWithIllegalPatchOp(PatchOp patchOp)
  {
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\"}"));

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }

  /**
   * verifies that the original path is returned if the values operand is empty
   */
  @Test
  public void testFixPathWithValuesListEmpty()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>();

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }

  /**
   * verifies that the original path is returned if a nested object was used instead of a simple object
   */
  @Test
  public void testFixPathWithNestedObject()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": {\"value\": \"123456\"}}"));

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }

  /**
   * verifies that the original path is returned if the value has an array instead of a simple value
   */
  @Test
  public void testFixPathWithArrayValue()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": [\"123456\"]}"));

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }

  /**
   * verifies that the original path is returned if the value object has several attributes
   */
  @Test
  public void testFixPathWithSeveralAttributesInValueObject()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\", \"display\": \"hello\"}"));

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }

  /**
   * verifies that the original path is returned if the value object has several attributes
   */
  @Test
  public void testFixPathWithNonObjectOnValueArray()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("[\"123456\"]"));

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }

  /**
   * verifies that the original path is returned if the value object has several attributes
   */
  @Test
  public void testFixPathWithNonJsonValue()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("123456"));

    MsAzurePatchRemoveWorkaroundHandler msAzurePatchRemoveWorkaroundHandler = new MsAzurePatchRemoveWorkaroundHandler(patchOp,
                                                                                                                      path,
                                                                                                                      values);
    String pathResult = msAzurePatchRemoveWorkaroundHandler.fixPath();

    Assertions.assertEquals(path, pathResult);
  }
}
