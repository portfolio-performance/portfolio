package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class IndustryClassificationView extends AbstractFinanceView
{

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsIndustries;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        IndustryClassificationTreeViewer viewer = new IndustryClassificationTreeViewer(parent, SWT.NONE);

        viewer.setInput(getClient());

        return viewer.getControl();
    }

    private class IndustryClassificationTreeViewer
    {
        private Composite container;
        private TreeViewer viewer;

        public IndustryClassificationTreeViewer(Composite parent, int style)
        {
            container = new Composite(parent, style);
            TreeColumnLayout layout = new TreeColumnLayout();
            container.setLayout(layout);

            viewer = new TreeViewer(container);

            TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
            column.getColumn().setText(Messages.ShortLabelIndustry);
            column.getColumn().setWidth(400);
            layout.setColumnData(column.getColumn(), new ColumnPixelData(400));
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    Item node = (Item) element;

                    if (node.isCategory())
                        return node.getCategory().getLabel();
                    else if (node.isSecurity())
                        return node.getSecurity().getName();
                    else if (node.isAccount())
                        return node.getAccount().getName();
                    else
                        return null;
                }

                @Override
                public Image getImage(Object element)
                {
                    Item node = (Item) element;

                    if (node.isCategory())
                        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                    else if (node.isSecurity())
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
                    else if (node.isAccount())
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                    else
                        return null;
                }
            });

            column = new TreeViewerColumn(viewer, SWT.RIGHT);
            column.getColumn().setText(Messages.ColumnActualPercent);
            column.getColumn().setWidth(60);
            layout.setColumnData(column.getColumn(), new ColumnPixelData(60));
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    double percentage = ((Item) element).getPercentage();
                    return String.format("%,10.1f", percentage * 100d); //$NON-NLS-1$
                }
            });

            column = new TreeViewerColumn(viewer, SWT.RIGHT);
            column.getColumn().setText(Messages.ColumnActualValue);
            column.getColumn().setWidth(100);
            layout.setColumnData(column.getColumn(), new ColumnPixelData(100));
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    long valuation = ((Item) element).getValuation();
                    return Values.Amount.format(valuation);
                }
            });

            viewer.getTree().setHeaderVisible(true);
            viewer.getTree().setLinesVisible(true);
            viewer.setContentProvider(new ItemContentProvider());
        }

        public void setInput(Client client)
        {
            IndustryClassification taxonomy = new IndustryClassification();
            ClientSnapshot snapshot = ClientSnapshot.create(client, Dates.today());
            PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

            Category rootCategory = taxonomy.getRootCategory();
            Item rootItem = new Item(rootCategory);

            Map<Category, Item> items = new HashMap<Category, Item>();

            buildTree(rootCategory, rootItem, items);

            assignSecurities(taxonomy, items, portfolio.getPositionsBySecurity());

            assignNonSecurities(rootItem, snapshot);

            pruneEmpty(rootItem);

            calculatePercentages(snapshot.getAssets(), rootItem);

            viewer.setInput(rootItem);
        }

        private void buildTree(Category category, Item item, Map<Category, Item> items)
        {
            items.put(category, item);

            for (Category childCategory : category.getChildren())
            {
                Item childItem = new Item(childCategory);
                item.getChildren().add(childItem);

                buildTree(childCategory, childItem, items);
            }
        }

        private void assignSecurities(IndustryClassification taxonomy, Map<Category, Item> items,
                        Map<Security, SecurityPosition> positions)
        {
            for (Map.Entry<Security, SecurityPosition> position : positions.entrySet())
            {
                Security security = position.getKey();
                long valuation = position.getValue().calculateValue();

                Category category = taxonomy.getCategoryById(security.getIndustryClassification());

                if (category == null)
                {
                    Item item = items.get(taxonomy.getRootCategory());
                    item.getChildren().add(new Item(security, valuation));
                    item.valuation += valuation;
                }
                else
                {
                    items.get(category).getChildren().add(new Item(security, valuation));
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
                Item child = new Item(account.getAccount(), account.getFunds());
                item.getChildren().add(child);
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

        public Control getControl()
        {
            return container;
        }
    }

    private static class Item
    {
        List<Item> children = new ArrayList<Item>();

        long valuation;
        double percentage;

        IndustryClassification.Category category;
        Security security;
        Account account;

        public Item(Category category)
        {
            this.category = category;
        }

        public Item(Security security, long valuation)
        {
            this.security = security;
            this.valuation = valuation;
        }

        public Item(Account account, long valuation)
        {
            this.account = account;
            this.valuation = valuation;
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
    }

    private static class ItemContentProvider implements ITreeContentProvider
    {
        private Item root;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            root = (Item) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return root.getChildren().toArray();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((Item) element).getChildren().isEmpty();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((Item) parentElement).getChildren().toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public void dispose()
        {}
    }
}
