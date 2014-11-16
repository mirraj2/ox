package jasonlib.swing.component;

import java.awt.Color;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.google.common.collect.Lists;

public class GTextArea extends JTextArea {

  private List<ChangeListener> listeners = Lists.newArrayListWithCapacity(1);

  public GTextArea() {
    this(null);
  }

  public GTextArea(String text) {
    setLineWrap(true);
    setWrapStyleWord(true);
    setEditable(false);
    setText(text);

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

  public GTextArea editable() {
    setEditable(true);
    return this;
  }

  public GTextArea rows(int rows) {
    setRows(rows);
    return this;
  }

  public GTextArea border() {
    Border border = BorderFactory.createLineBorder(Color.lightGray);
    setBorder(BorderFactory.createCompoundBorder(border,
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    return this;
  }

  public void addChangeListener(ChangeListener changeListener) {
    listeners.add(changeListener);
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
