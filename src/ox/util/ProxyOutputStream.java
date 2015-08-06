package ox.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProxyOutputStream extends FilterOutputStream {

    public ProxyOutputStream(OutputStream proxy) {
        super(proxy);
    }

    @Override
    public void write(int idx) throws IOException {
        out.write(idx);
    }

    @Override
    public void write(byte[] bts) throws IOException {
        out.write(bts);
    }

    @Override
    public void write(byte[] bts, int st, int end) throws IOException {
        out.write(bts, st, end);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

}