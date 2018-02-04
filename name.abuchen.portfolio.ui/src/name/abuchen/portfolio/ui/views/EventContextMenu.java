package name.abuchen.portfolio.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
// obsolete import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.EventFeed;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UpdateEventsJob;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
//obsolete import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
//obsolete import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.wizards.splits.StockSplitWizard;

public class EventContextMenu
{
    private AbstractFinanceView owner;

    public EventContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager manager, final Security security)
    {
        this.menuAboutToShow(manager, security, null);
    }

    public void menuAboutToShow(IMenuManager manager, final Security security, final Portfolio portfolio)
    {
        if (owner.getClient().getSecurities().isEmpty())
            return;

        // if the security has no currency code, e.g. is an index, then show now
        // menus to create transactions
        if (security != null && security.getCurrencyCode() == null)
        {
            manager.add(new BookmarkMenu(owner.getPart(), security));
            return;
        }

        new OpenDialogAction(owner, Messages.SecurityMenuDividends + "...") //$NON-NLS-1$
                        .type(AccountTransactionDialog.class) //
                        .parameters(AccountTransaction.Type.DIVIDENDS) //
                        .with(portfolio != null ? portfolio.getReferenceAccount() : null) //
                        .with(security) //
                        .addTo(manager);

        manager.add(new Action(Messages.SecurityMenuStockSplit)
        {
            @Override
            public void run()
            {
                StockSplitWizard wizard = new StockSplitWizard(owner.getClient(), security);
                WizardDialog dialog = new WizardDialog(owner.getActiveShell(), wizard);
                if (dialog.open() == Dialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        manager.add(new Separator());
        Action action = new Action(Messages.SecurityMenuUpdateEvents)
        {
            @Override
            public void run()
            {
                new UpdateEventsJob(owner.getClient(), security).schedule();
            }
        };
        // enable only if online updates are configured
        action.setEnabled(!EventFeed.MANUAL.equals(security.getEventFeed()));
        manager.add(action);

    }
}
