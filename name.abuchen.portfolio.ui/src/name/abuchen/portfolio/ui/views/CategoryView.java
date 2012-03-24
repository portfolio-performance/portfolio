package name.abuchen.portfolio.ui.views;

import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
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

        notifyModelUpdated();

        return assets.getControl();
    }

    @Override
    public void notifyModelUpdated()
    {
        snapshot = ClientSnapshot.create(getClient(), Dates.today());

        model = CategoryModel.create(snapshot);

        security2position = new HashMap<Security, SecurityPosition>();
        if (snapshot.getJointPortfolio() != null)
            for (SecurityPosition position : snapshot.getJointPortfolio().getPositions())
                security2position.put(position.getSecurity(), position);

        account2position = new HashMap<Account, AccountSnapshot>();
        for (AccountSnapshot accountSnapshot : snapshot.getAccounts())
            account2position.put(accountSnapshot.getAccount(), accountSnapshot);

        assets.setInput(this);
        assets.refresh();
        assets.expandToLevel(3);
    }

    private void notifyHasChanged()
    {
        model.recalculateActuals(snapshot);
        model.recalculateTargets();
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

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), assets.getTree());

        assets.setLabelProvider(new CategoryLabelProvider(resources));
        assets.setContentProvider(new CategoryContentProvider());

        new CellEditorFactory(assets, CategoryModel.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                notifyHasChanged();
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
                        category.addCategory(new Category(Messages.LabelNewCategory, 100 - category.getSubject()
                                        .getChildrenPercentage()));
                        notifyHasChanged();
                    }
                });
            }

            if (category.getChildren().isEmpty())
            {
                MenuManager securities = new MenuManager(Messages.AssetAllocationMenuAssignSecurity);
                for (final Security s : getClient().getSecurities())
                {
                    securities.add(new Action(s.getName())
                    {
                        @Override
                        public void run()
                        {
                            category.getSubject().addSecurity(s);
                            notifyHasChanged();
                        }
                    });
                }
                manager.add(securities);

                MenuManager cash = new MenuManager(Messages.AssetAllocationMenuAssignAccount);
                for (final Account a : getClient().getAccounts())
                {
                    cash.add(new Action(a.getName())
                    {
                        @Override
                        public void run()
                        {
                            category.getSubject().addAccount(a);
                            notifyHasChanged();
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
                        notifyHasChanged();
                    }
                });
            }
        }
        else if (selection instanceof Security)
        {
            final Security security = (Security) selection;

            manager.add(new Action(Messages.SecurityMenuBuy)
            {
                @Override
                public void run()
                {
                    BuySellSecurityDialog dialog = new BuySellSecurityDialog(getClientEditor().getSite().getShell(),
                                    getClient(), security, PortfolioTransaction.Type.BUY);
                    if (dialog.open() == BuySellSecurityDialog.OK)
                        notifyModelUpdated();
                }
            });

            manager.add(new Action(Messages.SecurityMenuSell)
            {
                @Override
                public void run()
                {
                    BuySellSecurityDialog dialog = new BuySellSecurityDialog(getClientEditor().getSite().getShell(),
                                    getClient(), security, PortfolioTransaction.Type.SELL);
                    if (dialog.open() == BuySellSecurityDialog.OK)
                        notifyModelUpdated();
                }
            });

            manager.add(new Action(Messages.SecurityMenuDividends)
            {
                @Override
                public void run()
                {
                    DividendsDialog dialog = new DividendsDialog(getClientEditor().getSite().getShell(), getClient(),
                                    security);
                    if (dialog.open() == DividendsDialog.OK)
                        notifyModelUpdated();
                }
            });

            manager.add(new Separator());
            manager.add(new Action(Messages.AssetAllocationMenuRemove)
            {
                @Override
                public void run()
                {
                    model.findFor(security).getSubject().removeSecurity(security);
                    notifyHasChanged();
                }
            });
        }
        else if (selection instanceof Account)
        {
            final Account account = (Account) selection;

            manager.add(new Action(Messages.AssetAllocationMenuRemove)
            {
                @Override
                public void run()
                {
                    model.findFor(account).getSubject().removeAccount(account);
                    notifyHasChanged();
                }
            });
        }
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
        public CategoryLabelProvider(LocalResourceManager resources)
        {}

        public Image getColumnImage(Object element, int columnIndex)
        {
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
                        return String.format("%,10.1f", (double) cat.getPercentage()); //$NON-NLS-1$
                    case 2:
                        return String.format("%,10.2f", cat.getTarget() / 100d); //$NON-NLS-1$
                    case 3:
                        // actual %
                        // --> root is compared to target = total assets
                        int actual = cat.getActual();
                        int base = cat.getParent() == null ? cat.getTarget() : cat.getParent().getActual();

                        return String.format("%,10.1f", ((double) actual / (double) base) * 100d); //$NON-NLS-1$
                    case 4:
                        return String.format("%,10.2f", cat.getActual() / 100d); //$NON-NLS-1$
                    case 5:
                        return String.format("%,10.1f", ((double) cat.getActual() / cat.getTarget()) * 100d - 100); //$NON-NLS-1$
                    case 6:
                        return String.format("%,10.2f", (cat.getActual() - cat.getTarget()) / 100d); //$NON-NLS-1$
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
                        double v = p == null ? 0 : p.calculateValue() / 100d;
                        return String.format("%,10.2f", v); //$NON-NLS-1$
                    case 5:
                    case 6:
                        CategoryModel cat = model.findFor(security);
                        SecurityPrice price = security.getSecurityPrice(Dates.today());

                        int gap = cat.getTarget() - cat.getActual();

                        int count = gap / price.getValue();

                        if (columnIndex == 5)
                            return String.format("%,10d", count); //$NON-NLS-1$
                        else
                            return String.format("%,10.2f", (count * price.getValue()) / 100d); //$NON-NLS-1$
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
                        return String.format("%,10.2f", s.getFunds() / 100d); //$NON-NLS-1$
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
