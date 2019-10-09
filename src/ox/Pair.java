package ox;

import com.google.common.base.Objects;

public class Pair<A, B> {

  public final A a;
  public final B b;

  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<>(a, b);
  }

  @Override
  public String toString() {
    return "(" + a + "," + b + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Pair<?, ?>)) {
      return false;
    }
    Pair<?, ?> that = (Pair<?, ?>) o;
    return Objects.equal(this.a, that.a) && Objects.equal(this.b, that.b);
  }

}
