package ox;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.count;
import static ox.util.Utils.only;
import static ox.util.Utils.propagate;
import static ox.util.Utils.sleep;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import ox.x.XList;

public class Threads {

  private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(16,
      new NamedThreadFactory(Threads.class, "pool"));

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
        Log.error(t);
      }
    };
  }

  public static <T> Parallelizer<T> get(int numThreads) {
    return new Parallelizer<T>(numThreads);
  }

  public static class Parallelizer<T> {
    private List<T> input;
    private final ExecutorService executor;
    private Throwable exception;
    private boolean failFast = false;
    private Duration timeout = null;

    private Parallelizer(int numThreads) {
      checkState(numThreads > 0, "numThreads=" + numThreads);
      executor = Executors.newFixedThreadPool(numThreads, new NamedThreadFactory(Threads.class, "Parallelizer"));
    }

    @SuppressWarnings("unchecked")
    public <K> Parallelizer<K> input(List<K> input) {
      this.input = (List<T>) input;
      return (Parallelizer<K>) this;
    }

    @SuppressWarnings("unchecked")
    public <K> Parallelizer<K> input(K[] input) {
      this.input = (List<T>) Arrays.asList(input);
      return (Parallelizer<K>) this;
    }

    public void execute(Runnable r) {
      executor.execute(() -> {
        try {
          r.run();
        } catch (Throwable t) {
          exception = t;
          Log.error(t);
        }
      });
    }

    public void await() {
      executor.shutdown();
      while (true) {
        if (failFast && exception != null) {
          break;
        }
        if (executor.isTerminated()) {
          break;
        }
        sleep(30);
      }
      if (exception != null) {
        throw propagate(exception);
      }
    }

    public void run(Consumer<? super T> callback) {
      List<Throwable> exceptions = Lists.newCopyOnWriteArrayList();

      // 0 = not yet run, 1 = running, 2 = completed
      int[] states = new int[input.size()];

      Lock lock = new Lock();
      for (int i = 0; i < input.size(); i++) {
        final int index = i;
        T o = input.get(i);
        lock.increment();
        executor.execute(() -> {
          states[index]++;
          try {
            callback.accept(o);
          } catch (Throwable t) {
            if (failFast) {
              Log.error(t);
            }
            Log.error("Problem with input: " + o);
            exceptions.add(t);
          } finally {
            states[index]++;
            lock.decrement();
          }
        });
      }
      boolean timedOut = !lock.await(timeout);
      executor.shutdown();
      if (!exceptions.isEmpty()) {
        if (exceptions.size() == 1) {
          throw new RuntimeException(only(exceptions));
        }
        exceptions.forEach(Log::error);
        throw new RuntimeException("Found " + exceptions.size() + " exceptions.");
      }
      if (timedOut) {
        XList<T> itemsTimedOut = count(0, input.size() - 1).filter(i -> states[i] == 1).map(i -> input.get(i));
        throw new RuntimeException("Timed out. Did not finish: " + itemsTimedOut.join(", "));
      }
    }

    public Parallelizer<T> timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Parallelizer<T> failFast() {
      this.failFast = true;
      return this;
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
