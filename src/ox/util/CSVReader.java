package ox.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class CSVReader {

  private final BufferedReader br;

  private StringBuilder sb = new StringBuilder();
  private int lastSize;
  private char delimiter = ',';
  private char escape = '"';

  public CSVReader(InputStream is) {
    br = new BufferedReader(new InputStreamReader(is));
  }

  public void forEach(Consumer<List<String>> callback) {
    List<String> m = nextLine();
    while (m != null) {
      callback.accept(m);
      m = nextLine();
    }
  }

  public List<String> nextLine() {
    String line;
    try {
      line = br.readLine();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    if (line == null) {
      return null;
    }

    return parseLine(line);
  }

  private List<String> parseLine(String line) {
    List<String> ret = Lists.newArrayListWithCapacity(lastSize);
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

}
