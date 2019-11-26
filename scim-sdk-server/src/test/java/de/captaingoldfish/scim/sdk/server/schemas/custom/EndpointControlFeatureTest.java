package de.captaingoldfish.scim.sdk.server.schemas.custom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;


/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 09:36 <br>
 * <br>
 */
public class EndpointControlFeatureTest
{

  /**
   * verifies that the default values are set to false
   */
  @Test
  public void testEmptyBuilder()
  {
    EndpointControlFeature controlFeature = EndpointControlFeature.builder().build();
    Assertions.assertFalse(controlFeature.isCreateDisabled());
    Assertions.assertFalse(controlFeature.isGetDisabled());
    Assertions.assertFalse(controlFeature.isListDisabled());
    Assertions.assertFalse(controlFeature.isUpdateDisabled());
    Assertions.assertFalse(controlFeature.isDeleteDisabled());
  }

  /**
   * this test will verify that the developer is not able to disable all of the resource type methods
   */
  @Test
  public void testNotAllMethodsAreDisabled()
  {
    try
    {
      EndpointControlFeature.builder()
                            .createDisabled(true)
                            .getDisabled(true)
                            .listDisabled(true)
                            .updateDisabled(true)
                            .deleteDisabled(true)
                            .build();
      Assertions.fail("this point must not be reached");
    }
    catch (InternalServerException ex)
    {
      Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
      Assertions.assertEquals("do not disable all endpoints. Disable the resource type itself instead", ex.getDetail());
    }
  }

  /**
   * verifies that the values are correctly set to true if used in the builder method
   */
  @Test
  public void testBuilderWithValuesSet()
  {
    EndpointControlFeature controlFeature = EndpointControlFeature.builder()
                                                                  .createDisabled(true)
                                                                  .getDisabled(true)
                                                                  .listDisabled(true)
                                                                  .updateDisabled(true)
                                                                  .build();
    Assertions.assertTrue(controlFeature.isCreateDisabled());
    Assertions.assertTrue(controlFeature.isGetDisabled());
    Assertions.assertTrue(controlFeature.isListDisabled());
    Assertions.assertTrue(controlFeature.isUpdateDisabled());
    Assertions.assertFalse(controlFeature.isDeleteDisabled());

    controlFeature = EndpointControlFeature.builder()
                                           .createDisabled(true)
                                           .getDisabled(true)
                                           .listDisabled(true)
                                           .deleteDisabled(true)
                                           .build();
    Assertions.assertTrue(controlFeature.isCreateDisabled());
    Assertions.assertTrue(controlFeature.isGetDisabled());
    Assertions.assertTrue(controlFeature.isListDisabled());
    Assertions.assertFalse(controlFeature.isUpdateDisabled());
    Assertions.assertTrue(controlFeature.isDeleteDisabled());
  }

  /**
   * if the provider tries to disable all endpoints the last setting must be returned
   */
  @Test
  public void testLastValueReturnsToFalse()
  {
    EndpointControlFeature controlFeature = EndpointControlFeature.builder()
                                                                  .createDisabled(true)
                                                                  .getDisabled(true)
                                                                  .listDisabled(true)
                                                                  .updateDisabled(true)
                                                                  .build();
    Assertions.assertThrows(InternalServerException.class, () -> controlFeature.setDeleteDisabled(true));
    Assertions.assertFalse(controlFeature.isDeleteDisabled());

    controlFeature.setUpdateDisabled(false);
    controlFeature.setDeleteDisabled(true);
    Assertions.assertThrows(InternalServerException.class, () -> controlFeature.setUpdateDisabled(true));
    Assertions.assertFalse(controlFeature.isUpdateDisabled());

    controlFeature.setListDisabled(false);
    controlFeature.setUpdateDisabled(true);
    Assertions.assertThrows(InternalServerException.class, () -> controlFeature.setListDisabled(true));
    Assertions.assertFalse(controlFeature.isListDisabled());

    controlFeature.setGetDisabled(false);
    controlFeature.setListDisabled(true);
    Assertions.assertThrows(InternalServerException.class, () -> controlFeature.setGetDisabled(true));
    Assertions.assertFalse(controlFeature.isGetDisabled());

    controlFeature.setCreateDisabled(false);
    controlFeature.setGetDisabled(true);
    Assertions.assertThrows(InternalServerException.class, () -> controlFeature.setCreateDisabled(true));
    Assertions.assertFalse(controlFeature.isCreateDisabled());
  }
}
