package ox.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Time {

  public static final ZoneId PACIFIC_TIME = ZoneId.of("America/Los_Angeles");
  public static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

  public static final DateTimeFormatter slashFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter longFormat = DateTimeFormatter.ofPattern("MMM d, yyyy");

  public static long timestamp(ZonedDateTime zdt) {
    return zdt.toInstant().toEpochMilli();
  }

  public static LocalDate toDate(long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(PACIFIC_TIME).toLocalDate();
  }

  public static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
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

  public static String slashFormat(LocalDate date) {
    return date == null ? "" : date.format(slashFormat);
  }

  public static String longFormat(LocalDate date) {
    return date == null ? "" : date.format(longFormat);
  }

}
