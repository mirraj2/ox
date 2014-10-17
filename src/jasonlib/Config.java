package jasonlib;

import java.io.File;
import com.google.common.base.Strings;
import static com.google.common.base.Preconditions.checkArgument;

public class Config {

  private final File configFile;
  private final Json json;

  private Config(File dir) {
    configFile = new File(dir, "config.json");

    if (configFile.exists()) {
      json = IO.from(configFile).toJson();
    } else {
      Log.debug("Creating a new config.json -> " + configFile);
      json = Json.object();
      save();
    }
  }

  private Config(String appName) {
    checkArgument(!Strings.isNullOrEmpty(appName));

    configFile = new File(OS.getLocalAppFolder(appName), "config.json");

    if (configFile.exists()) {
      json = IO.from(configFile).toJson();
    } else {
      Log.debug("Creating a new config.json");
      json = Json.object();
      save();
    }
  }

  public String get(String key) {
    return json.getOrNull(key);
  }

  public void put(String key, String value) {
    json.with(key, value);
    save();
  }

  private void save() {
    IO.from(json).to(configFile);
  }

  public static Config load(File dir) {
    return new Config(dir);
  }

  public static Config load(String appName) {
    checkArgument(!Strings.isNullOrEmpty(appName));

    return new Config(OS.getLocalAppFolder(appName));
  }

}
