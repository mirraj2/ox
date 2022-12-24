package ox.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import ox.File;

public class CSVWriter {

  private PrintStream out;
  private StringBuilder buffer = new StringBuilder();

  public CSVWriter(File file) {
    this(file.outputStream());
  }

  public CSVWriter(OutputStream os) {
    this.out = new PrintStream(os);
  }

  public void write(Object... row) {
    write(Arrays.asList(row));
  }

  public void write(List<? extends Object> row) {
    int size = row.size();
    for (int i = 0; i < size; i++) {
      Object o = row.get(i);
      if (o != null) {
        String s = o.toString();
        if (s.indexOf(',') != -1 || s.indexOf('\n') != -1 || s.indexOf('"') != -1) {
          buffer.append('"');
          buffer.append(s);
          buffer.append('"');
        } else {
          buffer.append(s);
        }
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
