package ox;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Functions.map;
import static ox.util.Utils.getExtension;

import java.io.InputStream;
import java.util.List;

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
    return map(file.listFiles(), File::new);
  }

  public File mkdirs() {
    file.mkdirs();
    return this;
  }

  public File delete() {
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

  public InputStream stream() {
    return IO.from(file).asStream();
  }

  @Override
  public String toString() {
    return getPath();
  }

  public static File desktop() {
    return new File(OS.getDesktop());
  }

  public static File desktop(String child) {
    return new File(OS.getDesktop()).child(child);
  }

  public static File temp(String child) {
    return new File(new java.io.File(OS.getTemporaryFolder(), child));
  }

  public static File of(java.io.File file) {
    return new File(file);
  }

}
