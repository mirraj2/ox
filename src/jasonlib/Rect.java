package jasonlib;

import static java.lang.Integer.parseInt;
import java.awt.Rectangle;
import java.util.Iterator;
import com.google.common.base.Splitter;

public class Rect {
  public final double x, y, w, h;

  public Rect(double x, double y, double w, double h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Rect)) {
      return false;
    }
    Rect r = (Rect) obj;
    return x == r.x && y == r.y && w == r.w && h == r.h;
  }

  public boolean intersects(Rect other) {
    if ((x >= other.x + other.w) || (x + w <= other.x)) {
      return false;
    }
    if ((y >= other.y + other.h) || (y + h <= other.y)) {
      return false;
    }
    return true;
  }

  public boolean contains(double x, double y) {
    if (x < this.x || y < this.y) {
      return false;
    }

    if (x >= this.x + w || y >= this.y + h) {
      return false;
    }

    return true;
  }

  public boolean contains(Rect other) {
    return other.x >= x && other.y >= y && other.maxX() <= other.maxX() && other.maxY() <= maxY();
  }

  public Rect centerOn(double xx, double yy) {
    return new Rect(xx - w / 2, yy - h / 2, w, h);
  }

  public Rect doubleSize() {
    return new Rect(x - w / 2, y - h / 2, w * 2, h * 2);
  }

  public Rect grow(double vx, double vy) {
    return new Rect(x - vx, y - vy, w + vx * 2, h + vy * 2);
  }

  public Rect moveX(double vx) {
    return new Rect(x + vx, y, w, h);
  }

  public Rect moveY(double vy) {
    return new Rect(x, y + vy, w, h);
  }

  public Rect constrainSize(double ww, double hh) {
    return new Rect(x, y, Math.min(w, ww), Math.min(h, hh));
  }

  public Rect translate(double dx, double dy) {
    return new Rect(x + dx, y + dy, w, h);
  }

  public Rect location(double x, double y) {
    return new Rect(x, y, w, h);
  }

  public Rect scale(double scale) {
    return new Rect(x * scale, y * scale, w * scale, h * scale);
  }

  public int x() {
    return (int) x;
  }

  public int y() {
    return (int) y;
  }

  public int w() {
    return (int) w;
  }

  public int h() {
    return (int) h;
  }

  public double centerX() {
    return x + w / 2;
  }

  public double centerY() {
    return y + h / 2;
  }

  public double maxX() {
    return x + w;
  }

  public double maxY() {
    return y + h;
  }

  public Rectangle convert() {
    return new Rectangle((int) x, (int) y, (int) w, (int) h);
  }

  @Override
  public String toString() {
    return x + ", " + y + ", " + w + ", " + h;
  }

  public Rect resizeWithin(Rect r) {
    return moveWithin(r.x, r.y, r.w, r.h);
  }

  public Rect moveWithin(double bx, double by, double bw, double bh) {
    double newX = Math.max(x, bx);
    double newY = Math.max(y, by);
    newX = Math.min(newX, bx + bw - w);
    newY = Math.min(newY, by + bh - h);
    return new Rect(newX, newY, w, h);
  }

  public Rect expandToInclude(double x, double y) {
    double fromX = Math.min(this.x, x);
    double fromY = Math.min(this.y, y);
    double toX = Math.max(maxX(), x);
    double toY = Math.max(maxY(), y);
    return new Rect(fromX, fromY, toX - fromX, toY - fromY);
  }

  public String serialize() {
    return x() + " " + y() + " " + w() + " " + h();
  }

  public static Rect parse(String s) {
    Iterator<String> iter = Splitter.on(' ').split(s).iterator();
    return new Rect(parseInt(iter.next()), parseInt(iter.next()), parseInt(iter.next()),
        parseInt(iter.next()));
  }

}
