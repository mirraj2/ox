package ox.swing.component;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class GScrollPane extends JScrollPane {

  public GScrollPane(JComponent view) {
    super(view);
    setOpaque(false);
    getViewport().setOpaque(false);
    setBorder(null);
    setViewportBorder(null);
    setMinimumSize(new Dimension());
  }

}
