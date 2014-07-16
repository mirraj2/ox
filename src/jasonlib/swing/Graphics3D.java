package jasonlib.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;

public class Graphics3D {

  private final Graphics2D g;

  private Graphics3D(Graphics2D g) {
    this.g = g;

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  }

  public Graphics3D setColor(int r, int g, int b) {
    return setColor(new Color(r, g, b));
  }

  public Graphics3D setColor(Color c) {
    g.setColor(c);
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

  public Graphics3D fill(Shape s) {
    g.fill(s);
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
