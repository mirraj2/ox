package ox.swing.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

  public void onChange(Runnable callback) {
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        callback.run();
      }
    });
  }

}
