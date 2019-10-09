package ox;

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
    if (o == null || !(o instanceof Pair<?, ?>)) {
      return false;
    }
    Pair<?, ?> other = (Pair<?, ?>) o;
    return this.a.equals(other.a) && this.b.equals(other.b);
  }

}
