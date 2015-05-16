package jasonlib.swing.component;

import java.awt.Color;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.google.common.collect.Lists;

public class GTextArea extends JTextArea {

  private List<Runnable> listeners = Lists.newArrayList();

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
        notifyChange();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        notifyChange();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        notifyChange();
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

  private void notifyChange() {
    for (Runnable r : listeners) {
      r.run();
    }
  }

  public void change(Runnable callback) {
    listeners.add(callback);
  }

  @Override
  public String getText() {
    return super.getText().trim();
  }

  public GTextArea scrollToBottom() {
    setCaretPosition(getDocument().getLength());
    return this;
  }

}
