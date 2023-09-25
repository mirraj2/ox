package ox.util;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.format;
import static ox.util.Utils.normalize;
import static ox.util.Utils.propagate;
import static ox.util.Utils.toBoolean;
import static ox.util.Utils.toDouble;
import static ox.util.Utils.toInt;
import static ox.util.Utils.toLong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import ox.IO;
import ox.Json;
import ox.Log;
import ox.Money;
import ox.x.XList;
import ox.x.XMap;

public class CSVReader {

  private static final LocalDate EXCEL_EPOCH_DATE = LocalDate.ofYearDay(1900, 1).minusDays(2);

  private final BufferedReader br;
  private StringBuilder sb = new StringBuilder();
  private int lastSize = 0;
  private char delimiter = ',';
  private char escape = '"';
  private boolean reuseBuffer = false;
  private boolean debug = false;
  private XList<String> buffer;

  public CSVReader(InputStream is) {
    this(new InputStreamReader(is, StandardCharsets.UTF_8));
  }

  public CSVReader(InputStream is, Charset charset) {
    this(new InputStreamReader(is, charset));
  }

  public CSVReader(String s) {
    this(new StringReader(s));
  }

  public CSVReader(Reader reader) {
    br = new BufferedReader(reader);
  }

  public CSVReader reuseBuffer() {
    reuseBuffer = true;
    return this;
  }

  public CSVReader debug() {
    debug = true;
    return this;
  }

  public CSVReader delimiter(char delimiter) {
    this.delimiter = delimiter;
    return this;
  }

  public CSVReader escape(char escapeCharacter) {
    this.escape = escapeCharacter;
    return this;
  }

  public CSVReader skipLines(int nLines) {
    for (int i = 0; i < nLines; i++) {
      try {
        br.readLine();
      } catch (IOException e) {
        throw propagate(e);
      }
    }
    return this;
  }

  public XMap<String, Integer> getHeaderIndex() {
    XMap<String, Integer> ret = XMap.create();
    List<String> row = nextLine();
    for (int i = 0; i < row.size(); i++) {
      ret.put(normalize(row.get(i)), i);
    }
    return ret;
  }

  public void forEachRow(Consumer<CSVRow> callback) {
    forEachRow(getHeaderIndex(), callback);
  }

  public void forEachRow(Map<String, Integer> header, Consumer<CSVRow> callback) {
    forEach(rowList -> callback.accept(new CSVRow(rowList, header)));
  }

  public void forEach(Consumer<XList<String>> callback) {
    XList<String> m = nextLine();
    while (m != null) {
      callback.accept(m);
      m = nextLine();
    }
    IO.close(br);
  }

  public void forEachBatch(int batchSize, Consumer<XList<XList<String>>> callback) {
    checkState(batchSize > 0, "Bad batchSize.");

    XList<XList<String>> batch = XList.create();
    forEach(row -> {
      batch.add(row);
      if (batch.size() == batchSize) {
        callback.accept(batch);
        batch.clear();
      }
    });
    if (batch.hasData()) {
      callback.accept(batch);
    }
  }

  public XList<XList<String>> getLines() {
    XList<XList<String>> ret = XList.create();
    forEach(ret::add);
    return ret;
  }

  public XList<String> nextLine() {
    String line;
    try {
      line = br.readLine();
      if (debug) {
        Log.debug("Read %s character line.", line.length());
      }
    } catch (IOException e) {
      throw propagate(e);
    }
    if (line == null) {
      return null;
    }

    try {
      return parseLine(line, br);
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  private XList<String> parseLine(String line, BufferedReader br) throws Exception {
    XList<String> ret = buffer;
    if (ret == null) {
      ret = XList.createWithCapacity(lastSize);
      if (reuseBuffer) {
        buffer = ret;
      }
    } else {
      buffer.clear();
    }

    boolean escaped = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == escape) {
        if (escaped) {
          // next character must be a delimiter in order to unescape (or we've reached end of file)
          if (i == line.length() - 1 || line.charAt(i + 1) == delimiter) {
            escaped = false;
          } else {
            sb.append(c);
          }
        } else {
          escaped = true;
        }
      } else if (!escaped && c == delimiter) {
        ret.add(sb.toString());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
      if (i == line.length() - 1) {
        if (escaped) {
          if (debug) {
            Log.debug("there was a newline which was escaped!");
          }
          String nextLine = br.readLine();
          if (nextLine != null) {
            line = line + '\n' + nextLine;
          }
        }
      }
    }
    ret.add(sb.toString());
    sb.setLength(0);

    if (lastSize == 0) {
      lastSize = ret.size();
    } else {
      checkState(ret.size() == lastSize, "Found a row with " + ret.size() +
          " elements when we previously saw a row with " + lastSize + " elements. " + ret);
    }
    return ret;
  }

  public static class CSVRow {
    private List<String> row;
    private Map<String, Integer> header;

    public CSVRow(List<String> row, Map<String, Integer> header) {
      this.row = row;
      this.header = header;
    }

    public XList<String> asList() {
      return XList.create(row);
    }

    public String get(String s) {
      Integer index = header.get(s);
      if (index == null) {
        // Log.warn("Could not find header: " + s);
        return "";
      }
      if (index >= row.size()) {
        return "";
      }
      return normalize(row.get(index));
    }

    public LocalDate getDate(String colName) {
      if (get(colName).contains("/") || get(colName).contains("-")) {
        return getParsedDate(colName);
      } else {
        return getExcelDate(colName);
      }
    }

    /**
     * See also `getExcelDate()`.
     */
    public LocalDate getISODate(String colName) {
      String val = get(colName);
      if (val.isEmpty()) {
        return null;
      }

      try {
        return LocalDate.parse(val);
      } catch (Exception e) {
        throw new RuntimeException(format("Couldn't parse '{0}' as Date, for {1} column.", val, colName));
      }
    }

    /**
     * Get date assuming it is stored as an "Excel" date, which is a number of days since the Excel epoch date.
     */
    public LocalDate getExcelDate(String colName) {
      String val = get(colName);
      if (val.isEmpty()) {
        return null;
      }

      try {
        return EXCEL_EPOCH_DATE.plusDays((int) Double.parseDouble(val));
      } catch (Exception e) {
        throw new RuntimeException(format("Couldn't parse '{0}' as Date, for {1} column.", val, colName));
      }
    }

    /**
     * Parse the following date formats to LocalDate
     * 
     * YYYY-MM-DD
     * 
     * MM-DD-YY, M-DD-YY, M-D-YY, MM-D-YY, MM-DD-YYYY
     * 
     * MM/DD/YY, M/DD/YY, M/D/YY, MM/D/YY, MM/DD/YYYY,
     * 
     */
    public LocalDate getParsedDate(String colName) {
      String val = get(colName);
      if (val.isEmpty()) {
        return null;
      }

      List<String> parsedDate = Splitter.on(CharMatcher.anyOf("-/")).splitToList(val);
      if (parsedDate.get(0).length() == 4) {
        // YYYY-MM-DD
        return getISODate(colName);
      }

      int year;
      if (parsedDate.get(2).length() == 2) {
        // MM/DD/YY
        // MM-DD-YY
        year = Integer.parseInt(parsedDate.get(2)) > 50 ? Integer.parseInt(parsedDate.get(2)) + 1900
            : Integer.parseInt(parsedDate.get(2)) + 2000;
      } else {
        // MM/DD/YYYY
        // MM-DD-YYYY
        year = Integer.parseInt(parsedDate.get(2));
      }

      try {
        return LocalDate.of(year, Integer.parseInt(parsedDate.get(0)), Integer.parseInt(parsedDate.get(1)));
      } catch (Exception e) {
        throw new RuntimeException(format("Couldn't parse '{0}' as Date, for {1} column.", val, colName));
      }
    }

    public Money getMoney(String colName) {
      String val = get(colName);
      try {
        return Money.parse(val);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Couldn't parse '" + val + "' as Money, for " + colName + " column.", e);
      }
    }

    public Integer getInt(String colName) {
      String val = get(colName);
      try {
        return toInt(val);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Couldn't parse '" + val + "' as Integer, for " + colName + " column.", e);
      }
    }

    public Long getLong(String colName) {
      String val = get(colName);
      try {
        return toLong(val);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Couldn't parse '" + val + "' as Long, for " + colName + " column.", e);
      }
    }

    public Double getDouble(String colName) {
      String val = get(colName);
      try {
        return toDouble(val);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Couldn't parse '" + val + "' as Double, for " + colName + " column.", e);
      }
    }

    public <T extends Enum<T>> T getEnum(String colName, Class<T> enumType) {
      String s = get(colName);
      if (s.isEmpty()) {
        return null;
      }
      return Utils.parseEnum(s, enumType);
    }

    public Boolean getBoolean(String colName) {
      return toBoolean(get(colName));
    }

    @Override
    public String toString() {
      return row.toString();
    }

    public Json toJson() {
      Json ret = Json.object();
      header.forEach((key, index) -> {
        ret.with(key, row.get(index));
      });
      return ret;
    }
  }

  public static CSVReader from(InputStream is) {
    return new CSVReader(is);
  }

}
