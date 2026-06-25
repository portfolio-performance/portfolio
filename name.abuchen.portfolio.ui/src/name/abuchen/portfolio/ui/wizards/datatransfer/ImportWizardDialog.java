package name.abuchen.portfolio.ui.wizards.datatransfer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * Wizard dialog for the import wizard that can be "frozen" while a modeless
 * transaction editor (opened from {@link ManualTransactionEntryPage}) is open.
 * <p>
 * While frozen, the user must not be able to finish, navigate, cancel or close
 * the wizard, because a separate modeless editor shell is still collecting a
 * transaction. The freeze covers three independent close/navigation paths:
 * <ul>
 * <li>the Back / Next / Finish / Cancel buttons (disabled),</li>
 * <li>the ESC key and any code path routing to cancel (cancelPressed no-op),</li>
 * <li>the window-X / Alt-F4 / Cmd-W / WM close request (SWT.Close vetoed).</li>
 * </ul>
 */
public class ImportWizardDialog extends WizardDialog
{
    private boolean editing = false;

    public ImportWizardDialog(Shell parentShell, IWizard newWizard)
    {
        super(parentShell, newWizard);
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        // veto window-manager close requests (X button, Alt-F4, Cmd-W) while a
        // transaction editor is open
        newShell.addListener(SWT.Close, event -> {
            if (editing)
                event.doit = false;
        });
    }

    /**
     * Enables / disables the wizard chrome. Called by
     * {@link ManualTransactionEntryPage} when a modeless transaction editor is
     * opened (true) and when it is closed (false).
     */
    public void setEditing(boolean editing)
    {
        this.editing = editing;
        setButtonEnabled(IDialogConstants.BACK_ID, !editing);
        setButtonEnabled(IDialogConstants.NEXT_ID, !editing);
        setButtonEnabled(IDialogConstants.FINISH_ID, !editing);
        setButtonEnabled(IDialogConstants.CANCEL_ID, !editing);
    }

    private void setButtonEnabled(int id, boolean enabled)
    {
        Button button = getButton(id);
        if (button != null && !button.isDisposed())
            button.setEnabled(enabled);
    }

    @Override
    protected void cancelPressed()
    {
        // ESC and the Cancel button both route here; ignore while frozen
        if (editing)
            return;
        super.cancelPressed();
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        // belt-and-suspenders in case any path bypasses the disabled buttons
        if (editing)
            return;
        super.buttonPressed(buttonId);
    }
}
