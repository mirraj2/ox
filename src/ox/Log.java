package ox;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ox.util.SplitOutputStream;
import ox.util.SynchronizedOutputStream;
import ox.util.Time;

public class Log {

  /**
   * If you're trying to find the source of a pesky log statement, set this to true.
   */
  public static boolean enableDebugMode = false;

  private static PrintStream originalOut = System.out;
  private static PrintStream originalErr = System.err;

  private static PrintStream out = originalOut;

  private static OutputStream lastFileOutput = null;

  private static File logFolder = null;
  private static LocalDate currentLogDate;

  private static Supplier<String> prefixSupplier = () -> "";

  private static Consumer<Throwable> exceptionHandler = (e) -> {
  };

  public static void showTimestamps() {
    prefix(() -> Instant.now() + " ");
  }

  public static void prefix(Supplier<String> prefixSupplier) {
    Log.prefixSupplier = checkNotNull(prefixSupplier);
  }

  public static void exceptionHandler(Consumer<Throwable> exceptionHandler) {
    Log.exceptionHandler = exceptionHandler;
  }

  public static void logToFolder(String appName) {
    logToFolder(File.appFolder(appName, "log"));
  }

  public static synchronized void logToFolder(File folder) {
    if (folder.equals(logFolder)) {
      return;
    }

    checkState(logFolder == null, "You've already called logToFolder!");

    logFolder = folder;
    logFolder.mkdirs();

    currentLogDate = LocalDate.now(Time.DEFAULT_TIME_ZONE);
    logToFile(logFolder.child(currentLogDate + ".log"));

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
        new NamedThreadFactory(Log.class).daemon());

    executor.scheduleAtFixedRate(Log::flush, 0, 100, TimeUnit.MILLISECONDS);
    executor.scheduleAtFixedRate(Log::rolloverLog, 1, 1, TimeUnit.MINUTES);

    // right before the JVM shuts down, make sure we flush the last of the log data
    Runtime.getRuntime().addShutdownHook(new Thread(Log::flush));
  }

  private static void flush() {
    System.out.flush();
    System.err.flush();
  }

  private static void rolloverLog() {
    LocalDate now = LocalDate.now(Time.DEFAULT_TIME_ZONE);
    if (now.equals(currentLogDate)) {
      return;
    }
    Log.info("Rolling over log to the next day.");
    currentLogDate = now;
    logToFile(logFolder.child(currentLogDate + ".log"));
  }

  private static synchronized void logToFile(File file) {
    try {
      OutputStream os = new SynchronizedOutputStream(new BufferedOutputStream(new FileOutputStream(file.file, true)));
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
    log(o, false, (Object[]) null);
  }

  private static void log(Object o, boolean isError, Object... args) {
    // without synchronizing here, you end up with weird cases like timestamps on their own line
    synchronized (out) {
      String prefix = prefixSupplier.get();
      if (!prefix.isEmpty()) {
        out.print(prefix);
      }

      if (enableDebugMode) {
        Thread.dumpStack();
      }

      if (o == null) {
        o = "null";
      }

      if (o instanceof Throwable) {
        Throwable t = (Throwable) o;
        t.printStackTrace(out);
        if (isError) {
          try {
            exceptionHandler.accept(t);
          } catch (Throwable tt) {
            tt.printStackTrace();
          }
        }
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
    log(o, false, args);
  }

  public static void info(Object o) {
    log(o);
  }

  public static void info(Object o, Object... args) {
    log(o, false, args);
  }

  public static void warn(Object o) {
    log(o);
  }

  public static void warn(Object o, Object... args) {
    log(o, false, args);
  }

  public static void error(Object o) {
    log(o, true, (Object[]) null);
  }

  public static void error(Object o, Object... args) {
    log(o, true, args);
  }

  public static void showAllJavaLogs() {
    setJavaLoggingLevel(Level.FINEST);
  }

  public static void setJavaLoggingLevel(Level level) {
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    rootLogger.setLevel(level);
    for (Handler h : rootLogger.getHandlers()) {
      h.setLevel(level);
    }
  }

}
