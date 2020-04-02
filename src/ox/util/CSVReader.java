package ox.util;

import static ox.util.Utils.propagate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CSVReader {

  private final BufferedReader br;

  private StringBuilder sb = new StringBuilder();
  private int lastSize;
  private char delimiter = ',';
  private char escape = '"';
  private boolean reuseBuffer = false;
  private List<String> buffer;

  public CSVReader(InputStream is) {
    this(new InputStreamReader(is));
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

  public Map<String, Integer> getHeaderIndex() {
    Map<String, Integer> ret = Maps.newLinkedHashMap();
    List<String> row = nextLine();
    for (int i = 0; i < row.size(); i++) {
      ret.put(row.get(i), i);
    }
    return ret;
  }

  public void forEach(Consumer<List<String>> callback) {
    List<String> m = nextLine();
    while (m != null) {
      callback.accept(m);
      m = nextLine();
    }
  }

  public List<List<String>> getLines() {
    List<List<String>> ret = Lists.newArrayList();
    forEach(ret::add);
    return ret;
  }

  public List<String> nextLine() {
    String line;
    try {
      line = br.readLine();
    } catch (IOException e) {
      throw propagate(e);
    }
    if (line == null) {
      return null;
    }

    return parseLine(line);
  }

  private List<String> parseLine(String line) {
    List<String> ret = buffer;
    if (ret == null) {
      ret = Lists.newArrayListWithCapacity(lastSize);
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
        escaped = !escaped;
      } else if (!escaped && c == delimiter) {
        ret.add(sb.toString());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
    }
    ret.add(sb.toString());
    sb.setLength(0);
    lastSize = ret.size();
    return ret;
  }

  public static CSVReader from(InputStream is) {
    return new CSVReader(is);
  }

}
