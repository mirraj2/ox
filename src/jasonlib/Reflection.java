package jasonlib;

import jasonlib.util.Utils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import com.google.common.base.Throwables;

public class Reflection {

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private static final Field modifiersField;

  static {
    try {
      modifiersField = Field.class.getDeclaredField("modifiers");
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
    modifiersField.setAccessible(true);
  }

  public static void load(Class<?> c) {
    try {
      Class.forName(c.getName());
    } catch (ClassNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(Object o, String fieldName) {
    Field field = getField(o.getClass(), fieldName);
    if (field == null) {
      return null;
    }
    try {
      field.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
      return (T) field.get(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void set(Object o, String fieldName, Object value) {
    Field field = getField(o.getClass(), fieldName);
    if (field == null) {
      return;
    }
    try {
      field.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      Class<?> type = field.getType();
      if (type.isEnum() && value instanceof String) {
        value = Utils.parseEnum((String) value, (Class<? extends Enum>) type);
      }
      field.set(o, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Object call(Object o, String methodName) {
    try {
      Method m = o.getClass().getDeclaredMethod(methodName);
      return m.invoke(o);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public static <T> T newInstance(Class<T> c) {
    return objenesis.newInstance(c);
  }

  private static Field getField(Class<?> c, String fieldName) {
    try {
      return c.getDeclaredField(fieldName);
    } catch (Exception e) {
      return null;
    }
  }

}
