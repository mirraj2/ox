package jasonlib.swing;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import com.google.common.collect.Lists;

public class GKeyboard {

  private static final List<KeyListener> listeners = Lists.newCopyOnWriteArrayList();

  private static final boolean[] keys = new boolean[256];

  private static final AWTEventListener globalKeyListener = new AWTEventListener() {
    @Override
    public void eventDispatched(AWTEvent a) {
      KeyEvent e = (KeyEvent) a;
      for (KeyListener listener : listeners) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
          listener.keyPressed(e);
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
          listener.keyReleased(e);
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
          listener.keyTyped(e);
        }
      }
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

  public static void addListener(KeyListener listener) {
    listeners.add(listener);
  }

  public static boolean isKeyDown(int keycode) {
    return keys[keycode];
  }

  public static boolean isSystemShortcut(int mod) {
    int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    return (mask & mod) == mask;
  }

}
