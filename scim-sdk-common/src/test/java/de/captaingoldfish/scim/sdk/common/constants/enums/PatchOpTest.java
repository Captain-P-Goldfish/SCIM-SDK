package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.exceptions.UnknownValueException;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class PatchOpTest
{

  @Test
  public void testGetInvalidPatchOp()
  {
    Assertions.assertThrows(UnknownValueException.class, () -> PatchOp.getByValue(""));
    Assertions.assertThrows(UnknownValueException.class, () -> PatchOp.getByValue(" "));
    Assertions.assertThrows(UnknownValueException.class, () -> PatchOp.getByValue(null));
    Assertions.assertThrows(UnknownValueException.class, () -> PatchOp.getByValue("patch"));
  }

  /**
   * verifies that the patchop value can be retrieved case insensitive
   */
  @ParameterizedTest
  @ValueSource(strings = {"add", "ADD", "AdD", "replace", "REPLACE", "rePlacE", "remove", "REMOVE", "rEmOve"})
  public void testgetPatchOpByValueIsCaseInsensitive(String patchOpValue)
  {
    Assertions.assertDoesNotThrow(() -> PatchOp.getByValue(patchOpValue));
  }
}
