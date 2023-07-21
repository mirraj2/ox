package ox;

public class UnhandledCaseException extends RuntimeException {

  public UnhandledCaseException(Object o) {
    this(String.valueOf(o));
  }

  public UnhandledCaseException(String s) {
    super("Unhandled case: " + s);
  }
  
}
