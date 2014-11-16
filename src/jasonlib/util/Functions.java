package jasonlib.util;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Functions {

  public static <A, B> List<B> map(List<A> list, Function<A, B> function) {
    checkNotNull(list, "list");
    checkNotNull(function, "function");

    List<B> ret = new ArrayList<B>(list.size());
    for (A element : list) {
      ret.add(function.apply(element));
    }
    return ret;
  }
  
}
