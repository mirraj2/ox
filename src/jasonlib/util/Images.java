package jasonlib.util;

import jasonlib.IO;
import jasonlib.swing.Graphics3D;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import com.google.common.base.Stopwatch;
import static com.google.common.base.Preconditions.checkArgument;

public class Images {

  private static final Logger logger = Logger.getLogger(Images.class);

  private static final String path = "C:/shit/SOTK/sprites/";

  public static BufferedImage resize(BufferedImage bi, int w, int h) {
    BufferedImage ret = new BufferedImage(w, h, bi.getType());

    while (bi.getWidth() > w * 2) {
      bi = resize(bi, w * 2, h * 2);
    }

    Graphics3D g = Graphics3D.create(ret.createGraphics());
    g.linearInterpolation();
    g.draw(bi, 0, 0, w, h);
    g.dispose();

    return ret;
  }

  public static boolean isTransparent(BufferedImage bi, int x, int y, int w, int h) {
    int[] data = bi.getRGB(x, y, w, h, null, 0, w);
    for (int rgb : data) {
      int alpha = (rgb >> 24) & 0xff;
      if (alpha != 0) {
        return false;
      }
    }
    return true;
  }

  public static void changeAlpha(BufferedImage bi, int rgb, int alpha) {
    checkArgument(bi.getType() == BufferedImage.TYPE_INT_ARGB, "The image must have an alpha channel!");

    Color c = new Color(rgb);
    Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);

    switchRGB(bi, rgb, c2.getRGB());
  }

  public static void switchRGB(BufferedImage bi, int rgbFrom, int rgbTo) {
    Stopwatch watch = Stopwatch.createStarted();
    int[] data = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
    int switched = 0;
    for (int i = 0; i < data.length; i++) {
      if (data[i] == rgbFrom) {
        data[i] = rgbTo;
        switched++;
      }
    }
    bi.setRGB(0, 0, bi.getWidth(), bi.getHeight(), data, 0, bi.getWidth());
    logger.debug("Switched " + switched + " pixels in " + watch);
  }

  private static BufferedImage withAlpha(BufferedImage bi) {
    if (bi.getType() == BufferedImage.TYPE_INT_ARGB) {
      return bi;
    }
    BufferedImage ret = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics g = ret.createGraphics();
    g.drawImage(bi, 0, 0, null);
    g.dispose();
    return ret;
  }

  private static void makeTransparentBackground() {
    for (File f : new File(path).listFiles()) {
      String s = f.getName();
      if (s.endsWith(".out.png")) {
        continue;
      }

      s = s.substring(0, s.length() - 4);

      BufferedImage bi = IO.from(f).toImage();
      bi = withAlpha(bi);
      changeAlpha(bi, bi.getRGB(0, 0), 0);
      IO.from(bi).to(new File(path, s + ".out.png"));
    }
  }

  public static void main(String[] args) {
    BasicConfigurator.configure();

    makeTransparentBackground();
  }

}
