package jasonlib.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

public class Graphics3D {

  private final Graphics2D g;

  private Graphics3D(Graphics2D g) {
    this.g = g;

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // this makes things SUPER SLOW on Mac OS
    // g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  }

  public Graphics3D color(int r, int g, int b) {
    return color(r, g, b, 255);
  }

  public Graphics3D color(int r, int g, int b, int a) {
    return color(new Color(r, g, b, a));
  }

  public Graphics3D color(Color c) {
    g.setColor(c);
    return this;
  }

  public Graphics3D line(int x1, int y1, int x2, int y2) {
    g.drawLine(x1, y1, x2, y2);
    return this;
  }

  public Graphics3D draw(Shape s) {
    g.draw(s);
    return this;
  }

  public Graphics3D draw(Image i, double x, double y) {
    g.drawImage(i, (int) x, (int) y, null);
    return this;
  }

  public Graphics3D draw(Image i, double x, double y, double w, double h) {
    g.drawImage(i, (int) x, (int) y, (int) w, (int) h, null);
    return this;
  }


  public Graphics3D draw(Image i, double dx, double dy, Rectangle sourceRegion) {
    Rectangle r = sourceRegion;
    return draw(i, dx, dy, r.x, r.y, r.width, r.height);
  }

  public Graphics3D draw(Image i, double dx1, double dy1, double sx1, double sy1, int w, int h) {
    g.drawImage(i, (int) dx1, (int) dy1, (int) dx1 + w, (int) dy1 + h,
        (int) sx1, (int) sy1, (int) sx1 + w, (int) sy1 + h, null);
    return this;
  }

  public Graphics3D draw(Image i, double dx1, double dy1, double dx2, double dy2, double sx1, double sy1, double sx2,
      double sy2) {
    g.drawImage(i, (int) dx1, (int) dy1, (int) dx2, (int) dy2, (int) sx1, (int) sy1, (int) sx2, (int) sy2, null);
    return this;
  }

  public Graphics3D fill(Shape s) {
    g.fill(s);
    return this;
  }

  public Graphics3D fillRect(double x, double y, double w, double h) {
    g.fillRect((int) x, (int) y, (int) w, (int) h);
    return this;
  }

  public Graphics3D translate(double x, double y) {
    g.translate(x, y);
    return this;
  }

  public Graphics3D clip(Shape s) {
    g.clip(s);
    return this;
  }

  public Graphics3D clearClip() {
    g.setClip(null);
    return this;
  }

  public Graphics3D copy(){
    return create(g.create());
  }

  public Graphics3D dispose() {
    g.dispose();
    return this;
  }

  public Graphics3D setStroke(double thickness) {
    g.setStroke(new BasicStroke((float) thickness));
    return this;
  }

  public Graphics3D alpha(double alpha) {
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
    return this;
  }

  public static Graphics3D create(Graphics g) {
    return new Graphics3D((Graphics2D) g);
  }

}
