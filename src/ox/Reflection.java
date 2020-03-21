package ox;

import static ox.util.Utils.propagate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import ox.util.Utils;
import sun.misc.Unsafe;

public class Reflection {

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private static final Map<String, Field> fieldCache = Maps.newConcurrentMap();
  private static final Field modifiersField;

  private static final Field NULL_FIELD;
  static {
    try {
      NULL_FIELD = Reflection.class.getDeclaredField("fieldCache");
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  static {
    disableWarning();
    try {
      modifiersField = Field.class.getDeclaredField("modifiers");
    } catch (Exception e) {
      throw propagate(e);
    }
    modifiersField.setAccessible(true);
  }

  /**
   * Turns off the warning that puts 5 warnings out to the console:
   * 
   * WARNING: An illegal reflective access operation has occurred
   */
  public static void disableWarning() {
    try {
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      Unsafe u = (Unsafe) theUnsafe.get(null);

      Class<?> c = Class.forName("jdk.internal.module.IllegalAccessLogger");
      Field logger = c.getDeclaredField("logger");
      u.putObjectVolatile(c, u.staticFieldOffset(logger), null);
    } catch (ClassNotFoundException e) {
      // do nothing
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void load(Class<?> c) {
    try {
      Class.forName(c.getName());
    } catch (ClassNotFoundException e) {
      throw propagate(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(Object o, String fieldName) {
    Field field = getField(o.getClass(), fieldName);
    if (field == null) {
      return null;
    }
    try {
      return (T) field.get(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets a static field.
   */
  public static void set(Class<?> c, String fieldName, Object value) {
    Field field = getField(c, fieldName);
    try {
      field.set(null, value);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw propagate(e);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void set(Object o, String fieldName, Object value) {
    Field field = getField(o.getClass(), fieldName);
    if (field == null) {
      return;
    }
    try {
      Class<?> type = field.getType();
      if (value instanceof String) {
        if (type.isEnum()) {
          value = Utils.parseEnum((String) value, (Class<? extends Enum>) type);
        } else if (type == LocalDateTime.class) {
          value = LocalDateTime.parse((String) value);
        } else if (type == Json.class) {
          value = new Json((String) value);
        } else if (type == LocalTime.class) {
          value = LocalTime.parse((String) value);
        } else if (type == UUID.class) {
          value = UUID.fromString((String) value);
        }
      } else if (value instanceof java.sql.Date) {
        if (type == LocalDate.class) {
          value = ((java.sql.Date) value).toLocalDate();
        }
      } else if (type == Money.class && value instanceof Long) {
        value = Money.fromLong((Long) value);
      } else if (type == Money.class && value instanceof Integer) {
        value = Money.fromLong((Integer) value);
      } else if (type == Instant.class && value instanceof Long) {
        value = Instant.ofEpochMilli((Long) value);
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
          if (m.getParameterCount() == 0) {
            return m.invoke(o);
          }
        }
      }
      for (Method m : o.getClass().getMethods()) {
        if (m.getName().equals(methodName)) {
          if (m.getParameterCount() == 0) {
            return m.invoke(o);
          }
        }
      }
      throw new RuntimeException("Method not found: " + o.getClass().getSimpleName() + "." + methodName);
    } catch (Exception e) {
      Log.error("Problem calling method: " + methodName);
      throw propagate(e);
    }
  }

  public static <T> T newInstance(Class<T> c) {
    return objenesis.newInstance(c);
  }

  private static Field getField(Class<?> c, String fieldName) {
    String key = c.getName() + fieldName;

    Field ret = fieldCache.get(key);
    if (ret != null) {
      return ret == NULL_FIELD ? null : ret;
    }

    try {
      ret = c.getDeclaredField(fieldName);
      ret.setAccessible(true);
      modifiersField.setInt(ret, ret.getModifiers() & ~Modifier.FINAL);
    } catch (NoSuchFieldException e) {
      Class<?> parent = c.getSuperclass();
      if (parent == null) {
        ret = null;
      } else {
        ret = getField(parent, fieldName);
      }
    } catch (IllegalAccessException e) {
      throw propagate(e);
    }

    if (ret == null) {
      fieldCache.put(key, NULL_FIELD);
    } else {
      fieldCache.put(key, ret);
    }

    return ret;
  }

  public static List<Field> getFields(Class<?> c) {
    return ImmutableList.copyOf(c.getDeclaredFields());
  }

  /**
   * For example:<br>
   * public abstract class AbstractDB<T><br>
   * public class UserDB extends AbstractDB<User> <br>
   * <br>
   * If this method is given UserDB.class as input, it will return User.class
   */
  public static Class<?> getGenericClass(Class<?> c) {
    Type t = c.getGenericSuperclass();
    if (t instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) t).getActualTypeArguments()[0];
    } else {
      return null;
    }
  }

}
