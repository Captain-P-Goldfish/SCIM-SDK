package de.captaingoldfish.scim.sdk.common.utils;

import org.apache.commons.lang3.StringUtils;

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
   * @return A URL encoded version of the input value
   */
  public static String urlEncode(String value)
  {
    try
    {
      return StringUtils.isBlank(value) ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
    catch (Exception e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
