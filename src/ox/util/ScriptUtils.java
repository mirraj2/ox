package ox.util;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;

import ox.File;
import ox.IO;
import ox.Log;
import ox.x.XList;

public class ScriptUtils {

  public static String run(String s) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    run(s, baos);
    return IO.from(baos.toByteArray()).toString();
  }

  public static int runWithNoErrorCheck(String s) {
    Log.debug(s);
    XList<String> m = XList.of("/bin/sh", "-c", s);
    return run(m, false, (File) null);
  }

  public static String runZSH(String s) {
    return runZSH(s, null);
  }

  public static String runZSH(String s, File workingDir) {
    Log.debug(s);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XList<String> m = XList.of("/bin/zsh", "-c", "--login", "source ~/.zshrc;" + s);
    run(m, baos, workingDir);
    return IO.from(baos.toByteArray()).toString();
  }

  public static String runCmd(String s) {
    return runCmd(s, null);
  }

  public static String runCmd(String s, File workingDir) {
    Log.debug(s);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XList<String> m = XList.of("cmd.exe", "/c", s);
    run(m, baos, workingDir);
    return IO.from(baos.toByteArray()).toString();
  }

  public static int run(String command, File workingDir) {
    Log.debug(command);
    XList<String> m = XList.of("/bin/sh", "-c", command);
    return run(m, true, workingDir);
  }

  private static int run(XList<String> command, boolean errorCheck, File workingDir) {
    try {
      ProcessBuilder pb = new ProcessBuilder().command(command);
      if (workingDir != null) {
        pb.directory(workingDir.file);
      }
      int ret = pb.inheritIO().start().waitFor();
      if(errorCheck) {
        checkState(ret == 0, "Return code: " + ret + " on command: " + command);
      }
      return ret;
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  /**
   * Redirects the outputstream of the process to the given one.
   */
  public static void run(String s, OutputStream out) {
    Log.debug(s);
    XList<String> m = XList.of("/bin/sh", "-c", s);
    run(m, out);
  }

  private static void run(XList<String> m, OutputStream out) {
    run(m, out, null);
  }

  /**
   * Redirects the outputstream of the process to the given one.
   */
  private static void run(XList<String> m, OutputStream out, File workingDir) {
    int exitStatus;
    try {
      ProcessBuilder pb = new ProcessBuilder().command(m);
      if (workingDir != null) {
        pb.directory(workingDir.file);
      }

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
