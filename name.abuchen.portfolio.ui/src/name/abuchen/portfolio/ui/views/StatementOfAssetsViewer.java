package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class StatementOfAssetsViewer
{
    private Composite container;
    private TableViewer assets;

    private Font boldFont;

    private ShowHideColumnHelper support;

    public StatementOfAssetsViewer(Composite parent)
    {
        createColumns(parent);
    }

    private void createColumns(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        assets = new TableViewer(container, SWT.FULL_SELECTION);

        support = new ShowHideColumnHelper(StatementOfAssetsViewer.class.getName(), assets, layout);

        Column column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object e)
            {
                if (e instanceof AssetPosition && ((AssetPosition) e).getSecurity() != null)
                    return ((AssetPosition) e).getPosition().getShares();
                else
                    return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnName, SWT.LEFT, 300);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof AssetCategory)
                    return ((AssetCategory) e).getAssetClass() != null ? ((AssetCategory) e).getAssetClass().name()
                                    : Messages.LabelTotalSum;
                else
                    return ((AssetPosition) e).getDescription();
            }

            @Override
            public Image getImage(Object e)
            {
                if (e instanceof AssetCategory)
                    return null;
                else
                    return PortfolioPlugin.image(((AssetPosition) e).getSecurity() != null ? PortfolioPlugin.IMG_SECURITY
                                    : PortfolioPlugin.IMG_ACCOUNT);
            }

            @Override
            public Font getFont(Object e)
            {
                return (e instanceof AssetCategory) ? boldFont : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnTicker, SWT.None, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof AssetPosition && ((AssetPosition) e).getSecurity() != null)
                    return ((AssetPosition) e).getSecurity().getTickerSymbol();
                else
                    return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnISIN, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof AssetPosition && ((AssetPosition) e).getSecurity() != null)
                    return ((AssetPosition) e).getSecurity().getIsin();
                else
                    return null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof AssetPosition && ((AssetPosition) e).getSecurity() != null)
                    return Values.Quote.format(((AssetPosition) e).getPosition().getPrice().getValue());
                else
                    return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnMarketValue, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof AssetCategory)
                    return Values.Amount.format(((AssetCategory) e).getValuation());
                else
                    return Values.Amount.format(((AssetPosition) e).getValuation());
            }

            @Override
            public Font getFont(Object e)
            {
                return (e instanceof AssetCategory) ? boldFont : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnShareInPercent, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof AssetCategory)
                    return Values.Percent.format(((AssetCategory) e).getShare());
                else
                    return Values.Percent.format(((AssetPosition) e).getShare());
            }

            @Override
            public Font getFont(Object e)
            {
                return (e instanceof AssetCategory) ? boldFont : null;
            }
        });
        support.addColumn(column);

        support.createColumns();

        assets.getTable().setHeaderVisible(true);
        assets.getTable().setLinesVisible(true);

        assets.setContentProvider(new StatementOfAssetsContentProvider());

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), assets.getTable());
        boldFont = resources.createFont(FontDescriptor.createFrom(assets.getTable().getFont()).setStyle(SWT.BOLD));
    }

    public void hookMenuListener(IMenuManager manager, final AbstractFinanceView view)
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

    public void pack()
    {
        if (!support.isUserConfigured())
            ViewerHelper.pack(assets);
    }

    public ShowHideColumnHelper getColumnHelper()
    {
        return support;
    }

    public TableViewer getTableViewer()
    {
        return assets;
    }

    public Control getControl()
    {
        return container;
    }

    public void setInput(ClientSnapshot snapshot)
    {
        internalSetInput(snapshot);
    }

    public void setInput(PortfolioSnapshot snapshot)
    {
        internalSetInput(snapshot);
    }

    private void internalSetInput(Object snapshot)
    {
        assets.getTable().setRedraw(false);
        try
        {
            assets.setInput(snapshot);
            assets.refresh();
        }
        finally
        {
            assets.getTable().setRedraw(true);
        }
    }

    private static class StatementOfAssetsContentProvider implements IStructuredContentProvider
    {
        private Object[] elements;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.elements = new Object[0];
            }
            else if (newInput instanceof ClientSnapshot)
            {
                this.elements = asList(((ClientSnapshot) newInput).groupByCategory());
            }
            else if (newInput instanceof PortfolioSnapshot)
            {
                this.elements = asList(((PortfolioSnapshot) newInput).groupByCategory());
            }
            else
            {
                throw new RuntimeException("Unsupported type: " + newInput.getClass().getName()); //$NON-NLS-1$
            }
        }

        private Object[] asList(List<AssetCategory> categories)
        {
            List<Object> answer = new ArrayList<Object>();
            for (AssetCategory cat : categories)
            {
                answer.add(cat);
                answer.addAll(cat.getPositions());
            }
            return answer.toArray();
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return this.elements;
        }

        @Override
        public void dispose()
        {}
    }
}
