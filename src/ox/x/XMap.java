package ox.x;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.base.Functions;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import ox.Log;

public class XMap<K, V> extends ForwardingMap<K, V> {

  private Map<K, V> delgate;

  public XMap(Map<K, V> delgate) {
    this.delgate = delgate;
  }

  @Override
  protected Map<K, V> delegate() {
    return delgate;
  }

  public <K2> XMap<K2, V> transformKeys(Function<K, K2> keyFunction) {
    return transform(keyFunction, Functions.identity());
  }

  public <V2> XMap<K, V2> transformValues(Function<V, V2> valueFunction) {
    return transform(Functions.identity(), valueFunction);
  }

  public <K2, V2> XMap<K2, V2> transform(Function<K, K2> keyFunction, Function<V, V2> valueFunction) {
    XMap<K2, V2> ret = create();
    forEach((k, v) -> {
      ret.put(keyFunction.apply(k), valueFunction.apply(v));
    });
    return ret;
  }

  public <T> XList<T> toList(BiFunction<K, V, T> mappingFunction) {
    XList<T> ret = XList.create();
    forEach((k, v) -> {
      ret.add(mappingFunction.apply(k, v));
    });
    return ret;
  }

  @Override
  public XSet<K> keySet() {
    return XSet.create(super.keySet());
  }

  @Override
  public XList<V> values() {
    return XList.create(super.values());
  }

  public XMap<K, V> log() {
    this.forEach((k, v) -> {
      Log.debug(k + " = " + v);
    });
    return this;
  }

  public boolean hasData() {
    return size() > 0;
  }

  public static <K, V> XMap<K, V> of(K key, V value) {
    XMap<K, V> ret = create();
    ret.put(key, value);
    return ret;
  }

  public static <K, V> XMap<K, V> create() {
    return new XMap<>(Maps.newLinkedHashMap());
  }

  public static <K, V> XMap<K, V> create(Map<K, V> input) {
    return new XMap<>(Maps.newLinkedHashMap(input));
  }
}
