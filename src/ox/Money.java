package ox;

import static ox.util.Utils.format;
import static ox.util.Utils.money;
import static ox.util.Utils.parseMoney;
import java.util.function.Function;

public class Money {

  public static final Money ZERO = Money.dollars(0);

  private final int cents;

  private Money(int cents) {
    this.cents = cents;
  }

  public Money negate() {
    return new Money(-cents);
  }

  public Money add(Money m) {
    return new Money(cents + m.cents);
  }

  public Money multiply(int n) {
    return new Money(cents * n);
  }

  public Money multiply(double n) {
    return new Money((int) (cents * n));
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

  public int toInt() {
    return cents;
  }

  public double toDouble() {
    return cents / 100.0;
  }

  @Override
  public String toString() {
    return money(toDouble());
  }

  public int getDollars() {
    return cents / 100;
  }

  public int getCents() {
    return cents % 100;
  }

  public String dollarsFormatted() {
    return format(getDollars());
  }

  public String centsFormatted() {
    int n = getCents();
    return n < 10 ? "0" + n : "" + n;
  }

  public static <T> Money sum(Iterable<T> items, Function<T, Money> mappingFunction) {
    int ret = 0;
    for (T item : items) {
      ret += mappingFunction.apply(item).toInt();
    }
    return fromInt(ret);
  }

  public static Money fromInt(int totalCents) {
    return new Money(totalCents);
  }

  public static Money dollars(int dollars) {
    return new Money(dollars * 100);
  }

  public static Money fromDouble(double d) {
    return fromInt((int) (d * 100));
  }

  public static Money parse(String s) {
    return fromDouble(parseMoney(s));
  }

}
