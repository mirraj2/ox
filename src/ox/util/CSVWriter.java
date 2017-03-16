package ox.util;

import static ox.util.Utils.propagate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class CSVWriter {

  private PrintStream out;
  private StringBuilder buffer = new StringBuilder();

  public CSVWriter(File file) {
    try {
      out = new PrintStream(new FileOutputStream(file));
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  public CSVWriter(OutputStream os) {
    this.out = new PrintStream(os);
  }

  public void write(List<String> row) {
    int size = row.size();
    for (int i = 0; i < size; i++) {
      String s = row.get(i);
      if (s.indexOf(',') != -1) {
        buffer.append('"');
        buffer.append(s);
        buffer.append('"');
      } else {
        buffer.append(s);
      }
      if (i < size - 1) {
        buffer.append(',');
      }
    }
    out.println(buffer.toString());
    buffer.setLength(0);
  }

  public void close() {
    out.close();
  }

}
