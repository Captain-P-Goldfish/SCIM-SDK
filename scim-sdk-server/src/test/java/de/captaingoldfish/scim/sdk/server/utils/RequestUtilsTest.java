package de.captaingoldfish.scim.sdk.server.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;


/**
 * author Pascal Knueppel <br>
 * created at: 13.10.2019 - 15:43 <br>
 * <br>
 */
public class RequestUtilsTest
{

  /**
   * will verify that no exception is thrown if only one of the parameters are given
   */
  @ParameterizedTest
  @CsvSource({"nickName,", ",nickName"})
  public void testGiveAttributesAndExcludedAttributes(String attributes, String excludedAttributes)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.validateAttributesAndExcludedAttributes(attributes,
                                                                                             excludedAttributes));
  }

  /**
   * will verify that a {@link BadRequestException} is thrown if the parameters are * given both
   */
  @Test
  public void testGiveAttributesAndExcludedAttributes()
  {
    final String attributes = "nickName";
    final String excludedAttributes = "userName";
    Assertions.assertThrows(BadRequestException.class,
                            () -> RequestUtils.validateAttributesAndExcludedAttributes(attributes, excludedAttributes));
  }

  @ParameterizedTest
  @ValueSource(strings = {"p1=v1", "p1=v1&p2=v2", "&p1=v1&&p2=v2&", "p1=&p2=", "p1&p2", "=v1&=v2"})
  public void testGetQueryParameters(String query)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.getQueryParameters(query));
  }

}
