package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class CategoryView extends AbstractFinanceView
{
    private TreeViewer assets;

    private ClientSnapshot snapshot;
    private CategoryModel model;

    private Map<Security, SecurityPosition> security2position;
    private Map<Account, AccountSnapshot> account2position;

    @Override
    protected String getTitle()
    {
        return Messages.LabelAssetAllocation;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        assets = createAssetsViewer(parent);

        updateSnapshot();
        model = CategoryModel.create(snapshot);

        assets.setInput(this);
        expandCategories();
        ViewerHelper.pack(assets);
        return assets.getControl();
    }

    @Override
    public void notifyModelUpdated()
    {
        updateSnapshot();
        model.recalculate(snapshot);
        assets.refresh(true);
    }

    private void updateSnapshot()
    {
        snapshot = ClientSnapshot.create(getClient(), Dates.today());
        security2position = new HashMap<Security, SecurityPosition>();
        for (SecurityPosition position : snapshot.getJointPortfolio().getPositions())
            security2position.put(position.getSecurity(), position);

        account2position = new HashMap<Account, AccountSnapshot>();
        for (AccountSnapshot accountSnapshot : snapshot.getAccounts())
            account2position.put(accountSnapshot.getAccount(), accountSnapshot);
    }

    private void onCategoryModelEdited()
    {
        model.recalculate(snapshot);
        markDirty();
        assets.refresh(true);
    }

    public TreeViewer createAssetsViewer(Composite parent)
    {
        TreeViewer assets = new TreeViewer(parent, SWT.FULL_SELECTION);

        TreeColumn column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnCategory);
        column.setWidth(400);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnTargetPercent);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(60);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnTargetValue);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(100);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnActualPercent);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(60);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnActualValue);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(100);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnDeltaPercent);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(60);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnDeltaValue);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(100);

        assets.getTree().setHeaderVisible(true);
        assets.getTree().setLinesVisible(true);

        assets.setLabelProvider(new CategoryLabelProvider());
        assets.setContentProvider(new CategoryContentProvider());

        new CellEditorFactory(assets, CategoryModel.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                onCategoryModelEdited();
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .editable("percentage") // //$NON-NLS-1$
                        .apply();

        hookContextMenu(assets.getTree(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        return assets;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Object selection = ((IStructuredSelection) assets.getSelection()).getFirstElement();

        if (selection instanceof CategoryModel)
        {
            final CategoryModel category = (CategoryModel) selection;

            if (category.getElements().isEmpty())
            {
                manager.add(new Action(Messages.AssetAllocationMenuAddNewCategory)
                {
                    @Override
                    public void run()
                    {
                        CategoryModel element = category.addCategory(new Category(Messages.LabelNewCategory,
                                        100 - category.getSubject().getChildrenPercentage()));
                        assets.setExpandedState(category, true);
                        onCategoryModelEdited();
                        assets.editElement(element, 0);
                    }
                });
            }

            if (category.getChildren().isEmpty())
            {
                Set<Object> assigned = new HashSet<Object>(model.getSubject().getTreeElements());

                MenuManager securities = new MenuManager(Messages.AssetAllocationMenuAssignSecurity);
                List<Security> list = getClient().getSecurities();
                Collections.sort(list, new Security.ByName());
                for (final Security s : list)
                {
                    if (assigned.contains(s))
                        continue;

                    securities.add(new Action(s.getName())
                    {
                        @Override
                        public void run()
                        {
                            category.getSubject().addSecurity(s);
                            assets.setExpandedState(category, true);
                            onCategoryModelEdited();
                        }
                    });
                }
                manager.add(securities);

                MenuManager cash = new MenuManager(Messages.AssetAllocationMenuAssignAccount);
                for (final Account a : getClient().getAccounts())
                {
                    if (assigned.contains(a))
                        continue;

                    cash.add(new Action(a.getName())
                    {
                        @Override
                        public void run()
                        {
                            category.getSubject().addAccount(a);
                            assets.setExpandedState(category, true);
                            onCategoryModelEdited();
                        }
                    });
                }
                manager.add(cash);
            }

            if (category.getParent() != null)
            {
                manager.add(new Action(Messages.AssetAllocationMenuDeleteCategory)
                {
                    @Override
                    public void run()
                    {
                        category.getParent().removeCategory(category);
                        onCategoryModelEdited();
                    }
                });
            }
        }
        else if (selection instanceof Security)
        {
            final Security security = (Security) selection;

            new SecurityContextMenu(this).menuAboutToShow(manager, security);

            manager.add(new Separator());
            manager.add(new Action(Messages.AssetAllocationMenuRemove)
            {
                @Override
                public void run()
                {
                    model.findFor(security).getSubject().removeSecurity(security);
                    onCategoryModelEdited();
                }
            });
        }
        else if (selection instanceof Account)
        {
            final Account account = (Account) selection;

            new AccountContextMenu(this).menuAboutToShow(manager, account);

            manager.add(new Separator());
            manager.add(new Action(Messages.AssetAllocationMenuRemove)
            {
                @Override
                public void run()
                {
                    model.findFor(account).getSubject().removeAccount(account);
                    onCategoryModelEdited();
                }
            });
        }
    }

    private void expandCategories()
    {
        List<CategoryModel> expanded = new ArrayList<CategoryModel>();
        Stack<CategoryModel> stack = new Stack<CategoryModel>();
        stack.push(model);
        while (!stack.isEmpty())
        {
            CategoryModel c = stack.pop();
            if (!c.getChildren().isEmpty())
            {
                expanded.add(c);
                stack.addAll(c.getChildren());
            }
        }
        assets.setExpandedElements(expanded.toArray(new CategoryModel[0]));
    }

    private class CategoryContentProvider implements ITreeContentProvider
    {
        private CategoryModel root;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput instanceof CategoryView)
                this.root = ((CategoryView) newInput).model;
            else
                this.root = null;
        }

        public Object[] getElements(Object inputElement)
        {
            return new Object[] { this.root };
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof CategoryModel)
            {
                CategoryModel category = (CategoryModel) parentElement;
                if (!category.getChildren().isEmpty())
                {
                    return category.getChildren().toArray(new Object[0]);
                }
                else if (!category.getElements().isEmpty()) { return category.getElements().toArray(new Object[0]); }
            }
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof CategoryModel)
            {
                return ((CategoryModel) element).getParent();
            }
            else
            {
                return this.root.findFor(element);
            }
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof CategoryModel && //
                            (!((CategoryModel) element).getChildren().isEmpty() || //
                            !((CategoryModel) element).getElements().isEmpty());
        }

        public void dispose()
        {}

    }

    private class CategoryLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider
    {
        public Image getColumnImage(Object element, int columnIndex)
        {
            if (columnIndex != 0)
                return null;

            if (element instanceof CategoryModel)
                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
            else if (element instanceof Account)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            else if (element instanceof Security)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            else
                return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            if (element instanceof CategoryModel)
            {
                CategoryModel cat = (CategoryModel) element;

                switch (columnIndex)
                {
                    case 0:
                        return cat.getName();
                    case 1:
                        return String.format("%,10.0f", (double) cat.getPercentage()); //$NON-NLS-1$
                    case 2:
                        return Values.Amount.format(cat.getTarget());
                    case 3:
                        // actual %
                        // --> root is compared to target = total assets
                        long actual = cat.getActual();
                        long base = cat.getParent() == null ? cat.getTarget() : cat.getParent().getActual();

                        return String.format("%,10.1f", ((double) actual / (double) base) * 100d); //$NON-NLS-1$
                    case 4:
                        return Values.Amount.format(cat.getActual());
                    case 5:
                        return String.format("%,10.1f", ((double) cat.getActual() / cat.getTarget()) * 100d - 100); //$NON-NLS-1$
                    case 6:
                        return Values.Amount.format(cat.getActual() - cat.getTarget());
                }
            }
            else if (element instanceof Security)
            {
                Security security = (Security) element;
                switch (columnIndex)
                {
                    case 0:
                        return security.getName();
                    case 4:
                        SecurityPosition p = security2position.get(security);
                        return Values.Amount.format(p == null ? 0 : p.calculateValue());
                    case 5:
                    case 6:
                        CategoryModel cat = model.findFor(security);
                        SecurityPrice price = security.getSecurityPrice(Dates.today());

                        long gap = cat.getTarget() - cat.getActual();

                        long count = price.getValue() != 0 ? gap / price.getValue() : 0;

                        if (columnIndex == 5)
                            return String.format("%,10d", count); //$NON-NLS-1$
                        else
                            return Values.Quote.format(count * price.getValue());
                }
            }
            else if (element instanceof Account)
            {
                Account account = (Account) element;
                switch (columnIndex)
                {
                    case 0:
                        return account.getName();
                    case 4:
                        AccountSnapshot s = account2position.get(account);
                        return Values.Amount.format(s.getFunds());
                }
            }
            return null;
        }

        public Color getBackground(Object element, int columnIndex)
        {
            return null;
        }

        public Color getForeground(Object element, int columnIndex)
        {
            if (element instanceof CategoryModel)
            {
                CategoryModel cat = (CategoryModel) element;
                int sum = cat.getParent() == null ? cat.getSubject().getPercentage() : cat.getParent().getSubject()
                                .getChildrenPercentage();
                if (sum != 100)
                    return Display.getCurrent().getSystemColor(SWT.COLOR_RED);

                if (columnIndex >= 5)
                    return Display.getCurrent().getSystemColor(
                                    cat.getActual() >= cat.getTarget() ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
                return null;
            }
            else
            {
                return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
            }
        }
    }
}
