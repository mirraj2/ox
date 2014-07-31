package jasonlib;

import com.google.common.base.Throwables;

public class Reflection {

  public static void load(Class<?> c) {
    try {
      Class.forName(c.getName());
    } catch (ClassNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

}
