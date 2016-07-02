/*
 * Copyright (c) 2014 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package ox;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.Proxy.Type.HTTP;
import static ox.util.Utils.urlEncode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

//taken from https://github.com/kevinsawicki/http-request
public class HttpRequest {

  public static final String CHARSET_UTF8 = "UTF-8";

  public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

  public static final String CONTENT_TYPE_JSON = "application/json";

  public static final String ENCODING_GZIP = "gzip";

  public static final String HEADER_ACCEPT = "Accept";

  public static final String HEADER_ACCEPT_CHARSET = "Accept-Charset";

  public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

  public static final String HEADER_AUTHORIZATION = "Authorization";

  public static final String HEADER_CACHE_CONTROL = "Cache-Control";

  public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

  public static final String HEADER_CONTENT_LENGTH = "Content-Length";

  public static final String HEADER_CONTENT_TYPE = "Content-Type";

  public static final String HEADER_DATE = "Date";

  public static final String HEADER_ETAG = "ETag";

  public static final String HEADER_EXPIRES = "Expires";

  public static final String HEADER_IF_NONE_MATCH = "If-None-Match";

  public static final String HEADER_LAST_MODIFIED = "Last-Modified";

  public static final String HEADER_LOCATION = "Location";

  public static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization";

  public static final String HEADER_REFERER = "Referer";

  public static final String HEADER_SERVER = "Server";

  public static final String HEADER_USER_AGENT = "User-Agent";

  public static final String METHOD_DELETE = "DELETE";

  public static final String METHOD_GET = "GET";

  public static final String METHOD_HEAD = "HEAD";

  public static final String METHOD_OPTIONS = "OPTIONS";

  public static final String METHOD_POST = "POST";

  public static final String METHOD_PUT = "PUT";

  public static final String METHOD_TRACE = "TRACE";

  public static final String PARAM_CHARSET = "charset";

  private static final String BOUNDARY = "00content0boundary00";

  private static final String CONTENT_TYPE_MULTIPART = "multipart/form-data; boundary="
      + BOUNDARY;

  private static final String CRLF = "\r\n";

  private static final String[] EMPTY_STRINGS = new String[0];

  private static SSLSocketFactory TRUSTED_FACTORY;

  private static HostnameVerifier TRUSTED_VERIFIER;

  private static String getValidCharset(final String charset) {
    if (charset != null && charset.length() > 0) {
      return charset;
    } else {
      return CHARSET_UTF8;
    }
  }

  private static SSLSocketFactory getTrustedFactory()
      throws HttpRequestException {
    if (TRUSTED_FACTORY == null) {
      final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
          // Intentionally left blank
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
          // Intentionally left blank
        }
      } };
      try {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAllCerts, new SecureRandom());
        TRUSTED_FACTORY = context.getSocketFactory();
      } catch (GeneralSecurityException e) {
        IOException ioException = new IOException(
            "Security exception configuring SSL context");
        ioException.initCause(e);
        throw new HttpRequestException(ioException);
      }
    }

    return TRUSTED_FACTORY;
  }

  private static HostnameVerifier getTrustedVerifier() {
    if (TRUSTED_VERIFIER == null) {
      TRUSTED_VERIFIER = new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };
    }

    return TRUSTED_VERIFIER;
  }

  private static StringBuilder addPathSeparator(final String baseUrl,
      final StringBuilder result) {
    // Add trailing slash if the base URL doesn't have any path segments.
    //
    // The following test is checking for the last slash not being part of
    // the protocol to host separator: '://'.
    if (baseUrl.indexOf(':') + 2 == baseUrl.lastIndexOf('/')) {
      result.append('/');
    }
    return result;
  }

  private static StringBuilder addParamPrefix(final String baseUrl,
      final StringBuilder result) {
    // Add '?' if missing and add '&' if params already exist in base url
    final int queryStart = baseUrl.indexOf('?');
    final int lastChar = result.length() - 1;
    if (queryStart == -1) {
      result.append('?');
    } else if (queryStart < lastChar && baseUrl.charAt(lastChar) != '&') {
      result.append('&');
    }
    return result;
  }

  private static StringBuilder addParam(final Object key, Object value,
      final StringBuilder result) {
    if (value != null && value.getClass().isArray()) {
      value = arrayToList(value);
    }

    if (value instanceof Iterable<?>) {
      Iterator<?> iterator = ((Iterable<?>) value).iterator();
      while (iterator.hasNext()) {
        result.append(key);
        result.append("[]=");
        Object element = iterator.next();
        if (element != null) {
          result.append(element);
        }
        if (iterator.hasNext()) {
          result.append("&");
        }
      }
    } else {
      result.append(key);
      result.append("=");
      if (value != null) {
        result.append(value);
      }
    }

    return result;
  }

  public interface ConnectionFactory {

    HttpURLConnection create(URL url) throws IOException;

    HttpURLConnection create(URL url, Proxy proxy) throws IOException;

    ConnectionFactory DEFAULT = new ConnectionFactory() {
      @Override
      public HttpURLConnection create(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
      }

      @Override
      public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
        return (HttpURLConnection) url.openConnection(proxy);
      }
    };
  }

  private static ConnectionFactory CONNECTION_FACTORY = ConnectionFactory.DEFAULT;

  public static void setConnectionFactory(final ConnectionFactory connectionFactory) {
    if (connectionFactory == null) {
      CONNECTION_FACTORY = ConnectionFactory.DEFAULT;
    } else {
      CONNECTION_FACTORY = connectionFactory;
    }
  }

  public interface UploadProgress {

    void onUpload(long uploaded, long total);

    UploadProgress DEFAULT = new UploadProgress() {
      @Override
      public void onUpload(long uploaded, long total) {
      }
    };
  }

  public static class HttpRequestException extends RuntimeException {

    private static final long serialVersionUID = -1170466989781746231L;

    public HttpRequestException(final IOException cause) {
      super(cause);
    }

    @Override
    public IOException getCause() {
      return (IOException) super.getCause();
    }
  }

  protected static abstract class Operation<V> implements Callable<V> {

    protected abstract V run() throws HttpRequestException, IOException;

    protected abstract void done() throws IOException;

    @Override
    public V call() throws HttpRequestException {
      boolean thrown = false;
      try {
        return run();
      } catch (HttpRequestException e) {
        thrown = true;
        throw e;
      } catch (IOException e) {
        thrown = true;
        throw new HttpRequestException(e);
      } finally {
        try {
          done();
        } catch (IOException e) {
          if (!thrown) {
            throw new HttpRequestException(e);
          }
        }
      }
    }
  }

  protected static abstract class CloseOperation<V> extends Operation<V> {

    private final Closeable closeable;

    private final boolean ignoreCloseExceptions;

    protected CloseOperation(final Closeable closeable,
        final boolean ignoreCloseExceptions) {
      this.closeable = closeable;
      this.ignoreCloseExceptions = ignoreCloseExceptions;
    }

    @Override
    protected void done() throws IOException {
      if (closeable instanceof Flushable) {
        ((Flushable) closeable).flush();
      }
      if (ignoreCloseExceptions) {
        try {
          closeable.close();
        } catch (IOException e) {
          // Ignored
        }
      } else {
        closeable.close();
      }
    }
  }

  protected static abstract class FlushOperation<V> extends Operation<V> {

    private final Flushable flushable;

    protected FlushOperation(final Flushable flushable) {
      this.flushable = flushable;
    }

    @Override
    protected void done() throws IOException {
      flushable.flush();
    }
  }

  public static class RequestOutputStream extends BufferedOutputStream {

    private final CharsetEncoder encoder;

    public RequestOutputStream(final OutputStream stream, final String charset,
        final int bufferSize) {
      super(stream, bufferSize);

      encoder = Charset.forName(getValidCharset(charset)).newEncoder();
    }

    public RequestOutputStream write(final String value) throws IOException {
      final ByteBuffer bytes = encoder.encode(CharBuffer.wrap(value));

      super.write(bytes.array(), 0, bytes.limit());

      return this;
    }
  }

  private static List<Object> arrayToList(final Object array) {
    if (array instanceof Object[]) {
      return Arrays.asList((Object[]) array);
    }

    List<Object> result = new ArrayList<Object>();
    // Arrays of the primitive types can't be cast to array of Object, so this:
    if (array instanceof int[]) {
      for (int value : (int[]) array) {
        result.add(value);
      }
    } else if (array instanceof boolean[]) {
      for (boolean value : (boolean[]) array) {
        result.add(value);
      }
    } else if (array instanceof long[]) {
      for (long value : (long[]) array) {
        result.add(value);
      }
    } else if (array instanceof float[]) {
      for (float value : (float[]) array) {
        result.add(value);
      }
    } else if (array instanceof double[]) {
      for (double value : (double[]) array) {
        result.add(value);
      }
    } else if (array instanceof short[]) {
      for (short value : (short[]) array) {
        result.add(value);
      }
    } else if (array instanceof byte[]) {
      for (byte value : (byte[]) array) {
        result.add(value);
      }
    } else if (array instanceof char[]) {
      for (char value : (char[]) array) {
        result.add(value);
      }
    }
    return result;
  }

  public static String append(final CharSequence url, final Map<?, ?> params, boolean encode) {
    final String baseUrl = url.toString();
    if (params == null || params.isEmpty()) {
      return baseUrl;
    }

    final StringBuilder result = new StringBuilder(baseUrl);

    addPathSeparator(baseUrl, result);
    addParamPrefix(baseUrl, result);

    if (!params.isEmpty()) {
      params.forEach((k, v) -> {
        addParam(urlEncode(k.toString()), urlEncode(v.toString()), result);
        result.append("&");
      });
      result.setLength(result.length() - 1);
    }

    return result.toString();
  }

  public static String append(final CharSequence url, final Object... params) {
    final String baseUrl = url.toString();
    if (params == null || params.length == 0) {
      return baseUrl;
    }

    if (params.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Must specify an even number of parameter names/values");
    }

    final StringBuilder result = new StringBuilder(baseUrl);

    addPathSeparator(baseUrl, result);
    addParamPrefix(baseUrl, result);

    addParam(params[0], params[1], result);

    for (int i = 2; i < params.length; i += 2) {
      result.append('&');
      addParam(params[i], params[i + 1], result);
    }

    return result.toString();
  }

  public static HttpRequest get(final CharSequence url)
      throws HttpRequestException {
    return new HttpRequest(url, METHOD_GET);
  }

  public static HttpRequest get(final URL url) throws HttpRequestException {
    return new HttpRequest(url, METHOD_GET);
  }

  public static HttpRequest get(CharSequence baseUrl, Map<?, ?> params) {
    return get(baseUrl, params, true);
  }

  public static HttpRequest get(CharSequence baseUrl, Map<?, ?> params, boolean encode) {
    String url = append(baseUrl, params, encode);
    return get(url);
  }

  public static HttpRequest get(final CharSequence baseUrl,
      final boolean encode, final Object... params) {
    String url = append(baseUrl, encode, params);
    return get(url);
  }

  public static HttpRequest post(final CharSequence url)
      throws HttpRequestException {
    return new HttpRequest(url, METHOD_POST);
  }

  public static HttpRequest post(final URL url) throws HttpRequestException {
    return new HttpRequest(url, METHOD_POST);
  }

  public static HttpRequest post(String url, Map<?, ?> params) {
    return post(url, params, true);
  }

  public static HttpRequest post(String url, Map<?, ?> params, boolean encode) {
    url = append(url, params, encode);
    return post(url);
  }

  public static HttpRequest post(String baseUrl, boolean encode, Object... params) {
    String url = append(baseUrl, encode, params);
    return post(url);
  }

  public static HttpRequest put(final CharSequence url)
      throws HttpRequestException {
    return new HttpRequest(url, METHOD_PUT);
  }

  public static HttpRequest put(final URL url) throws HttpRequestException {
    return new HttpRequest(url, METHOD_PUT);
  }

  public static HttpRequest put(final CharSequence baseUrl,
      final Map<?, ?> params, final boolean encode) {
    String url = append(baseUrl, params, encode);
    return put(url);
  }

  public static HttpRequest put(final CharSequence baseUrl,
      final boolean encode, final Object... params) {
    String url = append(baseUrl, encode, params);
    return put(url);
  }

  public static HttpRequest delete(final CharSequence url)
      throws HttpRequestException {
    return new HttpRequest(url, METHOD_DELETE);
  }

  public static HttpRequest delete(final URL url) throws HttpRequestException {
    return new HttpRequest(url, METHOD_DELETE);
  }

  public static HttpRequest delete(final CharSequence baseUrl,
      final Map<?, ?> params, final boolean encode) {
    String url = append(baseUrl, params, encode);
    return delete(url);
  }

  public static HttpRequest delete(final CharSequence baseUrl,
      final boolean encode, final Object... params) {
    String url = append(baseUrl, encode, params);
    return delete(url);
  }

  public static HttpRequest options(final CharSequence url)
      throws HttpRequestException {
    return new HttpRequest(url, METHOD_OPTIONS);
  }

  public static HttpRequest options(final URL url) throws HttpRequestException {
    return new HttpRequest(url, METHOD_OPTIONS);
  }

  public static HttpRequest trace(final CharSequence url)
      throws HttpRequestException {
    return new HttpRequest(url, METHOD_TRACE);
  }

  public static HttpRequest trace(final URL url) throws HttpRequestException {
    return new HttpRequest(url, METHOD_TRACE);
  }

  public static void keepAlive(final boolean keepAlive) {
    setProperty("http.keepAlive", Boolean.toString(keepAlive));
  }

  public static void maxConnections(final int maxConnections) {
    setProperty("http.maxConnections", Integer.toString(maxConnections));
  }

  public static void proxyHost(final String host) {
    setProperty("http.proxyHost", host);
    setProperty("https.proxyHost", host);
  }

  public static void proxyPort(final int port) {
    final String portValue = Integer.toString(port);
    setProperty("http.proxyPort", portValue);
    setProperty("https.proxyPort", portValue);
  }

  public static void nonProxyHosts(final String... hosts) {
    if (hosts != null && hosts.length > 0) {
      StringBuilder separated = new StringBuilder();
      int last = hosts.length - 1;
      for (int i = 0; i < last; i++) {
        separated.append(hosts[i]).append('|');
      }
      separated.append(hosts[last]);
      setProperty("http.nonProxyHosts", separated.toString());
    } else {
      setProperty("http.nonProxyHosts", null);
    }
  }

  private static String setProperty(final String name, final String value) {
    final PrivilegedAction<String> action;
    if (value != null) {
      action = new PrivilegedAction<String>() {

        @Override
        public String run() {
          return System.setProperty(name, value);
        }
      };
    } else {
      action = new PrivilegedAction<String>() {

        @Override
        public String run() {
          return System.clearProperty(name);
        }
      };
    }
    return AccessController.doPrivileged(action);
  }

  private HttpURLConnection connection = null;

  private final URL url;

  private final String requestMethod;

  private RequestOutputStream output;

  private boolean multipart;

  private boolean form;

  private boolean ignoreCloseExceptions = true;

  private int bufferSize = 8192;

  private long totalSize = -1;

  private long totalWritten = 0;

  private String httpProxyHost;

  private int httpProxyPort;

  private UploadProgress progress = UploadProgress.DEFAULT;

  public HttpRequest(final CharSequence url, final String method)
      throws HttpRequestException {
    try {
      this.url = new URL(url.toString());
    } catch (MalformedURLException e) {
      throw new HttpRequestException(e);
    }
    this.requestMethod = method;
  }

  public HttpRequest(final URL url, final String method)
      throws HttpRequestException {
    this.url = url;
    this.requestMethod = method;
  }

  private Proxy createProxy() {
    return new Proxy(HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
  }

  private HttpURLConnection createConnection() {
    try {
      final HttpURLConnection connection;
      if (httpProxyHost != null) {
        connection = CONNECTION_FACTORY.create(url, createProxy());
      } else {
        connection = CONNECTION_FACTORY.create(url);
      }
      connection.setRequestMethod(requestMethod);
      return connection;
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  @Override
  public String toString() {
    return method() + ' ' + url();
  }

  public HttpURLConnection getConnection() {
    if (connection == null) {
      connection = createConnection();
    }
    return connection;
  }

  public HttpRequest ignoreCloseExceptions(final boolean ignore) {
    ignoreCloseExceptions = ignore;
    return this;
  }

  public boolean ignoreCloseExceptions() {
    return ignoreCloseExceptions;
  }

  public int status() throws HttpRequestException {
    try {
      closeOutput();
      return getConnection().getResponseCode();
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  public String message() throws HttpRequestException {
    try {
      closeOutput();
      return getConnection().getResponseMessage();
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  public HttpRequest disconnect() {
    getConnection().disconnect();
    return this;
  }

  public HttpRequest chunk(final int size) {
    getConnection().setChunkedStreamingMode(size);
    return this;
  }

  public HttpRequest bufferSize(final int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Size must be greater than zero");
    }
    bufferSize = size;
    return this;
  }

  public int bufferSize() {
    return bufferSize;
  }

  protected ByteArrayOutputStream byteStream() {
    final int size = contentLength();
    if (size > 0) {
      return new ByteArrayOutputStream(size);
    } else {
      return new ByteArrayOutputStream();
    }
  }

  public String body(final String charset) throws HttpRequestException {
    final ByteArrayOutputStream output = byteStream();
    try {
      copy(buffer(), output);
      return output.toString(getValidCharset(charset));
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  public String body() throws HttpRequestException {
    return body(charset());
  }

  public Json json() {
    return new Json(body());
  }

  public byte[] bytes() throws HttpRequestException {
    final ByteArrayOutputStream output = byteStream();
    try {
      copy(buffer(), output);
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return output.toByteArray();
  }

  public BufferedInputStream buffer() throws HttpRequestException {
    return new BufferedInputStream(stream(), bufferSize);
  }

  public InputStream stream() throws HttpRequestException {
    InputStream stream;
    if (status() < HTTP_BAD_REQUEST) {
      try {
        stream = getConnection().getInputStream();
      } catch (IOException e) {
        throw new HttpRequestException(e);
      }
    } else {
      stream = getConnection().getErrorStream();
      if (stream == null) {
        try {
          stream = getConnection().getInputStream();
        } catch (IOException e) {
          if (contentLength() > 0) {
            throw new HttpRequestException(e);
          } else {
            stream = new ByteArrayInputStream(new byte[0]);
          }
        }
      }
    }

    if (ENCODING_GZIP.equals(contentEncoding())) {
      try {
        return new GZIPInputStream(stream);
      } catch (IOException e) {
        throw new HttpRequestException(e);
      }
    } else {
      return stream;
    }
  }

  public InputStreamReader reader(final String charset)
      throws HttpRequestException {
    try {
      return new InputStreamReader(stream(), getValidCharset(charset));
    } catch (UnsupportedEncodingException e) {
      throw new HttpRequestException(e);
    }
  }

  public InputStreamReader reader() throws HttpRequestException {
    return reader(charset());
  }

  public BufferedReader bufferedReader(final String charset)
      throws HttpRequestException {
    return new BufferedReader(reader(charset), bufferSize);
  }

  public BufferedReader bufferedReader() throws HttpRequestException {
    return bufferedReader(charset());
  }

  public HttpRequest receive(final File file) throws HttpRequestException {
    final OutputStream output;
    try {
      output = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
    } catch (FileNotFoundException e) {
      throw new HttpRequestException(e);
    }
    return new CloseOperation<HttpRequest>(output, ignoreCloseExceptions) {
      @Override
      protected HttpRequest run() throws HttpRequestException, IOException {
        return receive(output);
      }
    }.call();
  }

  public HttpRequest receive(final OutputStream output)
      throws HttpRequestException {
    try {
      return copy(buffer(), output);
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  public HttpRequest receive(final PrintStream output)
      throws HttpRequestException {
    return receive((OutputStream) output);
  }

  public HttpRequest receive(final Appendable appendable)
      throws HttpRequestException {
    final BufferedReader reader = bufferedReader();
    return new CloseOperation<HttpRequest>(reader, ignoreCloseExceptions) {

      @Override
      public HttpRequest run() throws IOException {
        final CharBuffer buffer = CharBuffer.allocate(bufferSize);
        int read;
        while ((read = reader.read(buffer)) != -1) {
          buffer.rewind();
          appendable.append(buffer, 0, read);
          buffer.rewind();
        }
        return HttpRequest.this;
      }
    }.call();
  }

  public HttpRequest receive(final Writer writer) throws HttpRequestException {
    final BufferedReader reader = bufferedReader();
    return new CloseOperation<HttpRequest>(reader, ignoreCloseExceptions) {

      @Override
      public HttpRequest run() throws IOException {
        return copy(reader, writer);
      }
    }.call();
  }

  public HttpRequest readTimeout(final int timeout) {
    getConnection().setReadTimeout(timeout);
    return this;
  }

  public HttpRequest connectTimeout(final int timeout) {
    getConnection().setConnectTimeout(timeout);
    return this;
  }

  public HttpRequest header(final String name, final String value) {
    getConnection().setRequestProperty(name, value);
    return this;
  }

  public HttpRequest header(final String name, final Number value) {
    return header(name, value != null ? value.toString() : null);
  }

  public HttpRequest headers(final Map<String, String> headers) {
    if (!headers.isEmpty()) {
      for (Entry<String, String> header : headers.entrySet()) {
        header(header);
      }
    }
    return this;
  }

  public HttpRequest header(final Entry<String, String> header) {
    return header(header.getKey(), header.getValue());
  }

  public String header(final String name) throws HttpRequestException {
    closeOutputQuietly();
    return getConnection().getHeaderField(name);
  }

  public Map<String, List<String>> headers() throws HttpRequestException {
    closeOutputQuietly();
    return getConnection().getHeaderFields();
  }

  public long dateHeader(final String name) throws HttpRequestException {
    return dateHeader(name, -1L);
  }

  public long dateHeader(final String name, final long defaultValue)
      throws HttpRequestException {
    closeOutputQuietly();
    return getConnection().getHeaderFieldDate(name, defaultValue);
  }

  public int intHeader(final String name) throws HttpRequestException {
    return intHeader(name, -1);
  }

  public int intHeader(final String name, final int defaultValue)
      throws HttpRequestException {
    closeOutputQuietly();
    return getConnection().getHeaderFieldInt(name, defaultValue);
  }

  public String[] headers(final String name) {
    final Map<String, List<String>> headers = headers();
    if (headers == null || headers.isEmpty()) {
      return EMPTY_STRINGS;
    }

    final List<String> values = headers.get(name);
    if (values != null && !values.isEmpty()) {
      return values.toArray(new String[values.size()]);
    } else {
      return EMPTY_STRINGS;
    }
  }

  public String parameter(final String headerName, final String paramName) {
    return getParam(header(headerName), paramName);
  }

  public Map<String, String> parameters(final String headerName) {
    return getParams(header(headerName));
  }

  protected Map<String, String> getParams(final String header) {
    if (header == null || header.length() == 0) {
      return Collections.emptyMap();
    }

    final int headerLength = header.length();
    int start = header.indexOf(';') + 1;
    if (start == 0 || start == headerLength) {
      return Collections.emptyMap();
    }

    int end = header.indexOf(';', start);
    if (end == -1) {
      end = headerLength;
    }

    Map<String, String> params = new LinkedHashMap<String, String>();
    while (start < end) {
      int nameEnd = header.indexOf('=', start);
      if (nameEnd != -1 && nameEnd < end) {
        String name = header.substring(start, nameEnd).trim();
        if (name.length() > 0) {
          String value = header.substring(nameEnd + 1, end).trim();
          int length = value.length();
          if (length != 0) {
            if (length > 2 && '"' == value.charAt(0)
                && '"' == value.charAt(length - 1)) {
              params.put(name, value.substring(1, length - 1));
            } else {
              params.put(name, value);
            }
          }
        }
      }

      start = end + 1;
      end = header.indexOf(';', start);
      if (end == -1) {
        end = headerLength;
      }
    }

    return params;
  }

  protected String getParam(final String value, final String paramName) {
    if (value == null || value.length() == 0) {
      return null;
    }

    final int length = value.length();
    int start = value.indexOf(';') + 1;
    if (start == 0 || start == length) {
      return null;
    }

    int end = value.indexOf(';', start);
    if (end == -1) {
      end = length;
    }

    while (start < end) {
      int nameEnd = value.indexOf('=', start);
      if (nameEnd != -1 && nameEnd < end
          && paramName.equals(value.substring(start, nameEnd).trim())) {
        String paramValue = value.substring(nameEnd + 1, end).trim();
        int valueLength = paramValue.length();
        if (valueLength != 0) {
          if (valueLength > 2 && '"' == paramValue.charAt(0)
              && '"' == paramValue.charAt(valueLength - 1)) {
            return paramValue.substring(1, valueLength - 1);
          } else {
            return paramValue;
          }
        }
      }

      start = end + 1;
      end = value.indexOf(';', start);
      if (end == -1) {
        end = length;
      }
    }

    return null;
  }

  public String charset() {
    return parameter(HEADER_CONTENT_TYPE, PARAM_CHARSET);
  }

  public HttpRequest userAgent(final String userAgent) {
    return header(HEADER_USER_AGENT, userAgent);
  }

  public HttpRequest referer(final String referer) {
    return header(HEADER_REFERER, referer);
  }

  public HttpRequest useCaches(final boolean useCaches) {
    getConnection().setUseCaches(useCaches);
    return this;
  }

  public HttpRequest acceptEncoding(final String acceptEncoding) {
    return header(HEADER_ACCEPT_ENCODING, acceptEncoding);
  }

  public HttpRequest acceptGzipEncoding() {
    return acceptEncoding(ENCODING_GZIP);
  }

  public HttpRequest acceptCharset(final String acceptCharset) {
    return header(HEADER_ACCEPT_CHARSET, acceptCharset);
  }

  public String contentEncoding() {
    return header(HEADER_CONTENT_ENCODING);
  }

  public String server() {
    return header(HEADER_SERVER);
  }

  public long date() {
    return dateHeader(HEADER_DATE);
  }

  public String cacheControl() {
    return header(HEADER_CACHE_CONTROL);
  }

  public String eTag() {
    return header(HEADER_ETAG);
  }

  public long expires() {
    return dateHeader(HEADER_EXPIRES);
  }

  public long lastModified() {
    return dateHeader(HEADER_LAST_MODIFIED);
  }

  public String location() {
    return header(HEADER_LOCATION);
  }

  public HttpRequest authorization(final String authorization) {
    return header(HEADER_AUTHORIZATION, authorization);
  }

  public HttpRequest proxyAuthorization(final String proxyAuthorization) {
    return header(HEADER_PROXY_AUTHORIZATION, proxyAuthorization);
  }

  public HttpRequest basic(final String name, final String password) {
    byte[] data = (name + ':' + password).getBytes(Charsets.UTF_8);
    return authorization("Basic " + BaseEncoding.base64().encode(data));
  }

  public HttpRequest proxyBasic(final String name, final String password) {
    byte[] data = (name + ':' + password).getBytes(Charsets.UTF_8);
    return proxyAuthorization("Basic " + BaseEncoding.base64().encode(data));
  }

  public HttpRequest ifModifiedSince(final long ifModifiedSince) {
    getConnection().setIfModifiedSince(ifModifiedSince);
    return this;
  }

  public HttpRequest ifNoneMatch(final String ifNoneMatch) {
    return header(HEADER_IF_NONE_MATCH, ifNoneMatch);
  }

  public HttpRequest contentType(final String contentType) {
    return contentType(contentType, null);
  }

  public HttpRequest contentType(final String contentType, final String charset) {
    if (charset != null && charset.length() > 0) {
      final String separator = "; " + PARAM_CHARSET + '=';
      return header(HEADER_CONTENT_TYPE, contentType + separator + charset);
    } else {
      return header(HEADER_CONTENT_TYPE, contentType);
    }
  }

  public String contentType() {
    return header(HEADER_CONTENT_TYPE);
  }

  public int contentLength() {
    return intHeader(HEADER_CONTENT_LENGTH);
  }

  public HttpRequest contentLength(final String contentLength) {
    return contentLength(Integer.parseInt(contentLength));
  }

  public HttpRequest contentLength(final int contentLength) {
    getConnection().setFixedLengthStreamingMode(contentLength);
    return this;
  }

  public HttpRequest accept(final String accept) {
    return header(HEADER_ACCEPT, accept);
  }

  public HttpRequest acceptJson() {
    return accept(CONTENT_TYPE_JSON);
  }

  protected HttpRequest copy(final InputStream input, final OutputStream output)
      throws IOException {
    return new CloseOperation<HttpRequest>(input, ignoreCloseExceptions) {

      @Override
      public HttpRequest run() throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = input.read(buffer)) != -1) {
          output.write(buffer, 0, read);
          totalWritten += read;
          progress.onUpload(totalWritten, totalSize);
        }
        return HttpRequest.this;
      }
    }.call();
  }

  protected HttpRequest copy(final Reader input, final Writer output)
      throws IOException {
    return new CloseOperation<HttpRequest>(input, ignoreCloseExceptions) {

      @Override
      public HttpRequest run() throws IOException {
        final char[] buffer = new char[bufferSize];
        int read;
        while ((read = input.read(buffer)) != -1) {
          output.write(buffer, 0, read);
          totalWritten += read;
          progress.onUpload(totalWritten, -1);
        }
        return HttpRequest.this;
      }
    }.call();
  }

  public HttpRequest progress(final UploadProgress callback) {
    if (callback == null) {
      progress = UploadProgress.DEFAULT;
    } else {
      progress = callback;
    }
    return this;
  }

  private HttpRequest incrementTotalSize(final long size) {
    if (totalSize == -1) {
      totalSize = 0;
    }
    totalSize += size;
    return this;
  }

  protected HttpRequest closeOutput() throws IOException {
    progress(null);
    if (output == null) {
      return this;
    }
    if (multipart) {
      output.write(CRLF + "--" + BOUNDARY + "--" + CRLF);
    }
    if (ignoreCloseExceptions) {
      try {
        output.close();
      } catch (IOException ignored) {
        // Ignored
      }
    } else {
      output.close();
    }
    output = null;
    return this;
  }

  protected HttpRequest closeOutputQuietly() throws HttpRequestException {
    try {
      return closeOutput();
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  protected HttpRequest openOutput() throws IOException {
    if (output != null) {
      return this;
    }
    getConnection().setDoOutput(true);
    final String charset = getParam(
        getConnection().getRequestProperty(HEADER_CONTENT_TYPE), PARAM_CHARSET);
    output = new RequestOutputStream(getConnection().getOutputStream(), charset,
        bufferSize);
    return this;
  }

  protected HttpRequest startPart() throws IOException {
    if (!multipart) {
      multipart = true;
      contentType(CONTENT_TYPE_MULTIPART).openOutput();
      output.write("--" + BOUNDARY + CRLF);
    } else {
      output.write(CRLF + "--" + BOUNDARY + CRLF);
    }
    return this;
  }

  protected HttpRequest writePartHeader(final String name, final String filename)
      throws IOException {
    return writePartHeader(name, filename, null);
  }

  protected HttpRequest writePartHeader(final String name,
      final String filename, final String contentType) throws IOException {
    final StringBuilder partBuffer = new StringBuilder();
    partBuffer.append("form-data; name=\"").append(name);
    if (filename != null) {
      partBuffer.append("\"; filename=\"").append(filename);
    }
    partBuffer.append('"');
    partHeader("Content-Disposition", partBuffer.toString());
    if (contentType != null) {
      partHeader(HEADER_CONTENT_TYPE, contentType);
    }
    return send(CRLF);
  }

  public HttpRequest part(final String name, final String part) {
    return part(name, null, part);
  }

  public HttpRequest part(final String name, final String filename,
      final String part) throws HttpRequestException {
    return part(name, filename, null, part);
  }

  public HttpRequest part(final String name, final String filename,
      final String contentType, final String part) throws HttpRequestException {
    try {
      startPart();
      writePartHeader(name, filename, contentType);
      output.write(part);
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return this;
  }

  public HttpRequest part(final String name, final Number part)
      throws HttpRequestException {
    return part(name, null, part);
  }

  public HttpRequest part(final String name, final String filename,
      final Number part) throws HttpRequestException {
    return part(name, filename, part != null ? part.toString() : null);
  }

  public HttpRequest part(final String name, final File part)
      throws HttpRequestException {
    return part(name, null, part);
  }

  public HttpRequest part(final String name, final String filename,
      final File part) throws HttpRequestException {
    return part(name, filename, null, part);
  }

  public HttpRequest part(final String name, final String filename,
      final String contentType, final File part) throws HttpRequestException {
    final InputStream stream;
    try {
      stream = new BufferedInputStream(new FileInputStream(part));
      incrementTotalSize(part.length());
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return part(name, filename, contentType, stream);
  }

  public HttpRequest part(final String name, final InputStream part)
      throws HttpRequestException {
    return part(name, null, null, part);
  }

  public HttpRequest part(final String name, final String filename,
      final String contentType, final InputStream part)
      throws HttpRequestException {
    try {
      startPart();
      writePartHeader(name, filename, contentType);
      copy(part, output);
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return this;
  }

  public HttpRequest partHeader(final String name, final String value)
      throws HttpRequestException {
    return send(name).send(": ").send(value).send(CRLF);
  }

  public HttpRequest send(final File input) throws HttpRequestException {
    final InputStream stream;
    try {
      stream = new BufferedInputStream(new FileInputStream(input));
      incrementTotalSize(input.length());
    } catch (FileNotFoundException e) {
      throw new HttpRequestException(e);
    }
    return send(stream);
  }

  public HttpRequest send(final byte[] input) throws HttpRequestException {
    if (input != null) {
      incrementTotalSize(input.length);
    }
    return send(new ByteArrayInputStream(input));
  }

  public HttpRequest send(final InputStream input) throws HttpRequestException {
    try {
      openOutput();
      copy(input, output);
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return this;
  }

  public HttpRequest send(final Reader input) throws HttpRequestException {
    try {
      openOutput();
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    final Writer writer = new OutputStreamWriter(output,
        output.encoder.charset());
    return new FlushOperation<HttpRequest>(writer) {

      @Override
      protected HttpRequest run() throws IOException {
        return copy(input, writer);
      }
    }.call();
  }

  public HttpRequest send(final CharSequence value) throws HttpRequestException {
    try {
      openOutput();
      output.write(value.toString());
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return this;
  }

  public OutputStreamWriter writer() throws HttpRequestException {
    try {
      openOutput();
      return new OutputStreamWriter(output, output.encoder.charset());
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
  }

  public HttpRequest form(final Map<?, ?> values) throws HttpRequestException {
    return form(values, CHARSET_UTF8);
  }

  public HttpRequest form(final Entry<?, ?> entry) throws HttpRequestException {
    return form(entry, CHARSET_UTF8);
  }

  public HttpRequest form(final Entry<?, ?> entry, final String charset)
      throws HttpRequestException {
    return form(entry.getKey(), entry.getValue(), charset);
  }

  public HttpRequest form(final Object name, final Object value)
      throws HttpRequestException {
    return form(name, value, CHARSET_UTF8);
  }

  public HttpRequest form(final Object name, final Object value, String charset)
      throws HttpRequestException {
    final boolean first = !form;
    if (first) {
      contentType(CONTENT_TYPE_FORM, charset);
      form = true;
    }
    charset = getValidCharset(charset);
    try {
      openOutput();
      if (!first) {
        output.write('&');
      }
      output.write(URLEncoder.encode(name.toString(), charset));
      output.write('=');
      if (value != null) {
        output.write(URLEncoder.encode(value.toString(), charset));
      }
    } catch (IOException e) {
      throw new HttpRequestException(e);
    }
    return this;
  }

  public HttpRequest form(final Map<?, ?> values, final String charset)
      throws HttpRequestException {
    if (!values.isEmpty()) {
      for (Entry<?, ?> entry : values.entrySet()) {
        form(entry, charset);
      }
    }
    return this;
  }

  public HttpRequest trustAllCerts() throws HttpRequestException {
    final HttpURLConnection connection = getConnection();
    if (connection instanceof HttpsURLConnection) {
      ((HttpsURLConnection) connection)
          .setSSLSocketFactory(getTrustedFactory());
    }
    return this;
  }

  public HttpRequest trustAllHosts() {
    final HttpURLConnection connection = getConnection();
    if (connection instanceof HttpsURLConnection) {
      ((HttpsURLConnection) connection)
          .setHostnameVerifier(getTrustedVerifier());
    }
    return this;
  }

  public URL url() {
    return getConnection().getURL();
  }

  public String method() {
    return getConnection().getRequestMethod();
  }

  public HttpRequest useProxy(final String proxyHost, final int proxyPort) {
    if (connection != null) {
      throw new IllegalStateException(
          "The connection has already been created. This method must be called before reading or writing to the request.");
    }

    this.httpProxyHost = proxyHost;
    this.httpProxyPort = proxyPort;
    return this;
  }

  public HttpRequest followRedirects(final boolean followRedirects) {
    getConnection().setInstanceFollowRedirects(followRedirects);
    return this;
  }

  public HttpRequest checkStatus() {
    int status = status();
    if (status < 200 || status >= 300) {
      try {
        Log.error(body());
      } catch (Throwable t) {
      }
      throw new IllegalStateException("Error status: " + status);
    }
    return this;
  }
}