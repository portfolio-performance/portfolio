package name.abuchen.portfolio.ui.handlers;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.Navigation;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class ViewNavigationMenuContribution
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

        clientInput.get().getNavigation().getRoots().forEach(item -> addMenuItem(0, item, menuItems, portfolioPart));
    }

    private void addMenuItem(int depth, Navigation.Item item, List<MMenuElement> menuItems, PortfolioPart part)
    {
        if (item.getViewClass() == null)
            menuItems.add(MMenuFactory.INSTANCE.createMenuSeparator());

        MMenuElement menuItem = createMenu(item, part);
        if (depth > 1)
            menuItem.setLabel("- " + menuItem.getLabel()); //$NON-NLS-1$
        menuItems.add(menuItem);

        item.getChildren().forEach(child -> addMenuItem(depth + 1, child, menuItems, part));
    }

    private MMenuElement createMenu(Navigation.Item item, PortfolioPart part)
    {
        MDirectMenuItem menuItem = modelService.createModelElement(MDirectMenuItem.class);
        menuItem.setLabel(item.getLabel());

        if (item.getImage() != null)
            menuItem.setIconURI(item.getImage().getImageURI());

        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.setContributionURI(
                        "bundleclass://" + PortfolioPlugin.PLUGIN_ID + "/" + OpenViewHandler.class.getName()); //$NON-NLS-1$//$NON-NLS-2$

        menuItem.getTransientData().put(Navigation.Item.class.getName(), item);
        menuItem.getTransientData().put(PortfolioPart.class.getName(), part);

        return menuItem;
    }
}
