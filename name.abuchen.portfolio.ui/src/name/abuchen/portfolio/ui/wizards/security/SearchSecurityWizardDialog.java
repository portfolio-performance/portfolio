package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;

public class SearchSecurityWizardDialog extends WizardDialog
{
    public SearchSecurityWizardDialog(Shell parentShell, Client client)
    {
        super(parentShell, new SearchSecurityWizard(client));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        Button finish = getButton(IDialogConstants.FINISH_ID);
        finish.setText(Messages.BtnLabelApply);
        setButtonLayoutData(finish);
    }

    public Security getSecurity()
    {
        return ((SearchSecurityWizard) this.getWizard()).getSecurity();
    }
}
