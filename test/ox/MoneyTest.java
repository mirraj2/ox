package ox;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class MoneyTest {

  @Test
  public void parse() {
    Assert.assertEquals(Money.parse("$12.34").toLong(), 1234L);
    Assert.assertEquals(Money.parse("12.34").toLong(), 1234L);
    Assert.assertEquals(Money.parse("$12").toLong(), 1200L);
    Assert.assertEquals(Money.parse("-$12").toLong(), -1200L);
    Assert.assertEquals(Money.parse("(12)").toLong(), -1200L);
    Assert.assertEquals(Money.parse("$12.34567").toLong(), 1235L);
  }

}
