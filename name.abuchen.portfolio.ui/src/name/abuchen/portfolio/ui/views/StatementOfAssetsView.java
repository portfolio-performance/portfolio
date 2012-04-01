package name.abuchen.portfolio.ui.views;

import java.util.EnumMap;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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

public class StatementOfAssetsView extends AbstractFinanceView
{
    private TreeViewer assets;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssets;
    }

    @Override
    public void notifyModelUpdated()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());
        assets.setInput(snapshot);
        assets.refresh();
        assets.expandAll();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        assets = createAssetsViewer(parent);
        hookContextMenu(assets.getControl(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                createMenuListener(manager, assets, StatementOfAssetsView.this);
            }
        });
        notifyModelUpdated();
        ViewerHelper.pack(assets);

        return assets.getTree();
    }

    public static void createMenuListener(IMenuManager manager, final TreeViewer assets, final AbstractFinanceView view)
    {
        Object element = ((IStructuredSelection) assets.getSelection()).getFirstElement();
        if (element == null || !(element instanceof AssetPosition))
            return;

        final Security security = ((AssetPosition) element).getSecurity();
        if (security == null)
            return;

        manager.add(new Action(Messages.SecurityMenuBuy)
        {
            @Override
            public void run()
            {
                BuySellSecurityDialog dialog = new BuySellSecurityDialog(view.getClientEditor().getSite().getShell(),
                                view.getClient(), security, PortfolioTransaction.Type.BUY);
                if (dialog.open() == BuySellSecurityDialog.OK)
                    view.notifyModelUpdated();
            }
        });

        manager.add(new Action(Messages.SecurityMenuSell)
        {
            @Override
            public void run()
            {
                BuySellSecurityDialog dialog = new BuySellSecurityDialog(view.getClientEditor().getSite().getShell(),
                                view.getClient(), security, PortfolioTransaction.Type.SELL);
                if (dialog.open() == BuySellSecurityDialog.OK)
                    view.notifyModelUpdated();
            }
        });

        manager.add(new Action(Messages.SecurityMenuDividends)
        {
            @Override
            public void run()
            {
                DividendsDialog dialog = new DividendsDialog(view.getClientEditor().getSite().getShell(), view
                                .getClient(), security);
                if (dialog.open() == DividendsDialog.OK)
                    view.notifyModelUpdated();
            }
        });
    }

    public static TreeViewer createAssetsViewer(Composite parent)
    {
        TreeViewer assets = new TreeViewer(parent, SWT.FULL_SELECTION);

        TreeColumn column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnShares);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(80);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnName);
        column.setWidth(300);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnTicker);
        column.setWidth(60);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnQuote);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(60);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnMarketValue);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(80);

        column = new TreeColumn(assets.getTree(), SWT.None);
        column.setText(Messages.ColumnShareInPercent);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(80);

        assets.getTree().setHeaderVisible(true);
        assets.getTree().setLinesVisible(true);

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), assets.getTree());

        assets.setLabelProvider(new StatementOfAssetsLabelProvider(resources));
        assets.setContentProvider(new StatementOfAssetsContentProvider());
        return assets;
    }

    private static class StatementOfAssetsContentProvider implements ITreeContentProvider
    {
        private AssetCategory[] categories;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.categories = new AssetCategory[0];
            }
            else if (newInput instanceof ClientSnapshot)
            {
                this.categories = ((ClientSnapshot) newInput).groupByCategory().toArray(new AssetCategory[0]);
            }
            else if (newInput instanceof PortfolioSnapshot)
            {
                this.categories = ((PortfolioSnapshot) newInput).groupByCategory().toArray(new AssetCategory[0]);
            }
            else
            {
                throw new RuntimeException("Unsupported type: " + newInput.getClass().getName()); //$NON-NLS-1$
            }
        }

        public Object[] getElements(Object inputElement)
        {
            return this.categories;
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof AssetCategory)
                return ((AssetCategory) parentElement).getPositions().toArray(new AssetPosition[0]);
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof SecurityPosition)
            {
                Security security = ((SecurityPosition) element).getSecurity();
                Security.AssetClass assetClass = security == null ? Security.AssetClass.CASH : security.getType();
                for (AssetCategory c : categories)
                {
                    if (c.getAssetClass() == assetClass)
                        return c;
                }

            }
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof AssetCategory && !((AssetCategory) element).getPositions().isEmpty();
        }

        public void dispose()
        {}

    }

    private static class StatementOfAssetsLabelProvider extends LabelProvider implements ITableLabelProvider,
                    IColorProvider
    {
        private final Color colorTotals;
        private final EnumMap<Security.AssetClass, Color> colors;

        public StatementOfAssetsLabelProvider(LocalResourceManager resources)
        {
            colors = new EnumMap<Security.AssetClass, Color>(Security.AssetClass.class);

            for (AssetClass a : AssetClass.values())
                colors.put(a, resources.createColor(Colors.valueOf(a.name()).swt()));

            this.colorTotals = resources.createColor(Colors.TOTALS.swt());
        }

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            if (element instanceof AssetCategory)
            {
                AssetCategory cat = (AssetCategory) element;

                switch (columnIndex)
                {
                    case 1:
                        return cat.getAssetClass() != null ? cat.getAssetClass().name() : Messages.LabelTotalSum;
                    case 4:
                        return String.format("%,10.2f", cat.getValuation() / 100d); //$NON-NLS-1$
                    case 5:
                        return String.format("%,10.2f", cat.getShare() * 100); //$NON-NLS-1$
                }
            }
            else if (element instanceof AssetPosition)
            {
                AssetPosition pos = (AssetPosition) element;

                switch (columnIndex)
                {
                    case 0:
                        return pos.getSecurity() != null ? String.valueOf(pos.getPosition().getShares()) : ""; //$NON-NLS-1$
                    case 1:
                        return pos.getDescription();
                    case 2:
                        return pos.getSecurity() != null ? pos.getSecurity().getTickerSymbol() : ""; //$NON-NLS-1$
                    case 3:
                        return pos.getSecurity() != null ? String.format("%,10.2f", pos //$NON-NLS-1$
                                        .getPosition().getPrice().getValue() / 100d) : ""; //$NON-NLS-1$
                    case 4:
                        return String.format("%,10.2f", pos.getValuation() / 100d); //$NON-NLS-1$
                    case 5:
                        return String.format("%,10.2f", pos.getShare() * 100); //$NON-NLS-1$
                }
            }
            return null;
        }

        public Color getBackground(Object element)
        {
            if (element instanceof AssetCategory)
            {
                AssetCategory c = (AssetCategory) element;

                if (c.getAssetClass() == null)
                    return colorTotals;

                return colors.get(c.getAssetClass());
            }

            return null;
        }

        public Color getForeground(Object element)
        {
            if (element instanceof AssetCategory)
                return Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

            return null;
        }

    }

}
