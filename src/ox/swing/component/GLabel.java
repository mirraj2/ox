package ox.swing.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class GLabel extends JLabel {

  public GLabel(Image i) {
    super(new ImageIcon(i));
  }

  public GLabel(Object o) {
    this(String.valueOf(o));
  }

  public GLabel() {
    this("");
  }

  public GLabel(String text) {
    super(text);
  }

  public GLabel font(String name, int size) {
    setFont(new Font(name, getFont().getStyle(), size));
    return this;
  }

  public GLabel bold() {
    setFont(getFont().deriveFont(Font.BOLD));
    return this;
  }

  public GLabel center() {
    setHorizontalAlignment(CENTER);
    return this;
  }

  public GLabel color(Color c) {
    setForeground(c);
    return this;
  }

  public GLabel click(Runnable callback) {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        callback.run();
      }
    });
    return this;
  }

}
