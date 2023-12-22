package ox.x;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;

import ox.util.Utils;

public class XSet<T> extends ForwardingSet<T> implements XCollection<T>, Set<T> {

  private final Set<T> delegate;

  private XSet(Set<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  protected Set<T> delegate() {
    return delegate;
  }

  @Override
  public <V> XSet<V> map(Function<T, V> mappingFunction) {
    XSet<V> ret = XSet.create();
    for (T t : this) {
      ret.add(mappingFunction.apply(t));
    }
    return ret;
  }
  
  /**
   * Unlike map(), which calls the function one time per element, the given function will only be called once. It is
   * passed this entire list as an argument.
   */
  public <V> V mapBulk(Function<? super XSet<T>, V> function) {
    return function.apply(this);
  }

  public XSet<T> ifContains(T element, Runnable callback) {
    if (this.contains(element)) {
      callback.run();
    }
    return this;
  }

  public XSet<T> minus(Set<T> set2) {
    return XSet.create(Sets.difference(this, set2));
  }

  public XSet<T> intersect(Set<T> set2) {
    return XSet.create(Sets.intersection(this, set2));
  }

  public XSet<T> union(Set<T> set2) {
    return XSet.create(Sets.union(this, set2));
  }

  @SuppressWarnings("unchecked")
  public <S extends T> XSet<S> filter(Class<S> classFilter) {
    XSet<S> ret = XSet.create();
    for (T item : this) {
      if (item != null && classFilter.isAssignableFrom(item.getClass())) {
        ret.add((S) item);
      }
    }
    return ret;
  }

  public XSet<T> filter(Predicate<T> filter) {
    XSet<T> ret = XSet.create();
    for (T item : this) {
      if (filter.test(item)) {
        ret.add(item);
      }
    }
    return ret;
  }

  public XSet<T> removeNull() {
    remove(null);
    return this;
  }

  public boolean hasData() {
    return size() > 0;
  }

  @Override
  public XOptional<T> only() {
    return XOptional.ofNullable(Utils.only(this));
  }

  @Override
  public XList<T> toList() {
    return XList.create(this);
  }

  public <B> XList<B> toList(Function<T, B> mappingFunction) {
    XList<B> ret = XList.createWithCapacity(size());
    for (T item : this) {
      ret.add(mappingFunction.apply(item));
    }
    return ret;
  }

  @Override
  public XSet<T> toSet() {
    return this;
  }

  @Override
  public XSet<T> log() {
    XCollection.super.log();
    return this;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @SafeVarargs
  public static <T> XSet<T> of(T... values) {
    XSet<T> ret = new XSet<>(new LinkedHashSet<>(values.length));
    Collections.addAll(ret, values);
    return ret;
  }

  public static <T> XSet<T> create() {
    return new XSet<>(new LinkedHashSet<>());
  }

  public static <T> XSet<T> empty() {
    return createWithCapacity(0);
  }

  public static <T> XSet<T> createWithCapacity(int initialCapacity) {
    return new XSet<>(new LinkedHashSet<>(initialCapacity));
  }

  public static <T> XSet<T> create(Iterable<? extends T> iter) {
    return new XSet<T>(Sets.newLinkedHashSet(iter));
  }

}
