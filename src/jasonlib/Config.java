package jasonlib;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;

public class Config {

  private static final Logger logger = Logger.getLogger(Config.class);

  private final File configFile;
  private final Json json;

  private Config(File dir) {
    configFile = new File(dir, "config.json");

    if (configFile.exists()) {
      json = IO.from(configFile).toJson();
    } else {
      logger.debug("Creating a new config.json -> " + configFile);
      json = new Json();
      save();
    }
  }

  private Config(String appName) {
    checkArgument(!Strings.isNullOrEmpty(appName));

    configFile = new File(OS.getLocalAppFolder(appName), "config.json");

    if (configFile.exists()) {
      json = IO.from(configFile).toJson();
    } else {
      logger.debug("Creating a new config.json");
      json = new Json();
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
