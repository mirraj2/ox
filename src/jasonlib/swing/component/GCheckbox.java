package jasonlib.swing.component;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;

public class GCheckbox extends JCheckBox {

  public GCheckbox(String text) {
    super(text);
  }

  public GCheckbox onChange(Runnable callback) {
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        callback.run();
      };
    });
    return this;
  }

  public GCheckbox color(Color c) {
    setForeground(c);
    return this;
  }

}
