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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import ox.util.Utils;

public class Reflection {

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private static final Map<String, Field> fieldCache = Maps.newHashMap();
  private static final Field modifiersField;

  static {
    try {
      modifiersField = Field.class.getDeclaredField("modifiers");
    } catch (Exception e) {
      throw propagate(e);
    }
    modifiersField.setAccessible(true);
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
        } else if (type == UUID.class) {
          value = UUID.fromString((String) value);
        }
      } else if (value instanceof java.sql.Date) {
        if (type == LocalDate.class) {
          value = ((java.sql.Date) value).toLocalDate();
        }
      } else if (type == Money.class && value instanceof Integer) {
        value = Money.fromInt((Integer) value);
      } else if (value instanceof Long && type == Instant.class) {
        value = Instant.ofEpochMilli((long) value);
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
    return fieldCache.computeIfAbsent(key, k -> {
      try {
        return c.getDeclaredField(fieldName);
      } catch (Exception e) {
        return null;
      }
    });
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
