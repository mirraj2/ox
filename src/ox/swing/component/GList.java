package ox.swing.component;

import java.util.Collection;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class GList<T> extends JList<T> {

  public GList(Collection<T> elements) {
    DefaultListModel<T> model = new DefaultListModel<>();
    for (T element : elements) {
      model.addElement(element);
    }
    setModel(model);
  }

  public void change(Runnable callback) {
    addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        callback.run();
      }
    });
  }

}
