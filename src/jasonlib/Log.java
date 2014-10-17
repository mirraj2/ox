package jasonlib;

import java.io.PrintStream;

public class Log {

  private static final PrintStream out = System.out;

  public static void debug(Object o) {
    out.println(o);
  }

  public static void debug(Object o, Object... args) {
    out.println(String.format(String.valueOf(o), args));
  }

  public static void info(Object o) {
    out.println(o);
  }

  public static void info(Object o, Object... args) {
    out.println(String.format(String.valueOf(o), args));
  }

  public static void warn(Object o) {
    out.println(o);
  }

  public static void error(Object o) {
    out.println(o);
  }

}
