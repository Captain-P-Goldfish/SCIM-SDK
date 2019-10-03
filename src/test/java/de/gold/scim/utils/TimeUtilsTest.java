package de.gold.scim.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.gold.scim.exceptions.InvalidDateTimeRepresentationException;


/**
 * author Pascal Knueppel <br>
 * created at: 29.09.2019 - 22:25 <br>
 * <br>
 */
public class TimeUtilsTest
{

  /**
   * will produce a number of timestamp arguments for testing date parsing on scim documents
   */
  public static Stream<Arguments> getTimeStampArguments()
  {
    return Stream.of(Arguments.of(OffsetDateTime.now().withNano(0).format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(14))
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(-14))
                                         .withHour(0)
                                         .withMinute(0)
                                         .withSecond(0)
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(Instant.now()
                                         .atOffset(ZoneOffset.ofHours(-10))
                                         .withNano(0)
                                         .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now().toString()),
                     Arguments.of(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now()
                                               .withSecond(0)
                                               .withNano(0)
                                               .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now()
                                               .atOffset(ZoneOffset.ofHours(3))
                                               .withHour(0)
                                               .withMinute(0)
                                               .withSecond(0)
                                               .withNano(0)
                                               .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of(LocalDateTime.now()
                                               .withHour(0)
                                               .withMinute(0)
                                               .withSecond(0)
                                               .withNano(0)
                                               .format(DateTimeFormatter.ISO_DATE_TIME)),
                     Arguments.of("2019-09-29T24:00:00"),
                     Arguments.of("2019-09-29T24:00:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000"),
                     Arguments.of("2019-09-29T24:00:00Z"),
                     Arguments.of("2019-09-29T24:00:00.0000000Z"),
                     Arguments.of("2019-09-29T24:00:00.0000000-10:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000+10:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000-14:00"),
                     Arguments.of("2019-09-29T24:00:00.0000000+14:00"));
  }

  /**
   * this test will show that the parsing of timestamps is successfully executed up to RFC7643 chapter 2.3.5
   */
  @ParameterizedTest
  @MethodSource("getTimeStampArguments")
  public void testDateTimeparsingSuccess(String dateTime)
  {
    Assertions.assertDoesNotThrow(() -> TimeUtils.parseDateTime(dateTime));
  }

  /**
   * this test will show that the parsing of timestamps will fail if the timestamps do not applyto RFC7643
   * chapter 2.3.5
   */
  @ParameterizedTest
  @ValueSource(strings = {"hello world", "123456", "2019-12-24", "2019-12-24 13:54:28"})
  public void testDateTimeparsingFail(String dateTime)
  {
    Assertions.assertThrows(InvalidDateTimeRepresentationException.class, () -> TimeUtils.parseDateTime(dateTime));
  }
}
