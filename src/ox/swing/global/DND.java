package ox.swing.global;

import static com.google.common.collect.Iterables.getOnlyElement;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.Collection;
import ox.swing.DragListener;
import com.google.common.base.Throwables;

public class DND {

  public static void addDragListener(Component c, final DragListener dragListener) {
    new DropTarget(c, new DropTargetListener() {
      @Override
      public void drop(DropTargetDropEvent d) {
        try {
          Transferable t = d.getTransferable();
          DataFlavor flavor = getBestFlavor(t.getTransferDataFlavors());

          if (flavor == null) {
            d.rejectDrop();
            return;
          }

          d.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

          Object data;
          try {
            data = t.getTransferData(flavor);
          } catch (UnsupportedFlavorException | IOException e) {
            throw Throwables.propagate(e);
          }

          if (data instanceof Collection) {
            Collection<?> c = (Collection<?>) data;
            if (c.size() == 1) {
              data = getOnlyElement(c);
            }
          }

          dragListener.handleDrop(data, d.getLocation().x, d.getLocation().y);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void dragOver(DropTargetDragEvent d) {
        if (dragListener.canDrop(d.getLocation().x, d.getLocation().y)) {
          d.acceptDrag(d.getDropAction());
        } else {
          d.rejectDrag();
        }
      }

      @Override
      public void dragExit(DropTargetEvent dte) {
        dragListener.dragExited();
      }

      @Override
      public void dragEnter(DropTargetDragEvent d) {
        dragListener.dragEntered();
      }

      @Override
      public void dropActionChanged(DropTargetDragEvent d) {
        // do nothing
      }
    });
  }

  private static DataFlavor getBestFlavor(DataFlavor[] flavors) {
    if (flavors.length == 0) {
      return null;
    }
    if (flavors.length == 1) {
      return flavors[0];
    }

    DataFlavor ret = null;

    for (DataFlavor flavor : flavors) {
      if (flavor.isFlavorJavaFileListType()) {
        return flavor;
      }
    }

    for (DataFlavor flavor : flavors) {
      if (flavor.isFlavorTextType()) {
        if (ret == null && flavor.getRepresentationClass() == String.class) {
          ret = flavor;
        }
      }
    }

    return ret;
  }

}
