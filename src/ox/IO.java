package ox;

import static com.google.common.base.Preconditions.checkNotNull;
import static ox.util.Utils.propagate;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import ox.util.Images;

public class IO {

  public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_0) AppleWebKit/537.36"
      + " (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36";

  public static Input from(Json json) {
    return from(json.toString());
  }

  public static Input from(String s) {
    return from(s.getBytes(Charsets.UTF_8));
  }

  public static Input from(byte[] data) {
    return from(new ByteArrayInputStream(data));
  }

  public static Input from(File parent, String childName) {
    return from(new File(parent, childName));
  }

  public static Input from(File file) {
    try {
      return from(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw propagate(e);
    }
  }

  public static Input from(Class<?> loader, String name) {
    checkNotNull(name);

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
    Input ret = new Input(buffer(is));
    if (is instanceof ZipInputStream) {
      ret.keepInputAlive();
    }
    return ret;
  }

  public static Input from(RenderedImage image) {
    return new Input(image);
  }

  private static URL url(String s) {
    try {
      return new URL(s);
    } catch (MalformedURLException e) {
      throw propagate(e);
    }
  }

  public static void close(Closeable c) {
    try {
      c.close();
    } catch (IOException e) {
      throw propagate(e);
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
    private boolean zipInput, gzipInput, gzipOutput;
    private String imageFormat;
    private boolean keepOutputAlive = false, keepInputAlive = false;
    private Integer timeout = null;
    private String method;
    private boolean acceptAllCerts = false;

    private Input(Object o) {
      this.o = checkNotNull(o);

      if (o.toString().endsWith(".zip")) {
        zipInput = true;
      } else if (o.toString().endsWith(".gzip") || o.toString().endsWith(".gz")) {
        gzipInput = true;
      }
    }

    public Input keepInputAlive() {
      keepInputAlive = true;
      return this;
    }

    public Input keepOutputAlive() {
      keepOutputAlive = true;
      return this;
    }

    public Input acceptAllCerts() {
      this.acceptAllCerts = true;
      return this;
    }

    public void to(File file) {
      if (o instanceof RenderedImage) {
        if (imageFormat == null) {
          imageFormat = getImageType(file);
        }
      }
      if (file.getName().endsWith(".gzip")) {
        gzipOutput = true;
      }
      try {
        to(new FileOutputStream(file));
      } catch (FileNotFoundException e) {
        throw propagate(e);
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
          RenderedImage r = (RenderedImage) o;
          if (r instanceof BufferedImage && imageFormat.equals("jpg")) {
            r = Images.withType((BufferedImage) o, BufferedImage.TYPE_INT_RGB);
          }
          ImageIO.write(r, imageFormat, os);
        } else {
          ByteStreams.copy(asStream(), os);
        }
      } catch (IOException e) {
        throw propagate(e);
      } finally {
        finish();
      }
    }

    public void toUrl(String url) {
      try {
        to(new URL(url));
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    public void to(URL url) {
      try {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (acceptAllCerts && conn instanceof HttpsURLConnection) {
          HttpsURLConnection https = (HttpsURLConnection) conn;
          SSLContext ctx = SSLContext.getInstance("SSLv3");
          ctx.init(null, new TrustManager[] { ACCEPT_ALL }, null);
          https.setSSLSocketFactory(ctx.getSocketFactory());
        }
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        to(conn.getOutputStream());
        conn.getInputStream().close();
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    public byte[] toByteArray() {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      to(os);
      return os.toByteArray();
    }

    public void toClipboard() {
      StringSelection stringSelection = new StringSelection(toString());
      Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
      clpbrd.setContents(stringSelection, null);
    }

    @Override
    public String toString() {
      try {
        return new String(toByteArray(), Charsets.UTF_8);
      } finally {
        finish();
      }
    }

    public Json toJson() {
      String s = toString();
      try {
        return new Json(s);
      } catch (Exception e) {
        Log.error("Problem parsing json: " + s);
        throw propagate(e);
      }
    }

    public BufferedImage toImage() {
      try {
        return ImageIO.read(asStream());
      } catch (IOException e) {
        throw propagate(e);
      } finally {
        finish();
      }
    }

    public Font toFont() {
      try {
        return Font.createFont(Font.TRUETYPE_FONT, asStream());
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    public InputStream asStream() {
      try {
        InputStream ret = null;
        if (o instanceof InputStream) {
          ret = (InputStream) o;
        } else if (o instanceof URL) {
          ret = asStream((URL) o);
        }
        if (ret == null) {
          throw new RuntimeException("Don't know how to turn " + o.getClass() + " into a stream.");
        }
        if (gzipInput) {
          ret = new GZIPInputStream(ret);
        } else if (zipInput) {
          ret = new ZipInputStream(ret);
          ((ZipInputStream) ret).getNextEntry();
        }
        ret = is = buffer(ret);
        return ret;
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    private InputStream asStream(URL url) throws Exception {
      URLConnection conn = url.openConnection();
      HttpURLConnection httpConn = conn instanceof HttpURLConnection ? (HttpURLConnection) conn : null;

      if (timeout != null) {
        conn.setConnectTimeout(timeout);
      }

      if (httpConn != null && method != null) {
        httpConn.setRequestMethod(method);
      }

      conn.setRequestProperty("User-Agent", USER_AGENT);
      if (gzipInput) {
        conn.setRequestProperty("Accept-Encoding", "gzip");
      }

      if (httpConn != null) {
        int code = httpConn.getResponseCode();
        if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
            || code == HttpURLConnection.HTTP_SEE_OTHER) {
          // redirected
          String location = conn.getHeaderField("Location");
          if (location != null) {
            httpConn.disconnect();
            return asStream(new URL(location));
          }
        }
      }

      return conn.getInputStream();
    }

    public Input gzipInput() {
      gzipInput = true;
      return this;
    }

    public Input zipInput() {
      zipInput = true;
      return this;
    }

    public Input gzipOutput() {
      gzipOutput = true;
      return this;
    }

    public Input imageFormat(String format) {
      this.imageFormat = format;
      return this;
    }

    public Input httpPost() {
      this.method = "POST";
      return this;
    }

    public Input timeout(Integer timeout) {
      this.timeout = timeout;
      return this;
    }

    private void finish() {
      if (is == null) {
        if (o instanceof Closeable) {
          close((Closeable) o);
        }
      } else {
        if (!keepInputAlive) {
          close(is);
          is = null;
        }
      }
      if (os != null) {
        if (keepOutputAlive) {
          try {
            os.flush();
          } catch (IOException e) {
            throw propagate(e);
          }
        } else {
          close(os);
          os = null;
        }
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

  private static final X509TrustManager ACCEPT_ALL = new X509TrustManager() {
    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    @Override
    public void checkClientTrusted(
        java.security.cert.X509Certificate[] certs, String authType) {
    }

    @Override
    public void checkServerTrusted(
        java.security.cert.X509Certificate[] certs, String authType) {
    }
  };

}
