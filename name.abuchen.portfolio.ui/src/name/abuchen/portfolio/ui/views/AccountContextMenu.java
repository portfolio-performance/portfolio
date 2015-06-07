package name.abuchen.portfolio.ui.views;

import java.util.EnumSet;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.TransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;

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
                        AccountTransaction.Type.FEES))
        {
            new OpenDialogAction(owner, type.toString() + "...") //$NON-NLS-1$
                            .type(AccountTransactionDialog.class) //
                            .parameters(type) //
                            .with(account) //
                            .with(security) //
                            .addTo(manager);
        }

        manager.add(new Separator());
        manager.add(new AbstractDialogAction(Messages.AccountMenuTransfer)
        {
            @Override
            Dialog createDialog()
            {
                return new TransferDialog(owner.getActiveShell(), owner.getClient(), account);
            }
        });

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

            new OpenDialogAction(owner, Messages.SecurityMenuBuy) //
                            .type(SecurityTransactionDialog.class) //
                            .parameters(PortfolioTransaction.Type.BUY) //
                            .with(portfolio[0]) //
                            .with(security) //
                            .addTo(manager);

            new OpenDialogAction(owner, Messages.SecurityMenuSell) //
                            .type(SecurityTransactionDialog.class) //
                            .parameters(PortfolioTransaction.Type.SELL) //
                            .with(portfolio[0]) //
                            .with(security) //
                            .addTo(manager);

            new OpenDialogAction(owner, Messages.SecurityMenuDividends) //
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

    private abstract class AbstractDialogAction extends Action
    {
        public AbstractDialogAction(String text)
        {
            super(text);
        }

        @Override
        public final void run()
        {
            Dialog dialog = createDialog();
            if (dialog.open() == TransferDialog.OK)
            {
                owner.markDirty();
                owner.notifyModelUpdated();
            }
        }

        abstract Dialog createDialog();
    }
}
