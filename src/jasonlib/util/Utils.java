package jasonlib.util;

import jasonlib.Log;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Pattern;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

public class Utils {

  private static final Pattern emailPattern = Pattern.compile(
      "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\." +
      "[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

  public static void debug(Object... objects) {
    Log.debug(Arrays.toString(objects));
  }

  public static <T extends Enum<T>> T parseEnum(String s, Class<T> enumType) {
    if (s == null) {
      return null;
    }
    return Enum.valueOf(enumType, s);
  }

  public static boolean isValidEmailAddress(String email) {
    return emailPattern.matcher(email).matches();
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
      if (f.getName().startsWith(".") ||
          ImmutableList.of("webbit", "Slick", "JSlick", "mod_open_src", "twitter4j", "javazoom", "Ostermiller", "org", "open_src")
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

  public static void main(String[] args) {
    printStats(new File("/users/jason/workspace/Mirrus"));
  }

}
