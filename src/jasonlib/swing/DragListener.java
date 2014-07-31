package jasonlib.swing;

public abstract class DragListener {

  public abstract void handleDrop(Object data, int x, int y);

  public void dragEntered() {
  }

  public void dragExited() {
  }

  public boolean canDrop(int x, int y) {
    return true;
  }

}
