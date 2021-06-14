package ox;

import static java.lang.Double.parseDouble;
import static ox.util.Utils.formatDecimal2;
import static ox.util.Utils.signum;

import ox.util.Utils;

/**
 * Immutable class representing a fractional percentage.
 */
public class Percent implements Comparable<Percent> {

  public static final Percent ZERO = new Percent(0.0), ONE_HUNDRED = new Percent(1.0);

  private final double value;

  private Percent(double value) {
    this.value = value;
  }

  /**
   * Gets the raw value, 1.0 representing 100%
   */
  public double getValue() {
    return value;
  }

  public boolean isZero() {
    return this.value == 0.0;
  }

  public boolean isOneHundred() {
    return this.value == 1.0;
  }

  public boolean isGreaterThan(Percent m) {
    return this.value > m.value;
  }

  public boolean isLessThan(Percent m) {
    return this.value < m.value;
  }

  public boolean isPositive() {
    return this.value > 0;
  }

  public boolean isNegative() {
    return this.value < 0;
  }

  @Override
  public int compareTo(Percent o) {
    return signum(this.value - o.value);
  }

  @Override
  public String toString() {
    return formatNoDecimals();
  }

  public String formatNoDecimals() {
    return format(false);
  }

  public String formatWithDecimals() {
    return format(true);
  }

  private String format(boolean decimals) {
    if (decimals) {
      return formatDecimal2(value * 100) + "%";
    } else {
      return Utils.format(value * 100) + "%";
    }
  }

  /**
   * Gets if this Percent is between 0% and 100%, inclusive.
   */
  public boolean isNormal() {
    return 0.0 <= value && value <= 1.0;
  }

  /**
   * Returns the compliment to this percentage, which when combined with this, adds to 100%.
   */
  public Percent compliment() {
    return new Percent(1.0 - value);
  }

  public Percent inverse() {
    return new Percent(1.0 / value);
  }

  public Percent multiply(Percent p) {
    return new Percent(value * p.value);
  }

  public static Percent of(double value) {
    return new Percent(value);
  }

  public static Percent divide(double numerator, double denominator) {
    return new Percent(numerator / denominator);
  }

  public static Percent parse(String s) {
    return new Percent(parseDouble(s.replace("%", "")) / 100.0);
  }

}
