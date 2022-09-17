package de.captaingoldfish.scim.sdk.common.etag;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 20.11.2019 - 09:18 <br>
 * <br>
 */
public class ETagTest
{

  /**
   * verifies that the default weak-value of the ETag builder is true
   */
  @ParameterizedTest
  @CsvSource({",", ",1", ",!lkdföä"})
  public void testBuilder(Boolean weak, String value)
  {
    ETag eTag = ETag.builder().weak(weak).tag(value).build();
    Assertions.assertTrue(eTag.isWeak());
    Assertions.assertEquals(StringUtils.stripToNull(value), eTag.getTag());
  }

  /**
   * verifies that the constructor throws an exception if the developer tries to add a value with quotes
   */
  @ParameterizedTest
  @ValueSource(strings = {"\"", "\"1\"", "\"\"!lkdföä"})
  public void testBuilderWithQuotesInValue(String illegalValue)
  {
    try
    {
      ETag.builder().tag(illegalValue).build();
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals("Please omit the quotes in the entity tag value '" + illegalValue + "'", ex.getDetail());
    }
  }

  /**
   * verifies that the parseEtag method works correctly
   *
   * @param weak If the eTag is weak or strong
   * @param expectedValue the value that should be found within the quotes of the entity tag
   */
  @ParameterizedTest
  @CsvSource({"true,", "true,1", "true,12", "true,!lkdföä", "false,", "false,1", "false,ölk1234!%&/"})
  public void testParseETag(boolean weak, String expectedValue)
  {
    String tag = "\"" + StringUtils.stripToEmpty(expectedValue) + "\"";
    String version = weak ? ETag.WEAK_IDENTIFIER + tag : tag;
    ETag eTag = ETag.newInstance(version);
    Assertions.assertEquals(weak, eTag.isWeak());
    Assertions.assertEquals(expectedValue, eTag.getTag());
    Assertions.assertEquals(version, eTag.getEntityTag());
  }

  /**
   * verifies that even non quoted strings are correctly added as ETag values
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "1", "12", "!lkdföä", "pölk1234!%&/"})
  public void testParseETagWithUnexpectedValues(String unexpectedValue)
  {
    String strippedValue = StringUtils.stripToNull(unexpectedValue);
    ETag eTag = ETag.newInstance(strippedValue);
    Assertions.assertTrue(eTag.isWeak());
    Assertions.assertEquals(strippedValue, eTag.getTag());
    Assertions.assertEquals(ETag.WEAK_IDENTIFIER + "\"" + unexpectedValue + "\"", eTag.getEntityTag());
  }

  /**
   * verifies that even non quoted strings are correctly added as ETag values
   */
  @ParameterizedTest
  @ValueSource(strings = {"\"\"", "\"1\"", "\"12\"", "\"!lkdföä\"", "\"pölk1234!%&/\""})
  public void testParseETagWithJustQuotedValues(String unexpectedValue)
  {
    ETag eTag = ETag.newInstance(unexpectedValue);
    Assertions.assertFalse(eTag.isWeak());
    Assertions.assertEquals(StringUtils.stripToNull(unexpectedValue.replaceAll("\"", "")), eTag.getTag());
    Assertions.assertEquals(unexpectedValue, eTag.getEntityTag());
  }

  /**
   * verifies that even non quoted strings are correctly added as ETag values
   */
  @ParameterizedTest
  @ValueSource(strings = {"\"\"\"", "1\"", "W/\"12", "W/\"\"\"\""})
  public void testParseETagWithIllegalValue(String illegalValue)
  {
    try
    {
      ETag.newInstance(illegalValue);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals("invalid entity tag value. Value was '" + illegalValue + "'. "
                              + "Value has a irregular number of quotes '"
                              + StringUtils.countMatches(illegalValue, "\"") + "'. Please take a look"
                              + " into RFC7232 for more detailed information of entity tags",
                              ex.getDetail());
    }
  }

  /**
   * verifies that the ETag comparison works correctly up to the following rules
   *
   * <pre>
   *    +--------+--------+-------------------+-----------------+
   *    | ETag 1 | ETag 2 | Strong Comparison | Weak Comparison |
   *    +--------+--------+-------------------+-----------------+
   *    | W/"1"  | W/"1"  | no match          | match           |
   *    | W/"1"  | W/"2"  | no match          | no match        |
   *    | W/"1"  | "1"    | no match          | match           |
   *    | "1"    | "1"    | match             | match           |
   *    +--------+--------+-------------------+-----------------+
   * </pre>
   */
  @ParameterizedTest
  @CsvSource({"W/\"1\",W/\"1\",true", "W/\"1\",W/\"2\",false", "W/\"1\",\"1\",true", "\"1\",\"1\",true",
              "\"1\",W/\"1\",false"})
  public void testCompareETags(String left, String right, boolean match)
  {
    ETag leftETag = ETag.parseETag(left);
    ETag rightETag = ETag.parseETag(right);
    if (match)
    {
      Assertions.assertEquals(leftETag, rightETag);
    }
    else
    {
      Assertions.assertNotEquals(leftETag, rightETag);
    }
  }

  @Test
  public void testToStringMethods()
  {
    ETag eTag = ETag.builder().tag("1").build();
    Assertions.assertEquals(eTag.getEntityTag(), eTag.toString());
    Assertions.assertEquals(eTag.getEntityTag(), eTag.toPrettyString());
  }

  @Test
  public void testEquals()
  {
    Assertions.assertNotEquals(ETag.builder().tag("1").build(), new Object());
    Assertions.assertEquals(ETag.builder().tag("1").build(), ETag.builder().tag("1").build());
  }
}
