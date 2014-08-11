package jasonlib;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import static com.google.common.base.Preconditions.checkNotNull;

public class IO {

  public static Input from(Json json) {
    return from(json.toString());
  }

  public static Input from(String s) {
    return from(s.getBytes(Charsets.UTF_8));
  }

  public static Input from(byte[] data) {
    return from(new ByteArrayInputStream(data));
  }

  public static Input from(File file) {
    try {
      return from(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw Throwables.propagate(e);
    }
  }

  public static Input from(Class<?> loader, String name) {
    URL url = loader.getResource(name);
    checkNotNull(url, "Could not find resource: " + name);
    return from(url);
  }

  public static Input fromURL(String url) {
    return from(url(url));
  }

  public static Input from(URL url) {
    return new Input(url);
  }

  public static Input from(InputStream is) {
    return new Input(buffer(is));
  }

  public static Input from(RenderedImage image) {
    return new Input(image);
  }

  private static URL url(String s) {
    try {
      return new URL(s);
    } catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    }
  }

  public static void close(Closeable c) {
    try {
      c.close();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static InputStream buffer(InputStream is) {
    if (!(is instanceof BufferedInputStream || is instanceof ByteArrayInputStream)) {
      is = new BufferedInputStream(is);
    }
    return is;
  }

  private static OutputStream buffer(OutputStream os) {
    if (!(os instanceof BufferedOutputStream || os instanceof ByteArrayOutputStream)) {
      os = new BufferedOutputStream(os);
    }
    return os;
  }

  public static class Input {

    private final Object o;
    private InputStream is;
    private OutputStream os;
    private boolean gzipInput, gzipOutput;
    private String imageFormat;

    private Input(Object o) {
      this.o = checkNotNull(o);
    }

    public void to(File file) {
      if (o instanceof RenderedImage) {
        imageFormat = getImageType(file);
      }
      try {
        to(new FileOutputStream(file));
      } catch (FileNotFoundException e) {
        throw Throwables.propagate(e);
      }
    }

    public void to(OutputStream os) {
      try {
        os = buffer(os);
        if (gzipOutput) {
          os = new GZIPOutputStream(os);
        }
        this.os = os;
        if (o instanceof RenderedImage) {
          ImageIO.write((RenderedImage) o, imageFormat, os);
        } else {
          ByteStreams.copy(asStream(), os);
        }
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } finally {
        finish();
      }
    }

    public void to(URL url) {
      try {
        to(new File(url.toURI()));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    public byte[] toByteArray() {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try {
        ByteStreams.copy(asStream(), os);
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
      return os.toByteArray();
    }

    @Override
    public String toString() {
      return new String(toByteArray(), Charsets.UTF_8);
    }

    public Json toJson() {
      InputStreamReader reader = new InputStreamReader(asStream(), Charsets.UTF_8);
      Json ret = new Json(reader);
      close(reader);
      return ret;
    }

    public BufferedImage toImage() {
      try {
        return ImageIO.read(asStream());
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } finally {
        finish();
      }
    }

    public Font toFont() {
      try {
        return Font.createFont(Font.TRUETYPE_FONT, asStream());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    public InputStream asStream() {
      try {
        InputStream ret = null;
        if (o instanceof InputStream) {
          ret = (InputStream) o;
        } else if (o instanceof URL) {
          URL url = (URL) o;
          URLConnection conn = url.openConnection();
          conn.setRequestProperty("User-Agent",
              "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 " +
                  "(KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36");
          ret = conn.getInputStream();
        }
        if (ret == null) {
          throw new RuntimeException("Don't know how to turn " + o.getClass() + " into a stream.");
        }
        if (gzipInput) {
          ret = new GZIPInputStream(ret);
        }
        ret = is = buffer(ret);
        return ret;
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    public Input gzipInput() {
      gzipInput = true;
      return this;
    }

    public Input gzipOutput() {
      gzipOutput = true;
      return this;
    }

    private void finish() {
      if (is == null) {
        if (o instanceof Closeable) {
          close((Closeable) o);
        }
      } else {
        close(is);
      }
      if (os != null) {
        close(os);
      }
    }

    private static String getImageType(File file) {
      String s = file.getPath();
      int i = s.lastIndexOf(".");
      if (i == -1) {
        return "png";
      }
      return s.substring(i + 1);
    }
  }

}
