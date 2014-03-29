package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.ui.dialogs.SecurityDeliveryDialog;
import name.abuchen.portfolio.ui.dialogs.SecurityTransferDialog;
import name.abuchen.portfolio.ui.util.WebLocationMenu;
import name.abuchen.portfolio.ui.wizards.splits.StockSplitWizard;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.wizard.WizardDialog;

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

        manager.add(new Action(Messages.SecurityMenuBuy)
        {
            @Override
            public void run()
            {
                BuySellSecurityDialog dialog = new BuySellSecurityDialog(owner.getClientEditor().getSite().getShell(),
                                owner.getClient(), portfolio, security, PortfolioTransaction.Type.BUY);
                if (dialog.open() == BuySellSecurityDialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        manager.add(new Action(Messages.SecurityMenuSell)
        {
            @Override
            public void run()
            {
                BuySellSecurityDialog dialog = new BuySellSecurityDialog(owner.getClientEditor().getSite().getShell(),
                                owner.getClient(), portfolio, security, PortfolioTransaction.Type.SELL);
                if (dialog.open() == BuySellSecurityDialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        manager.add(new Action(Messages.SecurityMenuDividends)
        {
            @Override
            public void run()
            {
                DividendsDialog dialog = new DividendsDialog(owner.getClientEditor().getSite().getShell(), owner
                                .getClient(), null, security);
                if (dialog.open() == DividendsDialog.OK)
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
                WizardDialog dialog = new WizardDialog(owner.getClientEditor().getSite().getShell(), wizard);
                if (dialog.open() == DividendsDialog.OK)
                {
                    owner.markDirty();
                    owner.notifyModelUpdated();
                }
            }
        });

        if (portfolio != null && owner.getClient().getPortfolios().size() > 1)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.SecurityMenuTransfer)
            {
                @Override
                public void run()
                {
                    SecurityTransferDialog dialog = new SecurityTransferDialog(owner.getClientEditor().getSite()
                                    .getShell(), owner.getClient(), portfolio);
                    if (dialog.open() == DividendsDialog.OK)
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
            manager.add(new Action(PortfolioTransaction.Type.DELIVERY_INBOUND.toString() + "...") //$NON-NLS-1$
            {
                @Override
                public void run()
                {
                    SecurityDeliveryDialog dialog = new SecurityDeliveryDialog(owner.getClientEditor().getSite()
                                    .getShell(), owner.getClient(), portfolio,
                                    PortfolioTransaction.Type.DELIVERY_INBOUND);
                    if (dialog.open() == SecurityDeliveryDialog.OK)
                    {
                        owner.markDirty();
                        owner.notifyModelUpdated();
                    }
                }
            });

            manager.add(new Action(PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString() + "...") //$NON-NLS-1$
            {
                @Override
                public void run()
                {
                    SecurityDeliveryDialog dialog = new SecurityDeliveryDialog(owner.getClientEditor().getSite()
                                    .getShell(), owner.getClient(), portfolio,
                                    PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    if (dialog.open() == SecurityDeliveryDialog.OK)
                    {
                        owner.markDirty();
                        owner.notifyModelUpdated();
                    }
                }
            });
        }

        if (security != null)
        {
            manager.add(new Separator());
            manager.add(new WebLocationMenu(security));
        }
    }
}
