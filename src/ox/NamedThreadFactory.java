package ox;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;
  private boolean daemon = false;

  public NamedThreadFactory(Class<?> creatorClass) {
    this(creatorClass, "");
  }

  public NamedThreadFactory(String name) {
    this(null, name);
  }

  public NamedThreadFactory(Class<?> creatorClass, String name) {
    StringBuilder sb = new StringBuilder(32);
    if (creatorClass != null) {
      sb.append(creatorClass.getName());
    }
    if (!name.isEmpty()) {
      if (sb.length() > 0) {
        sb.append('-');
      }
      sb.append(name);
    }
    sb.append('-');
    this.namePrefix = sb.toString();
  }

  public NamedThreadFactory daemon() {
    daemon = true;
    return this;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread ret = new Thread(r, namePrefix + threadNumber.getAndIncrement());
    if (daemon) {
      ret.setDaemon(daemon);
    }
    return ret;
  }

}
