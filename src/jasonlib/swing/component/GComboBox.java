package jasonlib.swing.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;
import javax.swing.JComboBox;

public class GComboBox<T> extends JComboBox<T> {

  @SuppressWarnings("unchecked")
  public GComboBox(T... choices) {
    this(Arrays.asList(choices));
  }

  public GComboBox(Collection<T> choices) {
    super(new Vector<T>(choices));
  }

  @SuppressWarnings("unchecked")
  @Override
  public T getSelectedItem() {
    return (T) super.getSelectedItem();
  }

  public GComboBox<T> select(T item) {
    setSelectedItem(item);
    return this;
  }

}
