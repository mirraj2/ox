package jasonlib;

import jasonlib.util.Utils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
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
      if (value instanceof String) {
        if (type.isEnum()) {
          value = Utils.parseEnum((String) value, (Class<? extends Enum>) type);
        } else if (type == LocalDateTime.class) {
          value = LocalDateTime.parse((String) value);
        } else if (type == Json.class) {
          value = new Json((String) value);
        }
      }
      field.set(o, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Object call(Object o, String methodName) {
    try {
      for (Method m : o.getClass().getDeclaredMethods()) {
        if (m.getName().equals(methodName)) {
          return m.invoke(o);
        }
      }
      for (Method m : o.getClass().getMethods()) {
        if (m.getName().equals(methodName)) {
          return m.invoke(o);
        }
      }
      throw new RuntimeException("Method not found: " + methodName);
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
