package jasonlib.swing.component;

import java.awt.Color;
import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JFrame;

public class GFrame extends JFrame {

  public GFrame() {
    this("");
  }

  public GFrame(String title) {
    super(title);

    size(800, 600);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setBackground(Color.white);
  }

  public GFrame start() {
    setVisible(true);
    return this;
  }

  public GFrame title(String title) {
    setTitle(title);
    return this;
  }

  public GFrame disposeOnClose() {
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    return this;
  }

  public GFrame centerOn(JComponent j) {
    setLocationRelativeTo(j);
    return this;
  }

  public GFrame content(JComponent content) {
    setContentPane(content);
    return this;
  }

  public GFrame size(int width, int height) {
    setSize(width, height);
    centerOn(null);
    return this;
  }

  @Override
  public void setContentPane(Container contentPane) {
    super.setContentPane(contentPane);

    revalidate();
    repaint();
  }

  public GFrame resizable(boolean b) {
    setResizable(b);
    return this;
  }

  public static GFrame create() {
    return new GFrame();
  }

}
