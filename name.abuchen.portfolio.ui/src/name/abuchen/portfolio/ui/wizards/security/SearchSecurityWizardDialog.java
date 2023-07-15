package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;

public class SearchSecurityWizardDialog extends WizardDialog // NOSONAR
{
    private static final int EMPTY_NEW_SECURITY_ID = 4711;

    private Security newSecurity;

    public SearchSecurityWizardDialog(Shell parentShell, Client client)
    {
        super(parentShell, new SearchSecurityWizard(client));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, EMPTY_NEW_SECURITY_ID, Messages.SecurityMenuEmptyInstrument, false);

        super.createButtonsForButtonBar(parent);

        Button finish = getButton(IDialogConstants.FINISH_ID);
        finish.setText(Messages.BtnLabelApply);
        setButtonLayoutData(finish);
    }

    @Override
    protected void finishPressed()
    {
        newSecurity = ((SearchSecurityWizard) this.getWizard()).getSecurity();
        super.finishPressed();
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == EMPTY_NEW_SECURITY_ID)
        {
            Client client = ((SearchSecurityWizard) this.getWizard()).getClient();

            newSecurity = new Security(null, client.getBaseCurrency());
            newSecurity.setFeed(QuoteFeed.MANUAL);

            setReturnCode(OK);
            close();
        }
        else
        {
            super.buttonPressed(buttonId);
        }
    }

    public Security getSecurity()
    {
        return newSecurity;
    }
}
