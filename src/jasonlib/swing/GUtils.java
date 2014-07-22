package jasonlib.swing;

import java.awt.image.BufferedImage;

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

}
