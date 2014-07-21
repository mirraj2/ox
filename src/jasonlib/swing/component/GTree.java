package jasonlib.swing.component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;

public class GTree extends JTree {

  public GTree(TreeModel treeModel) {
    super(treeModel);

    removeIcons();
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

}
