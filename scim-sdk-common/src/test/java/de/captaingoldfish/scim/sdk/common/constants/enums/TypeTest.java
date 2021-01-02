package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.exceptions.UnknownValueException;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class TypeTest
{

  /**
   * verifies that an exception is thrown if an unknown value is used
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "unknown"})
  public void testUnknownTypeTest(String value)
  {
    Assertions.assertThrows(UnknownValueException.class, () -> Type.getByValue(StringUtils.stripToNull(value)));
  }
}
