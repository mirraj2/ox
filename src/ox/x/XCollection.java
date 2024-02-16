package ox.x;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingCollection;

import ox.Log;
import ox.Threads;
import ox.util.Functions;
import ox.util.Utils;

public abstract class XCollection<T> extends ForwardingCollection<T> implements Iterable<T>, Collection<T> {

  private int maxThreads = 1;

  public abstract <V> XCollection<V> map(Function<T, V> function);

  @Override
  public void forEach(Consumer<? super T> callback) {
    if (maxThreads == 1) {
      super.forEach(callback);
    } else {
      if (hasData()) {
        Threads.get(Math.min(size(), maxThreads)).input(this.toList()).failFast().run(callback);
      }
      resetConcurrency();
    }
  }

  /**
   * Sets up the next operation to run on multiple threads (if supported).
   */
  public XCollection<T> concurrent(int maxThreads) {
    checkState(maxThreads > 0, "maxThreads must be a positive number.");
    this.maxThreads = maxThreads;
    return this;
  }

  private void resetConcurrency() {
    this.maxThreads = 1;
  }

  public XOptional<T> only() {
    int size = size();
    checkState(size < 2, "Expected one element, but had " + size);

    return first();
  }

  /**
   * Returns the singular element in this collection. If there are zero or multiple elements, returns EMPTY.
   */
  public XOptional<T> single() {
    if (size() > 1) {
      return XOptional.empty();
    }
    return first();
  }

  public XOptional<T> first() {
    Iterator<T> iter = iterator();
    if (iter.hasNext()) {
      return XOptional.ofNullable(iterator().next());
    }
      return XOptional.empty();
  }

  public boolean hasData() {
    return size() > 0;
  }

  public abstract XList<T> toList();

  public abstract XSet<T> toSet();

  public <V> XMap<V, T> index(Function<T, V> function) {
    return Functions.index(this, function);
  }

  public <B> XMap<T, B> toMap(Function<T, B> valueFunction) {
    return toMap(Function.identity(), valueFunction);
  }

  public <A, B> XMap<A, B> toMap(Function<T, A> keyFunction, Function<T, B> valueFunction) {
    XMap<A, B> ret = XMap.create();
    this.forEach(t -> {
      ret.put(keyFunction.apply(t), valueFunction.apply(t));
    });
    return ret;
  }

  /**
   * @exception if the set of values of {@code function} does not have exactly one element.
   * @return the unique value obtained from applying the function to the elements in this list.
   */
  public <V> V toUnique(Function<T, ? extends V> function) {
    return toSet(function).only().orElseNull();
  }

  public <V> XSet<V> toSet(Function<T, V> function) {
    return Functions.toSet(this, function);
  }

  public <K, V> XMultimap<K, V> toMultimap(Function<? super T, K> keyFunction,
      Function<? super T, V> valueFunction) {
    return Functions.buildMultimap(this, keyFunction, valueFunction);
  }

  public <V> XMultimap<V, T> indexMultimap(Function<? super T, V> function) {
    return Functions.indexMultimap(this, function);
  }

  public T reduce(T identity, BinaryOperator<T> reducer) {
    T ret = identity;
    for (T item : this) {
      ret = reducer.apply(ret, item);
    }
    return ret;
  }

  public String join(String separator) {
    return Joiner.on(separator).join(this);
  }

  public T random() {
    return Utils.random(this);
  }

  public T random(Random random) {
    return Utils.random(this, random);
  }

  public XCollection<T> log() {
    if (isEmpty()) {
      Log.debug("<Empty>");
    } else {
      forEach(Log::debug);
    }
    return this;
  }

  @Override
  public boolean equals(Object object) {
    return object == this || delegate().equals(object);
  }

}
