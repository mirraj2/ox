package jasonlib.swing;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class GFrame extends JFrame {

  public GFrame() {
    this("", null);
  }

  public GFrame(String title, JComponent content) {
    this(title, content, 1200, 800);
  }

  public GFrame(String title, JComponent content, int width, int height) {
    super(title);

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setContentPane(content);
    setSize(width, height);

    setLocationRelativeTo(null);
  }

  public GFrame start() {
    setVisible(true);
    return this;
  }

}
