package ox.util;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import ox.x.XList;

public class Regex {

  private static final Map<String, Pattern> patternCache = Maps.newConcurrentMap();

  public static Pattern pattern(String s) {
    return patternCache.computeIfAbsent(s, Pattern::compile);
  }

  /**
   * Returns true if the entire input matches the pattern.
   */
  public static boolean isExactMatch(String s, String input) {
    return input.equals(match(pattern(s), input));
  }

  public static String match(String pattern, String document) {
    return match(pattern(pattern), document);
  }

  /**
   * Gets the first match for the given pattern.
   */
  public static String match(Pattern pattern, String document) {
    Matcher m = pattern.matcher(document);
    if (!m.find()) {
      return null;
    }
    if (m.groupCount() == 0) {
      return m.group();
    }
    return m.group(1);
  }

  public static XList<String> matches(String pattern, String document) {
    return matches(pattern(pattern), document);
  }

  /**
   * Gets all matches for the given pattern.
   */
  public static XList<String> matches(Pattern pattern, String document) {
    XList<String> ret = XList.create();
    run(pattern, document, m -> {
      if (m.groupCount() == 0) {
        ret.add(m.group());
      } else {
        ret.add(m.group(1));
      }
    });
    return ret;
  }

  public static Matcher run(String pattern, String document, Consumer<Matcher> callback) {
    return run(pattern(pattern), document, callback);
  }

  public static Matcher run(Pattern pattern, String document, Consumer<Matcher> callback) {
    Matcher m = pattern.matcher(document);
    while (m.find()) {
      callback.accept(m);
    }
    return m;
  }

  public static String replaceAll(String pattern, String document, Function<Matcher, String> callback) {
    return replaceAll(pattern(pattern), document, callback);
  }

  public static String replaceAll(Pattern pattern, String document, Function<Matcher, String> callback) {
    StringBuffer ret = new StringBuffer();
    run(pattern, document, m -> {
      m.appendReplacement(ret, callback.apply(m));
    }).appendTail(ret);
    return ret.toString();
  }
}
