package de.captaingoldfish.scim.sdk.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;


public class EncodingUtilsTest
{


  /**
   * This test will show that the provided value has spaces escaped in the output.
   */
  @Test
  public void testUrlEncodingSuccess()
  {
    Assertions.assertEquals("Hello+World", EncodingUtils.urlEncode("Hello World"));
  }

  /**
   * This test will show that the provided value has spaces unescaped in the output.
   */
  @Test
  public void testUrlDecodingSuccess()
  {
    Assertions.assertEquals("Hello World", EncodingUtils.urlDecode("Hello+World"));
  }

  /**
   * This test will show that the provided values do not result in an exception when encoded.
   */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "+", "test"})
  public void testUrlEncodingGeneralCaseSuccess(String value)
  {
    Assertions.assertDoesNotThrow(() -> EncodingUtils.urlEncode(value));
  }

  /**
   * This test will show that the provided values do not result in an exception when encoded.
   */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "+", "test"})
  public void testUrlDecodingGeneralCaseSuccess(String value)
  {
    Assertions.assertDoesNotThrow(() -> EncodingUtils.urlEncode(value));
  }

}
