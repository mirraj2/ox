package ox;

import java.util.Map;

import com.google.common.collect.Maps;

public class Config {

  private static final Map<String, Config> configCache = Maps.newConcurrentMap();

  private final File configFile;
  private final Json json;

  private Config(File file) {
    this.configFile = file;

    if (configFile.exists()) {
      json = IO.from(configFile).toJson();
    } else {
      Log.debug("Creating a new config.json -> " + configFile);
      json = Json.object();
      save();
    }
  }

  public String get(String key) {
    return json.get(key);
  }

  public String get(String key, String defaultValue) {
    String ret = json.get(key);
    return ret == null ? defaultValue : ret;
  }

  public int getInt(String key, int defaultValue) {
    Integer ret = json.getInt(key);
    return ret == null ? defaultValue : ret;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    Boolean ret = json.getBoolean(key);
    return ret == null ? defaultValue : ret;
  }

  public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass) {
    return getEnum(key, enumClass, null);
  }

  public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T defaultValue) {
    T ret = json.getEnum(key, enumClass);
    return ret == null ? defaultValue : ret;
  }

  public Json json() {
    return json;
  }

  public void put(String key, String value) {
    json.with(key, value);
    save();
  }

  private void save() {
    IO.from(json.prettyPrint() + "\n").to(configFile);
  }

  public static Config load(String appName) {
    return load(File.appFolder(appName, "config.json"));
  }

  public static Config load(File file) {
    return configCache.computeIfAbsent(file.getPath(), s -> new Config(file));
  }

}
