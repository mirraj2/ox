package ox.x;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class XMultimap<K, V> extends ForwardingMultimap<K, V> {

  private Multimap<K, V> delgate;

  public XMultimap(Multimap<K, V> delgate) {
    this.delgate = delgate;
  }

  /**
   * Note: unlike a normal multimap, changes to this returned list will NOT affect the underlying multimap.
   */
  @Override
  public XList<V> get(K key) {
    Collection<V> c = super.get(key);
    return XList.create(c);
  }

  @Override
  public XList<V> values() {
    return XList.create(super.values());
  }

  @Override
  public XSet<K> keySet() {
    Set<K> set = super.keySet();
    return XSet.create(set);
  }

  @Override
  protected Multimap<K, V> delegate() {
    return delgate;
  }

  public static <K, V> XMultimap<K, V> create() {
    return new XMultimap<>(LinkedListMultimap.create());
  }

}
