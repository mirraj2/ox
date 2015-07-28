package ox.swing.global;

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import ox.swing.component.GPanel;
import com.google.common.collect.Lists;

public class Components {

  public static void onShow(final Component c, final Runnable r) {
    c.addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if (c.isVisible()) {
          c.removeHierarchyListener(this);
          r.run();
        }
      }
    });
  }

  public static void setContent(Container a, JComponent b) {
    a.removeAll();
    a.add(b);
    refresh(a);
  }

  public static Window getWindow(Component c) {
    return getAncestorOfClass(Window.class, c);
  }

  public static JFrame getFrame(Component c) {
    return getAncestorOfClass(JFrame.class, c);
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

  public static <T extends Component> T getChildOfClass(Class<T> c, Component parent) {
    List<T> ret = Lists.newArrayList();
    getChildrenOfClass(c, (Container) parent, ret);
    if (ret.isEmpty()) {
      return null;
    }
    if (ret.size() > 1) {
      throw new RuntimeException("Found multiple children of class : " + c);
    }
    return ret.get(0);
  }

  public static <T> List<T> getChildrenOfClass(Class<T> c, Container parent) {
    List<T> ret = Lists.newArrayList();
    getChildrenOfClass(c, parent, ret);
    return ret;
  }

  @SuppressWarnings("unchecked")
  private static <T> void getChildrenOfClass(Class<T> c, Container parent, List<T> targetList) {
    for (int i = 0; i < parent.getComponentCount(); i++) {
      Component child = parent.getComponent(i);
      if (c.isAssignableFrom(child.getClass())) {
        targetList.add((T) child);
      }

      if (child instanceof Container) {
        getChildrenOfClass(c, (Container) child, targetList);
      }
    }
  }

  public static JComponent center(Component c) {
    JComponent ret = new GPanel();
    ret.add(Box.createHorizontalGlue(), "width 100%, span, wrap");
    ret.add(Box.createVerticalGlue(), "height 100%");
    ret.add(c, "alignx center, aligny center, width pref!, height pref!");
    return ret;
  }

  public static void refresh(Component component) {
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
