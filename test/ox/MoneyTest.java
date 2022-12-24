package ox;

import static com.google.common.base.Preconditions.checkState;

import org.junit.jupiter.api.Test;

import com.google.common.base.Objects;

public class MoneyTest {

  @Test
  public void parse() {
    checkState(Objects.equal(Money.parse("$12.34").toLong(), 1234L));
    checkState(Objects.equal(Money.parse("12.34").toLong(), 1234L));
    checkState(Objects.equal(Money.parse("$12").toLong(), 1200L));
    checkState(Objects.equal(Money.parse("-$12").toLong(), -1200L));
    checkState(Objects.equal(Money.parse("(12)").toLong(), -1200L));
    checkState(Objects.equal(Money.parse("$12.34567").toLong(), 1235L));
  }

}
