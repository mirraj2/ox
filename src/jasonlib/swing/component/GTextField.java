package jasonlib.swing.component;

import java.util.List;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.google.common.collect.Lists;

public class GTextField extends JTextField {

  private List<ChangeListener> listeners = Lists.newArrayListWithCapacity(1);

  public GTextField() {
    this("");
  }

  public GTextField(String text) {
    super(text);

    setColumns(10);

    getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) {
        change();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        change();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        change();
      }
    });
  }

  public GTextField columns(int columns) {
    this.setColumns(columns);
    return this;
  }

  public GTextField addChangeListener(ChangeListener changeListener) {
    listeners.add(changeListener);
    return this;
  }

  private void change() {
    for (ChangeListener c : listeners) {
      c.stateChanged(null);
    }
  }

  @Override
  public String getText() {
    return super.getText().trim();
  }

}
