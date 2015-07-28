package ox.util;

import static com.google.common.base.Preconditions.checkArgument;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import ox.Log;
import ox.swing.Graphics3D;
import com.google.common.base.Stopwatch;

public class Images {

  // public static void main(String[] args) {
  // File dir = new File("/users/jason/desktop/forge/rez");
  // File target = new File("/Users/jason/.forge/armory/0.png");
  // BufferedImage bigImage = IO.from(target).toImage();
  //
  // for (int id : new int[] { 6, 8, 9, 10 }) {
  // File f = new File(dir, id + ".png");
  // BufferedImage bi = IO.from(f).toImage();
  // bigImage = appendToBottom(bigImage, bi);
  // }
  // IO.from(bigImage).to(target);
  // }

  // private static BufferedImage appendToBottom(BufferedImage a, BufferedImage b) {
  // BufferedImage bi = new BufferedImage(a.getWidth(), a.getHeight() + b.getHeight(), BufferedImage.TYPE_INT_ARGB);
  // Graphics2D g = bi.createGraphics();
  // g.drawImage(a, 0, 0, null);
  // g.drawImage(b, 0, a.getHeight(), null);
  // g.dispose();
  // return bi;
  // }

  /**
   * Corrects the type to INT_ARGB
   */
  public static BufferedImage convertType(BufferedImage bi) {
    if (bi.getType() == BufferedImage.TYPE_INT_ARGB) {
      return bi;
    }

    BufferedImage ret = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = ret.createGraphics();
    g.drawImage(bi, 0, 0, null);
    g.dispose();
    return ret;
  }

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
    Log.debug("Switched " + switched + " pixels in " + watch);
  }

  public static BufferedImage rotate(BufferedImage img, double angle) {
    if (angle == 0) {
      return img;
    }

    double sin = Math.abs(Math.sin(Math.toRadians(angle))), cos = Math.abs(Math.cos(Math.toRadians(angle)));

    int w = img.getWidth(), h = img.getHeight();

    int newWidth = (int) Math.floor(w * cos + h * sin);
    int newHeight = (int) Math.floor(h * cos + w * sin);

    BufferedImage ret = new BufferedImage(newWidth, newHeight, img.getType());
    Graphics2D g = ret.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    g.translate((newWidth - w) / 2, (newHeight - h) / 2);
    g.rotate(Math.toRadians(angle), w / 2, h / 2);
    g.drawRenderedImage(img, null);
    g.dispose();

    return ret;
  }

}
