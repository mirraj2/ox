package jasonlib.swing.global;

import static com.google.common.base.Preconditions.checkNotNull;
import jasonlib.swing.Graphics3D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

public class GUtils {

  public static BufferedImage scale(BufferedImage bi, double scale) {
    int w = (int) (bi.getWidth() * scale);
    int h = (int) (bi.getHeight() * scale);
    BufferedImage ret = new BufferedImage(w, h, bi.getType());

    Graphics3D g = Graphics3D.create(ret.createGraphics());

    g.draw(bi, 0, 0, w, h);

    g.dispose();

    return ret;
  }

  public static void setWindowLocationRelativeTo(Window popup, JFrame frame) {
    checkNotNull(popup, "window");
    checkNotNull(frame, "target");

    GraphicsConfiguration gc = getGraphicsConfiguration(frame.getBounds());

    Point p = frame.getLocationOnScreen();

    Rectangle r =
        new Rectangle(p.x + (frame.getWidth() - popup.getWidth()) / 2,
            p.y + (frame.getHeight() - popup.getHeight()) / 2, popup.getWidth(),
            popup.getHeight());

    Rectangle monitorBounds = gc.getBounds();

    // constrain the target location to the monitor's bounds.
    r.x = Math.max(r.x, monitorBounds.x);
    r.x = (int) (Math.min(r.getMaxX(), monitorBounds.getMaxX()) - r.width);
    r.y = Math.max(r.y, monitorBounds.y);
    r.y = (int) (Math.min(r.getMaxY(), monitorBounds.getMaxY()) - r.height);

    popup.setBounds(r);
  }

  public static GraphicsConfiguration getGraphicsConfiguration(Rectangle bounds) {
    GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();

    GraphicsDevice[] devices = e.getScreenDevices();

    if (devices.length == 0) {
      return null;
    }

    GraphicsConfiguration bestConfiguration = null;
    int bestIntersectionScore = 0;

    for (GraphicsDevice gd : devices) {
      GraphicsConfiguration defaultConfiguration = gd.getDefaultConfiguration();
      Rectangle intersection = defaultConfiguration.getBounds().intersection(bounds);
      int intersectionScore = intersection.width * intersection.height;
      if (bestConfiguration == null || intersectionScore > bestIntersectionScore) {
        bestConfiguration = defaultConfiguration;
        bestIntersectionScore = intersectionScore;
      }
    }

    return bestConfiguration;
  }

}
