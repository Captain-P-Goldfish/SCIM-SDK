package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class UniquenessTest
{

  /**
   * verify that "NONE" is the default value for uniqueness
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "unknown"})
  public void testGetDefaultUniqueness(String value)
  {
    Assertions.assertEquals(Uniqueness.NONE, Uniqueness.getByValue(StringUtils.stripToNull(value)));
  }
}
