package ox.util;

import com.google.common.base.Stopwatch;

import ox.Log;

public class Perf {

  public static void test(int numTrials, int numExecutionsPerTrial, Runnable function1, Runnable function2) {
    for (int trial = 0; trial < numTrials; trial++) {
      Log.debug("Trial #" + trial);

      Stopwatch watch = Stopwatch.createStarted();
      for (int i = 0; i < numExecutionsPerTrial; i++) {
        function1.run();
      }
      Log.debug("function1 took " + watch);

      watch = Stopwatch.createStarted();
      for (int i = 0; i < numExecutionsPerTrial; i++) {
        function2.run();
      }
      Log.debug("function2 took " + watch);

      Log.debug("------");
    }
  }

}
