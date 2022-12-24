package ox.util;

import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

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

}
