package jasonlib.swing;

import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class GFrame extends JFrame {

  public GFrame() {
    this("");
  }

  public GFrame(String title) {
    super(title);

    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  public GFrame start() {
    setVisible(true);
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

}
