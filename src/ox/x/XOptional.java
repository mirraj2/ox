package ox.x;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.function.Consumer;

/**
 * Like Optional, but with some additional functionality.
 */
public class XOptional<T> {

  private final T value;

  private XOptional(T value) {
    this.value = value;
  }

  public T get() {
    checkState(isPresent(), "Tried to get value from an empty Optional!");
    return value;
  }

  public boolean isPresent() {
    return value != null;
  }

  public boolean isEmpty() {
    return !isPresent();
  }

  public XOptional<T> ifPresent(Consumer<T> callback) {
    if (isPresent()) {
      callback.accept(value);
    }
    return this;
  }

  public XOptional<T> ifEmpty(Runnable callback) {
    if (!isEmpty()) {
      callback.run();
    }
    return this;
  }

  public T orElse(T alternativeValue) {
    return isPresent() ? this.value : alternativeValue;
  }

  public static <T> XOptional<T> empty() {
    return new XOptional<>(null);
  }

  public static <T> XOptional<T> of(T value) {
    checkNotNull(value);
    return new XOptional<T>(value);
  }

  public static <T> XOptional<T> ofNullable(T value) {
    return new XOptional<>(value);
  }

}
