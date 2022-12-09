package ox.x;

import java.util.function.Function;

import ox.util.Functions;

public interface XCollection<T> extends Iterable<T> {

  public <V> XCollection<V> map(Function<T, V> function);

  public default <V> XMap<V, T> index(Function<T, V> function) {
    return Functions.index(this, function);
  }

}
