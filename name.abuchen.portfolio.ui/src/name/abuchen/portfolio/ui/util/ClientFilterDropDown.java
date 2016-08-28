package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioPlusClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;

public class ClientFilterDropDown extends AbstractDropDown
{
    private static class Item
    {
        private String label;
        private ClientFilter filter;

        public Item(String label, ClientFilter filter)
        {
            this.label = label;
            this.filter = filter;
        }
    }

    private final Consumer<ClientFilter> listener;

    private List<ClientFilterDropDown.Item> items = new ArrayList<>();
    private ClientFilterDropDown.Item selectedItem;

    public ClientFilterDropDown(ToolBar toolBar, Client client, Consumer<ClientFilter> listener)
    {
        super(toolBar, Messages.MenuChooseClientFilter, Images.FILTER_OFF.image(), SWT.NONE);

        this.listener = listener;

        selectedItem = new Item(Messages.PerformanceChartLabelEntirePortfolio, c -> c);
        items.add(selectedItem);

        client.getPortfolios().forEach(portfolio -> {
            items.add(new Item(portfolio.getName(), new PortfolioClientFilter(portfolio)));
            items.add(new Item(portfolio.getName() + " + " + portfolio.getReferenceAccount().getName(), //$NON-NLS-1$
                            new PortfolioPlusClientFilter(portfolio)));
        });
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        items.stream().forEach(item -> {
            Action action = new SimpleAction(item.label, a -> {
                selectedItem = item;
                getToolItem().setImage(items.indexOf(selectedItem) == 0 ? Images.FILTER_OFF.image()
                                : Images.FILTER_ON.image());

                listener.accept(item.filter);
            });
            action.setChecked(item.equals(selectedItem));
            manager.add(action);
        });
    }

    public ClientFilter getSelectedFilte()
    {
        return selectedItem.filter;
    }
}
