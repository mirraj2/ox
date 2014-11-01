package jasonlib.swing.component;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.border.Border;

public class GTextArea extends JTextArea {

  public GTextArea() {
    this(null);
  }

  public GTextArea(String text) {
    setLineWrap(true);
    setWrapStyleWord(true);
    setEditable(false);
    setText(text);
  }

  public GTextArea editable() {
    setEditable(true);
    return this;
  }

  public GTextArea rows(int rows) {
    setRows(rows);
    return this;
  }

  public GTextArea border() {
    Border border = BorderFactory.createLineBorder(Color.lightGray);
    setBorder(BorderFactory.createCompoundBorder(border,
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    return this;
  }

}
