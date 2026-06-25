package name.abuchen.portfolio.ui.wizards.pdfdebug;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Wizard dialog for the "Create PDF debug" wizard that remembers its size and
 * position across invocations.
 */
public class CreatePDFDebugWizardDialog extends WizardDialog
{
    public CreatePDFDebugWizardDialog(Shell parentShell, IWizard newWizard)
    {
        super(parentShell, newWizard);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return DialogSettings.getOrCreateSection(PortfolioPlugin.getDefault().getDialogSettings(),
                        CreatePDFDebugWizardDialog.class.getSimpleName());
    }

    @Override
    protected Point getInitialSize()
    {
        var size = super.getInitialSize();

        // enlarge the computed default height by 50% to give the debug text more
        // room; a size the user has explicitly chosen (and which is persisted)
        // takes precedence
        var settings = getDialogBoundsSettings();
        if (settings == null || settings.get("DIALOG_HEIGHT") == null) //$NON-NLS-1$
            size.y = (int) (size.y * 1.5);

        return size;
    }
}
