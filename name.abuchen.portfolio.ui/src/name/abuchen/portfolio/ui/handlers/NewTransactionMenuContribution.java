package name.abuchen.portfolio.ui.handlers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.AbstractTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class NewTransactionMenuContribution
{
    @Inject
    private EModelService modelService;

    @AboutToShow
    public void aboutToShow(@Named(IServiceConstants.ACTIVE_PART) MPart part, List<MMenuElement> items,
                    IEclipseContext context)
    {
        if (!MenuHelper.isClientPartActive(part))
            return;

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();

        items.add(create(portfolioPart, SecurityTransactionDialog.class, PortfolioTransaction.Type.BUY));
        items.add(create(portfolioPart, SecurityTransactionDialog.class, PortfolioTransaction.Type.SELL));
        items.add(create(portfolioPart, SecurityTransactionDialog.class, PortfolioTransaction.Type.DELIVERY_INBOUND));
        items.add(create(portfolioPart, SecurityTransactionDialog.class, PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        if (portfolioPart.getClient().getActivePortfolios().size() > 1)
            items.add(create(portfolioPart, SecurityTransferDialog.class, Messages.LabelSecurityTransfer));

        items.add(MMenuFactory.INSTANCE.createMenuSeparator());

        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.DIVIDENDS));

        items.add(MMenuFactory.INSTANCE.createMenuSeparator());

        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.DEPOSIT));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.REMOVAL));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.INTEREST));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.INTEREST_CHARGE));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.FEES));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.FEES_REFUND));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.TAXES));
        items.add(create(portfolioPart, AccountTransactionDialog.class, AccountTransaction.Type.TAX_REFUND));

        items.add(MMenuFactory.INSTANCE.createMenuSeparator());

        if (portfolioPart.getClient().getActiveAccounts().size() > 1)
            items.add(create(portfolioPart, AccountTransferDialog.class, Messages.LabelAccountTransfer));
    }

    private MMenuElement create(PortfolioPart part, Class<? extends AbstractTransactionDialog> dialog, Object parameter)
    {
        MDirectMenuItem menuItem = modelService.createModelElement(MDirectMenuItem.class);
        menuItem.setLabel(parameter.toString() + "..."); //$NON-NLS-1$

        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.setContributionURI(
                        "bundleclass://" + PortfolioPlugin.PLUGIN_ID + "/" + NewTransactionHandler.class.getName()); //$NON-NLS-1$//$NON-NLS-2$

        menuItem.getTransientData().put(PortfolioPart.class.getName(), part);
        menuItem.getTransientData().put(AbstractTransactionDialog.class.getName(), dialog);
        menuItem.getTransientData().put(UIConstants.Parameter.NAME, parameter);

        return menuItem;
    }
}
