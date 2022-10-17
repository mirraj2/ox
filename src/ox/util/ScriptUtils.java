package ox.util;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.collect.Lists;

import ox.IO;
import ox.Log;
import ox.x.XList;

public class ScriptUtils {

  public static void run(String s) {
    Log.debug(s);
    XList<String> m = XList.of("/bin/sh", "-c", s);
    checkState(0 == run(m, true), "Problem with command: " + s);
  }

  public static int runWithNoErrorCheck(String s) {
    Log.debug(s);
    XList<String> m = XList.of("/bin/sh", "-c", s);
    return run(m, false);
  }

  public static void runZSH(String s) {
    Log.debug(s);
    XList<String> m = XList.of("/bin/zsh", "-c", "--login", "source ~/.zshrc;" + s);
    checkState(0 == run(m, true));
  }

  private static int run(XList<String> command, boolean checkError) {
    try {
      ProcessBuilder pb = new ProcessBuilder().command(command);
      return pb.inheritIO().start().waitFor();
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  /**
   * Redirects the outputstream of the process to the given one.
   */
  public static void run(String s, OutputStream out) {
    Log.debug(s);
    List<String> m = Lists.newArrayList("/bin/sh", "-c", s);
    int exitStatus;
    try {
      ProcessBuilder pb = new ProcessBuilder().command(m);
      pb.redirectError(Redirect.INHERIT);
      Process process = pb.start();
      IO.from(process.getInputStream()).to(out);
      exitStatus = process.waitFor();
    } catch (Exception e) {
      throw propagate(e);
    }
    checkState(exitStatus == 0, "Status: " + exitStatus);
  }

  public static String runAndgetOutput(String command) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    run(command, baos);
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

}
