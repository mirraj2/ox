package jasonlib.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class GSplitPane extends JSplitPane {

  public GSplitPane() {
    setUI(new BasicSplitPaneUI() {
      @Override
      public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this) {
          @Override
          public void paint(Graphics g) {
            g.setColor(Color.gray);
            if (getOrientation() == HORIZONTAL_SPLIT) {
              g.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
            } else {
              g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
            super.paint(g);
          }
        };
      }
    });
    setBorder(null);
    setContinuousLayout(true);
    setOpaque(false);
    setDividerSize(5);
  }

  public static GSplitPane topBottom(Component top, Component bottom) {
    GSplitPane ret = new GSplitPane();
    ret.setOrientation(JSplitPane.VERTICAL_SPLIT);
    ret.setTopComponent(top);
    ret.setBottomComponent(bottom);
    return ret;
  }

  public static GSplitPane leftRight(Component left, Component right) {
    GSplitPane ret = new GSplitPane();
    ret.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    ret.setLeftComponent(left);
    ret.setRightComponent(right);
    return ret;
  }

}
