package de.captaingoldfish.scim.sdk.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/**
 * This class provides methods for encoding data into specific formats.
 */
public class EncodingUtils
{

  /**
   * This method takes the input string and URL Encodes it using java.net.URLEncoder
   *
   * @param value The value to be URL encoded.
   * @return A URL encoded version of the input value.
   */
  public static String urlEncode(String value)
  {
    if (value == null)
    {
      return null;
    }

    try
    {
      return StringUtils.isBlank(value) ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
    catch (Exception e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * @param value The value to be URL decoded.
   * @return A URL decoded version of the input value.
   */
  public static String urlDecode(String value)
  {
    if (value == null)
    {
      return null;
    }

    try
    {
      return StringUtils.isBlank(value) ? "" : URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
    }
    catch (Exception e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
