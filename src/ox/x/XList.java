package ox.x;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.sleep;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;

import ox.Log;
import ox.Threads;

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
public class XList<T> extends ForwardingList<T> implements XCollection<T> {

  private static final int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();

  private final List<T> delgate;

  private int maxThreads = 1;

  public XList() {
    this(new ArrayList<>());
  }

  private XList(List<T> delegate) {
    this.delgate = delegate;
  }

  @SuppressWarnings("unchecked")
  public XList<T> add(T... items) {
    for (T item : items) {
      add(item);
    }
    return this;
  }

  public XList<T> replace(int index, Function<T, T> replacementFunction) {
    T newVal = replacementFunction.apply(get(index));
    set(index, newVal);
    return this;
  }

  public XOptional<T> getOptional(int index) {
    if (index < 0 || index >= size()) {
      return XOptional.empty();
    }
    return XOptional.of(get(index));
  }

  @Override
  protected List<T> delegate() {
    return delgate;
  }

  public XList<T> removeNulls() {
    return filter(Predicates.notNull());
  }

  @SuppressWarnings("unchecked")
  public <S> XList<S> filter(Class<S> classFilter) {
    XList<S> ret = new XList<>();
    for (T item : this) {
      if (item != null && classFilter.isAssignableFrom(item.getClass())) {
        ret.add((S) item);
      }
    }
    return ret;
  }

  public XList<T> filter(Predicate<T> filter) {
    XList<T> ret = new XList<>(new ArrayList<>(Math.min(size(), 10)));
    for (T item : this) {
      if (filter.test(item)) {
        ret.add(item);
      }
    }
    return ret;
  }

  /**
   * Return true if any elements satisfy the condition.
   */
  public boolean any(Predicate<T> condition) {
    for (T item : this) {
      if (condition.test(item)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void forEach(Consumer<? super T> callback) {
    if (maxThreads == 1) {
      super.forEach(callback);
    } else {
      if (hasData()) {
        Threads.get(Math.min(size(), maxThreads)).input(this).run(callback);
      }
      resetConcurrency();
    }
  }

  /**
   * Just like a forEach(), but ensures that the callback is not called too often -- based on the time specified.
   */
  public void forEachRateLimited(long time, TimeUnit unit, Consumer<? super T> callback) {
    final long minMillisToWait = unit.toMillis(time);

    final int size = size();

    for (int i = 0; i < size; i++) {
      if (i == size - 1) {
        // we reached the last item, no need to wait after.
        callback.accept(get(i));
      } else {
        long startTime = System.currentTimeMillis();
        callback.accept(get(i));
        long endTime = System.currentTimeMillis();

        long timeToWait = minMillisToWait - (endTime - startTime);
        if (timeToWait > 0) {
          sleep(timeToWait);
        }
      }
    }
  }

  @Override
  public <V> XList<V> map(Function<T, V> function) {
    XList<V> ret = XList.createWithCapacity(size());

    forEach(item -> {
      V toAdd = function.apply(item);
      synchronized (ret) {
        ret.add(toAdd);
      }
    });

    return ret;
  }

  /**
   * [[A, B], [C, D, E]] -> [A, B, C, D, E]
   */
  @SuppressWarnings("unchecked")
  public <V> XList<V> flatten() {
    XList<V> ret = new XList<>();
    for (T item : this) {
      if (!(item instanceof Iterable)) {
        throw new IllegalStateException("Expected all elements in this list to be Iterable, but found: " + item);
      }
      ((Iterable<V>) item).forEach(ret::add);
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

  @SuppressWarnings({ "unchecked", "rawtypes" })
  /**
   * Returns the maximum value in this collection. Assumes that all elements in this collection are Comparable
   */
  public XOptional<T> max() {
    if (isEmpty()) {
      return XOptional.empty();
    }
    return XOptional.of((T) Collections.max((Collection<? extends Comparable>) this));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  /**
   * Returns the maximum value in this collection. Assumes that all elements in this collection are Comparable
   */
  public XOptional<T> min() {
    if (isEmpty()) {
      return XOptional.empty();
    }
    return XOptional.of((T) Collections.min((Collection<? extends Comparable>) this));
  }

  @Override
  public XSet<T> toSet() {
    return XSet.create(this);
  }

  /**
   * Mutates this list.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public XList<T> sortSelf() {
    Collections.sort((List<? extends Comparable>) this);
    return this;
  }

  /**
   * Mutates this list.
   */
  public XList<T> sortSelf(Comparator<? super T> comparator) {
    sort(comparator);
    return this;
  }

  @SuppressWarnings("unchecked")
  public <V> XList<T> sortSelf(Function<T, Comparable<V>> mapping) {
    return sortSelf((a, b) -> {
      return mapping.apply(a).compareTo((V) mapping.apply(b));
    });
  }

  public XList<T> shuffleSelf() {
    Collections.shuffle(this);
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

  @Override
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

  public XList<T> log() {
    forEach(Log::debug);
    return this;
  }

  public boolean hasData() {
    return size() > 0;
  }

  /**
   * Iterates through all pairs of elements in this List. O(n^2)
   */
  public XList<T> iterateAllPairs(BiConsumer<T, T> callback) {
    for (int i = 0; i < size(); i++) {
      for (int j = i + 1; j < size(); j++) {
        callback.accept(get(i), get(j));
      }
    }
    return this;
  }

  /**
   * Splits this list into chunks.
   * 
   * Example: If this list had 13 items and chunkSize was set to 5, the result would have 3 chunks: [5 items, 5 items, 3
   * items]
   */
  public XList<XList<T>> chunks(int chunkSize) {
    XList<XList<T>> ret = XList.create();
    for (int i = 0; i < size(); i += chunkSize) {
      ret.add(limit(i, chunkSize));
    }
    return ret;
  }

  public XList<T> concurrent() {
    return concurrent(NUM_PROCESSORS * 2);
  }

  public XList<T> concurrentAll() {
    return concurrent(Integer.MAX_VALUE);
  }

  /**
   * Sets up the next operation to run on multiple threads (if supported).
   */
  public XList<T> concurrent(int maxThreads) {
    checkState(maxThreads > 0, "maxThreads must be a positive number.");
    this.maxThreads = maxThreads;
    return this;
  }

  private void resetConcurrency() {
    this.maxThreads = 1;
  }

  @Override
  public int size() {
    return delgate.size();
  }

  @Override
  public XList<T> toList() {
    return this;
  }

  @SuppressWarnings("unchecked")
  public T[] toArray(Class<T> componentType) {
    T[] ret = (T[]) Array.newInstance(componentType, size());
    for (int i = 0; i < size(); i++) {
      ret[i] = get(i);
    }
    return ret;
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
