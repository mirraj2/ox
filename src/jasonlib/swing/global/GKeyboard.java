package jasonlib.swing.global;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class GKeyboard {

  private static final Logger logger = Logger.getLogger(GKeyboard.class);

  private static final Multimap<Component, KeyListener> componentKeyListenerMap = ArrayListMultimap.create();
  private static final List<KeyListener> listeners = Lists.newCopyOnWriteArrayList();
  private static final Stack<Component> componentStack = new Stack<Component>();
  private static final boolean[] keys = new boolean[256];

  public static void addListener(KeyListener listener) {
    listeners.add(listener);
  }

  public static void addKeyListener(Component component, KeyListener keyListener) {
    checkNotNull(component, "component");
    checkNotNull(keyListener, "keyListener");

    componentKeyListenerMap.put(component, keyListener);
  }

  public static void removeKeyListener(Component component, KeyListener keyListener) {
    checkNotNull(component, "component");
    checkNotNull(keyListener, "keyListener");

    componentKeyListenerMap.remove(component, keyListener);
  }

  public static void removeAllListeners(Component c) {
    componentKeyListenerMap.removeAll(c);
    if (c instanceof Container) {
      for (Component cc : ((Container) c).getComponents()) {
        removeAllListeners(cc);
      }
    }
  }

  public static boolean isKeyDown(int keycode) {
    return keys[keycode];
  }

  public static boolean isSystemShortcut(int mod) {
    int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    return (mask & mod) == mask;
  }

  private static void goUpHierarchyNotifyingListeners(KeyEvent event, Component deepestComponent) {
    Component current = deepestComponent;
    while (current != null) {
      componentStack.add(current);
      current = current.getParent();
    }

    while (!componentStack.isEmpty() && !event.isConsumed()) {
      current = componentStack.pop();
      Collection<KeyListener> listeners = componentKeyListenerMap.get(current);
      if (!listeners.isEmpty()) {
        Object oldSource = event.getSource();
        event.setSource(current);
        notifyListeners(listeners, event);
        event.setSource(oldSource);
      }
    }

    componentStack.clear();
  }

  private static void notifyListeners(Collection<KeyListener> listeners, KeyEvent event) {
    KeyListener[] listenerBuffer = listeners.toArray(new KeyListener[listeners.size()]);

    for (KeyListener listener : listenerBuffer) {
      try {
        notifyListener(listener, event);
      } catch (Exception e) {
        logger.error("", e);
      }
    }
  }

  private static void notifyListener(KeyListener listener, KeyEvent event) {
    int id = event.getID();

    if (id == KeyEvent.KEY_PRESSED) {
      listener.keyPressed(event);
    } else if (id == KeyEvent.KEY_RELEASED) {
      listener.keyReleased(event);
    } else if (id == KeyEvent.KEY_TYPED) {
      listener.keyTyped(event);
    }
  }

  private static final AWTEventListener globalKeyListener = new AWTEventListener() {
    @Override
    public void eventDispatched(AWTEvent a) {
      notifyListeners(listeners, (KeyEvent) a);
    }
  };

  static {
    Toolkit.getDefaultToolkit().addAWTEventListener(globalKeyListener, KeyEvent.KEY_EVENT_MASK);

    addListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (isSystemShortcut(e.getModifiers())) {
          return;
        }

        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
          keys[code] = true;
        }

        goUpHierarchyNotifyingListeners(e, e.getComponent());
      }

      @Override
      public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
          keys[code] = false;
        }
      }
    });
  }

}
