package ox.util;

import java.io.IOException;
import java.io.OutputStream;

public class SplitOutputStream extends ProxyOutputStream {

  protected OutputStream branch;

  public SplitOutputStream(OutputStream out, OutputStream branch) {
    super(out);
    this.branch = branch;
  }

  @Override
  public synchronized void write(byte[] b) throws IOException {
    super.write(b);
    this.branch.write(b);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    super.write(b, off, len);
    this.branch.write(b, off, len);
  }

  @Override
  public synchronized void write(int b) throws IOException {
    super.write(b);
    this.branch.write(b);
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    this.branch.flush();
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.branch.close();
  }

}
