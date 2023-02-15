package ox;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.isNullOrEmpty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

/**
 * Immutable class representing a fractional percentage.
 */
public class Percent implements Comparable<Percent> {

  public static final Percent ZERO = new Percent(0.0), ONE_HUNDRED = new Percent(1.0);

  private final BigDecimal value;

  private Percent(double value) {
    this.value = new BigDecimal(value);
  }

  private Percent(String value) {
    this.value = new BigDecimal(value);
  }

  private Percent(BigDecimal value) {
    this.value = value;
  }

  /**
   * Gets the raw value, 1.0 representing 100%
   */
  public BigDecimal getValue() {
    return value;
  }

  public boolean isZero() {
    return value.compareTo(BigDecimal.ZERO) == 0;
  }

  public boolean isOneHundred() {
    return value.compareTo(BigDecimal.ONE) == 0;
  }

  public boolean isGreaterThan(Percent m) {
    return this.value.compareTo(m.value) > 0;
  }

  public boolean isLessThan(Percent m) {
    return this.value.compareTo(m.value) < 0;
  }

  public boolean isPositive() {
    return isGreaterThan(Percent.ZERO);
  }

  public boolean isNegative() {
    return isLessThan(Percent.ZERO);
  }

  @Override
  public int compareTo(Percent o) {
    return this.value.compareTo(o.value);
  }

  @Override
  public String toString() {
    return formatNoDecimals();
  }

  public String formatNoDecimals() {
    return this.value.multiply(BigDecimal.valueOf(100)).setScale(0, BigDecimal.ROUND_HALF_EVEN).toPlainString() + "%";
  }

  public String formatWithDecimals(int nDecimals) {
    return this.value.multiply(BigDecimal.valueOf(100))
        .setScale(nDecimals, RoundingMode.HALF_EVEN)
        .toPlainString() + "%";
  }

  public String formatWithDecimals() {
    return this.value.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
  }

  public String formatWithMaxLength(int maxLen) {
    String s = this.value.multiply(BigDecimal.valueOf(100))
        .setScale(20, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
    if (s.length() + 1 <= maxLen) {
      return s + "%";
    }

    int i = s.indexOf('.');
    if (i == -1) {
      checkState(s.length() < maxLen);
      return s + "%";
    }
    checkState(i + 2 < maxLen);

    return s.substring(0, maxLen - 1) + "%";
  }

  /**
   * Gets if this Percent is between 0% and 100%, inclusive.
   */
  public boolean isNormal() {
    return !isNegative() && !isGreaterThan(Percent.ONE_HUNDRED);
  }

  /**
   * Returns the compliment to this percentage, which when combined with this, adds to 100%.
   */
  public Percent compliment() {
    return new Percent(BigDecimal.ONE.subtract(value));
  }

  public Percent inverse() {
    return new Percent(BigDecimal.ONE.divide(value, 20, RoundingMode.HALF_EVEN));
  }

  public Percent add(Percent p) {
    return new Percent(value.add(p.value));
  }

  public Percent add(double d) {
    return add(Percent.of(d));
  }

  public Percent subtract(Percent p) {
    return new Percent(value.subtract(p.value));
  }

  public Percent multiply(Percent p) {
    return new Percent(value.multiply(p.value));
  }

  public Percent multiply(long n) {
    return new Percent(value.multiply(BigDecimal.valueOf(n)));
  }

  public static <T> Percent sum(Iterable<T> iter, Function<T, Percent> function) {
    BigDecimal ret = BigDecimal.ZERO;
    for (T t : iter) {
      ret = ret.add(function.apply(t).value);
    }
    return new Percent(ret);
  }

  public static Percent of(double value) {
    return new Percent(value);
  }

  public static Percent divide(double numerator, double denominator) {
    return new Percent(numerator / denominator);
  }

  public static Percent divide(Money numerator, Money denominator) {
    return new Percent(1.0 * numerator.toLong() / denominator.toLong());
  }

  public static Percent parse(String s) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    return new Percent(new BigDecimal(s.replace("%", "")).divide(BigDecimal.valueOf(100)));
  }

}
