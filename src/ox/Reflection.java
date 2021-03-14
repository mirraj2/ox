package ox;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import ox.util.Utils;
import ox.x.XList;
import ox.x.XOptional;
import sun.misc.Unsafe;

public class Reflection {

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private static final Map<String, Field> fieldCache = Maps.newConcurrentMap();
  private static final Map<String, Method> methodCache = Maps.newConcurrentMap();
  private static final Field modifiersField;

  private static final Field NULL_FIELD;
  private static final Method NULL_METHOD;
  static {
    try {
      NULL_FIELD = Reflection.class.getDeclaredField("fieldCache");
      NULL_METHOD = Reflection.class.getDeclaredMethod("nullMethod");
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

  public static void nullMethod() {
    // this is for the NULL_METHOD field
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

  public static <T> T get(Object o, String fieldName) {
    Field field = getField(o.getClass(), fieldName);
    return get(o, field);
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(Object o, Field field) {
    if (field == null) {
      return null;
    }
    try {
      return (T) field.get(o);
    } catch (Exception e) {
      throw propagate(e);
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
    Class<?> originalType = field.getType();
    Class<?> rawType = originalType;
    if (originalType == Optional.class || originalType == XOptional.class) {
      rawType = getTypeArgument(field.getGenericType());
    }
    if (value instanceof String) {
      if (rawType.isEnum()) {
        value = Utils.parseEnum((String) value, (Class<? extends Enum>) rawType);
      } else if (rawType == LocalDateTime.class) {
        value = LocalDateTime.parse((String) value);
      } else if (rawType == Json.class) {
        value = new Json((String) value);
      } else if (rawType == LocalTime.class) {
        value = LocalTime.parse((String) value);
      } else if (rawType == UUID.class) {
        value = UUID.fromString((String) value);
      }
    } else if (value instanceof java.sql.Date) {
      if (rawType == LocalDate.class) {
        value = ((java.sql.Date) value).toLocalDate();
      }
    } else if (value instanceof Long) {
      if (rawType == Money.class) {
        value = Money.fromLong((Long) value);
      } else if (rawType == Instant.class) {
        value = Instant.ofEpochMilli((Long) value);
      }
    } else if (value instanceof Integer) {
      if (rawType == Money.class) {
        value = Money.fromLong((Integer) value);
      } else if (rawType == Long.class) {
        value = ((Integer) value).longValue();
      }
    }

    if (originalType == Optional.class) {
      value = Optional.ofNullable(value);
    } else if (originalType == XOptional.class) {
      value = XOptional.ofNullable(value);
    }

    try {
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

  /**
   * Constructs an instance of the class without calling any constructors.
   */
  public static <T> T newInstance(Class<T> c) {
    return objenesis.newInstance(c);
  }

  /**
   * Constructs an instance of the class using its default (empty) constructor.
   */
  public static <T> T constructNewInstance(Class<T> c) {
    try {
      return c.getConstructor().newInstance();
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  public static Field getField(Object o, String fieldName) {
    return getField(o.getClass(), fieldName);
  }

  public static Field getField(Class<?> c, String fieldName) {
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

  public static XList<Field> getFields(Class<?> c) {
    return XList.of(c.getDeclaredFields());
  }

  public static XList<Method> getMethods(Class<?> c) {
    return XList.of(c.getDeclaredMethods());
  }

  public static Method getMethod(Class<?> c, String methodName) {
    return methodCache.computeIfAbsent(c.getName() + methodName, s -> {
      try {
        Method ret = c.getMethod(methodName);
        ret.setAccessible(true);
        return ret;
      } catch (NoSuchMethodException e) {
        return NULL_METHOD;
      } catch (Exception e) {
        throw propagate(e);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public static <T> T callMethod(Object o, String methodName) {
    Method m = getMethod(o.getClass(), methodName);
    try {
      return (T) m.invoke(o);
    } catch (Exception e) {
      throw propagate(e);
    }
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
      return getTypeArgument(t);
    } else {
      return null;
    }
  }

  private static Class<?> getTypeArgument(Type t) {
    Type type = ((ParameterizedType) t).getActualTypeArguments()[0];
    if (type instanceof ParameterizedType) {
      type = ((ParameterizedType) type).getRawType();
    }
    return (Class<?>) type;
  }

  @SuppressWarnings("unchecked")
  public static <T> XList<Constructor<T>> getConstructors(Class<?> c) {
    return XList.of((Constructor<T>[]) c.getConstructors());
  }

  @SuppressWarnings("unchecked")
  public static <T> XList<Class<? extends T>> findClasses(String packageName, Class<T> classType) {
    XList<Class<? extends T>> ret = XList.create();
    for (Class<?> c : findClasses(packageName)) {
      if (classType.isAssignableFrom(c)) {
        ret.add((Class<? extends T>) c);
      }
    }
    return ret;
  }

  public static XList<Class<?>> findClasses(String packageName) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Enumeration<URL> resources;
    try {
      resources = classLoader.getResources(packageName.replace('.', '/'));
    } catch (IOException e) {
      throw propagate(e);
    }
    List<File> dirs = Lists.newArrayList();
    Iterators.forEnumeration(resources).forEachRemaining(url -> {
      dirs.add(new File(url.getFile()));
    });
    XList<Class<?>> classes = XList.create();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return classes;
  }

  private static List<Class<?>> findClasses(File dir, String packageName) {
    List<Class<?>> classes = Lists.newArrayList();
    for (File file : dir.listFiles()) {
      String name = file.getName();
      if (file.isDirectory()) {
        checkState(!name.contains("."));
        classes.addAll(findClasses(file, packageName + "." + name));
      } else if (name.endsWith(".class")) {
        try {
          String s = packageName + '.' + name.substring(0, name.length() - 6);
          classes.add(Class.forName(s));
        } catch (ClassNotFoundException e) {
          throw propagate(e);
        }
      }
    }
    return classes;
  }

  public static boolean isAbstract(Class<?> c) {
    return Modifier.isAbstract(c.getModifiers());
  }

  public static boolean isPublic(Field f) {
    return Modifier.isPublic(f.getModifiers());
  }

  public static ClassWrapper is(Class<?> a) {
    return new ClassWrapper(a);
  }

  public static class ClassWrapper {

    private final Class<?> a;

    public ClassWrapper(Class<?> a) {
      this.a = a;
    }

    public boolean subclassOf(Class<?> b) {
      return b.isAssignableFrom(a);
    }

    public boolean superclassOf(Class<?> b) {
      return a.isAssignableFrom(b);
    }
  }

}
