package jasonlib.swing.global;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.google.common.collect.Lists;

public class Components {

  public static void onShow(final Component c, final Runnable r) {
    c.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        c.removeComponentListener(this);
        r.run();
      }
    });
  }

  public static void focus(JComponent component) {
    JFrame frame = getAncestorOfClass(JFrame.class, component);
    if (frame != null) {
      frame.toFront();
    }

    component.requestFocusInWindow();
    component.addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        final Component c = e.getComponent();
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            c.requestFocusInWindow();
          }
        });
        c.removeHierarchyListener(this);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public static <T extends Component> T getAncestorOfClass(Class<T> clazz, Component comp) {
    checkNotNull(clazz, "clazz");
    checkNotNull(comp, "comp");

    while (comp != null && !clazz.isAssignableFrom(comp.getClass())) {
      if (comp instanceof JPopupMenu) {
        comp = ((JPopupMenu) comp).getInvoker();
      } else {
        comp = comp.getParent();
      }
    }

    return (T) comp;
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

  public static void refresh(Container component) {
    component.revalidate();
    component.validate();
    component.repaint();
  }

  /**
   * Links windows so that when one is shown, all are shown.
   */
  public static void linkWindows(final Window... windows) {
    final WindowFocusListener listener = new WindowFocusListener() {
      long lastFocusGainTime;
      @Override
      public void windowGainedFocus(WindowEvent e) {
        // avoid an infinite loop of focus events
        if (System.currentTimeMillis() - lastFocusGainTime < 1000) {
          return;
        }

        lastFocusGainTime = System.currentTimeMillis();
        for (Window window : windows) {
          window.toFront();
        }
        e.getWindow().toFront();
      }
      @Override
      public void windowLostFocus(WindowEvent e) {
      }
    };
    for (Window window : windows) {
      window.addWindowFocusListener(listener);
    }
  }

}
