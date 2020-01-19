package name.abuchen.portfolio.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.wizards.splits.StockSplitWizard;

public class SecurityContextMenu
{
    private AbstractFinanceView owner;

    public SecurityContextMenu(AbstractFinanceView owner)
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

        new OpenDialogAction(owner, Messages.SecurityMenuBuy + "...") //$NON-NLS-1$
                        .type(SecurityTransactionDialog.class) //
                        .parameters(PortfolioTransaction.Type.BUY) //
                        .with(portfolio) //
                        .with(security) //
                        .addTo(manager);

        new OpenDialogAction(owner, Messages.SecurityMenuSell + "...") //$NON-NLS-1$
                        .type(SecurityTransactionDialog.class) //
                        .parameters(PortfolioTransaction.Type.SELL) //
                        .with(portfolio) //
                        .with(security) //
                        .addTo(manager);

        new OpenDialogAction(owner, Messages.SecurityMenuDividends + "...") //$NON-NLS-1$
                        .type(AccountTransactionDialog.class) //
                        .parameters(AccountTransaction.Type.DIVIDENDS) //
                        .with(portfolio != null ? portfolio.getReferenceAccount() : null) //
                        .with(security) //
                        .addTo(manager);

        new OpenDialogAction(owner, AccountTransaction.Type.TAXES + "...") //$NON-NLS-1$
                        .type(AccountTransactionDialog.class) //
                        .parameters(AccountTransaction.Type.TAXES) //
                        .with(portfolio != null ? portfolio.getReferenceAccount() : null) //
                        .with(security) //
                        .addTo(manager);

        new OpenDialogAction(owner, AccountTransaction.Type.TAX_REFUND + "...") //$NON-NLS-1$
                        .type(AccountTransactionDialog.class) //
                        .parameters(AccountTransaction.Type.TAX_REFUND) //
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

        if (owner.getClient().getActivePortfolios().size() > 1)
        {
            manager.add(new Separator());
            new OpenDialogAction(owner, Messages.SecurityMenuTransfer) //
                            .type(SecurityTransferDialog.class) //
                            .with(portfolio) //
                            .with(security) //
                            .addTo(manager);
        }

        manager.add(new Separator());
        new OpenDialogAction(owner, PortfolioTransaction.Type.DELIVERY_INBOUND.toString() + "...") //$NON-NLS-1$
                        .type(SecurityTransactionDialog.class) //
                        .parameters(PortfolioTransaction.Type.DELIVERY_INBOUND) //
                        .with(portfolio) //
                        .with(security) //
                        .addTo(manager);

        new OpenDialogAction(owner, PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString() + "...") //$NON-NLS-1$
                        .type(SecurityTransactionDialog.class) //
                        .parameters(PortfolioTransaction.Type.DELIVERY_OUTBOUND) //
                        .with(portfolio) //
                        .with(security) //
                        .addTo(manager);

        if (security != null)
        {
            manager.add(new Separator());
            manager.add(new BookmarkMenu(owner.getPart(), security));
        }
    }
}
