package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class ReturnedTest
{

  @Test
  public void testReturnedTest()
  {
    Assertions.assertEquals(Returned.DEFAULT, Returned.getByValue(""));
    Assertions.assertEquals(Returned.DEFAULT, Returned.getByValue(" "));
    Assertions.assertEquals(Returned.DEFAULT, Returned.getByValue(null));
    Assertions.assertEquals(Returned.DEFAULT, Returned.getByValue("unknown"));
  }
}
