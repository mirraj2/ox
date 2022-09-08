package ox;

import static ox.util.Functions.map;
import static ox.util.Utils.isNullOrEmpty;
import static ox.util.Utils.parseEnum;

import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import ox.x.XList;

public class Json implements Iterable<String> {

  private static final JsonParser parser = new JsonParser();

  private final JsonElement e;

  private Json() {
    this.e = new JsonObject();
  }

  public Json(byte[] data) {
    this(new String(data, Charsets.UTF_8));
  }

  public Json(String data) {
    this(parser.parse(data));
  }

  public Json(Reader reader) {
    this(parser.parse(reader));
  }

  public Json(JsonElement e) {
    this.e = e;
  }

  public String get(String key) {
    JsonElement e = getElement(key);
    return e == null || e.isJsonNull() ? null : e.getAsString();
  }

  public Integer getInt(String key) {
    String s = get(key);
    if (isNullOrEmpty(s)) {
      return null;
    } else {
      try {
        return Integer.valueOf(s);
      } catch (NumberFormatException e) {
        throw new NumberFormatException(String.format("Expected %s to be an integer, but was '%s'", key, s));
      }
    }
  }

  public Long getLong(String key) {
    String s = get(key);
    return isNullOrEmpty(s) ? null : Long.valueOf(s);
  }

  public Double getDouble(String key) {
    String s = get(key);
    if (isNullOrEmpty(s)) {
      return null;
    } else {
      try {
        return Double.valueOf(s.replace(",", ""));
      } catch (NumberFormatException e) {
        throw new NumberFormatException(String.format("Expected %s to be a double, but was '%s'", key, s));
      }
    }
  }

  public Boolean getBoolean(String key) {
    JsonElement e = getElement(key);
    if (e == null || e.isJsonNull()) {
      return null;
    }
    return e.getAsBoolean();
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    Boolean b = getBoolean(key);
    return b == null ? defaultValue : b;
  }

  public <T extends Enum<T>> T getEnum(String key, Class<T> enumType) {
    String s = get(key);
    return isNullOrEmpty(s) ? null : parseEnum(s, enumType);
  }

  public LocalDate getDate(String key) {
    String s = get(key);
    return isNullOrEmpty(s) ? null : LocalDate.parse(s);
  }

  public LocalTime getTime(String key) {
    String s = get(key);
    return isNullOrEmpty(s) ? null : LocalTime.parse(s);
  }

  public Money getMoney(String key) {
    String s = get(key);
    return isNullOrEmpty(s) ? null : Money.parse(s);
  }

  public Percent getPercent(String key) {
    String s = get(key);
    return isNullOrEmpty(s) ? null : Percent.parse(s);
  }

  public Json getJson(String key) {
    JsonElement e = getElement(key);
    return e == null ? null : new Json(e);
  }

  public Object getObject(String key) {
    return toObject(getElement(key));
  }

  private Object toObject(JsonElement e) {
    if (e == null) {
      return null;
    }
    if (e.isJsonObject() || e.isJsonArray()) {
      return new Json(e);
    } else if (e.isJsonPrimitive()) {
      JsonPrimitive jp = e.getAsJsonPrimitive();
      if (jp.isNumber()) {
        return jp.getAsNumber();
      } else if (jp.isBoolean()) {
        return jp.getAsBoolean();
      } else if (jp.isString()) {
        return jp.getAsString();
      } else {
        throw new IllegalStateException(this + "");
      }
    } else {
      throw new IllegalStateException(this + "");
    }
  }

  private JsonElement getElement(String key) {
    return obj().get(key);
  }

  public boolean has(String key) {
    JsonElement e = getElement(key);
    if (e == null || e instanceof JsonNull) {
      return false;
    }
    if (e.isJsonPrimitive()) {
      return !e.getAsString().isEmpty();
    }
    return true;
  }

  /**
   * Unlike 'has', this will return true if there is a key with an empty value.
   */
  public boolean hasKey(String key) {
    JsonElement e = getElement(key);
    return e != null;
  }

  public Json with(String key, Object value) {
    if (value == null) {
      return this;
    }
    if (value instanceof String) {
      return with(key, (String) value);
    } else if (value instanceof Percent) {
      return with(key, ((Percent) value).formatWithDecimals());
    } else if (value instanceof Number) {
      return with(key, (Number) value);
    } else if (value instanceof Boolean) {
      return with(key, ((Boolean) value).booleanValue());
    } else if (value instanceof Json) {
      return with(key, (Json) value);
    } else if (value.getClass().isEnum()) {
      return with(key, (Enum<?>) value);
    } else if (value instanceof Iterable) {
      return with(key, Json.array((Iterable<?>) value));
    } else {
      return with(key, value.toString());
    }
  }

  public Json with(String key, String value) {
    if (value == null) {
      obj().remove(key);
    } else {
      obj().addProperty(key, value);
    }
    return this;
  }

  public Json with(String key, Number value) {
    if (value != null) {
      obj().addProperty(key, value);
    }
    return this;
  }

  public Json with(String key, boolean value) {
    obj().addProperty(key, value);
    return this;
  }

  public Json with(String key, Enum<?> value) {
    if (value != null) {
      with(key, value.toString());
    }
    return this;
  }

  public Json with(String key, Json value) {
    if (value != null) {
      obj().add(key, value.e);
    }
    return this;
  }

  public Json add(Json element) {
    arr().add(element.e);
    return this;
  }

  public Json addAll(Iterable<Json> elements) {
    JsonArray array = arr();
    for (Json e : elements) {
      array.add(e.e);
    }
    return this;
  }

  public Json addNull() {
    arr().add(JsonNull.INSTANCE);
    return this;
  }

  public Json add(Object o) {
    if (o instanceof Json) {
      return add((Json) o);
    } else if (o instanceof Boolean) {
      return add((Boolean) o);
    } else if (o instanceof String) {
      return add((String) o);
    } else if (o instanceof Number) {
      return add((Number) o);
    } else {
      if (o == null) {
        arr().add((JsonElement) null);
        return this;
      } else {
        return add(o.toString());
      }
    }
  }

  public Json add(Boolean b) {
    arr().add(new JsonPrimitive(b));
    return this;
  }

  public Json add(String s) {
    arr().add(new JsonPrimitive(s));
    return this;
  }

  public Json add(Number n) {
    arr().add(new JsonPrimitive(n));
    return this;
  }

  public Json set(int index, String s) {
    arr().set(index, new JsonPrimitive(s));
    return this;
  }

  public Json remove(String... keys) {
    for (String key : keys) {
      remove(key);
    }
    return this;
  }

  public Json remove(String s) {
    if (isArray()) {
      arr().remove(new JsonPrimitive(s));
    } else {
      obj().remove(s);
    }
    return this;
  }

  public Json getJson(int index) {
    return new Json(arr().get(index));
  }

  public int getInt(int index) {
    return arr().get(index).getAsInt();
  }

  public String get(int index) {
    JsonElement e = arr().get(index);
    return e.isJsonNull() ? null : e.getAsString();
  }

  public Json remove(int index) {
    arr().remove(index);
    return this;
  }

  public int size() {
    if (isArray()) {
      return arr().size();
    } else {
      return Iterables.size(this);
    }
  }

  public Json clear() {
    if (isArray()) {
      JsonArray array = arr();
      for (int i = array.size() - 1; i >= 0; i--) {
        array.remove(i);
      }
    } else {
      for (String key : this) {
        obj().remove(key);
      }
    }
    return this;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public JsonObject asJsonObject() {
    return obj();
  }

  public byte[] asByteArray() {
    return e.toString().getBytes(Charsets.UTF_8);
  }

  public XList<String> asStringArray() {
    return map(arr(), e -> e.isJsonNull() ? null : e.getAsString());
  }

  public XList<Integer> asIntArray() {
    return map(arr(), JsonElement::getAsInt);
  }

  public XList<Long> asLongArray() {
    return map(arr(), JsonElement::getAsLong);
  }

  public XList<Float> asFloatArray() {
    return map(arr(), j -> j.isJsonNull() ? null : j.getAsFloat());
  }

  public XList<Double> asDoubleArray() {
    return map(arr(), j -> j.isJsonNull() ? null : j.getAsDouble());
  }

  public XList<Json> asJsonArray() {
    return map(arr(), Json::new);
  }

  private JsonObject obj() {
    return e.getAsJsonObject();
  }

  private JsonArray arr() {
    return e.getAsJsonArray();
  }

  @Override
  public String toString() {
    if (e.isJsonPrimitive()) {
      JsonPrimitive p = (JsonPrimitive) e;
      if (p.isString()) {
        return p.getAsString();
      }
    }
    return e.toString();
  }

  public String prettyPrint() {
    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    return gson.toJson(e);
  }

  public Json log() {
    Log.debug(prettyPrint());
    return this;
  }

  public boolean isArray() {
    return e instanceof JsonArray;
  }

  public boolean isObject() {
    return e instanceof JsonObject;
  }

  public boolean isNull() {
    return e instanceof JsonNull;
  }

  @Override
  public Iterator<String> iterator() {
    if (isArray()) {
      return Iterators.transform(arr().iterator(), JsonElement::getAsString);
    } else {
      return obj().keySet().iterator();
    }
  }

  public Json appendTo(Json object, String key) {
    object.with(key, this);
    return this;
  }

  public Json rename(String oldKeyName, String newKeyName) {
    JsonElement e = obj().remove(oldKeyName);
    obj().add(newKeyName, e);
    return this;
  }

  public Json forEach(BiConsumer<String, Object> callback) {
    obj().entrySet().forEach(entry -> callback.accept(entry.getKey(), toObject(entry.getValue())));
    return this;
  }

  public Json copy() {
    return new Json(toString());
  }

  /**
   * This will merge all fields from the given object into this object (except for fields already contained in this
   * object).
   */
  public Json merge(Json object) {
    return merge(object, false);
  }

  /**
   * @param override If override is on, values in the given object will override values
   */
  public Json merge(Json object, boolean override) {
    for (String key : object) {
      if (override || !this.hasKey(key)) {
        this.with(key, object.getObject(key));
      }
    }
    return this;
  }

  @Override
  public int hashCode() {
    return e.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Json)) {
      return false;
    }
    return this.e.equals(((Json) o).e);
  }

  public static Json object() {
    return new Json();
  }

  public static Json array() {
    return new Json(new JsonArray());
  }

  public static Json array(Iterable<?> data) {
    Json ret = array();
    if (data instanceof Json) {
      ret.add((Json) data);
      return ret;
    }
    for (Object o : data) {
      if (o instanceof String) {
        ret.add((String) o);
      } else if (o instanceof Number) {
        ret.add((Number) o);
      } else if (o instanceof Json) {
        ret.add((Json) o);
      } else if (o == null) {
        ret.e.getAsJsonArray().add((String) null);
      } else {
        ret.add(o.toString());
      }
    }
    return ret;
  }

  public static <T> Json array(Iterable<T> data, Function<T, ?> mapper) {
    return array(map(data, mapper));
  }

  public static <T> Json array(T[] data, Function<T, ?> mapper) {
    return array(map(data, mapper));
  }

  public static Json array(double[] data) {
    Json ret = Json.array();
    if (data != null) {
      for (double d : data) {
        ret.add(d);
      }
    }
    return ret;
  }

  public static Json array(long[] data) {
    Json ret = Json.array();
    if (data != null) {
      for (long n : data) {
        ret.add(n);
      }
    }
    return ret;
  }

  public static Json array(int[] data) {
    Json ret = Json.array();
    if (data != null) {
      for (int n : data) {
        ret.add(n);
      }
    }
    return ret;
  }

  public static Json array(Object... data) {
    return array(Arrays.asList(data));
  }

}
