package jasonlib.swing.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.JButton;

public class GButton extends JButton {

  public GButton(Action action) {
    super(action);

    init();
  }

  public GButton(String text) {
    super(text);

    init();
  }

  private void init() {
    setFocusable(false);
  }

  public GButton click(final Runnable r) {
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        r.run();
      }
    });
    return this;
  }

  public void setDefault(boolean b) {
  }
}
