package ox.x;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

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

  public <K2> XMap<K2, V> transformKeys(Function<K, K2> mappingFunction) {
    XMap<K2, V> ret = create();
    forEach((k, v) -> {
      ret.put(mappingFunction.apply(k), v);
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
  public XList<V> values() {
    return XList.create(super.values());
  }

  public XMap<K, V> log() {
    this.forEach((k, v) -> {
      Log.debug(k + " = " + v);
    });
    return this;
  }

  public static <K, V> XMap<K, V> create() {
    return new XMap<>(Maps.newLinkedHashMap());
  }

  public static <K, V> XMap<K, V> create(Map<K, V> input) {
    return new XMap<>(Maps.newLinkedHashMap(input));
  }
}
