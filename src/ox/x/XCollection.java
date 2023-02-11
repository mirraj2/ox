package ox.x;

import java.util.Collection;
import java.util.function.Function;

import com.google.common.base.Joiner;

import ox.util.Functions;

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

  public default String join(String separator) {
    return Joiner.on(separator).join(this);
  }

}
