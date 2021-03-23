package ox.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.size;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import ox.x.XList;
import ox.x.XMap;
import ox.x.XMultimap;
import ox.x.XSet;

public final class Functions {

  public static <A, B> XList<B> map(A[] array, Function<A, B> function) {
    return map(Arrays.asList(array), function);
  }

  public static <A, B> XList<B> map(Iterable<A> input, Function<A, B> function) {
    checkNotNull(input, "input");
    checkNotNull(function, "function");

    XList<B> ret = XList.createWithCapacity(size(input));
    for (A element : input) {
      ret.add(function.apply(element));
    }
    return ret;
  }

  public static <A, B> XSet<B> toSet(Iterable<A> input, Function<A, B> function) {
    checkNotNull(input, "input");
    checkNotNull(function, "function");

    XSet<B> ret = XSet.create(new LinkedHashSet<>(size(input)));
    for (A element : input) {
      ret.add(function.apply(element));
    }
    return ret;
  }

  public static <V> Iterable<V> reverse(Iterable<V> iter) {
    if (iter instanceof List) {
      return Lists.reverse((List<V>) iter);
    }
    return Lists.reverse(ImmutableList.copyOf(iter));
  }

  public static <K, V> XMap<K, V> index(Iterable<V> input, Function<V, K> function) {
    XMap<K, V> ret = XMap.create();
    input.forEach(x -> ret.put(function.apply(x), x));
    return ret;
  }

  public static <K, V> Map<K, V> indexAllowNulls(Iterable<V> input, Function<V, K> function) {
    Map<K, V> ret = Maps.newHashMap();
    for (V v : input) {
      ret.put(function.apply(v), v);
    }
    return ret;
  }

  /**
   * Unlike Multimaps.index, this allows 'null' keys and values.
   */
  public static <K, V> XMultimap<K, V> indexMultimap(Iterable<V> input, Function<? super V, K> function) {
    XMultimap<K, V> ret = XMultimap.create();
    for (V v : input) {
      ret.put(function.apply(v), v);
    }
    return ret;
  }

  public static <K, V, T> XMultimap<K, V> buildMultimap(Iterable<T> input, Function<? super T, K> keyFunction,
      Function<? super T, V> valueFunction) {
    XMultimap<K, V> ret = XMultimap.create();
    for (T t : input) {
      ret.put(keyFunction.apply(t), valueFunction.apply(t));
    }
    return ret;
  }

  public static <K1, K2, V1, V2> XMultimap<K2, V2> transformMultimap(Multimap<K1, V1> multimap,
      Function<? super K1, K2> keyFunction, Function<? super V1, V2> valueFunction) {
    XMultimap<K2, V2> ret = XMultimap.create();
    multimap.forEach((k, v) -> {
      ret.put(keyFunction.apply(k), valueFunction.apply(v));
    });
    return ret;
  }

  public static <T> XList<T> filter(Iterable<T> input, Predicate<? super T> filter) {
    checkNotNull(input, "input");
    checkNotNull(filter, "filter");

    XList<T> ret = XList.createWithCapacity(size(input));
    for (T t : input) {
      if (filter.test(t)) {
        ret.add(t);
      }
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  public static <T, F extends T> XList<F> filter(Iterable<T> input, Class<F> classFilter) {
    checkNotNull(classFilter);
    return (XList<F>) filter(input, t -> t != null && classFilter.isAssignableFrom(t.getClass()));
  }

  public static double sum(Iterable<? extends Number> input) {
    double ret = 0;
    for (Number n : input) {
      ret += n.doubleValue();
    }
    return ret;
  }

  public static <T> double sum(Iterable<T> input, Function<T, Number> function) {
    double ret = 0;
    for (T t : input) {
      Number n = function.apply(t);
      if (n != null) {
        ret += n.doubleValue();
      }
    }
    return ret;
  }

  @SafeVarargs
  public static <T extends Comparable<? super T>> T max(T... values) {
    return max(Arrays.asList(values));
  }

  public static <T extends Comparable<? super T>> T max(Iterable<T> iter) {
    T ret = null;
    for (T t : iter) {
      if (ret == null || t.compareTo(ret) > 0) {
        ret = t;
      }
    }
    return ret;
  }

  @SafeVarargs
  public static <T extends Comparable<? super T>> T min(T... values) {
    return min(Arrays.asList(values));
  }

  public static <T extends Comparable<? super T>> T min(Iterable<T> iter) {
    T ret = null;
    for (T t : iter) {
      if (ret == null || t.compareTo(ret) < 0) {
        ret = t;
      }
    }
    return ret;
  }

  public static <T> Consumer<T> emptyConsumer() {
    return t -> {
    };
  }

  public static Runnable emptyRunnable() {
    return () -> {
    };
  };

  public static <T> Predicate<T> not(Predicate<T> t) {
    return t.negate();
  }

  /**
   * Iterates through both iterables at the same time, calling the consumer for each pair of elements.
   * 
   * This will iterate until either one of the iterables runs out of elements.
   */
  public static <A, B> void splice(Iterable<A> iterA, Iterable<B> iterB, BiConsumer<A, B> consumer) {
    Iterator<A> a = iterA.iterator();
    Iterator<B> b = iterB.iterator();

    while (a.hasNext() && b.hasNext()) {
      consumer.accept(a.next(), b.next());
    }
  }

}
