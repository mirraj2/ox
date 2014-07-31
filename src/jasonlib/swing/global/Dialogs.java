package jasonlib.swing.global;

import jasonlib.swing.component.GButton;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import static com.google.common.base.Preconditions.checkNotNull;

public class Dialogs {

  public static final String PROP_SUBMIT = Dialogs.class.getSimpleName() + ".submit";
  public static final String PROP_NO = Dialogs.class.getSimpleName() + ".no";
  public static final String PROP_CANCEL = Dialogs.class.getSimpleName() + ".cancel";
  public static final String PROP_SIZE = Dialogs.class.getSimpleName() + ".size";

  public static String showModalPopup(Component parent, JComponent toPopup, boolean showOkCancel,
      String confirmText, String cancelText, boolean isResizable, final boolean autoFocus, boolean mapEnterToOK) {
    checkNotNull(toPopup, "toPopup");

    final JFrame frame;
    if (parent != null) {
      frame = Components.getAncestorOfClass(JFrame.class, parent);
    } else {
      frame = null;
    }

    JComponent content = toPopup;
    if (showOkCancel) {
      content = wrapWithOkCancel(content, confirmText, cancelText, mapEnterToOK);
    }
    content.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1, false));

    // HACKHACK satnam - there is sometimes a bug where the popup seems to be cut-off. Adding 5 to
    // the pref size seems to fix it.
    Dimension dim = content.getPreferredSize();
    dim.width += 5;
    dim.height += 5;
    content.setPreferredSize(dim);

    final JDialog dialog = new JDialog(frame, true);
    dialog.setName(null);
    dialog.setUndecorated(!isResizable);
    dialog.setContentPane(content);
    dialog.pack();
    dialog.setResizable(isResizable);

    if (frame != null) {
      GUtils.setWindowLocationRelativeTo(dialog, frame);
    } else {
      dialog.setLocationRelativeTo(null);
    }

    final AtomicReference<String> returnValue = new AtomicReference<String>();

    // add our listener
    PropertyChangeListener listener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if (name.equals(PROP_SUBMIT) || name.equals(PROP_NO) || name.equals(PROP_CANCEL)) {
          returnValue.set(name);
          dialog.setVisible(false);
        } else if (name.equals(PROP_SIZE)) {
          dialog.pack();
          // re-center
          if (frame != null) {
            GUtils.setWindowLocationRelativeTo(dialog, frame);
          } else {
            dialog.setLocationRelativeTo(null);
          }
        }
      }
    };
    toPopup.addPropertyChangeListener(listener);

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowActivated(WindowEvent e) {
        dialog.getContentPane().requestFocus();
        GFocus.focusBestChild(dialog.getContentPane());
      }
    });

    dialog.setVisible(true);

    GKeyboard.removeAllListeners(dialog);
    toPopup.removePropertyChangeListener(listener);
    dialog.dispose();

    return returnValue.get();
  }

  public static JComponent wrapWithOkCancel(final JComponent c, String confirmText,
      String cancelText, final boolean mapEnterToOK) {
    final JPanel ret = new JPanel(new MigLayout("insets 0, gap 0"));
    JPanel buttonPanel = new JPanel(new MigLayout("insets 5 10 5 10, gap 10"));
    buttonPanel.setBackground(Color.white);
    final GButton okButton = new GButton(confirmText), cancelButton = new GButton(cancelText);
    okButton.setDefault(true);

    if (confirmText != null) {
      buttonPanel.add(okButton, "gapleft push");
    }

    if (cancelText != null) {
      buttonPanel.add(cancelButton);
    }

    ret.add(c, "width 100%, height 100%, wrap");
    ret.add(buttonPanel, "width 100%, height pref!, alignx right");

    final AbstractAction submitAction = new AbstractAction("Ok") {
      @Override
      public void actionPerformed(ActionEvent e) {
        c.firePropertyChange(PROP_SUBMIT, false, true);
      }
    };

    final AbstractAction cancelAction = new AbstractAction("Cancel") {
      @Override
      public void actionPerformed(ActionEvent e) {
        c.firePropertyChange(PROP_CANCEL, false, true);
      }
    };

    final KeyAdapter keyAdapter = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          cancelAction.actionPerformed(null);
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && mapEnterToOK) {
          submitAction.actionPerformed(null);
        }
      }
    };

    GKeyboard.addKeyListener(ret, keyAdapter);
    okButton.addActionListener(submitAction);
    cancelButton.addActionListener(cancelAction);

    Dimension dim = c.getPreferredSize();
    Dimension dim2 = buttonPanel.getPreferredSize();
    ret.setPreferredSize(new Dimension(Math.max(dim.width, dim2.width), dim.height + dim2.height));
    return ret;
  }

}
