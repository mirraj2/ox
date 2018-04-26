package ox.util;

import static ox.util.Utils.isNullOrEmpty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Map;

import com.google.common.collect.Maps;

public class Time {

  public static final ZoneId PACIFIC_TIME = ZoneId.of("America/Los_Angeles");
  public static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

  public static final DateTimeFormatter slashFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter longFormat = DateTimeFormatter.ofPattern("MMM d, yyyy");

  private static final Map<String, DateTimeFormatter> formatCache = Maps.newHashMap();

  public static long timestamp(LocalDate date) {
    return timestamp(date.atStartOfDay(PACIFIC_TIME));
  }

  public static long timestamp(ZonedDateTime zdt) {
    return zdt.toInstant().toEpochMilli();
  }

  public static LocalDate toDate(Long timestamp) {
    return timestamp == null ? null : toDateTime(timestamp).toLocalDate();
  }

  public static ZonedDateTime toDateTime(long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(PACIFIC_TIME);
  }

  public static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
  }

  public static LocalDate max(LocalDate a, LocalDate b) {
    return a.isAfter(b) ? a : b;
  }

  public static LocalDate now() {
    return LocalDate.now(PACIFIC_TIME);
  }

  public static int daysSince(long timestamp) {
    return (int) ChronoUnit.DAYS.between(Instant.ofEpochMilli(timestamp), Instant.now());
  }

  public static int minutesSince(long timestamp) {
    return (int) ChronoUnit.MINUTES.between(Instant.ofEpochMilli(timestamp), Instant.now());
  }

  public static String slashFormat(TemporalAccessor date) {
    return date == null ? "" : slashFormat.format(date);
  }

  public static String longFormat(TemporalAccessor date) {
    return date == null ? "" : longFormat.format(date);
  }

  public static String format(TemporalAccessor date, String format) {
    if (date == null) {
      return "";
    }
    DateTimeFormatter dtf = formatCache.computeIfAbsent(format, DateTimeFormatter::ofPattern);
    return dtf.format(date);
  }

  public static LocalDate parseDate(String s, String format) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    DateTimeFormatter dtf = formatCache.computeIfAbsent(format, pattern -> {
      return new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern(pattern)
          .toFormatter();
    });
    return LocalDate.parse(s, dtf);
  }

}
