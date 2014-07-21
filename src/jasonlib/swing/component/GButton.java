package jasonlib.swing.component;

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

}
