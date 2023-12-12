package ox.x;

import java.util.Collection;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import com.google.common.base.Joiner;

import ox.util.Functions;
import ox.util.Utils;

public interface XCollection<T> extends Iterable<T>, Collection<T> {

  public <V> XCollection<V> map(Function<T, V> function);

  public XOptional<T> only();

  public XList<T> toList();

  public XSet<T> toSet();

  public default <V> XMap<V, T> index(Function<T, V> function) {
    return Functions.index(this, function);
  }

  public default <B> XMap<T, B> toMap(Function<T, B> valueFunction) {
    return toMap(Function.identity(), valueFunction);
  }

  public default <A, B> XMap<A, B> toMap(Function<T, A> keyFunction, Function<T, B> valueFunction) {
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
  public default <V> V toUnique(Function<T, ? extends V> function) {
    return toSet(function).only().orElseNull();
  }

  public default <V> XSet<V> toSet(Function<T, V> function) {
    return Functions.toSet(this, function);
  }

  public default <K, V> XMultimap<K, V> toMultimap(Function<? super T, K> keyFunction,
      Function<? super T, V> valueFunction) {
    return Functions.buildMultimap(this, keyFunction, valueFunction);
  }

  public default <V> XMultimap<V, T> indexMultimap(Function<? super T, V> function) {
    return Functions.indexMultimap(this, function);
  }

  public default T reduce(T identity, BinaryOperator<T> reducer) {
    T ret = identity;
    for (T item : this) {
      ret = reducer.apply(ret, item);
    }
    return ret;
  }

  public default String join(String separator) {
    return Joiner.on(separator).join(this);
  }

  public default T random() {
    return Utils.random(this);
  }

  public default T random(Random random) {
    return Utils.random(this, random);
  }

}
