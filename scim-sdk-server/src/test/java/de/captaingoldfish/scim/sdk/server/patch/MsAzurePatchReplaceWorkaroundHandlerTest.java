package de.captaingoldfish.scim.sdk.server.patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;


public class MsAzurePatchReplaceWorkaroundHandlerTest
{

  /**
   * verifies that the values is correctly handled in simple cases
   */
  @Test
  public void testFixValues()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"name.givenName\": \"captain\", \"name.familyName\": \"goldfish\" }"));
    final List<String> expectedValues = new ArrayList<>(Arrays.asList("{\"name\":{\"givenName\":\"captain\",\"familyName\":\"goldfish\"}}"));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertIterableEquals(expectedValues, valuesResult);
  }


  /**
   * verifies that the original values is returned on REMOVE and ADD operations
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REMOVE"})
  public void testFixValuesWithIllegalPatchOp(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"name.givenName\": \"captain\", \"name.familyName\": \"goldfish\" }"));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand is empty
   */
  @Test
  public void testFixValuesWithValuesListEmpty()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>();

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand has more than one item
   */
  @Test
  public void testFixValuesWithValuesListMoreThanOneItem()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("", ""));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand is not valid json
   */
  @Test
  public void testFixValuesWithValuesNotValidJson()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("{ not valid"));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand is not a JSON object
   */
  @Test
  public void testFixValuesWithValuesNotJsonObject()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("[ \"array\", \"not\", \"object\" ]"));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if fieldName contains multiple dots (sub-sub-attribute)
   */
  @Test
  public void testFixValuesWithFieldNameWithMoreThanOneDot()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"first.second.third\": \"value\" }"));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned in simple extension cases
   */
  @Test
  public void testFixValuesWithExtensionWithoutWorkaround()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"schemas\": [ \"urn:ietf:params:scim:schemas:core:2.0:User\", \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\" ], \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": { \"costCenter\": \"1234\" } }"));


    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the workaround is applied with extension
   */
  @Test
  public void testFixValuesWithExtensionWithWorkaroundApplied()
  {
    final PatchOp patchOp = PatchOp.REPLACE;
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"schemas\": [ \"urn:ietf:params:scim:schemas:core:2.0:User\", \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\" ], \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": { \"manager.displayName\": \"captain\" } }"));
    final List<String> expectedValues = new ArrayList<>(Arrays.asList("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\"],\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":{\"manager\":{\"displayName\":\"captain\"}}}"));


    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertIterableEquals(expectedValues, valuesResult);
  }


  /**
   * verifies that the workaround is applied on direct resource and fully qualified enterprise user
   */
  @Test
  public void testFixValuesWithUserAndEnterpriseUser()
  {
    final PatchOp patchOp = PatchOp.REPLACE;

    final List<String> values = new ArrayList<>(Arrays.asList("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\"],\"name.givenName\":\"goldfish\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":{\"costCenter\":\"1234\"}}"));
    final List<String> expectedValues = new ArrayList<>(Arrays.asList("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\"],\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":{\"costCenter\":\"1234\"},\"name\":{\"givenName\":\"goldfish\"}}"));

    MsAzurePatchReplaceWorkaroundHandler msAzurePatchReplaceWorkaroundHandler = new MsAzurePatchReplaceWorkaroundHandler(patchOp,
                                                                                                                         values);
    List<String> valuesResult = msAzurePatchReplaceWorkaroundHandler.fixValues();

    Assertions.assertIterableEquals(expectedValues, valuesResult);
  }


}

