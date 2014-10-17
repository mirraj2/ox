package jasonlib.swing.global;

import jasonlib.Log;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import com.google.common.collect.Lists;

public class GFocus {

  private static boolean debug = false;

  static {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (debug) {
          Object newOwner = evt.getNewValue();
          Log.debug("Focus Owner: " + (newOwner == null ? "null" : newOwner.getClass()));
        }
      }
    });
  }

  public static void debug() {
    debug = true;
  }

  public static void focus(final Component component) {
    component.requestFocus();

    if (component.hasFocus()) {
      return;
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        component.requestFocus();
      }
    });
  }

  public static void focusBestChild(Container component) {
    List<JComponent> children = Lists.newArrayList();
    recurse(component, children);

    JComponent toFocus = null;
    for (JComponent child : children) {
      if (child instanceof JTextComponent) {
        toFocus = child;
        break;
      }
    }

    if (toFocus != null) {
      focus(toFocus);
    }
  }

  private static void recurse(Container j, List<JComponent> list) {
    for (int i = 0; i < j.getComponentCount(); i++) {
      Component c = j.getComponent(i);
      if (c instanceof Container) {
        if (c instanceof JComponent) {
          list.add((JComponent) c);
        }
        recurse((Container) c, list);
      }
    }
  }

}
