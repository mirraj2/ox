package ox;

import static ox.util.Utils.propagate;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ox.util.SplitOutputStream;
import ox.util.Time;

public class Log {

  public static final ZoneId PACIFIC_TIME = ZoneId.of("America/Los_Angeles");

  /**
   * If you're trying to find the source of a pesky log statement, set this to true.
   */
  private static final boolean debugMode = false;

  private static PrintStream originalOut = System.out;
  private static PrintStream originalErr = System.err;

  private static PrintStream out = originalOut;

  private static OutputStream lastFileOutput = null;

  private static File logFolder;
  private static LocalDate currentLogDate;

  public static void logToFolder(File folder) {
    logFolder = folder;
    logFolder.mkdirs();

    currentLogDate = LocalDate.now(Time.DEFAULT_TIME_ZONE);
    logToFile(new File(logFolder, currentLogDate + ".log"));

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    executor.scheduleAtFixedRate(() -> {
      System.out.flush();
      System.err.flush();
    }, 0, 100, TimeUnit.MILLISECONDS);

    executor.scheduleAtFixedRate(Log::rolloverLog, 1, 1, TimeUnit.MINUTES);
  }

  private static void rolloverLog() {
    LocalDate now = LocalDate.now(Time.DEFAULT_TIME_ZONE);
    if (now.equals(currentLogDate)) {
      return;
    }
    Log.info("Rolling over log to the next day.");
    currentLogDate = now;
    logToFile(new File(logFolder, currentLogDate + ".log"));
  }

  private static synchronized void logToFile(File file) {
    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file, true));
      System.setOut(new PrintStream(new SplitOutputStream(originalOut, os)));
      System.setErr(new PrintStream(new SplitOutputStream(originalErr, os)));
      out = System.out;

      if (lastFileOutput != null) {
        IO.close(lastFileOutput);
      }
      lastFileOutput = os;
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  private static void log(Object o) {
    log(o, (Object[]) null);
  }

  private static void log(Object o, Object... args) {
    if (debugMode) {
      Thread.dumpStack();
    }

    if (o == null) {
      o = "null";
    }

    if (o instanceof Throwable) {
      Throwable t = (Throwable) o;
      t.printStackTrace(out);
      return;
    }

    if (args == null) {
      if (o.getClass().isArray()) {
        o = arrayToString(o);
      }
      out.println(o);
    } else {
      out.println(String.format(String.valueOf(o), args));
    }
  }

  private static String arrayToString(Object array) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    int len = Array.getLength(array);
    for (int i = 0; i < len; i++) {
      sb.append(Array.get(array, i)).append(", ");
    }
    if (sb.length() > 2) {
      sb.setLength(sb.length() - 2);
    }
    sb.append(']');
    return sb.toString();
  }

  public static void debug(Object o) {
    log(o);
  }

  public static void debug(Object o, Object... args) {
    log(o, args);
  }

  public static void info(Object o) {
    log(o);
  }

  public static void info(Object o, Object... args) {
    log(o, args);
  }

  public static void warn(Object o) {
    log(o);
  }

  public static void warn(Object o, Object... args) {
    log(o, args);
  }

  public static void error(Object o) {
    log(o);
  }

}
