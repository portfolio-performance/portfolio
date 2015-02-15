package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.SecurityAccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.SecurityTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.util.WebLocationMenu;
import name.abuchen.portfolio.ui.wizards.splits.StockSplitWizard;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.wizard.WizardDialog;

public class SecurityContextMenu
{
    private static class SecurityTransactionAction extends Action
    {
        private AbstractFinanceView owner;
        private Portfolio portfolio;
        private Security security;
        private PortfolioTransaction.Type type;

        public SecurityTransactionAction(AbstractFinanceView owner, Type type, String label, Portfolio portfolio,
                        Security security)
        {
            super(label);
            this.owner = owner;
            this.type = type;
            this.portfolio = portfolio;
            this.security = security;
        }

        @Override
        public void run()
        {
            SecurityTransactionDialog dialog = owner.getPart().make(SecurityTransactionDialog.class, type);
            if (portfolio != null)
                dialog.setPortfolio(portfolio);
            if (security != null)
                dialog.setSecurity(security);

            if (dialog.open() == SecurityTransactionDialog.OK)
            {
                owner.markDirty();
                owner.notifyModelUpdated();
            }
        }
    }

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

        manager.add(new SecurityTransactionAction(owner, PortfolioTransaction.Type.BUY, Messages.SecurityMenuBuy,
                        portfolio, security));

        manager.add(new SecurityTransactionAction(owner, PortfolioTransaction.Type.SELL, Messages.SecurityMenuSell,
                        portfolio, security));

        manager.add(new Action(Messages.SecurityMenuDividends)
        {
            @Override
            public void run()
            {
                SecurityAccountTransactionDialog dialog = new SecurityAccountTransactionDialog(owner.getActiveShell(),
                                AccountTransaction.Type.DIVIDENDS, owner.getClient(), null, security);
                if (dialog.open() == SecurityAccountTransactionDialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        manager.add(new Action(AccountTransaction.Type.TAX_REFUND.toString() + "...") //$NON-NLS-1$
        {
            @Override
            public void run()
            {
                SecurityAccountTransactionDialog dialog = new SecurityAccountTransactionDialog(owner.getActiveShell(),
                                AccountTransaction.Type.TAX_REFUND, owner.getClient(), null, security);
                if (dialog.open() == SecurityAccountTransactionDialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        manager.add(new Action(Messages.SecurityMenuStockSplit)
        {
            @Override
            public void run()
            {
                StockSplitWizard wizard = new StockSplitWizard(owner.getClient(), security);
                WizardDialog dialog = new WizardDialog(owner.getActiveShell(), wizard);
                if (dialog.open() == SecurityAccountTransactionDialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        if (portfolio != null && owner.getClient().getActivePortfolios().size() > 1)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.SecurityMenuTransfer)
            {
                @Override
                public void run()
                {
                    SecurityTransferDialog dialog = new SecurityTransferDialog(owner.getActiveShell(), owner
                                    .getClient(), portfolio);
                    if (dialog.open() == SecurityAccountTransactionDialog.OK)
                    {
                        owner.markDirty();
                        owner.notifyModelUpdated();
                    }
                }
            });
        }

        if (portfolio != null)
        {
            manager.add(new Separator());
            manager.add(new SecurityTransactionAction(owner, PortfolioTransaction.Type.DELIVERY_INBOUND,
                            PortfolioTransaction.Type.DELIVERY_INBOUND.toString() + "...", portfolio, security)); //$NON-NLS-1$
            manager.add(new SecurityTransactionAction(owner, PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                            PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString() + "...", portfolio, security)); //$NON-NLS-1$
        }

        if (security != null)
        {
            manager.add(new Separator());
            manager.add(new WebLocationMenu(security));
        }
    }
}
