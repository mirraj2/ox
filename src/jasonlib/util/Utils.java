package jasonlib.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class);

  private static final Pattern emailPattern = Pattern.compile(
      "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\." +
      "[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

  public static void debug(Object... objects) {
    logger.debug(Arrays.toString(objects));
  }

  public static boolean isValidEmailAddress(String email) {
    return emailPattern.matcher(email).matches();
  }

}
