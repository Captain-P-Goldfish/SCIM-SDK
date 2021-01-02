package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class SortOrderTest
{

  @Test
  public void testGetSortOrderWithEmptyString()
  {
    Assertions.assertNull(SortOrder.getByValue(""));
    Assertions.assertNull(SortOrder.getByValue(" "));
    Assertions.assertNull(SortOrder.getByValue(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ascending", "ASCENDING", "AsCeNdInG"})
  public void testGetAscendingSortOrder(String value)
  {
    Assertions.assertEquals(SortOrder.ASCENDING, SortOrder.getByValue(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"descending", "DESCENDING", "DeScEnDiNg"})
  public void testGetDescendingSortOrder(String value)
  {
    Assertions.assertEquals(SortOrder.DESCENDING, SortOrder.getByValue(value));
  }

  @Test
  public void testGetSortOrderIllegalValue()
  {
    Assertions.assertThrows(BadRequestException.class, () -> SortOrder.getByValue("unknown"));
  }
}
