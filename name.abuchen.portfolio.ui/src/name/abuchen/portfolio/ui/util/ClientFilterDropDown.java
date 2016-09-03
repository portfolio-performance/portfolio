package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;

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

    private final Client client;
    private final Consumer<ClientFilter> listener;

    private List<ClientFilterDropDown.Item> items = new ArrayList<>();
    private ClientFilterDropDown.Item selectedItem;

    public ClientFilterDropDown(ToolBar toolBar, Client client, Consumer<ClientFilter> listener)
    {
        super(toolBar, Messages.MenuChooseClientFilter, Images.FILTER_OFF.image(), SWT.NONE);

        this.client = client;
        this.listener = listener;

        selectedItem = new Item(Messages.PerformanceChartLabelEntirePortfolio, c -> c);
        items.add(selectedItem);

        client.getPortfolios().forEach(portfolio -> {
            items.add(new Item(portfolio.getName(), new PortfolioClientFilter(portfolio)));
            items.add(new Item(portfolio.getName() + " + " + portfolio.getReferenceAccount().getName(), //$NON-NLS-1$
                            new PortfolioClientFilter(portfolio, portfolio.getReferenceAccount())));
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

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelClientFilterNew, a -> createCustomFilter()));
    }

    private void createCustomFilter()
    {
        ListSelectionDialog dialog = new ListSelectionDialog(getToolBar().getShell(), new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return element instanceof Account ? Images.ACCOUNT.image() : Images.PORTFOLIO.image();
            }
        });

        dialog.setTitle(Messages.LabelClientFilterDialogTitle);
        dialog.setMessage(Messages.LabelClientFilterDialogMessage);

        List<Object> elements = new ArrayList<>();
        elements.addAll(client.getPortfolios());
        elements.addAll(client.getAccounts());
        dialog.setElements(elements);

        if (dialog.open() == ListSelectionDialog.OK)
        {
            Object[] selected = dialog.getResult();
            if (selected.length > 0)
            {
                List<Portfolio> portfolios = Arrays.stream(selected).filter(o -> o instanceof Portfolio)
                                .map(o -> (Portfolio) o).collect(Collectors.toList());
                List<Account> accounts = Arrays.stream(selected).filter(o -> o instanceof Account).map(o -> (Account) o)
                                .collect(Collectors.toList());

                Item newItem = new Item(
                                String.join(", ", //$NON-NLS-1$
                                                Arrays.stream(selected).map(String::valueOf)
                                                                .collect(Collectors.toList())),
                                new PortfolioClientFilter(portfolios, accounts));

                selectedItem = newItem;
                items.add(newItem);
                getToolItem().setImage(Images.FILTER_ON.image());
                listener.accept(newItem.filter);
            }
        }
    }

    public ClientFilter getSelectedFilte()
    {
        return selectedItem.filter;
    }
}
