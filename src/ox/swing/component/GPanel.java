package ox.swing.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import net.miginfocom.swing.MigLayout;

public class GPanel extends JPanel implements Scrollable {

  public GPanel() {
    this(new MigLayout());
  }

  public GPanel(LayoutManager layout) {
    super(layout);
    setOpaque(false);
    setFocusable(true);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
      }
    });
  }

  @Override
  public void setBackground(Color color) {
    super.setBackground(color);
    setOpaque(true);
  }

  public void add(String label, String constraints) {
    add(new JLabel(label), constraints);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 32;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 32 * 10;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  public void onResize(Runnable r) {
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        r.run();
      }
    });
  }

}
