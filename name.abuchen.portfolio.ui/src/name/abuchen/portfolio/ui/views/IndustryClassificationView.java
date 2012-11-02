package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class IndustryClassificationView extends AbstractFinanceView
{
    public static class Item
    {
        private Item parent;
        private List<Item> children = new ArrayList<Item>();

        private long valuation;
        private double percentage;

        private IndustryClassification.Category category;
        private Security security;
        private Account account;

        public Item(Item parent, Category category)
        {
            this.parent = parent;
            this.category = category;
        }

        public Item(Item parent, Security security, long valuation)
        {
            this.parent = parent;
            this.security = security;
            this.valuation = valuation;
        }

        public Item(Item parent, Account account, long valuation)
        {
            this.parent = parent;
            this.account = account;
            this.valuation = valuation;
        }

        public Item getParent()
        {
            return parent;
        }

        public boolean isCategory()
        {
            return category != null;
        }

        public IndustryClassification.Category getCategory()
        {
            return category;
        }

        public boolean isSecurity()
        {
            return security != null;
        }

        public Security getSecurity()
        {
            return security;
        }

        public boolean isAccount()
        {
            return account != null;
        }

        public Account getAccount()
        {
            return account;
        }

        public long getValuation()
        {
            return valuation;
        }

        public double getPercentage()
        {
            return percentage;
        }

        public List<Item> getChildren()
        {
            return children;
        }

        public List<Item> getPath()
        {
            LinkedList<Item> path = new LinkedList<Item>();

            Item item = this;
            while (item != null)
            {
                path.addFirst(item);
                item = item.getParent();
            }

            return path;
        }

        public String getLabel()
        {
            if (isCategory())
                return category.getLabel();
            else if (isSecurity())
                return security.getName();
            else if (isAccount())
                return account.getName();
            else
                return super.toString();
        }

        @Override
        public String toString()
        {
            return getLabel();
        }
    }

    private static final String IDENTIFIER = IndustryClassificationView.class.getName() + "-VIEW"; //$NON-NLS-1$

    private Item rootItem;

    private DropdownMenu dropdown;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsIndustries;
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        super.addButtons(toolBar);
        dropdown = new DropdownMenu(toolBar);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        rootItem = calculateRootItem();

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new StackLayout());

        IndustryClassificationTreeMapViewer mapViewer = new IndustryClassificationTreeMapViewer(container, SWT.NONE);
        mapViewer.setInput(rootItem);
        dropdown.add(Messages.LabelViewTreeMap, PortfolioPlugin.IMG_VIEW_TREEMAP, mapViewer.getControl());

        IndustryClassificationTreeViewer treeViewer = new IndustryClassificationTreeViewer(container, SWT.NONE);
        treeViewer.setInput(rootItem);
        dropdown.add(Messages.LabelViewTable, PortfolioPlugin.IMG_VIEW_TABLE, treeViewer.getControl());

        dropdown.select(getClientEditor().getPreferenceStore().getInt(IDENTIFIER));

        return container;
    }

    @Override
    public void dispose()
    {
        getClientEditor().getPreferenceStore().setValue(IDENTIFIER, dropdown.getSelectedIndex());
        super.dispose();
    }

    private Item calculateRootItem()
    {
        IndustryClassification taxonomy = new IndustryClassification();
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

        Category rootCategory = taxonomy.getRootCategory();
        Item rootItem = new Item(null, rootCategory);
        Category otherCategory = new Category(Category.OTHER_ID, rootCategory, Messages.LabelWithoutClassification);
        Item otherItem = new Item(rootItem, otherCategory);
        rootItem.getChildren().add(otherItem);

        Map<Category, Item> items = new HashMap<Category, Item>();

        buildTree(rootCategory, rootItem, items);

        assignSecurities(taxonomy, items, portfolio.getPositionsBySecurity(), otherItem);

        assignNonSecurities(otherItem, snapshot);

        pruneEmpty(rootItem);

        calculatePercentages(snapshot.getAssets(), rootItem);

        sortBySize(rootItem);

        return rootItem;
    }

    private void buildTree(Category category, Item item, Map<Category, Item> items)
    {
        items.put(category, item);

        for (Category childCategory : category.getChildren())
        {
            Item childItem = new Item(item, childCategory);
            item.getChildren().add(childItem);

            buildTree(childCategory, childItem, items);
        }
    }

    private void assignSecurities(IndustryClassification taxonomy, Map<Category, Item> items,
                    Map<Security, SecurityPosition> positions, Item otherItem)
    {
        for (Map.Entry<Security, SecurityPosition> position : positions.entrySet())
        {
            Security security = position.getKey();
            long valuation = position.getValue().calculateValue();

            Category category = taxonomy.getCategoryById(security.getIndustryClassification());

            if (category == null)
            {
                otherItem.getChildren().add(new Item(otherItem, security, valuation));
                otherItem.valuation += valuation;
            }
            else
            {
                Item item = items.get(category);
                item.getChildren().add(new Item(item, security, valuation));
                List<Category> path = category.getPath();
                for (int ii = 0; ii < path.size(); ii++)
                    items.get(path.get(ii)).valuation += valuation;
            }
        }
    }

    private void assignNonSecurities(Item item, ClientSnapshot snapshot)
    {
        for (AccountSnapshot account : snapshot.getAccounts())
        {
            if (account.getFunds() == 0)
                continue;
            Item child = new Item(item, account.getAccount(), account.getFunds());
            item.getChildren().add(child);
            item.valuation += account.getFunds();
        }
    }

    private void pruneEmpty(Item root)
    {
        LinkedList<Item> stack = new LinkedList<Item>();
        stack.add(root);

        while (!stack.isEmpty())
        {
            Item item = stack.remove();
            Iterator<Item> iterator = item.getChildren().iterator();
            while (iterator.hasNext())
            {
                Item child = iterator.next();
                if (child.getValuation() == 0)
                    iterator.remove();
            }

            stack.addAll(item.getChildren());
        }
    }

    private void calculatePercentages(long assets, Item root)
    {
        LinkedList<Item> stack = new LinkedList<Item>();
        stack.add(root);

        while (!stack.isEmpty())
        {
            Item item = stack.remove();
            item.percentage = (double) item.valuation / (double) assets;

            stack.addAll(item.getChildren());
        }
    }

    private void sortBySize(Item item)
    {
        if (!item.getChildren().isEmpty())
        {

            Collections.sort(item.getChildren(), new Comparator<Item>()
            {
                @Override
                public int compare(Item o1, Item o2)
                {
                    return Long.valueOf(o2.getValuation()).compareTo(Long.valueOf(o1.getValuation()));
                }
            });

            for (Item child : item.getChildren())
                sortBySize(child);

        }
    }

    private static class DropdownMenu extends SelectionAdapter
    {
        private ToolBar toolBar;
        private ToolItem dropdown;
        private Menu menu;

        public DropdownMenu(ToolBar toolBar)
        {
            this.toolBar = toolBar;

            dropdown = new ToolItem(toolBar, SWT.DROP_DOWN);
            dropdown.addSelectionListener(this);

            menu = new Menu(dropdown.getParent().getShell());

            toolBar.addDisposeListener(new DisposeListener()
            {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    if (!menu.isDisposed())
                        menu.dispose();
                }
            });
        }

        public int getSelectedIndex()
        {
            return (Integer) dropdown.getData();
        }

        public void select(int index)
        {
            int selected = index >= 0 && index < menu.getItemCount() ? index : 0;
            menu.getItem(selected).notifyListeners(SWT.Selection, new Event());
        }

        public void add(String item, String imageKey, final Control viewer)
        {
            MenuItem menuItem = new MenuItem(menu, SWT.NONE);
            menuItem.setText(item);
            menuItem.setImage(PortfolioPlugin.image(imageKey));
            menuItem.setData(Integer.valueOf(menu.getItemCount() - 1));
            menuItem.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    MenuItem selected = (MenuItem) event.widget;
                    dropdown.setImage(selected.getImage());
                    dropdown.setToolTipText(selected.getText());
                    dropdown.setData(selected.getData());

                    toolBar.getParent().layout();

                    Composite parent = viewer.getParent();
                    parent.layout();

                    StackLayout layout = (StackLayout) parent.getLayout();
                    layout.topControl = viewer;
                    parent.layout();
                }
            });
        }

        public void widgetSelected(SelectionEvent event)
        {
            ToolItem item = (ToolItem) event.widget;
            Rectangle rect = item.getBounds();
            Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
            menu.setLocation(pt.x, pt.y + rect.height);
            menu.setVisible(true);
        }
    }

}
