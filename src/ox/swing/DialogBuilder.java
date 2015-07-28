package ox.swing;

import java.awt.Component;
import javax.swing.JComponent;
import ox.swing.global.Dialogs;

public final class DialogBuilder {

  private Component parent;
  private boolean showOkCancel = true, resizable = false, autoFocus = true, mapEnterToOK = false;
  private String confirmText = "OK", cancelText = "Cancel";

  private DialogBuilder() {
  }

  public DialogBuilder parent(Component parent) {
    this.parent = parent;
    return this;
  }

  public DialogBuilder hideOKCancel() {
    this.showOkCancel = false;
    return this;
  }

  public DialogBuilder resizable() {
    this.resizable = true;
    return this;
  }

  public DialogBuilder mapEnterToOK() {
    this.mapEnterToOK = true;
    return this;
  }

  public DialogBuilder confirmText(String confirmText) {
    this.confirmText = confirmText;
    return this;
  }

  public DialogBuilder cancelText(String cancelText) {
    this.cancelText = cancelText;
    return this;
  }

  /**
   * This method will show the popup.
   *
   * Returns TRUE if the dialog was submitted (not canceled).
   */
  public boolean popup(JComponent popup) {
    return Dialogs.PROP_SUBMIT.equals(Dialogs.showModalPopup(parent, popup, showOkCancel,
        confirmText, cancelText, resizable, autoFocus, mapEnterToOK));
  }

  public static DialogBuilder get() {
    return new DialogBuilder();
  }

}