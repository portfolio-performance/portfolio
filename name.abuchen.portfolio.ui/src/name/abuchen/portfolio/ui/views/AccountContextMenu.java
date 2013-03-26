package name.abuchen.portfolio.ui.views;

import java.util.EnumSet;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.ui.dialogs.OtherAccountTransactionsDialog;
import name.abuchen.portfolio.ui.dialogs.TransferDialog;

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

    public void menuAboutToShow(IMenuManager manager, final Account account)
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
                    return new OtherAccountTransactionsDialog(owner.getClientEditor().getSite().getShell(), owner
                                    .getClient(), account, type);
                }
            });
        }

        manager.add(new Separator());
        manager.add(new AbstractDialogAction(Messages.AccountMenuTransfer)
        {
            @Override
            Dialog createDialog()
            {
                return new TransferDialog(owner.getClientEditor().getSite().getShell(), owner.getClient(), account);
            }
        });

        // show security related actions only if
        // (a) a portfolio exists and (b) securities exist

        if (!owner.getClient().getPortfolios().isEmpty() && !owner.getClient().getSecurities().isEmpty())
        {
            manager.add(new Separator());
            manager.add(new AbstractDialogAction(Messages.SecurityMenuBuy)
            {
                @Override
                Dialog createDialog()
                {
                    return new BuySellSecurityDialog(owner.getClientEditor().getSite().getShell(), owner.getClient(),
                                    null, PortfolioTransaction.Type.BUY);
                }
            });

            manager.add(new AbstractDialogAction(Messages.SecurityMenuSell)
            {
                @Override
                Dialog createDialog()
                {
                    return new BuySellSecurityDialog(owner.getClientEditor().getSite().getShell(), owner.getClient(),
                                    null, PortfolioTransaction.Type.SELL);
                }
            });

            manager.add(new AbstractDialogAction(Messages.SecurityMenuDividends)
            {
                @Override
                Dialog createDialog()
                {
                    return new DividendsDialog(owner.getClientEditor().getSite().getShell(), owner.getClient(), null);
                }
            });
        }

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
