package jasonlib.swing.component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

public class GPanel extends JPanel {

  public GPanel() {
    setLayout(new MigLayout());
    setOpaque(false);
  }

  public void add(String label, String constraints) {
    add(new JLabel(label), constraints);
  }

}
