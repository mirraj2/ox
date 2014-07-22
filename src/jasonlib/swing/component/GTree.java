package jasonlib.swing.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;

public class GTree extends JTree {

  public GTree(TreeModel treeModel) {
    super(treeModel);

    removeIcons();

    addMouseListener(mouseListener);
  }

  @Override
  public void setRootVisible(boolean rootVisible) {
    super.setRootVisible(rootVisible);
    setShowsRootHandles(!rootVisible);
  }

  public void removeIcons() {
    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
    renderer.setLeafIcon(null);
    renderer.setClosedIcon(null);
    renderer.setOpenIcon(null);
  }

  public void expandAll() {
    for (int i = 0; i < getRowCount(); i++) {
      expandRow(i);
    }
  }

  private final MouseAdapter mouseListener = new MouseAdapter() {
    public void mousePressed(MouseEvent e) {
      int row = getRowForLocation(e.getX(), e.getY());
      if (row == -1) {
        clearSelection();
      }
    };
  };

}
