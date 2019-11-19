package de.captaingoldfish.scim.sdk.common.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import de.captaingoldfish.scim.sdk.common.exceptions.InvalidDateTimeRepresentationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 29.09.2019 - 21:17 <br>
 * <br>
 * this class shall help by parsing timestamps send within a scim request
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtils
{

  /**
   * year fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String YEAR_FRAGMENT = "([1-9]\\d\\d\\d|0\\d\\d\\d)";

  /**
   * month fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String MONTH_FRAGMENT = "(0[1-9]|1[0-2])";

  /**
   * day fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String DAY_FRAGMENT = "(0[1-9]|[12]\\d|3[01])";

  /**
   * hour fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String HOUR_FRAGMENT = "([01]\\d|2[0-3])";

  /**
   * minute fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String MINUTE_FRAGMENT = "[0-5]\\d";

  /**
   * second fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String SECOND_FRAGMENT = "[0-5]\\d(\\.\\d+)?";

  /**
   * end of day fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String END_OF_DAY_FRAGMENT = "24:00:00(\\.0+)?";

  /**
   * timezone fragment as defined in xsd-schema specification
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag">https://www.w3.org/TR/xmlschema11-2/#nt-yrFrag</a>
   */
  private static final String TIMEZONE_OFFSET_FRAGMENT = "(Z|([+\\-])((0\\d|1[0-3]):" + MINUTE_FRAGMENT + "|14:00))";

  /**
   * a partial date time definition that is probably printed by {@link java.time.LocalDateTime} toString
   * methods<br>
   * e.g.: 2019-09-29T21:16:50 <br>
   * notable that the TimeZone indicator 'Z' is missing in this representation. The problem is that this
   * representation cannot by parsed directly by {@link OffsetDateTime} why we are going to use this regular
   * expression to identify the problematic datetime representations
   */
  private static final String XSD_DATE_TIME_PARTIAL_FORMAT = YEAR_FRAGMENT + "-" + MONTH_FRAGMENT + "-" + DAY_FRAGMENT
                                                             + "T(" + HOUR_FRAGMENT + ":" + MINUTE_FRAGMENT + ":"
                                                             + SECOND_FRAGMENT + "|" + END_OF_DAY_FRAGMENT + ")";

  /**
   * the fully qualified xsd-datetime definition
   *
   * @see <a href=
   *      "https://www.w3.org/TR/xmlschema11-2/#dateTime">https://www.w3.org/TR/xmlschema11-2/#dateTime</a>
   */
  public static final String XSD_DATE_TIME_FORMAT = XSD_DATE_TIME_PARTIAL_FORMAT + TIMEZONE_OFFSET_FRAGMENT;

  /**
   * this method will try to parse the date time send in a scim resource request. Please not that the timestamp
   * format must apply to the xsd:datetime definition from W3C XML-schema definition specification as pointed in
   * RFC7643 in chapter 2.3.5
   *
   * @param dateTime the date time representation that should be parsed
   * @return the parsed date time in UTC or null if the given parameter was null
   * @throws InvalidDateTimeRepresentationException if the given string does not apply to the xsd:dateTime
   *           definition
   */
  public static Instant parseDateTime(String dateTime)
  {
    if (dateTime == null)
    {
      return null;
    }
    final String errorMessage = "value '" + dateTime + "' does not match the xsd:dateTime definition: "
                                + XSD_DATE_TIME_FORMAT;
    String tmpDateTime = dateTime;
    if (tmpDateTime.matches(XSD_DATE_TIME_PARTIAL_FORMAT))
    {
      tmpDateTime = tmpDateTime + "Z";
    }
    if (!tmpDateTime.matches(XSD_DATE_TIME_FORMAT))
    {
      throw new InvalidDateTimeRepresentationException(errorMessage, null, null, null);
    }
    if (tmpDateTime.matches(".*?" + END_OF_DAY_FRAGMENT + ".*"))
    {
      tmpDateTime = tmpDateTime.replace("24:00:00", "23:59:59");
    }
    try
    {
      return OffsetDateTime.parse(tmpDateTime).toInstant();
    }
    catch (DateTimeParseException ex)
    {
      throw new InvalidDateTimeRepresentationException(errorMessage, ex, null, null);
    }
  }
}
