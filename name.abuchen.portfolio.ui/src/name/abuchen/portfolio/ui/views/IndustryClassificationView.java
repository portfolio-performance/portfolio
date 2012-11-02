package name.abuchen.portfolio.ui.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import name.abuchen.portfolio.ui.util.ViewDropdownMenu;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class IndustryClassificationView extends AbstractFinanceView
{
    private static final String IDENTIFIER = IndustryClassificationView.class.getName() + "-VIEW"; //$NON-NLS-1$

    private TreeMapItem rootItem;

    private ViewDropdownMenu dropdown;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsIndustries;
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        super.addButtons(toolBar);
        dropdown = new ViewDropdownMenu(toolBar);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        rootItem = calculateRootItem();

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new StackLayout());

        TreeMapViewer mapViewer = new TreeMapViewer(container, SWT.NONE);
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

    private TreeMapItem calculateRootItem()
    {
        IndustryClassification taxonomy = new IndustryClassification();
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

        Category rootCategory = taxonomy.getRootCategory();
        TreeMapItem rootItem = new TreeMapItem(null, rootCategory);
        Category otherCategory = new Category(Category.OTHER_ID, rootCategory, Messages.LabelWithoutClassification);
        TreeMapItem otherItem = new TreeMapItem(rootItem, otherCategory);
        rootItem.getChildren().add(otherItem);

        Map<Category, TreeMapItem> items = new HashMap<Category, TreeMapItem>();

        buildTree(rootCategory, rootItem, items);

        assignSecurities(taxonomy, items, portfolio.getPositionsBySecurity(), otherItem);

        assignNonSecurities(otherItem, snapshot);

        rootItem.pruneEmpty();
        rootItem.calculatePercentages(snapshot.getAssets());
        rootItem.sortBySize();

        return rootItem;
    }

    private void buildTree(Category category, TreeMapItem item, Map<Category, TreeMapItem> items)
    {
        items.put(category, item);

        for (Category childCategory : category.getChildren())
        {
            TreeMapItem childItem = new TreeMapItem(item, childCategory);
            item.getChildren().add(childItem);

            buildTree(childCategory, childItem, items);
        }
    }

    private void assignSecurities(IndustryClassification taxonomy, Map<Category, TreeMapItem> items,
                    Map<Security, SecurityPosition> positions, TreeMapItem otherItem)
    {
        for (Map.Entry<Security, SecurityPosition> position : positions.entrySet())
        {
            Security security = position.getKey();
            long valuation = position.getValue().calculateValue();

            Category category = taxonomy.getCategoryById(security.getIndustryClassification());

            if (category == null)
            {
                otherItem.getChildren().add(new TreeMapItem(otherItem, security, valuation));
                otherItem.setValuation(otherItem.getValuation() + valuation);
            }
            else
            {
                TreeMapItem item = items.get(category);
                item.getChildren().add(new TreeMapItem(item, security, valuation));
                List<Category> path = category.getPath();
                for (int ii = 0; ii < path.size(); ii++)
                {
                    TreeMapItem child = items.get(path.get(ii));
                    child.setValuation(child.getValuation() + valuation);
                }
            }
        }
    }

    private void assignNonSecurities(TreeMapItem item, ClientSnapshot snapshot)
    {
        for (AccountSnapshot account : snapshot.getAccounts())
        {
            if (account.getFunds() == 0)
                continue;
            TreeMapItem child = new TreeMapItem(item, account.getAccount(), account.getFunds());
            item.getChildren().add(child);
            item.setValuation(item.getValuation() + account.getFunds());
        }
    }
}
