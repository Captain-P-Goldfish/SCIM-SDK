package de.captaingoldfish.scim.sdk.server.patch.msazure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PersonRole;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;


/**
 * @author Pascal Knueppel
 * @since 07.10.2023
 */
public class MsAzurePatchValueSubAttributeRebuilderTest
{

  /**
   * makes sure that the workaround-handler returns the original list in case of a remove-operation
   */
  @DisplayName("Remove operations are not handled")
  @Test
  public void testRemoveOperationsAreNotHandles()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"{\\\"display\\\":\\\"DocumentMgmt-BuyerAdmin\\\"}\"}",
                                                              "{\"value\": \"{\\\"display\\\":\\\"Buyer-Admin\\\"}\"}"));


    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that an empty or a null list of values will not cause any errors
   */
  @DisplayName("Empty or null lists do not cause errors")
  @Test
  public void testEmptyListsDoNotCauseErrors()
  {
    final PatchOp patchOp = PatchOp.ADD;

    {
      MsAzurePatchValueSubAttributeRebuilder handler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                  new ArrayList<>());
      List<String> fixedValues = handler.fixValues();
      Assertions.assertTrue(fixedValues.isEmpty());
    }
    {
      MsAzurePatchValueSubAttributeRebuilder handler = new MsAzurePatchValueSubAttributeRebuilder(patchOp, null);
      List<String> fixedValues = handler.fixValues();
      Assertions.assertNull(fixedValues);
    }
  }

  /**
   * makes sure that no error occurs if the sub-value attribute contains an invalid json structure
   */
  @DisplayName("Invalid-Json-Object in value-subAttribute does not cause errors")
  @Test
  public void testInvalidJsonObjectInSubValueCausesNoError()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = new ArrayList<>(Arrays.asList("{\"display\" \"Admin\"", "{\"display\" \"User\""));

    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that no error occurs if the sub-value attribute is a normal patch-value request without
   * underlying JSON-objects
   */
  @DisplayName("None-Json-Object in value-subAttribute does not cause errors")
  @Test
  public void testNonJsonObjectInSubValueCausesNoError()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = new ArrayList<>(Arrays.asList("{\"display\": \"Admin\"}", "{\"display\": \"User\"}"));

    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that no error occurs if the value-attribute is a normal patch-value request without underlying
   * JSON-objects
   */
  @DisplayName("Array-Json-Object in value-attribute does not cause errors")
  @Test
  public void testArrayJsonObjectInValueCausesNoError()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = new ArrayList<>(Arrays.asList("[{\"display\": \"Admin\"}]",
                                                              "[{\"display\": \"User\"}]"));

    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that no error occurs if the sub-value-attribute is a normal patch-value request without
   * underlying JSON-objects
   */
  @DisplayName("Array-Json-Object in value-subAttribute does not cause errors")
  @Test
  public void testArrayJsonObjectInSubValueCausesNoError()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = new ArrayList<>(Arrays.asList("{\"display\": [\"Admin\"]}",
                                                              "{\"display\": [\"User\"]}"));

    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * Makes sure that the handler is ignored if the value-sub-attribute has siblings.
   */
  @DisplayName("Handler does not execute if sub-value-attribute has siblings")
  @Test
  public void testIgnoreInnerSubValueIfMoreThanOneAttribute()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = Collections.singletonList("{\"value\": \"{\\\"display\\\":\\\"DocumentMgmt-BuyerAdmin\\\"}\","
                                                          + "\"$ref\": \"123456789\"}");


    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that the node is not parsed if the value-sub-attribute contains illegal non parseable json
   */
  @DisplayName("Handler is ignored if the inner value-sub-attribute contains illegal json")
  @Test
  public void testInnerValueSubAttributeIsIgnoredWhenIllegalJson()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = Collections.singletonList("{\"value\": \"{\\\"display\\\"\\\"DocumentMgmt-BuyerAdmin\\\"\"}");


    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that the node is not parsed if the value-sub-attribute is an array instead of an object
   */
  @DisplayName("Handler is ignored if the inner value-sub-attribute is an array")
  @Test
  public void testInnerValueSubAttributeIsIgnoredWhenArray()
  {
    final PatchOp patchOp = PatchOp.ADD;
    final List<String> values = Collections.singletonList("{\"value\": \"[{\\\"display\\\":\\\"DocumentMgmt-BuyerAdmin\\\"}]\"}");


    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(fixedValues, values);
  }

  /**
   * makes sure that the node is not parsed if the value-sub-attribute is an array instead of an object
   */
  @DisplayName("Handler works wor ADD and REPLACE operations")
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testHandlerWorksForAddAndReplaceOps(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"{\\\"display\\\":\\\"DocumentMgmt-BuyerAdmin\\\"}\"}",
                                                              "{\"value\": \"{\\\"display\\\":\\\"Buyer-Admin\\\"}\"}"));

    MsAzurePatchValueSubAttributeRebuilder workaroundHandler = new MsAzurePatchValueSubAttributeRebuilder(patchOp,
                                                                                                          values);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertNotEquals(fixedValues, values);
    {
      PersonRole personRole1 = JsonHelper.readJsonDocument(fixedValues.get(0), PersonRole.class);
      Assertions.assertEquals("DocumentMgmt-BuyerAdmin", personRole1.getDisplay().get());
    }
    {
      PersonRole personRole2 = JsonHelper.readJsonDocument(fixedValues.get(1), PersonRole.class);
      Assertions.assertEquals("Buyer-Admin", personRole2.getDisplay().get());
    }
  }
}
