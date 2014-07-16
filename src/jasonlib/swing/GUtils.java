package jasonlib.swing;

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class GUtils {

  public static void focus(JComponent component) {
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
