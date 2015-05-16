package jasonlib.swing.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.google.common.collect.Lists;

public class GTextField extends JTextField {

  private List<Runnable> listeners = Lists.newArrayListWithCapacity(1);

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

  /**
   * selects all the text on focus
   */
  public GTextField focusSelects() {
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        select(0, getText().length());
      }
    });
    return this;
  }

  public GTextField color(Color c) {
    setForeground(c);
    return this;
  }

  public GTextField font(String name, int size) {
    setFont(new Font(name, getFont().getStyle(), size));
    return this;
  }

  public GTextField bold() {
    setFont(getFont().deriveFont(Font.BOLD));
    return this;
  }

  public GTextField columns(int columns) {
    this.setColumns(columns);
    return this;
  }

  public GTextField onChange(Runnable changeListener) {
    listeners.add(changeListener);
    return this;
  }

  private void change() {
    for (Runnable c : listeners) {
      c.run();
    }
  }

  @Override
  public String getText() {
    return super.getText().trim();
  }

  public GTextField onEnter(Runnable callback) {
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        callback.run();
      }
    });
    return this;
  }

}
