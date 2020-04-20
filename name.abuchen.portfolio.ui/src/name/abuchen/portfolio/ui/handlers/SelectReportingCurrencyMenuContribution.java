package name.abuchen.portfolio.ui.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.ItemType;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class SelectReportingCurrencyMenuContribution
{
    @Inject
    private EModelService modelService;

    @AboutToShow
    public void aboutToShow(@Named(IServiceConstants.ACTIVE_PART) MPart part, List<MMenuElement> menuItems)
    {
        Optional<ClientInput> clientInput = MenuHelper.getActiveClientInput(part, false);
        if (!clientInput.isPresent())
            return;

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        Client client = clientInput.get().getClient();

        client.getUsedCurrencies().forEach(currency -> menuItems.add(createMenu(currency, portfolioPart)));

        menuItems.add(MMenuFactory.INSTANCE.createMenuSeparator());

        List<CurrencyUnit> available = CurrencyUnit.getAvailableCurrencyUnits();
        Collections.sort(available);
        available.forEach(currency -> menuItems.add(createMenu(currency, portfolioPart)));
    }

    private MMenuElement createMenu(CurrencyUnit currency, PortfolioPart part)
    {
        MDirectMenuItem menuItem = modelService.createModelElement(MDirectMenuItem.class);
        menuItem.setLabel(currency.getLabel());
        menuItem.setType(ItemType.CHECK);

        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.setContributionURI("bundleclass://" + PortfolioPlugin.PLUGIN_ID + "/" //$NON-NLS-1$//$NON-NLS-2$
                        + SelectReportingCurrencyHandler.class.getName());

        menuItem.getTransientData().put(CurrencyUnit.class.getName(), currency);
        menuItem.getTransientData().put(PortfolioPart.class.getName(), part);

        return menuItem;
    }
}
