package ox;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.normalize;
import static ox.util.Utils.sleep;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import ox.x.XOptional;

public class Await {

  private static final Duration MIN_TIME_BETWEEN_LOGS = Duration.ofSeconds(2);

  private Duration timeBetweenChecks;
  private XOptional<Duration> timeout = XOptional.empty();
  private String taskName = "";

  private Await(Duration timeBetweenChecks) {
    this.timeBetweenChecks = checkNotNull(timeBetweenChecks);
  }

  public Await timeout(Duration timeout) {
    this.timeout = XOptional.of(timeout);
    return this;
  }

  public Await verbose(String taskName) {
    this.taskName = normalize(taskName);
    return this;
  }

  public void await(Supplier<Boolean> callback) {
    XOptional<Instant> timeoutInstant = timeout.map(t -> Instant.now().plus(t));
    
    Instant lastLogTime = null;
    while (true) {
      if (callback.get()) {
        return;
      }
      timeoutInstant.ifPresent(t -> {
        checkState(Instant.now().isBefore(t), "await() call timed out after " + timeout);
      });
      if (!taskName.isEmpty()
          && (lastLogTime == null || lastLogTime.plus(MIN_TIME_BETWEEN_LOGS).isBefore(Instant.now()))) {
        lastLogTime = Instant.now();
        Log.debug("Awaiting " + taskName);
      }
      sleep(timeBetweenChecks.toMillis());
    }
  }

  public static Await every(Duration timeBetweenChecks) {
    return new Await(timeBetweenChecks);
  }

}
