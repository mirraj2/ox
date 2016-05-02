package ox.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getLast;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import java.awt.Color;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import ox.Log;

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
      } else if (c == '_') {
        up = true;
        sb.setCharAt(i, ' ');
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
    double ret = parseDouble(moneyMatcher.removeFrom(s));
    if (s.charAt(0) == '-' || s.charAt(0) == '–' || s.charAt(0) == '(') {
      ret = -ret;
    }
    return ret;
  }

  public static double parsePercent(String s) {
    return parseDouble(s.replace("%", ""));
  }

  public static String money(double d) {
    return money(d, true);
  }

  public static String money(double d, boolean decimals) {
    boolean negative = d < 0;
    DecimalFormat format = decimals ? decimalFormat2 : noDecimalFormat;
    if (negative) {
      return "-$" + format.format(-d);
    } else {
      return "$" + format.format(d);
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

  public static String formatDecimal(double d, int decimalPlaces) {
    StringBuilder pattern = new StringBuilder("#,##0.");
    for (int i = 0; i < decimalPlaces; i++) {
      pattern.append("0");
    }
    DecimalFormat format = new DecimalFormat(pattern.toString());
    return format.format(d);
  }

  public static void debug(Object... objects) {
    Log.debug(Arrays.toString(objects));
  }

  public static <T extends Enum<T>> T parseEnum(String s, Class<T> enumType) {
    if (s == null) {
      return null;
    }
    s = s.replace(' ', '_');
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
    return phoneNumber.length() >= 7; // TODO
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
    if (list.isEmpty()) {
      throw new IllegalStateException("Can't select a random element from an empty list!");
    }
    return list.get(random(list.size()));
  }

  public static List<Integer> count(int from, int to) {
    List<Integer> ret = Lists.newArrayList();
    for (int i = from; i <= to; i++) {
      ret.add(i);
    }
    return ret;
  }

  public static List<Double> count(double from, double to, double step) {
    List<Double> ret = Lists.newArrayList();
    for (double i = from; i <= to; i += step) {
      ret.add(i);
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] toArray(Collection<T> data, Class<T> c) {
    T[] ret = (T[]) Array.newInstance(c, data.size());
    data.toArray(ret);
    return ret;
  }

  public static <T> T first(Collection<T> c) {
    if (c.isEmpty()) {
      return null;
    }
    return c.iterator().next();
  }

  public static <T> T last(Collection<T> c) {
    if (c.isEmpty()) {
      return null;
    }
    return getLast(c);
  }

  public static <T> T only(Collection<T> c) {
    if (c.isEmpty()) {
      return null;
    }
    checkArgument(c.size() == 1, "Expected one element, but found " + c.size() + " :: " + c);
    return c.iterator().next();
  }

  public static Color withAlpha(Color c, int alpha) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
  }

  public static String normalize(String s) {
    return s == null ? "" : trim(s);
  }

  public static Double normalize(Double d) {
    return d == null ? 0.0 : d;
  }

  public static Integer normalize(Integer i) {
    return i == null ? 0 : i;
  }

  public static Boolean normalize(Boolean b) {
    return b == null ? Boolean.FALSE : b;
  }

  public static String checkNotEmpty(String s) {
    checkState(!isNullOrEmpty(s));
    return s;
  }

  public static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }

  public static Integer toInt(String s) {
    return isNullOrEmpty(s) ? null : parseInt(s);
  }

  public static Double toDouble(String s) {
    return isNullOrEmpty(s) ? null : parseDouble(s);
  }

  public static Long toLong(String s) {
    return isNullOrEmpty(s) ? null : parseLong(s);
  }

  public static Boolean toBoolean(String s) {
    return isNullOrEmpty(s) ? null : Boolean.parseBoolean(s);
  }

  public static LocalDate toDate(String s) {
    return isNullOrEmpty(s) ? null : LocalDate.parse(s);
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
