package ox.util;

import com.google.common.base.CharMatcher;

public class Matchers {

  public static CharMatcher javaDigit() {
    return JAVA_DIGIT;
  }

  public static CharMatcher javaLetter() {
    return JAVA_LETTER;
  }

  public static CharMatcher javaLetterOrDigit() {
    return JAVA_LETTER_OR_DIGIT;
  }

  public static CharMatcher javaLowerCase() {
    return JAVA_LOWERCASE;
  }

  public static CharMatcher javaUpperCase() {
    return JAVA_UPPERCASE;
  }

  public static CharMatcher whitespace() {
    return CharMatcher.whitespace();
  }

  private static final CharMatcher JAVA_DIGIT = new CharMatcher() {
    @Override
    public boolean matches(char c) {
      return Character.isDigit(c);
    }
  };

  private static final CharMatcher JAVA_LETTER = new CharMatcher() {
    @Override
    public boolean matches(char c) {
      return Character.isLetter(c);
    }
  };

  private static final CharMatcher JAVA_LETTER_OR_DIGIT = new CharMatcher() {
    @Override
    public boolean matches(char c) {
      return Character.isLetterOrDigit(c);
    }
  };

  private static final CharMatcher JAVA_LOWERCASE = new CharMatcher() {
    @Override
    public boolean matches(char c) {
      return Character.isLowerCase(c);
    }
  };

  private static final CharMatcher JAVA_UPPERCASE = new CharMatcher() {
    @Override
    public boolean matches(char c) {
      return Character.isUpperCase(c);
    }
  };

}
