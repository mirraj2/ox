package ox;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.getExtension;
import static ox.util.Utils.propagate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import com.google.common.base.Predicate;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import ox.x.XList;

public class File {

  public static boolean ignoreDSStore = true;

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

  public String getNameWithoutExtension() {
    String extension = extension();
    if (extension.isEmpty()) {
      return getName();
    }
    String name = getName();
    return name.substring(0, name.length() - extension.length() - 1);
  }

  public String getPath() {
    return file.getPath();
  }

  public String quotePath() {
    return "\"" + getPath() + "\"";
  }

  public boolean exists() {
    return file.exists();
  }

  public File rename(File toName) {
    file.renameTo(toName.file);
    return this;
  }

  public File copyTo(File destination) {
    checkState(!this.equals(destination));
    try {
      java.nio.file.Files.copy(this.file.toPath(), destination.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw propagate(e);
    }
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

  public XList<File> children() {
    if (!file.isDirectory()) {
      return XList.empty();
    }
    java.io.File[] files = file.listFiles();
    checkState(files != null, file + " does not exist.");
    XList<File> ret = XList.createWithCapacity(files.length);
    for (java.io.File file : files) {
      if (ignoreDSStore && file.getName().equals(".DS_Store")) {
        continue;
      }
      ret.add(new File(file));
    }
    return ret;
  }

  public void walkTree(Consumer<File> callback) {
    callback.accept(this);
    children().forEach(child -> child.walkTree(callback));
  }

  public XList<File> filterTree(Predicate<File> filter) {
    XList<File> ret = XList.create();
    walkTree(file -> {
      if (filter.test(file)) {
        ret.add(file);
      }
    });
    return ret;
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

  public File deleteRecursive() {
    if (file.isDirectory()) {
      children().forEach(File::deleteRecursive);
    }
    file.delete();
    return this;
  }

/**
 * Copies the contents of this file into the given directory. Recursively copies all subfolders and files.
 */
  public void copyContentsInto(File targetDir) {
    for (File child : children()) {
      if (child.isDirectory()) {
        child.copyContentsInto(targetDir.child(child.getName()));
      } else {
        child.copyTo(targetDir.child(child.getName()));
      }
    }
  }

  /**
   * Deletes all children of this file
   */
  public File empty() {
    children().forEach(File::deleteRecursive);
    return this;
  }

  public File deleteOnExit() {
    file.deleteOnExit();
    return this;
  }

  public long length() {
    return file.length();
  }

  public long getLastModifiedTimestamp() {
    return file.lastModified();
  }

  public boolean isDirectory() {
    return file.isDirectory();
  }

  public InputStream inputStream() {
    return IO.from(file).asStream();
  }

  public OutputStream outputStream() {
    return outputStream(false);
  }

  public OutputStream outputStream(boolean append) {
    try {
      return new FileOutputStream(file, append);
    } catch (FileNotFoundException e) {
      throw propagate(e);
    }
  }

  public MappedByteBuffer toByteBuffer() {
    try {
      FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
      return channel.map(MapMode.READ_ONLY, 0, channel.size());
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  public File log() {
    Log.debug(IO.from(file).toString());
    return this;
  }

  @Override
  public String toString() {
    return getPath();
  }

  public void openUI() {
    OS.open(file);
  }

  public String getRelativePath(File parentFile) {
    return getPath().substring(parentFile.getPath().length() + 1);
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

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof File)) {
      return false;
    }
    return this.file.equals(((File) obj).file);
  }

  /**
   * Determines (from data within the file) what MIME type this file has e.g. image/gif
   */
  public String getContentType() {
    try {
      return file.toURI().toURL().openConnection().getContentType();
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  /**
   * If this is a directory, gets the size of all files in this directory, otherwise gets the size of this file.
   */
  public long totalSize() {
    if (this.isDirectory()) {
      long ret = 0;
      for (File child : children()) {
        ret += child.totalSize();
      }
      return ret;
    } else {
      return this.length();
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

  /**
   * This method will automatically delete the file after the callback is run.
   */
  public static void temp(Consumer<File> callback) {
    File file = temp();
    try {
      callback.accept(file);
    } finally {
      file.delete();
    }
  }

  public static void tempFolder(Consumer<File> callback) {
    File file = new File(Files.createTempDir());
    try {
      callback.accept(file);
    } finally {
      file.deleteRecursive();
    }
  }

  /**
   * This method will automatically delete the file after the callback is run.
   */
  public static void temp(String fileName, Consumer<File> callback) {
    File file = temp(fileName);
    try {
      callback.accept(file);
    } finally {
      file.delete();
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

  public static File fromURL(URL url) {
    try {
      return of(new java.io.File(url.toURI()));
    } catch (URISyntaxException e) {
      throw propagate(e);
    }
  }

  public static File of(java.io.File file) {
    return new File(file);
  }

  public static File ofPath(String path) {
    return new File(path);
  }

}
