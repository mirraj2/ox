package jasonlib;

public class Lock {

  private Object lock = new Object();
  private int counter = 0;

  public void increment() {
    synchronized (lock) {
      counter++;
    }
  }

  public void decrement() {
    synchronized (lock) {
      counter--;
      if (counter == 0) {
        lock.notifyAll();
      }
    }
  }

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

}
