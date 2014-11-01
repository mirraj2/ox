package jasonlib;

import java.io.PrintStream;
import java.lang.reflect.Array;

public class Log {

  private static final PrintStream out = System.out;

  private static void log(Object o) {
    log(o, (Object[]) null);
  }

  private static void log(Object o, Object... args) {
    if (o == null) {
      o = "null";
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
    sb.setLength(sb.length() - 2);
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

  public static void error(Object o) {
    log(o);
  }

}
