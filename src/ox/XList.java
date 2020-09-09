package ox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class XList<T> extends ArrayList<T> {

  public XList() {
  }

  public XList(Iterable<T> iter) {
    iter.forEach(this::add);
  }

  @SuppressWarnings("unchecked")
  public <S extends T> XList<S> filter(Class<S> classFilter) {
    XList<S> ret = new XList<>();
    for (T item : this) {
      if (item != null && classFilter.isAssignableFrom(item.getClass())) {
        ret.add((S) item);
      }
    }
    return ret;
  }

  public XList<T> filter(Predicate<T> filter) {
    XList<T> ret = new XList<>();
    for (T item : this) {
      if (filter.test(item)) {
        ret.add(item);
      }
    }
    return ret;
  }

  public <V> XList<V> map(Function<T, V> function) {
    XList<V> ret = new XList<>();
    for (T item : this) {
      ret.add(function.apply(item));
    }
    return ret;
  }

  public Optional<T> only() {
    int size = size();
    if (size == 1) {
      return Optional.ofNullable(get(0));
    } else if (size == 0) {
      return Optional.empty();
    } else {
      throw new IllegalStateException("Expected one element, but had " + size());
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  /**
   * Returns the maximum value in this collection. Assumes that all elements in this collection are Comparable
   */
  public T max() {
    return (T) Collections.max((Collection<? extends Comparable>) this);
  }

  public XList<T> log() {
    Log.debug(this);
    return this;
  }

  public static <T> XList<T> create() {
    return new XList<T>();
  }

  public static <T> XList<T> create(Iterable<T> iter) {
    return new XList<T>(iter);
  }

}
