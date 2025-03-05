package ox.util;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import com.google.common.base.Splitter;

import ox.Log;
import ox.Reflection;
import ox.Threads;
import ox.x.XList;
import ox.x.XMap;
import ox.x.XOptional;

public abstract class AbstractConsole {

  private final XMap<String, Consumer<XList<String>>> handlers = XMap.create();

  public AbstractConsole() {
    XList<Method> publicMethods = XList.of(getClass().getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()));

    for (Method m : publicMethods) {
      Parameter[] params = m.getParameters();
      String command = m.getName().toLowerCase();
      String key = command + params.length;
      checkState(!handlers.containsKey(key), "Duplicate command " + command);
      handlers.put(key, args -> {
        XList<Object> input = XList.create();
        for (int i = 0; i < params.length; i++) {
          Parameter param = params[i];
          if (args.size() <= i) {
            checkState(param.getType() == XOptional.class, "Missing required argument: " + param.getName());
            input.add(XOptional.empty());
            continue;
          }
          input.add(convert(args.get(i), param));
        }
        try {
          m.invoke(this, input.toArray());
        } catch (Exception e) {
          throw propagate(e);
        }
      });
    }

    listen();
  }

  private Object convert(String s, Parameter p) {
    return Reflection.convert(s, p.getParameterizedType());
  }

  public boolean handle(XList<String> m) {
    if (m.isEmpty()) {
      Log.warn("No command entered.");
      return false;
    }

    String command = m.get(0).toLowerCase();
    int numArgs = m.size() - 1;
    Consumer<XList<String>> handler = handlers.get(command + numArgs);

    if (handler == null) {
      Log.warn("Unknown command: " + command);
      Log.debug("Available commands: " + handlers.keySet());
      return false;
    }

    XList<String> args = m.offset(1);
    if (args.size() == 1 && args.get(0).startsWith("\"") && args.get(0).endsWith("\"")) {
      args.set(0, args.get(0).substring(1, args.get(0).length() - 1)); // Remove surrounding quotes
    }

    handler.accept(args);
    Log.info("Finished command: " + command);

    return true;
  }

  private void listen() {
    Threads.run(() -> {
      while (true) {
        String line = System.console().readLine();
        if (line == null || line.trim().isEmpty()) {
          continue;
        }

        XList<String> m = XList.create(Splitter.onPattern("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)").split(line));

        try {
          handle(m);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
