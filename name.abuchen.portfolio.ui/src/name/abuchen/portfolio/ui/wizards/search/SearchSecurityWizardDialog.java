package name.abuchen.portfolio.ui.wizards.search;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
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
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);

        // issue: if the dialog is resized smaller than the initial content, the
        // wizard is not propagating resize events to the content because of the
        // PageContainerFillLayout. Therefore an embedded scrollable control is
        // never triggered to show scroll bars.

        // workaround: prevent the user from resizing the dialog smaller than
        // the hard-coded minimum size of 650x700 pixels. We cannot use the
        // initial size of the dialog, because we remember the last size via the
        // dialog settings.

        newShell.addControlListener(new ControlAdapter()
        {
            boolean initialized = false;

            @Override
            public void controlResized(ControlEvent e)
            {
                if (!initialized)
                {
                    newShell.setMinimumSize(new Point(650, 700));
                    initialized = true;
                }
            }
        });
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

    public void triggerFinish()
    {
        finishPressed();
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
