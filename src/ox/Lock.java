package ox;

import java.time.Duration;
import java.time.Instant;

/**
 * A simple lock based on a counter.
 */
public class Lock {

  private Object lock = new Object();
  private int counter = 0;

  public Lock() {
    this(0);
  }

  public Lock(int counter) {
    this.counter = counter;
  }

  /**
   * Increases the counter by one.
   */
  public void increment() {
    synchronized (lock) {
      counter++;
    }
  }

  /**
   * Decreases the counter by one.
   */
  public void decrement() {
    synchronized (lock) {
      counter--;
      if (counter == 0) {
        lock.notifyAll();
      }
    }
  }

  public void set(int counter) {
    synchronized (lock) {
      this.counter = counter;
      if (counter == 0) {
        lock.notifyAll();
      }
    }
  }

  public void await() {
    await(null);
  }

  /**
   * Blocks until the counter is back to zero.
   *
   * @return false if we timed out.
   */
  public boolean await(Duration timeout) {
    Instant t = timeout == null ? null : Instant.now().plus(timeout);
    synchronized (lock) {
      while (counter > 0) {
        if (t != null && Instant.now().isAfter(t)) {
          return false;
        }
        try {
          lock.wait(timeout == null ? 0 : timeout.toMillis());
        } catch (InterruptedException e) {
        }
      }
    }
    return true;
  }

  public int getCounter() {
    return counter;
  }

}
