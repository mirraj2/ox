package ox;

import static ox.util.Utils.propagate;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Threads {

  private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(16);

  public static void run(Runnable r) {
    pool.execute(wrap(r));
  }

  public static <T> Future<T> submit(Callable<T> c) {
    return pool.submit(c);
  }

  public static ThreadBuilder every(long n, TimeUnit unit) {
    ThreadBuilder ret = new ThreadBuilder();
    ret.n = n;
    ret.unit = unit;
    return ret;
  }

  private static void run(ThreadBuilder builder) {
    if (builder.unit == null) {
      pool.execute(builder.r);
    } else {
      pool.scheduleAtFixedRate(builder.r, builder.delay, builder.n, builder.unit);
    }
  }

  private static Runnable wrap(Runnable r) {
    return () -> {
      try {
        r.run();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    };
  }

  public static <T> Parallelizer<T> get(int numThreads) {
    return new Parallelizer<T>(numThreads);
  }

  public static class Parallelizer<T> {
    private Iterable<T> input;
    private final ExecutorService executor;
    private Throwable exception;

    private Parallelizer(int numThreads) {
      executor = Executors.newFixedThreadPool(numThreads);
    }

    @SuppressWarnings("unchecked")
    public <K> Parallelizer<K> input(Iterable<K> input) {
      this.input = (Iterable<T>) input;
      return (Parallelizer<K>) this;
    }

    @SuppressWarnings("unchecked")
    public <K> Parallelizer<K> input(K[] input) {
      this.input = (Iterable<T>) Arrays.asList(input);
      return (Parallelizer<K>) this;
    }

    public void execute(Runnable r) {
      executor.execute(() -> {
        try {
          r.run();
        } catch (Throwable t) {
          exception = t;
          t.printStackTrace();
        }
      });
    }

    public void await() {
      executor.shutdown();
      try {
        executor.awaitTermination(1000, TimeUnit.DAYS);
      } catch (InterruptedException e) {
        throw propagate(e);
      }
      if (exception != null) {
        throw new RuntimeException(exception);
      }
    }

    public void run(Consumer<T> callback) {
      Lock lock = new Lock();
      for (T o : input) {
        lock.increment();
        executor.execute(() -> {
          try {
            callback.accept(o);
          } catch (Throwable t) {
            // t.printStackTrace();
            exception = t;
          } finally {
            lock.decrement();
          }
        });
      }
      lock.await();
      executor.shutdown();
      if (exception != null) {
        throw new RuntimeException(exception);
      }
    }
  }

  public static class ThreadBuilder {
    public Runnable r;
    public long n;
    public TimeUnit unit;
    public long delay = 0;

    public void run(Runnable r) {
      this.r = wrap(r);
      Threads.run(this);
    }

    public ThreadBuilder delay(long delay) {
      this.delay = delay;
      return this;
    }
  }

}
