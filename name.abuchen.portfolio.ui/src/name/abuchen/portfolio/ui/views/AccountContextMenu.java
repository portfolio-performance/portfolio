package name.abuchen.portfolio.ui.views;

import java.util.EnumSet;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;

public class AccountContextMenu
{
    private AbstractFinanceView owner;

    public AccountContextMenu(AbstractFinanceView owner)
    {
        this.owner = owner;
    }

    public void menuAboutToShow(IMenuManager manager, final Account account, final Security security)
    {
        if (account == null)
            return;

        for (final AccountTransaction.Type type : EnumSet.of( //
                        AccountTransaction.Type.INTEREST, //
                        AccountTransaction.Type.DEPOSIT, //
                        AccountTransaction.Type.REMOVAL, //
                        AccountTransaction.Type.TAXES, //
                        AccountTransaction.Type.FEES, //
                        AccountTransaction.Type.INTEREST_CHARGE))
        {
            new OpenDialogAction(owner, type.toString() + "...") //$NON-NLS-1$
                            .type(AccountTransactionDialog.class) //
                            .parameters(type) //
                            .with(account) //
                            .with(security) //
                            .addTo(manager);
        }

        manager.add(new Separator());

        new OpenDialogAction(owner, Messages.AccountMenuTransfer) //
                        .type(AccountTransferDialog.class) //
                        .with(account) //
                        .addTo(manager);

        manager.add(new Separator());

        // show security related actions only if
        // (a) a portfolio exists and (b) securities exist

        if (!owner.getClient().getActivePortfolios().isEmpty() && !owner.getClient().getSecurities().isEmpty())
        {
            // preselect a portfolio that has the current
            // account as a reference account
            final Portfolio[] portfolio = new Portfolio[1];
            for (Portfolio p : owner.getClient().getActivePortfolios())
            {
                if (p.getReferenceAccount().equals(account))
                {
                    portfolio[0] = p;
                    break;
                }
            }

            new OpenDialogAction(owner, Messages.SecurityMenuBuy + "...") //$NON-NLS-1$
                            .type(SecurityTransactionDialog.class) //
                            .parameters(PortfolioTransaction.Type.BUY) //
                            .with(portfolio[0]) //
                            .with(security) //
                            .addTo(manager);

            new OpenDialogAction(owner, Messages.SecurityMenuSell + "...") //$NON-NLS-1$
                            .type(SecurityTransactionDialog.class) //
                            .parameters(PortfolioTransaction.Type.SELL) //
                            .with(portfolio[0]) //
                            .with(security) //
                            .addTo(manager);

            new OpenDialogAction(owner, Messages.SecurityMenuDividends + "...") //$NON-NLS-1$
                            .type(AccountTransactionDialog.class) //
                            .parameters(AccountTransaction.Type.DIVIDENDS) //
                            .with(account) //
                            .with(security) //
                            .addTo(manager);
        }

        new OpenDialogAction(owner, AccountTransaction.Type.TAX_REFUND + "...") //$NON-NLS-1$
                        .type(AccountTransactionDialog.class) //
                        .parameters(AccountTransaction.Type.TAX_REFUND) //
                        .with(account) //
                        .with(security) //
                        .addTo(manager);
    }
}
