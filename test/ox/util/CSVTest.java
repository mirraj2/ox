package ox.util;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import ox.IO;
import ox.x.XList;

public class CSVTest {

  @Test
  public void test() {
    XList<String> input = XList.of("a", "b", "c", "this is a \"quoted\" word");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CSVWriter writer = new CSVWriter(baos);
    writer.write(input);
    writer.close();

    CSVReader reader = new CSVReader(new ByteArrayInputStream(baos.toByteArray()));
    XList<String> output = reader.nextLine();

    checkState(input.equals(output), input + " vs " + output);

  }

  @Test
  public void jakeAndJZTest() {
    XList<String> input = XList.of("a", "A \"on\" B, C\nD", "z");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CSVWriter writer = new CSVWriter(baos);
    writer.write(input);
    writer.close();

    CSVReader reader = new CSVReader(new ByteArrayInputStream(baos.toByteArray()));
    XList<String> output = reader.nextLine();

    checkState(input.equals(output), input + " vs " + output);
  }

  /**
   * Whitespace characters are trimmed from header Strings for use as keys.
   */
  @Test
  public void nospaceCharTest() {
    // Here there is a leading no-break whitespace character in the header.
    String content = "\uFEFF" + "foo, bar\n" + "42, 69";
    CSVReader reader = CSVReader.from(IO.from(content).asStream());
    AtomicBoolean found42 = new AtomicBoolean(false);
    reader.forEachRow(row -> {
      if (row.get("foo").equals("42")) {
        found42.set(true);
      }
    });

    assertTrue(found42.get());
  }

}
