package ox.swing.component;

import java.awt.Graphics;
import javax.swing.JComponent;
import ox.swing.Graphics3D;

public abstract class GCanvas extends JComponent {

  @Override
  protected final void paintComponent(Graphics g) {
    render(Graphics3D.create(g));
  }
  
  protected abstract void render(Graphics3D g);
  
}
