package ox;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Functions.map;
import static ox.util.Utils.getExtension;
import static ox.util.Utils.propagate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class File {

  public final java.io.File file;

  private File(String path) {
    this(new java.io.File(path));
  }

  private File(java.io.File file) {
    this.file = file;
  }

  public String getName() {
    return file.getName();
  }

  public String getPath() {
    return file.getPath();
  }

  public boolean exists() {
    return file.exists();
  }

  public File rename(File toName) {
    file.renameTo(toName.file);
    return this;
  }

  public File parent() {
    return new File(file.getParentFile());
  }

  public File child(String name) {
    return new File(new java.io.File(file, name));
  }

  public File sibling(String name) {
    return new File(new java.io.File(file.getParentFile(), name));
  }

  public File withExtension(String extension) {
    String s = getName();
    int i = s.lastIndexOf('.');
    if (i == -1) {
      return new File(file.getPath() + extension);
    }
    return sibling(s.substring(0, i + 1) + extension);
  }

  public String extension() {
    return getExtension(getPath());
  }

  public List<File> children() {
    if (!file.isDirectory()) {
      return ImmutableList.of();
    }
    java.io.File[] files = file.listFiles();
    checkState(files != null, file + " does not exist.");
    return map(files, File::new);
  }

  public void walkTree(Consumer<File> callback) {
    callback.accept(this);
    children().forEach(child -> child.walkTree(callback));
  }

  public File mkdirs() {
    file.mkdirs();
    return this;
  }

  public File touch() {
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw propagate(e);
    }
    return this;
  }

  public File delete() {
    if (!file.exists()) {
      return this;
    }
    checkState(file.delete());
    return this;
  }

  public File deleteOnExit() {
    file.deleteOnExit();
    return this;
  }

  public long length() {
    return file.length();
  }

  public boolean isDirectory() {
    return file.isDirectory();
  }

  public InputStream stream() {
    return IO.from(file).asStream();
  }

  @Override
  public String toString() {
    return getPath();
  }

  public void openUI() {
    OS.open(file);
  }

  public void streamLines(Predicate<String> callback) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      while (true) {
        String line = br.readLine();
        if (line == null || !callback.apply(line)) {
          break;
        }
      }
      IO.close(br);
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  public String hash() {
    try {
      return Files.asByteSource(file).hash(Hashing.sha256()).toString();
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  public static File desktop() {
    return new File(OS.getDesktop());
  }

  public static File desktop(String child) {
    return desktop().child(child);
  }

  public static File downloads() {
    return new File(OS.getDownloadsFolder());
  }

  public static File downloads(String child) {
    return downloads().child(child);
  }

  public static File home() {
    return new File(OS.getHomeFolder());
  }

  public static File home(String child) {
    return home().child(child);
  }

  public static File temp() {
    try {
      return of(java.nio.file.Files.createTempFile(null, null).toFile());
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  public static File temp(String child) {
    return new File(new java.io.File(OS.getTemporaryFolder(), child));
  }

  public static File appFolder(String appName) {
    return new File(OS.getAppFolder(appName));
  }

  public static File appFolder(String appName, String child) {
    return appFolder(appName).child(child);
  }

  public static File of(java.io.File file) {
    return new File(file);
  }

  public static File ofPath(String path) {
    return new File(path);
  }

}
