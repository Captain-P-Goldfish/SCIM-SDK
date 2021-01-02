package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class MutabilityTest
{

  @Test
  public void testMutabilityDefaultIsReadWrite()
  {
    Assertions.assertEquals(Mutability.READ_WRITE, Mutability.getByValue(""));
    Assertions.assertEquals(Mutability.READ_WRITE, Mutability.getByValue(" "));
    Assertions.assertEquals(Mutability.READ_WRITE, Mutability.getByValue(null));
    Assertions.assertEquals(Mutability.READ_WRITE, Mutability.getByValue("unknown"));
  }
}
