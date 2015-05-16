package jasonlib.swing.component;

import jasonlib.swing.Graphics3D;
import java.awt.Graphics;
import javax.swing.JComponent;

public abstract class GCanvas extends JComponent {

  @Override
  protected final void paintComponent(Graphics g) {
    render(Graphics3D.create(g));
  }
  
  protected abstract void render(Graphics3D g);
  
}
