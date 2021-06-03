package ox;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;

public class MoneyTest {

  public void parse() {
    checkState(Objects.equal(Money.parse("$12.34").toLong(), 1234L));
    checkState(Objects.equal(Money.parse("12.34").toLong(), 1234L));
    checkState(Objects.equal(Money.parse("$12").toLong(), 1200L));
    checkState(Objects.equal(Money.parse("-$12").toLong(), -1200L));
    checkState(Objects.equal(Money.parse("(12)").toLong(), -1200L));
    checkState(Objects.equal(Money.parse("$12.34567").toLong(), 1235L));
  }

}
