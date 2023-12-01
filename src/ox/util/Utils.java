package ox.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.isEmpty;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

import ox.Log;
import ox.Money;
import ox.Percent;
import ox.x.XList;
import ox.x.XOptional;

public class Utils {

  private static final CharMatcher whiteSpace = CharMatcher.whitespace().or(CharMatcher.anyOf("\0\uFEFF"))
      .precomputed();

  private static final Pattern emailPattern = Pattern.compile(
      "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\." +
          "[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

  private static final Pattern formatPattern = Pattern.compile("\\{(\\d+)\\}");

  public static final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00#########");
  public static final DecimalFormat decimalFormat2 = new DecimalFormat("#,##0.00");
  public static final DecimalFormat noDecimalFormat = new DecimalFormat("#,##0");

  private static final SecureRandom random = new SecureRandom();
  private static final Base64.Encoder tokenEncoder = Base64.getUrlEncoder().withoutPadding();

  public static String capitalize(String s) {
    if (isNullOrEmpty(s)) {
      return s;
    }

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

  public static String sentenceCase(String s) {
    if (isNullOrEmpty(s)) {
      return s;
    }

    char[] chars = s.toCharArray();
    chars[0] = Character.toUpperCase(chars[0]);
    for (int i = 1; i < chars.length; i++) {
      char c = chars[i];
      if (c == '_') {
        chars[i] = ' ';
      } else {
        chars[i] = Character.toLowerCase(c);
      }
    }
    return new String(chars);
  }

  private static final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };

  /**
   * example: format("I ate {0} {1}", 3, "apples")
   */
  public static String format(String s, Object... args) {
    return Regex.replaceAll(formatPattern, s, matcher -> {
      String group = matcher.group(1);
      int index;
      try {
        index = parseInt(group);
      } catch (Exception e) {
        return group;
      }
      if (index < args.length) {
        return String.valueOf(args[index]);
      }
      return group;
    });
  }

  public static String formatBytes(long size) {
    if (size <= 0) {
      return "0";
    }
    int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
    return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
  }

  public static String money(double d) {
    return money(d, true);
  }

  public static String money(double d, boolean decimals) {
    boolean negative = d < 0;
    DecimalFormat format = decimals ? decimalFormat2 : noDecimalFormat;
    synchronized (format) {
      if (negative) {
        return "-$" + format.format(-d);
      } else {
        return "$" + format.format(d);
      }
    }
  }

  public static String format(double d) {
    synchronized (noDecimalFormat) {
      return noDecimalFormat.format(d);
    }
  }

  public static String formatDecimal(double d) {
    synchronized (decimalFormat) {
      return decimalFormat.format(d);
    }
  }

  public static String formatDecimal2(double d) {
    synchronized (decimalFormat2) {
      return decimalFormat2.format(d);
    }
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
    return tryParseEnum(s, enumType).orElseThrow(() -> new IllegalArgumentException("No enum: " + enumType + "." + s));
  }

  public static <T extends Enum<T>> XOptional<T> tryParseEnum(String s, Class<T> enumType) {
    if (isNullOrEmpty(s)) {
      return XOptional.empty();
    }
    Optional<T> o = Enums.getIfPresent(enumType, s.replace(' ', '_').replace('-', '_').toUpperCase());
    if (o.isPresent()) {
      return XOptional.of(o.get());
    }
    for (T constant : enumType.getEnumConstants()) {
      if (Matchers.javaLetterOrDigit().retainFrom(constant.toString()).equalsIgnoreCase(s)) {
        return XOptional.of(constant);
      }
    }

    return XOptional.empty();
  }

  public static boolean isValidPhoneNumber(String phoneNumber) {
    String digits = Matchers.javaDigit().retainFrom(phoneNumber);
    return digits.length() >= 10;
  }

  public static String formatPhone(String phoneNumber) {
    if (phoneNumber == null) {
      return null;
    }
    if (phoneNumber.startsWith("+1")) {
      phoneNumber = phoneNumber.substring(2);
    }
    if (phoneNumber.length() != 10) {
      return phoneNumber;
    }
    return "(" + phoneNumber.substring(0, 3) + ") " + phoneNumber.substring(3, 6) + "-" + phoneNumber.substring(6, 10);
  }

  public static boolean isValidEmailAddress(String email) {
    if (isNullOrEmpty(email)) {
      return false;
    }
    return emailPattern.matcher(email).matches();
  }

  public static boolean isAlphaNumeric(String s) {
    return Matchers.javaLetterOrDigit().matchesAllOf(s);
  }

  public static String trim(String s) {
    return whiteSpace.trimFrom(s);
  }

  public static String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw propagate(e);
    }
  }

  public static String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw propagate(e);
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

  public static <T> T random(Collection<T> c) {
    return random(c, Utils.random);
  }

  public static <T> T random(Collection<T> c, Random random) {
    if (c.isEmpty()) {
      return null;
    }
    return Iterables.get(c, random.nextInt(c.size()));
  }

  /**
   * Gets a list of all numbers between `from` and `to`, INCLUSIVE.
   */
  public static XList<Integer> count(int from, int to) {
    XList<Integer> ret = XList.create();
    for (int i = from; i <= to; i++) {
      ret.add(i);
    }
    return ret;
  }

  public static XList<Double> count(double from, double to, double step) {
    XList<Double> ret = XList.create();
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

  public static <T> T last(Iterable<T> c) {
    if (isEmpty(c)) {
      return null;
    }
    return getLast(c);
  }

  public static <T> T only(Iterable<T> c) {
    Iterator<T> iter = c.iterator();
    if (!iter.hasNext()) {
      return null;
    }
    T ret = iter.next();
    checkArgument(!iter.hasNext(), "Expected one element, but found multiple.");
    return ret;
  }

  public static String normalize(String s) {
    if (s == null) {
      return "";
    }

    String ret = trim(s);

    // convert multiple spaces into a single space
    while (ret.contains("  ")) {
      ret = ret.replace("  ", " ");
    }

    return ret;
  }

  public static String removeDiacriticalMarks(String s) {
    s = Normalizer.normalize(s, Normalizer.Form.NFD);
    return s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
  }

  public static double normalize(Double n) {
    return n == null ? 0.0 : n;
  }

  public static int normalize(Integer n) {
    return n == null ? 0 : n;
  }

  public static long normalize(Long n) {
    return n == null ? 0 : n;
  }

  public static boolean normalize(Boolean b) {
    return b == null ? Boolean.FALSE : b;
  }

  public static Money normalize(Money m) {
    return m == null ? Money.ZERO : m;
  }

  public static Percent normalize(Percent p) {
    return p == null ? Percent.ZERO : p;
  }

  public static String checkNotEmpty(String s) {
    checkState(!isNullOrEmpty(s));
    return s;
  }

  public static String checkNotEmpty(String s, Object errorMessage) {
    checkState(!isNullOrEmpty(s), errorMessage);
    return s;
  }

  public static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }

  public static String last(String s, int numCharacters) {
    return s.substring(s.length() - numCharacters, s.length());
  }

  public static String first(String s, String delimiter) {
    int i = s.indexOf(delimiter);
    checkState(i != -1, "'" + delimiter + "' not found in " + s);
    return s.substring(0, i);
  }

  public static String second(String s, String delimiter) {
    int i = s.indexOf(delimiter);
    checkState(i != -1, "'" + delimiter + "' not found in " + s);
    return s.substring(i + delimiter.length());
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

  public static Money toMoney(String s) {
    return isNullOrEmpty(s) ? null : Money.parse(s);
  }

  public static String bytesToString(byte[] data) {
    return new String(data, StandardCharsets.UTF_8);
  }

  public static String uuid() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * uuid() is for if you need a unique identifier.
   * 
   * token() is better for things where you don't want anyone to guess the token, such as for authenticating a user.
   */
  public static String token() {
    byte[] buffer = new byte[20];
    random.nextBytes(buffer);
    return tokenEncoder.encodeToString(buffer);
  }

  public static URI uri(String s) {
    try {
      return new URI(s);
    } catch (URISyntaxException e) {
      throw propagate(e);
    }
  }

  public static <T> void sort(List<T> list, Comparator<? super T> c) {
    if (list.size() < 2) {
      return;
    }
    list.sort(c);
  }

  /**
   * "foo/bar/ack.png" -> "png"
   */
  public static String getExtension(String path) {
    int i = path.lastIndexOf(".");
    if (i == -1) {
      return "";
    }
    String ret = path.substring(i + 1);
    if (!Matchers.javaLetterOrDigit().matchesAllOf(ret)) {
      return "";
    }
    return ret;
  }

  /**
   * 5 "apple" would return "5 apples"<br>
   * 1 "apple" would return "1 apple"
   */
  public static final String plural(long count, String word) {
    StringBuilder sb = new StringBuilder();
    sb.append(count).append(' ').append(word);
    if (count != 1) {
      sb.append('s');
    }
    return sb.toString();
  }

  public static String abbreviate(String s, int maxCharacterLength) {
    if (s.length() > maxCharacterLength) {
      return s.substring(0, maxCharacterLength - 3) + "...";
    }
    return s;
  }

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw propagate(e);
    }
  }

  public static RuntimeException propagate(Throwable throwable) {
    Throwables.throwIfUnchecked(throwable);
    throw new RuntimeException(throwable);
  }

  /**
   * If the given throwable was caused by an InterruptedException, this will throw the passed in exception.
   */
  public static void propagateInterruption(Throwable throwable) {
    getCause(throwable, InterruptedException.class).ifPresent(ie -> propagate(throwable));
  }

  public static <T extends Throwable> XOptional<T> getCause(Throwable t, Class<T> causeType) {
    return XList.create(Throwables.getCausalChain(t)).filter(causeType).first();
  }

  public static <T> T attempt(Supplier<T> function, int maxTries, int delayBetweenTriesMillis) {
    checkState(maxTries > 0);

    Throwable toThrow = null;
    for (int i = 0; i < maxTries; i++) {
      try {
        return function.get();
      } catch (Throwable t) {
        Log.error("Utils.attempt: Exception thrown, trying again...");
        toThrow = t;
        if (i < maxTries - 1) {
          sleep(delayBetweenTriesMillis);
        }
      }
    }
    throw propagate(toThrow);
  }

  public static XList<String> split(String s, String separator) {
    return XList.create(Splitter.on(separator).trimResults().omitEmptyStrings().split(s));
  }

}
