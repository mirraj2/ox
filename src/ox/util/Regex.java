package ox.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Regex {

  private static final Map<String, Pattern> patternCache = Maps.newConcurrentMap();

  /**
   * Returns true if the entire input matches the pattern.
   */
  public static boolean isExactMatch(String pattern, String input) {
    return input.equals(match(pattern, input));
  }

  /**
   * Gets the first match for the given pattern.
   */
  public static String match(String pattern, String document) {
    Pattern p = patternCache.computeIfAbsent(pattern, Pattern::compile);
    Matcher m = p.matcher(document);
    if (!m.find()) {
      return null;
    }
    if (m.groupCount() == 0) {
      return m.group();
    }
    return m.group(1);
  }

  /**
   * Gets all matches for the given pattern.
   */
  public static List<String> matches(String pattern, String document) {
    Pattern p = patternCache.computeIfAbsent(pattern, Pattern::compile);
    Matcher m = p.matcher(document);
    List<String> ret = Lists.newArrayList();
    while (m.find()) {
      if (m.groupCount() == 0) {
        ret.add(m.group());
      } else {
        ret.add(m.group(1));
      }
    }
    return ret;
  }

  public static String replaceAll(String pattern, String document, Function<Matcher, String> callback) {
    Pattern p = patternCache.computeIfAbsent(pattern, Pattern::compile);
    Matcher m = p.matcher(document);
    StringBuffer ret = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(ret, callback.apply(m));
    }
    m.appendTail(ret);
    return ret.toString();
  }
}
