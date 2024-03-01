package ox.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

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

  public void write(Collection<? extends Object> row) {
    int size = row.size();
    for (Object o : row) {
      if (o != null) {
        String s = o.toString().replaceAll("\"", "\"\"");
        if (s.indexOf(',') != -1 || s.indexOf('\n') != -1 || s.indexOf('"') != -1) {
          buffer.append('"');
          buffer.append(s);
          buffer.append('"');
        } else {
          buffer.append(s);
        }
      }
      buffer.append(',');
    }
    if (size > 0) {
      // delete the trailing comma
      buffer.setLength(buffer.length() - 1);
    }
    out.println(buffer.toString());
    buffer.setLength(0);
  }

  public void close() {
    out.close();
  }

}
