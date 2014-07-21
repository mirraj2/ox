package jasonlib.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.google.common.collect.Lists;

public class GUtils {

  public static void focus(JComponent component) {
    JFrame frame = getAncestorOfClass(component, JFrame.class);
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
  public static <T> T getAncestorOfClass(JComponent c, Class<T> clazz) {
    Component current = c;
    while (current != null) {
      if (clazz.isAssignableFrom(current.getClass())) {
        return (T) current;
      }
      current = current.getParent();
    }
    return null;
  }

  public static void focusBestChild(JComponent component) {
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

  public static BufferedImage scale(BufferedImage bi, double scale) {
    int w = (int) (bi.getWidth() * scale);
    int h = (int) (bi.getHeight() * scale);
    BufferedImage ret = new BufferedImage(w, h, bi.getType());

    Graphics3D g = Graphics3D.create(ret.createGraphics());

    g.draw(bi, 0, 0, w, h);

    g.dispose();

    return ret;
  }

}
