package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;

public class SearchSecurityWizardDialog extends WizardDialog // NOSONAR
{
    private static final int EMPTY_NEW_SECURITY_ID = 4711;

    private final AbstractFinanceView view;
    private final Watchlist watchlist;

    public SearchSecurityWizardDialog(Shell parentShell, AbstractFinanceView view, Watchlist watchlist, Client client)
    {
        super(parentShell, new SearchSecurityWizard(client));
        this.view = view;
        this.watchlist = watchlist;
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
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == EMPTY_NEW_SECURITY_ID)
        {
            Client client = ((SearchSecurityWizard) this.getWizard()).getClient();

            Security newSecurity = new Security();
            newSecurity.setFeed(QuoteFeed.MANUAL);
            newSecurity.setCurrencyCode(client.getBaseCurrency());

            setReturnCode(CANCEL);
            close();

            Dialog dialog = view.make(EditSecurityDialog.class, newSecurity);

            if (dialog.open() == Window.OK)
            {
                client.addSecurity(newSecurity);

                if (watchlist != null)
                    watchlist.getSecurities().add(newSecurity);

                client.markDirty();

                new UpdateQuotesJob(client, newSecurity).schedule();
            }
        }
        else
        {
            super.buttonPressed(buttonId);
        }
    }

    public Security getSecurity()
    {
        return ((SearchSecurityWizard) this.getWizard()).getSecurity();
    }
}
