package ox;

import static ox.util.Utils.format;
import static ox.util.Utils.isNullOrEmpty;
import static ox.util.Utils.signum;

import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.base.Functions;

import ox.util.Utils;

public class Money implements Comparable<Money> {

  public static final Money ZERO = Money.dollars(0);
  private static final CharMatcher moneyMatcher = CharMatcher.anyOf("$£€ ,-–()").precomputed();

  private final long cents;

  private Money(long cents) {
    this.cents = cents;
  }

  public Money negate() {
    return new Money(-cents);
  }

  public Money add(Money m) {
    return new Money(cents + m.cents);
  }

  public Money subtract(Money m) {
    return new Money(cents - m.cents);
  }

  public Money multiply(int n) {
    return new Money(cents * n);
  }

  public Money multiply(double n) {
    return new Money((long) (cents * n));
  }

  public Money divide(int n) {
    return new Money(cents / n);
  }

  public boolean isGreaterThan(Money m) {
    return this.cents > m.cents;
  }

  public boolean isLessThan(Money m) {
    return this.cents < m.cents;
  }

  public boolean isPositive() {
    return this.cents > 0;
  }

  public boolean isNegative() {
    return this.cents < 0;
  }

  public boolean isZero() {
    return this.cents == 0;
  }

  public long toLong() {
    return cents;
  }

  public double toDouble() {
    return cents / 100.0;
  }

  @Override
  public String toString() {
    return Utils.money(toDouble());
  }

  public long getDollars() {
    return cents / 100;
  }

  public int getCents() {
    return (int) Math.abs(cents % 100);
  }

  public String dollarsFormatted() {
    return format(getDollars());
  }

  public String centsFormatted() {
    int n = getCents();
    return n < 10 ? "0" + n : "" + n;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(cents);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Money)) {
      return false;
    }
    Money that = (Money) obj;
    return this.cents == that.cents;
  }

  @Override
  public int compareTo(Money o) {
    return signum(this.cents - o.cents);
  }

  public static <T> Money sum(Iterable<Money> items) {
    return sum(items, Functions.identity());
  }

  public static <T> Money sum(Iterable<T> items, Function<T, Money> mappingFunction) {
    long ret = 0;
    for (T item : items) {
      ret += mappingFunction.apply(item).toLong();
    }
    return fromLong(ret);
  }

  public static Money sum(Money a, Money b) {
    return a.add(b);
  }

  public static Money min(Money a, Money b) {
    return a.cents <= b.cents ? a : b;
  }

  public static Money max(Money a, Money b) {
    return a.cents >= b.cents ? a : b;
  }

  public static Money fromLong(long totalCents) {
    return new Money(totalCents);
  }

  public static Money dollars(long dollars) {
    return new Money(dollars * 100);
  }

  public static Money fromDouble(Double d) {
    return d == null ? null : fromLong(Math.round(d * 100));
  }

  public static Money parse(String s) {
    if (isNullOrEmpty(s)) {
      return null;
    }
    double d = Double.parseDouble(moneyMatcher.removeFrom(s));
    d = d * 100;

    long n = Math.round(d);

    if (s.charAt(0) == '-' || s.charAt(0) == '–' || s.charAt(0) == '(') {
      n = -n;
    }

    return new Money(n);
  }

}
