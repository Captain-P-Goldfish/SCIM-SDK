package de.captaingoldfish.scim.sdk.server.schemas.custom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


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
   * verifies that the values are correctly set to true if used in the builder method
   */
  @ParameterizedTest
  @CsvSource({"true,true,true,true,true", "true,true,true,true,false", "true,true,true,false,true",
              "true,true,false,true,true", "true,false,true,true,true", "false,true,true,true,true"})
  public void testBuilderWithValuesSet(boolean create, boolean get, boolean list, boolean update, boolean delete)
  {
    EndpointControlFeature controlFeature = EndpointControlFeature.builder()
                                                                  .createDisabled(create)
                                                                  .getDisabled(get)
                                                                  .listDisabled(list)
                                                                  .updateDisabled(update)
                                                                  .deleteDisabled(delete)
                                                                  .build();
    Assertions.assertEquals(create, controlFeature.isCreateDisabled());
    Assertions.assertEquals(get, controlFeature.isGetDisabled());
    Assertions.assertEquals(list, controlFeature.isListDisabled());
    Assertions.assertEquals(update, controlFeature.isUpdateDisabled());
    Assertions.assertEquals(delete, controlFeature.isDeleteDisabled());

    if (create && get && list && update && delete)
    {
      Assertions.assertTrue(controlFeature.isResourceTypeDisabled());
    }
    else
    {
      Assertions.assertFalse(controlFeature.isResourceTypeDisabled());
    }
  }
}
