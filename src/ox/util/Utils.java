package ox.util;

import static java.lang.Double.parseDouble;
import java.awt.Color;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import ox.Log;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

public class Utils {

  private static final Pattern emailPattern = Pattern.compile(
      "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\." +
          "[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

  public static final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00#########");
  public static final DecimalFormat decimalFormat2 = new DecimalFormat("#,##0.00");
  public static final DecimalFormat noDecimalFormat = new DecimalFormat("#,##0");
  private static final CharMatcher moneyMatcher = CharMatcher.anyOf("$£€ ,-–()").precomputed();

  public static String capitalize(String s) {
    StringBuilder sb = new StringBuilder(s.toLowerCase());
    boolean up = true;
    for (int i = 0; i < sb.length(); i++) {
      char c = sb.charAt(i);
      if (c == ' ') {
        up = true;
      } else if (up) {
        sb.setCharAt(i, Character.toUpperCase(c));
        up = false;
      }
    }
    return sb.toString();
  }

  public static String formatBytes(long size) {
    if (size <= 0) {
      return "0";
    }
    final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
    int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
    return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
  }

  public static double parseMoney(String s) {
    s = trim(s);
    double ret = parseDouble(moneyMatcher.removeFrom(s));
    if (s.charAt(0) == '-' || s.charAt(0) == '–' || s.charAt(0) == '(') {
      ret = -ret;
    }
    return ret;
  }

  public static String money(double d) {
    boolean negative = d < 0;
    if (negative) {
      return "-$" + formatDecimal2(-d);
    } else {
      return "$" + formatDecimal2(d);
    }
  }

  public static String format(double d) {
    return noDecimalFormat.format(d);
  }

  public static String formatDecimal(double d) {
    return decimalFormat.format(d);
  }

  public static String formatDecimal2(double d) {
    return decimalFormat2.format(d);
  }

  public static void debug(Object... objects) {
    Log.debug(Arrays.toString(objects));
  }

  public static <T extends Enum<T>> T parseEnum(String s, Class<T> enumType) {
    if (s == null) {
      return null;
    }
    Optional<T> o = Enums.getIfPresent(enumType, s);
    if (o.isPresent()) {
      return o.get();
    }
    for (T constant : enumType.getEnumConstants()) {
      if (constant.toString().equalsIgnoreCase(s)) {
        return constant;
      }
    }
    throw new IllegalArgumentException("No enum: " + enumType + "." + s);
  }

  public static boolean isValidPhoneNumber(String phoneNumber) {
    return true; // TODO
  }

  public static boolean isValidEmailAddress(String email) {
    return emailPattern.matcher(email).matches();
  }

  public static boolean isAlphaNumeric(String s) {
    return CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(s);
  }

  public static String trim(String s) {
    return CharMatcher.WHITESPACE.trimFrom(s);
  }

  public static void printStats(File dir) {
    int[] counts = new int[2];
    try {
      recurse(dir, counts);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    System.out.println(counts[0] + " java files.");
    System.out.println(counts[1] + " lines of code.");
  }

  private static void recurse(File f, int[] counts) throws Exception {
    if (f.isDirectory()) {
      if (f.getName().startsWith(".")
          ||
          ImmutableList.of("webbit", "Slick", "JSlick", "mod_open_src", "twitter4j", "javazoom", "Ostermiller", "org",
              "open_src")
              .contains(f.getName())) {
        return;
      }
      for (File ff : f.listFiles()) {
        recurse(ff, counts);
      }
    } else {
      if (f.getName().endsWith(".java")) {
        counts[0]++;
        int lineCount = Files.readLines(f, Charsets.UTF_8).size();
        if (lineCount > 500) {
          System.out.println(f);
        }
        counts[1] += lineCount;
      }
    }
  }

  public static String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

  public static int signum(double d) {
    return (int) Math.signum(d);
  }

  public static int random(int n) {
    return (int) (Math.random() * n);
  }

  public static <T> T random(T[] array) {
    return array[random(array.length)];
  }

  public static <T> T random(List<T> list) {
    return list.get(random(list.size()));
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] toArray(Collection<T> data, Class<T> c) {
    T[] ret = (T[]) Array.newInstance(c, data.size());
    data.toArray(ret);
    return ret;
  }

  public static <T> T first(Collection<T> c) {
    return c.iterator().next();
  }

  public static Color withAlpha(Color c, int alpha) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
  }

  public static String normalize(String s) {
    return s == null ? "" : s;
  }

  public static Double normalize(Double d) {
    return d == null ? 0.0 : d;
  }

  public static Integer normalize(Integer i) {
    return i == null ? 0 : i;
  }

  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }

  public static void logLoggers() {
    System.setErr(new PrintStream(new FilterOutputStream(System.out) {
      @Override
      public void write(int b) throws IOException {

        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
          System.out.println(e);
        }
      }
    }));
  }

}
