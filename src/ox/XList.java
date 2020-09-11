package ox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.google.common.collect.Multimap;

import ox.util.Functions;

/**
 * <p>
 * This class offers methods similar to those in Java's Stream API. This class offers convenience at the cost of
 * performance. 99.9% of the time, this performance difference will not matter and this class will be a superior
 * alternative.
 * </p>
 * 
 * <pre>
 * Using Java Stream API:
 * 
 * List<T> foo = new ArrayList<>();
 * 
 * foo.stream().map(...).collect(Collectors.toList());
 * </pre>
 * 
 * <pre>
 * Using XList:
 * 
 * XList<T> foo = new XList<>();
 * 
 * foo.map(...) //much easier!
 * </pre>
 */
public class XList<T> extends ArrayList<T> {

  public XList() {
  }

  public XList(int capacity) {
    super(capacity);
  }

  public XList(Iterable<T> iter) {
    iter.forEach(this::add);
  }

  @SafeVarargs
  public XList(T... values) {
    for (T value : values) {
      add(value);
    }
  }

  public XList<T> removeNulls() {
    return filter(Predicates.notNull());
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

  /**
   * Unlike map(), which calls the function one time per element, the given function will only be called once. It is
   * passed this entire list as an argument.
   */
  public <V> V mapBulk(Function<? super XList<T>, V> function) {
    return function.apply(this);
  }

  public <V> Set<V> toSet(Function<T, V> function) {
    return Functions.toSet(this, function);
  }

  public <V> Map<V, T> index(Function<T, V> function) {
    return Functions.index(this, function);
  }

  public <V> Multimap<V, T> indexMultimap(Function<? super T, V> function) {
    return Functions.indexMultimap(this, function);
  }

  public XList<T> sortSelf(Comparator<? super T> comparator) {
    sort(comparator);
    return this;
  }

  /**
   * Gets a list containing at MOST the limit number of items.
   */
  public XList<T> limit(int maxResults) {
    return limit(0, maxResults);
  }

  public XList<T> limit(int offset, int maxResults) {
    List<T> toAdd = subList(Math.min(offset, size()), Math.min(size(), offset + maxResults));

    XList<T> ret = XList.createWithCapacity(toAdd.size());
    ret.addAll(toAdd);
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

  public static <T> XList<T> empty() {
    return createWithCapacity(0);
  }

  public static <T> XList<T> of(T t) {
    XList<T> ret = createWithCapacity(1);
    ret.add(t);
    return ret;
  }

  public static <T extends Enum<T>> XList<T> ofEnum(Class<T> enumClass) {
    return create(enumClass.getEnumConstants());
  }

  public static <T> XList<T> createWithCapacity(int capacity) {
    return new XList<T>(capacity);
  }

  @SafeVarargs
  public static <T> XList<T> create(T... values) {
    return new XList<>(values);
  }

  public static <T> XList<T> create(Iterable<T> iter) {
    return new XList<T>(iter);
  }

}
