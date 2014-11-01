package jasonlib.swing.component;

import java.awt.LayoutManager;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

public class GPanel extends JPanel {

  public GPanel() {
    this(new MigLayout());
  }

  public GPanel(LayoutManager layout) {
    super(layout);
    setOpaque(false);
  }

  public void add(String label, String constraints) {
    add(new JLabel(label), constraints);
  }

}
