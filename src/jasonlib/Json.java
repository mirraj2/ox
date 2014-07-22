package jasonlib;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class Json implements Iterable<String> {

  private static final JsonParser parser = new JsonParser();

  private final JsonElement e;

  public Json() {
    this.e = new JsonObject();
  }

  public Json(byte[] data) {
    this(parser.parse(new String(data, Charsets.UTF_8)));
  }

  public Json(JsonElement e) {
    this.e = e;
  }

  public String get(String key) {
    return getElement(key).getAsString();
  }

  public int getInt(String key) {
    return getElement(key).getAsInt();
  }

  public double getDouble(String key) {
    return getElement(key).getAsDouble();
  }

  public boolean getBoolean(String key) {
    return getElement(key).getAsBoolean();
  }

  public Json getJson(String key) {
    return new Json(getElement(key));
  }

  private JsonElement getElement(String key) {
    JsonElement ret = obj().get(key);
    checkArgument(ret != null, "Field not found: " + key);
    return ret;
  }

  public String getOrNull(String key) {
    if (!has(key)) {
      return null;
    }
    return get(key);
  }

  public boolean has(String key) {
    return obj().has(key);
  }

  public Json with(String key, String value) {
    checkNotNull(value, "Tried to associate null with field: " + key);
    obj().addProperty(key, value);
    return this;
  }

  public Json with(String key, int value) {
    obj().addProperty(key, value);
    return this;
  }

  public Json with(String key, double value) {
    obj().addProperty(key, value);
    return this;
  }

  public Json with(String key, boolean value) {
    obj().addProperty(key, value);
    return this;
  }

  public Json with(String key, Json value) {
    checkNotNull(value);
    obj().add(key, value.e);
    return this;
  }

  public Json with(String key, Iterable<?> items) {
    JsonArray array = new JsonArray();
    for (Object e : items) {
      array.add(new JsonPrimitive(e.toString()));
    }
    obj().add(key, array);
    return this;
  }

  public Json add(Json element) {
    array().add(element.e);
    return this;
  }

  public JsonObject asJsonObject() {
    return obj();
  }

  public byte[] asByteArray() {
    return e.toString().getBytes(Charsets.UTF_8);
  }

  public List<String> asStringArray() {
    List<String> ret = Lists.newArrayListWithCapacity(e.getAsJsonArray().size());
    for (JsonElement item : e.getAsJsonArray()) {
      ret.add(item.getAsString());
    }
    return ret;
  }

  public List<Json> asJsonArray() {
    List<Json> ret = Lists.newArrayListWithCapacity(e.getAsJsonArray().size());
    for (JsonElement item : e.getAsJsonArray()) {
      ret.add(new Json(item));
    }
    return ret;
  }

  public Map<String, String> asStringMap() {
    Map<String, String> ret = Maps.newHashMap();
    for (String key : this) {
      ret.put(key, get(key));
    }
    return ret;
  }

  private JsonObject obj() {
    return e.getAsJsonObject();
  }

  private JsonArray array() {
    return e.getAsJsonArray();
  }

  @Override
  public String toString() {
    return e.toString();
  }

  @Override
  public Iterator<String> iterator() {
    Set<Entry<String, JsonElement>> entries = obj().entrySet();
    List<String> ret = Lists.newArrayListWithCapacity(entries.size());
    for (Entry<String, JsonElement> e : entries) {
      ret.add(e.getKey());
    }
    return ret.iterator();
  }

}
