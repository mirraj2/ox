package ox.swing.component;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JComponent;
import javax.swing.JFrame;
import ox.swing.global.GFocus;

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
    GFocus.focus(getContentPane());
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

  public GFrame doNothingOnClose() {
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    return this;
  }

  public GFrame alwaysOnTop() {
    setAlwaysOnTop(true);
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

  public GFrame maximize() {
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(dim.width, dim.height);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
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

  public GFrame onClose(Runnable r) {
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        r.run();
      }
    });
    return this;
  }

  public static GFrame create() {
    return new GFrame();
  }

}
