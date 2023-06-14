package ox.util;

import java.io.IOException;
import java.io.OutputStream;

public class SynchronizedOutputStream extends OutputStream {

      private final OutputStream out;

      public SynchronizedOutputStream(OutputStream out) {
          this.out = out;
      }

      @Override
      public synchronized void write(int b) throws IOException {
          out.write(b);
      }

      @Override
      public synchronized void write(byte[] b) throws IOException {
          out.write(b);
      }

      @Override
      public synchronized void write(byte[] b, int off, int len) throws IOException {
          out.write(b, off, len);
      }

      @Override
      public synchronized void flush() throws IOException {
          out.flush();
      }

      @Override
      public synchronized void close() throws IOException {
          out.close();
      }
  }
