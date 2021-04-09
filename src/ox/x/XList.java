package ox.x;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;

import ox.Log;
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
public class XList<T> extends ForwardingList<T> {

  private final List<T> delgate;

  public XList() {
    this(new ArrayList<>());
  }

  private XList(List<T> delegate) {
    this.delgate = delegate;
  }

  public XList<T> replace(int index, Function<T, T> replacementFunction) {
    T newVal = replacementFunction.apply(get(index));
    set(index, newVal);
    return this;
  }

  @Override
  protected List<T> delegate() {
    return delgate;
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

  public XSet<T> toSet() {
    return XSet.create(this);
  }

  public <V> XSet<V> toSet(Function<T, V> function) {
    return Functions.toSet(this, function);
  }

  public <V> XMap<V, T> index(Function<T, V> function) {
    return Functions.index(this, function);
  }

  public <V> XMultimap<V, T> indexMultimap(Function<? super T, V> function) {
    return Functions.indexMultimap(this, function);
  }

  public <A, B> XMap<A, B> toMap(Function<T, A> keyFunction, Function<T, B> valueFunction) {
    XMap<A, B> ret = XMap.create();
    this.forEach(t -> {
      ret.put(keyFunction.apply(t), valueFunction.apply(t));
    });
    return ret;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public XList<T> sortSelf() {
    Collections.sort((List<? extends Comparable>) this);
    return this;
  }

  public XList<T> sortSelf(Comparator<? super T> comparator) {
    sort(comparator);
    return this;
  }

  public XList<T> reverse() {
    return XList.create(Lists.reverse(this));
  }

  /**
   * Gets a list containing at MOST the limit number of items.
   */
  public XList<T> limit(int maxResults) {
    return limit(0, maxResults);
  }

  public XList<T> offset(int offset) {
    return limit(offset, size());
  }

  public XList<T> limit(int offset, int maxResults) {
    List<T> toAdd = subList(Math.min(offset, size()), Math.min(size(), offset + maxResults));

    XList<T> ret = XList.createWithCapacity(toAdd.size());
    ret.addAll(toAdd);
    return ret;
  }

  public XOptional<T> first() {
    if (isEmpty()) {
      return XOptional.empty();
    }
    return XOptional.ofNullable(get(0));
  }

  public XOptional<T> last() {
    return isEmpty() ? XOptional.empty() : XOptional.ofNullable(get(size() - 1));
  }

  public XOptional<T> only() {
    int size = size();
    if (size == 1) {
      return XOptional.ofNullable(get(0));
    } else if (size == 0) {
      return XOptional.empty();
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
    forEach(Log::debug);
    return this;
  }

  public boolean hasData() {
    return size() > 0;
  }

  public static <T> XList<T> create() {
    return new XList<T>();
  }

  public static <T> XList<T> empty() {
    return createWithCapacity(0);
  }

  public static <T extends Enum<T>> XList<T> allOf(Class<T> enumClass) {
    return of(enumClass.getEnumConstants());
  }

  public static <T> XList<T> of(T t) {
    XList<T> ret = createWithCapacity(1);
    ret.add(t);
    return ret;
  }

  @SafeVarargs
  public static <T> XList<T> of(T... values) {
    return new XList<>(Lists.newArrayList(values));
  }

  public static <T> XList<T> createWithCapacity(int capacity) {
    return new XList<T>(Lists.newArrayListWithCapacity(capacity));
  }

  public static <T> XList<T> create(Iterable<? extends T> iter) {
    return new XList<T>(Lists.newArrayList(iter));
  }

  public static <T> XList<T> create(Collection<? extends T> c) {
    return new XList<T>(Lists.newArrayList(c));
  }

}
