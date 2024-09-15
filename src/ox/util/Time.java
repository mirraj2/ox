package ox.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static ox.util.Utils.isNullOrEmpty;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Map;

import com.google.common.collect.Maps;

public class Time {

  public static final ZoneId PACIFIC = ZoneId.of("US/Pacific");
  public static final ZoneId MOUNTAIN = ZoneId.of("US/Mountain");
  public static final ZoneId CENTRAL = ZoneId.of("US/Central");
  public static final ZoneId EASTERN = ZoneId.of("US/Eastern");
  public static ZoneId DEFAULT_TIME_ZONE = PACIFIC;

  public static final DateTimeFormatter slashFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter longFormat = DateTimeFormatter.ofPattern("MMM d, yyyy");
  public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a");

  private static final Map<String, DateTimeFormatter> formatCache = Maps.newHashMap();

  private static final InheritableThreadLocal<Clock> threadClocks = new InheritableThreadLocal<>();
  private static final Clock SYSTEM_CLOCK = Clock.system(DEFAULT_TIME_ZONE);
  private static Clock globalClock = SYSTEM_CLOCK;

  public static Instant timestamp(LocalDate date) {
    return timestamp(date.atStartOfDay(DEFAULT_TIME_ZONE));
  }

  public static Instant timestamp(ZonedDateTime zdt) {
    return zdt.toInstant();
  }

  public static LocalDate toDate(Instant instant) {
    // TODO LocalDate.ofInstant() once we are on java9
    return instant == null ? null : toDateTime(instant).toLocalDate();
  }

  public static ZonedDateTime toDateTime(Instant instant) {
    return instant.atZone(DEFAULT_TIME_ZONE);
  }

  public static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
  }

  public static LocalDate max(LocalDate a, LocalDate b) {
    return a.isAfter(b) ? a : b;
  }

  public static Instant max(Instant a, Instant b) {
    return a.isAfter(b) ? a : b;
  }

  public static LocalDate now() {
    return LocalDate.now(getClock());
  }

  public static LocalDate now(ZoneId timezone) {
    return LocalDate.now(getClock().withZone(timezone));
  }

  public static Instant nowInstant() {
    return Instant.now(getClock());
  }

  public static LocalDateTime nowLocalDateTime() {
    return LocalDateTime.now(getClock());
  }

  public static LocalDateTime nowLocalDateTime(ZoneId timezone) {
    return LocalDateTime.now(getClock().withZone(timezone));
  }

  public static int daysSince(long timestamp) {
    return daysSince(Instant.ofEpochMilli(timestamp));
  }

  public static int daysSince(Instant timestamp) {
    return (int) ChronoUnit.DAYS.between(timestamp, Instant.now());
  }

  public static int minutesSince(long timestamp) {
    return (int) ChronoUnit.MINUTES.between(Instant.ofEpochMilli(timestamp), Instant.now());
  }

  public static String slashFormat(TemporalAccessor date) {
    if (date instanceof Instant) {
      date = toDate((Instant) date);
    }
    return date == null ? "" : slashFormat.format(date);
  }

  public static String longFormat(TemporalAccessor date) {
    if (date instanceof Instant) {
      date = toDate((Instant) date);
    }
    return date == null ? "" : longFormat.format(date);
  }

  public static String format(LocalTime time) {
    return format(time, "h:mm a");
  }

  public static String format(TemporalAccessor date, String format) {
    if (date == null) {
      return "";
    }
    if (date instanceof Instant) {
      date = toDate((Instant) date);
    }
    DateTimeFormatter dtf;
    synchronized (formatCache) {
      dtf = formatCache.computeIfAbsent(format, DateTimeFormatter::ofPattern);
    }
    return dtf.format(date);
  }

  public static LocalDate parseDate(String s) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    return LocalDate.parse(s);
  }

  public static LocalDate parseDate(String s, DateTimeFormatter format) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    return LocalDate.parse(s, format);
  }

  /**
   * Attempts to try each of the given formats to parse the date.
   */
  public static LocalDate parseDate(String s, String... formats) {
    for (String format : formats) {
      try {
        return parseDate(s, format);
      } catch (Exception e) {
      }
    }
    throw new RuntimeException(s + " could not be parsed with any of the formats: " + formats);
  }

  public static LocalDate parseDate(String s, String format) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    DateTimeFormatter dtf = formatCache.computeIfAbsent(format, Time::createFormatter);
    return LocalDate.parse(s, dtf);
  }

  public static LocalTime parseTime(String s) {
    return parseTime(s, "h:mm a");
  }

  public static LocalTime parseTime(String s, String format) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    DateTimeFormatter dtf = formatCache.computeIfAbsent(format, Time::createFormatter);
    return LocalTime.parse(s, dtf);
  }

  private static DateTimeFormatter createFormatter(String pattern) {
      return new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern(pattern)
          .toFormatter();
  }

  public static void setDefaultTimeZone(ZoneId zone) {
    Time.DEFAULT_TIME_ZONE = checkNotNull(zone);
  }

  public static int getDaysInYear(int year) {
    LocalDate date = LocalDate.ofYearDay(year, 1);
    return Math.toIntExact(ChronoUnit.DAYS.between(date, date.plusYears(1)));
  }

  public static Clock getClock() {
    Clock ret = threadClocks.get();
    if (ret == null) {
      ret = globalClock;
    }
    return ret;
  }

  /**
   * Used for testing. All calls to now() methods in this thread and child threads will use the passed in clock instead
   * of the system clock.
   */
  public static void setClock(Clock clock) {
    threadClocks.set(clock);
  }

  public static void resetClock() {
    threadClocks.remove();
  }

  /**
   * Sets the clock for the entire process unlike {@code Time.setClock} that only sets the clock for the current thread
   * and its sub-threads. Used by the TimeMachine to mimic moving time forward for demonstrating/testing functionality
   * that depends on the passage of time such as Cron jobs.
   */
  public static void setGlobalClock(Clock clock) {
    globalClock = clock;
  }

  public static void resetGlobalClock() {
    globalClock = SYSTEM_CLOCK;
  }

}
