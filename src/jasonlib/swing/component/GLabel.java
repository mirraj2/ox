package jasonlib.swing.component;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;

public class GLabel extends JLabel {

  public GLabel(Object o) {
    this(String.valueOf(o));
  }

  public GLabel(){
    this("");
  }

  public GLabel(String text) {
    super(text);
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

}
