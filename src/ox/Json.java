package ox;

import static com.google.common.base.Preconditions.checkNotNull;
import static ox.util.Functions.map;
import java.io.Reader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

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

  private Json(JsonElement e) {
    this.e = e;
  }

  public String get(String key) {
    JsonElement e = getElement(key);
    return e == null ? null : e.getAsString();
  }

  public Integer getInt(String key) {
    String s = get(key);
    return s == null || s.isEmpty() ? null : Integer.valueOf(s);
  }

  public Long getLong(String key) {
    String s = get(key);
    return s == null || s.isEmpty() ? null : Long.valueOf(s);
  }

  public Double getDouble(String key) {
    String s = get(key);
    return s == null || s.isEmpty() ? null : Double.valueOf(s.replace(",", ""));
  }

  public Boolean getBoolean(String key) {
    String s = get(key);
    return s == null || s.isEmpty() ? null : Boolean.valueOf(s);
  }

  public Json getJson(String key) {
    JsonElement e = getElement(key);
    return e == null ? null : new Json(e);
  }

  public Object getObject(String key) {
    JsonElement e = getElement(key);
    if (e == null) {
      return null;
    }
    if (e.isJsonObject() || e.isJsonArray()) {
      return getJson(key);
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
    if (e == null) {
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
    } else if (value instanceof Number) {
      return with(key, (Number) value);
    } else if (value instanceof Boolean) {
      return with(key, ((Boolean) value).booleanValue());
    } else if(value instanceof LocalDate){
      return with(key, value.toString());
    } else if (value instanceof Json) {
      return with(key, (Json) value);
    } else if (value.getClass().isEnum()) {
      return with(key, (Enum<?>) value);
    } else if (value instanceof Iterable) {
      return with(key, Json.array((Iterable<?>) value));
    } else {
      throw new RuntimeException("Unhandled type: " + value.getClass().getSimpleName());
    }
  }

  public Json with(String key, String value) {
    if (value != null) {
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
    checkNotNull(value, "Tried to associate null with field: " + key);
    return with(key, value.toString());
  }

  public Json with(String key, Json value) {
    checkNotNull(value);
    obj().add(key, value.e);
    return this;
  }

  public Json add(Json element) {
    e.getAsJsonArray().add(element.e);
    return this;
  }

  public Json add(Boolean b) {
    e.getAsJsonArray().add(new JsonPrimitive(b));
    return this;
  }

  public Json add(String s) {
    e.getAsJsonArray().add(new JsonPrimitive(s));
    return this;
  }

  public Json add(Number n) {
    e.getAsJsonArray().add(new JsonPrimitive(n));
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
    return arr().get(index).getAsString();
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
      while (size() > 0) {
        arr().remove(0);
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

  public List<String> asStringArray() {
    return map(e.getAsJsonArray(), JsonElement::getAsString);
  }

  public List<Integer> asIntArray() {
    return map(e.getAsJsonArray(), JsonElement::getAsInt);
  }

  public List<Long> asLongArray() {
    return map(e.getAsJsonArray(), JsonElement::getAsLong);
  }

  public List<Double> asDoubleArray() {
    return map(e.getAsJsonArray(), JsonElement::getAsDouble);
  }

  public List<Json> asJsonArray() {
    return map(e.getAsJsonArray(), Json::new);
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

  public boolean isArray() {
    return e instanceof JsonArray;
  }

  public boolean isObject() {
    return e instanceof JsonObject;
  }

  @Override
  public Iterator<String> iterator() {
    if (isArray()) {
      return asStringArray().iterator();
    }

    return map(obj().entrySet(), Entry::getKey).iterator();
  }

  public Json copy() {
    return new Json(toString());
  }

  public static Json object() {
    return new Json();
  }

  public static Json array() {
    return new Json(new JsonArray());
  }

  public static Json array(Iterable<?> data) {
    Json ret = array();
    for (Object o : data) {
      if (o instanceof String) {
        ret.add((String) o);
      } else if (o instanceof Number) {
        ret.add((Number) o);
      } else if (o instanceof Json) {
        ret.add((Json) o);
      } else {
        ret.add(o.toString());
      }
    }
    return ret;
  }

  public static <T> Json array(Collection<T> data, Function<T, ?> mapper) {
    return array(map(data, mapper));
  }

  public static Json array(Object... data) {
    return array(Arrays.asList(data));
  }

}