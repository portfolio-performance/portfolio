package name.abuchen.portfolio.ui.views;

import java.util.EnumSet;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.OtherAccountTransactionsDialog;
import name.abuchen.portfolio.ui.dialogs.SecurityAccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.TransferDialog;
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
            manager.add(new AbstractDialogAction(type.toString() + "...") //$NON-NLS-1$
            {
                @Override
                Dialog createDialog()
                {
                    return new OtherAccountTransactionsDialog(owner.getActiveShell(), owner.getClient(), account, type);
                }
            });
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

            manager.add(new AbstractDialogAction(Messages.SecurityMenuBuy)
            {
                @Override
                Dialog createDialog()
                {
                    SecurityTransactionDialog dialog = owner.getPart().make(SecurityTransactionDialog.class,
                                    PortfolioTransaction.Type.BUY);
                    if (portfolio[0] != null)
                        dialog.setPortfolio(portfolio[0]);
                    if (security != null)
                        dialog.setSecurity(security);

                    return dialog;
                }
            });

            manager.add(new AbstractDialogAction(Messages.SecurityMenuSell)
            {
                @Override
                Dialog createDialog()
                {
                    SecurityTransactionDialog dialog = owner.getPart().make(SecurityTransactionDialog.class,
                                    PortfolioTransaction.Type.SELL);
                    if (portfolio[0] != null)
                        dialog.setPortfolio(portfolio[0]);
                    if (security != null)
                        dialog.setSecurity(security);
                    return dialog;
                }
            });

            manager.add(new AbstractDialogAction(Messages.SecurityMenuDividends)
            {
                @Override
                Dialog createDialog()
                {
                    return new SecurityAccountTransactionDialog(owner.getActiveShell(),
                                    AccountTransaction.Type.DIVIDENDS, owner.getClient(), account, null);
                }
            });
        }

        manager.add(new AbstractDialogAction(AccountTransaction.Type.TAX_REFUND + "...") //$NON-NLS-1$
        {
            @Override
            Dialog createDialog()
            {
                return new SecurityAccountTransactionDialog(owner.getActiveShell(), AccountTransaction.Type.TAX_REFUND,
                                owner.getClient(), account, null);
            }
        });

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
