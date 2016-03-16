package ox.util;

import java.time.ZonedDateTime;

public class Time {

  public static long timestamp(ZonedDateTime zdt) {
    return zdt.toInstant().toEpochMilli();
  }

}
