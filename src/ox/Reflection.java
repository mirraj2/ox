package ox;

import static ox.util.Utils.first;
import static ox.util.Utils.propagate;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;

import ox.util.Time;
import ox.util.Utils;
import ox.x.XList;
import ox.x.XOptional;
import ox.x.XSet;

import sun.misc.Unsafe;

public class Reflection {

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private static final Map<String, Field> fieldCache = Maps.newConcurrentMap();
  private static final Map<String, Method> methodCache = Maps.newConcurrentMap();
  private static final Map<Class<?>, XList<Field>> allFieldsCache = Maps.newConcurrentMap();
  private static final Table<Class<?>, Class<?>, Function<Object, Object>> converters = HashBasedTable.create();
  private static Field modifiersField;
  private static final Map<String, Boolean> hasOverrideCache = Maps.newConcurrentMap();

  public static final XSet<Class<?>> BOXED_TYPES = XSet.of(Byte.class, Short.class, Integer.class, Long.class,
      Float.class, Double.class, Character.class, Boolean.class);

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
    } catch (NoSuchFieldException e) {
      Log.warn("Reflection: Running on JDK without Field.modifiers.");
    }
    if (modifiersField != null) {
      modifiersField.setAccessible(true);
    }
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
      Log.error(e);
    }
  }

  public static void load(Class<?> c) {
    try {
      Class.forName(c.getName());
    } catch (ClassNotFoundException e) {
      throw propagate(e);
    }
  }

  public static <T> T get(Class<?> c, String staticFieldName) {
    Field field = getField(c, staticFieldName);
    return get(null, field);
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
      field.setAccessible(true);
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

  public static void set(Object o, String fieldName, Object value) {
    Field field = getField(o.getClass(), fieldName);
    set(o, field, value);
  }

  public static void set(Object o, Field field, Object value) {
    try {
      value = convert(value, field.getGenericType());
      field.setAccessible(true);
      field.set(o, value);
    } catch (Exception e) {
      Log.error("Problem setting field: " + field.getName());
      throw new RuntimeException(e);
    }
  }

  public static <T> T convert(Object value, Type targetType) {
    return convert(value, targetType, TypeToken.of(targetType).getRawType());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T> T convert(Object value, Type targetType, Class<?> wrappedClass) {
    Class<?> targetClass;
    if (!(value instanceof XOptional || value instanceof Optional)
        && (wrappedClass == Optional.class || wrappedClass == XOptional.class)) {
      targetClass = getClassArgument(targetType);
    } else {
      targetClass = wrappedClass;
    }
    if (value instanceof String) {
      String s = (String) value;
      if (targetClass.isEnum()) {
        value = Utils.parseEnum(s, (Class<? extends Enum>) targetClass);
      } else if (targetClass == LocalDateTime.class) {
        value = LocalDateTime.parse(s);
      } else if (targetClass == LocalDate.class) {
        value = Time.parseDate(s);
      } else if (targetClass == LocalTime.class) {
        value = LocalTime.parse(s);
      } else if (targetClass == Json.class) {
        value = new Json(s);
      } else if (targetClass == UUID.class) {
        value = UUID.fromString(s);
      } else if (targetClass == Percent.class) {
        value = Percent.parse(s);
      } else if (targetClass == ZoneId.class) {
        value = ZoneId.of(s);
      } else if (targetClass == Money.class) {
        value = Money.parse(s);
      } else if (targetClass == Boolean.class) {
        value = Boolean.parseBoolean(s);
      } else if (targetClass == Integer.class || targetClass == int.class) {
        value = Integer.parseInt(s);
      }
    } else if (value instanceof java.sql.Date) {
      if (targetClass == LocalDate.class) {
        value = ((java.sql.Date) value).toLocalDate();
      }
    } else if (value instanceof Long) {
      if (targetClass == Money.class) {
        value = Money.fromLong((Long) value);
      } else if (targetClass == Instant.class) {
        value = Instant.ofEpochMilli((Long) value);
      }
    } else if (value instanceof Integer) {
      if (targetClass == Money.class) {
        value = Money.fromLong((Integer) value);
      } else if (targetClass == Long.class) {
        value = ((Integer) value).longValue();
      } else if (targetClass == String.class) {
        value = value.toString();
      } else if (targetClass == boolean.class || targetClass == Boolean.class) {
        value = ((Integer) value) != 0;
      }
    } else if (value instanceof Number) {
      if (targetClass == Money.class) {
        value = Money.parse(value.toString());
      } else if (targetClass == int.class || targetClass == Integer.class) {
        value = Integer.parseInt(value.toString());
      }
    } else if (value instanceof Enum) {
      if (targetClass == String.class) {
        value = ((Enum) value).name();
      }
    } else if (value instanceof Json) {
      if (targetClass == String.class) {
        value = value.toString();
      }
    }

    if (value != null && !targetClass.isPrimitive() && !targetClass.isAssignableFrom(value.getClass())) {
      Function<Object, Object> converter = converters.get(value.getClass(), targetClass);
      if (converter == null) {
        throw new IllegalStateException(
            "Trying to convert " + value.getClass() + " to incompatible type: " + targetClass.getSimpleName());
      }
      value = converter.apply(value);
    }

    if (wrappedClass == Optional.class) {
      if (!(value instanceof Optional)) {
        value = Optional.ofNullable(value);
      }
    } else if (wrappedClass == XOptional.class) {
      if (!(value instanceof XOptional)) {
        value = XOptional.ofNullable(value);
      }
    }

    return (T) value;
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
      Constructor<T> constructor = c.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
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
      if (modifiersField != null) {
        modifiersField.setInt(ret, ret.getModifiers() & ~Modifier.FINAL);
      }
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

  public static XList<Field> getNonStaticFields(Class<?> c) {
    return getFields(c).filter(field -> !Modifier.isStatic(field.getModifiers()));
  }

  /**
   * Gets all non-static fields (including inherited fields).
   */
  public static XList<Field> getAllFields(Class<?> c) {
    return allFieldsCache.computeIfAbsent(c, key -> {
      XList<Field> ret = XList.create();
      getClassHierarchy(c).reverse().forEach(cc -> ret.addAll(getNonStaticFields(cc)));
      return ret;
    });
  }

  public static XList<Field> getPublicNonStaticFields(Class<?> c) {
    return getFields(c).filter(field -> !Modifier.isStatic(field.getModifiers())
        && Modifier.isPublic(field.getModifiers()));
  }

  public static XList<Method> getMethods(Class<?> c) {
    return XList.of(c.getDeclaredMethods());
  }

  public static XList<Method> getMethods(Class<?> c, String methodName) {
    return XList.of(c.getMethods()).filter(m -> m.getName().equals(methodName));
  }

  public static Method getMethod(Class<?> c, String methodName) {
    Method ret = methodCache.computeIfAbsent(c.getName() + methodName, s -> {
      Method varargs = null;
      for (Method m : c.getDeclaredMethods()) {
        if (m.getName().equals(methodName)) {
          if (m.isVarArgs()) {
            varargs = m;
          }
          if (m.getParameterCount() == 0) {
            m.setAccessible(true);
            return m;
          }
        }
      }
      for (Method m : c.getMethods()) {
        if (m.getName().equals(methodName)) {
          if (m.getParameterCount() == 0) {
            m.setAccessible(true);
            return m;
          }
        }
      }
      if (varargs != null) {
        varargs.setAccessible(true);
        return varargs;
      }

      return NULL_METHOD;
    });

    if (ret == NULL_METHOD) {
      throw new RuntimeException("Method not found: " + c.getSimpleName() + "." + methodName);
    }

    return ret;
  }

  private static Method getDeclaredMethodByName(Class<?> c, String methodName) {
    for (Method m : c.getDeclaredMethods()) {
      if (m.getName().equals(methodName)) {
        return m;
      }
    }
    return null;
  }

  /**
   * Returns true if the given class overrides a parent class's implementation of the given method.
   */
  public static boolean hasOverride(Class<?> c, String methodName) {
   return hasOverrideCache.computeIfAbsent(c.getName() + methodName, key -> {
     return getDeclaredMethodByName(c, methodName) != null;
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
   * public abstract class AbstractDB&lt;T&gt;<br>
   * public class UserDB extends AbstractDB&lt;User&gt; <br>
   * <br>
   * If this method is given UserDB.class as input, it will return User.class
   */
  public static Class<?> getGenericClass(Class<?> c) {
    return getGenericClass(c, 0);
  }

  /**
   * TestDB extends AbstractDB<XOptional<String>>
   * 
   * index 0 = XOptional<br>
   * index 1 = String
   */
  public static Class<?> getGenericClass(Class<?> c, int index) {
    Type t = c.getGenericSuperclass();
    for (int i = 0; i <= index; i++) {
      if (t instanceof ParameterizedType) {
        t = getTypeArgument(t);
      } else {
        return null;
      }
    }
    return toClass(t);
  }

  public static Type getTypeArgument(Type t) {
    Class<?> c = toClass(t);
    if (c.isArray()) {
      return c.getComponentType();
    } else {
      return ((ParameterizedType) t).getActualTypeArguments()[0];
    }
  }

  public static Class<?> getClassArgument(Type t) {
    Type type = getTypeArgument(t);
    return toClass(type);
  }

  @SuppressWarnings("unchecked")
  public static <I, O> void registerConverter(Class<I> inputClass, Class<O> outputClass, Function<I, O> converter) {
    converters.put(inputClass, outputClass, (Function<Object, Object>) converter);
  }

  @SuppressWarnings("unchecked")
  public static <T> XList<Constructor<T>> getConstructors(Class<T> c) {
    return XList.of((Constructor<T>[]) c.getDeclaredConstructors()).sortSelf((a, b) -> {
      return ComparisonChain.start()
          .compare(a.getParameterCount(), b.getParameterCount())
          .result();
    });
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
    try {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      String path = packageName.replace('.', '/');
      Enumeration<URL> resources = classLoader.getResources(path);
      List<URI> dirs = Lists.newArrayList();
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        dirs.add(resource.toURI());
      }
      Set<String> classes = Sets.newHashSet();
      for (URI directory : dirs) {
        classes.addAll(findClasses(directory, packageName));
      }
      XList<Class<?>> classList = XList.create();
      for (String className : classes) {
        if (className.startsWith(packageName) && !className.contains("$")) {
          classList.add(Class.forName(className));
        }
      }
      return classList;
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  private static Set<String> findClasses(URI directory, String packageName) throws Exception {
    final String scheme = directory.getScheme();

    if (scheme.equals("jar") && directory.getSchemeSpecificPart().contains("!")) {
      return findClassesInJar(directory);
    } else if (scheme.equals("file")) {
      return findClassesInFileSystemDirectory(directory, packageName);
    }

    throw new IllegalStateException(
        "cannot handle URI with scheme [" + scheme + "]" +
            "; received directory=[" + directory + "], packageName=[" + packageName + "]");
  }

  private static Set<String> findClassesInJar(URI jarDirectory) throws Exception {
    Set<String> ret = Sets.newHashSet();

    URL jar = new URL(first(jarDirectory.getSchemeSpecificPart(), "!"));
    ZipInputStream zip = new ZipInputStream(jar.openStream());
    while (true) {
      ZipEntry entry = zip.getNextEntry();
      if (entry == null) {
        break;
      }
      String name = entry.getName();
      if (name.endsWith(".class") && !name.contains("$")) {
        ret.add(name.substring(0, name.length() - 6).replace('/', '.'));
      }
    }

    return ret;
  }

  private static Set<String> findClassesInFileSystemDirectory(URI fileSystemDirectory, String packageName) {
    Set<String> ret = Sets.newHashSet();

    for (File file : new File(fileSystemDirectory).listFiles()) {
      String name = file.getName();
      if (file.isDirectory()) {
        ret.addAll(findClassesInFileSystemDirectory(file.getAbsoluteFile().toURI(), packageName + "." + name));
      } else if (name.endsWith(".class")) {
        ret.add(packageName + '.' + name.substring(0, name.length() - 6));
      }
    }

    return ret;
  }

  public static boolean isAbstract(Class<?> c) {
    return Modifier.isAbstract(c.getModifiers());
  }

  public static boolean isPublic(Field f) {
    return Modifier.isPublic(f.getModifiers());
  }

  public static boolean isTransient(Field f) {
    return Modifier.isTransient(f.getModifiers());
  }

  public static boolean isFinalSynthetic(Field f) {
    return f.isSynthetic() && Modifier.isFinal(f.getModifiers());
  }

  public static void init(Class<?> c) {
    try {
      Class.forName(c.getName());
    } catch (ClassNotFoundException e) {
      throw propagate(e);
    }
  }

  /**
   * Gets the class hierarchy starting from the given class and going up to Object.class
   */
  public static XList<Class<?>> getClassHierarchy(Class<?> c) {
    XList<Class<?>> ret = XList.create();

    do {
      ret.add(c);
      c = c.getSuperclass();
    } while (c != Object.class && c != null);

    return ret;
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> toClass(Type type) {
    if (type instanceof Class) {
      return (Class<T>) type;
    } else if (type instanceof ParameterizedType) {
      return (Class<T>) ((ParameterizedType) type).getRawType();
    } else {
      return (Class<T>) TypeToken.of(type).getRawType();
    }
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
