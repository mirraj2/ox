package ox;

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

  /**
   * Blocks until the counter is back to zero.
   */
  public void await() {
    synchronized (lock) {
      while (counter != 0) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          return;
        }
      }
    }
  }

  public int getCounter() {
    return counter;
  }

}
