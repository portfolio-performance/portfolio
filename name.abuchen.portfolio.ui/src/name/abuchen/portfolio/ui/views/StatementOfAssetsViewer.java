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
import name.abuchen.portfolio.snapshot.SecurityPosition;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

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
                Element element = (Element) e;
                return element.isSecurity() ? element.getSecurityPosition().getShares() : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnName, SWT.LEFT, 300);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isCategory())
                    return element.getCategory().getAssetClass() != null ? element.getCategory().getAssetClass().name()
                                    : Messages.LabelTotalSum;
                else
                    return element.getPosition().getDescription();
            }

            @Override
            public Image getImage(Object e)
            {
                Element element = (Element) e;
                if (element.isPosition())
                    return PortfolioPlugin.image(element.isSecurity() ? PortfolioPlugin.IMG_SECURITY
                                    : PortfolioPlugin.IMG_ACCOUNT);
                return null;
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isCategory() ? boldFont : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnTicker, SWT.None, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                return element.isSecurity() ? element.getSecurity().getTickerSymbol() : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnISIN, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                return element.isSecurity() ? element.getSecurity().getIsin() : null;
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
                Element element = (Element) e;
                return element.isSecurity() ? Values.Quote.format(element.getSecurityPosition().getPrice().getValue())
                                : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnMarketValue, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isCategory())
                    return Values.Amount.format(element.getCategory().getValuation());
                else
                    return Values.Amount.format(element.getPosition().getValuation());
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isCategory() ? boldFont : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnShareInPercent, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isCategory())
                    return Values.Percent.format(element.getCategory().getShare());
                else
                    return Values.Percent.format(element.getPosition().getShare());
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isCategory() ? boldFont : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnPurchasePrice, SWT.RIGHT, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    long purchasePrice = element.getSecurityPosition().getFIFOPurchasePrice();
                    return purchasePrice == 0 ? null : Values.Amount.format(purchasePrice);
                }
                return null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnPurchaseValue, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    long purchaseValue = element.getSecurityPosition().getFIFOPurchaseValue();
                    return purchaseValue == 0 ? null : Values.Amount.format(purchaseValue);
                }
                return null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnProfitLoss, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    long delta = element.getSecurityPosition().getDelta();
                    return delta == 0 ? null : Values.Amount.format(delta);
                }
                return null;
            }

            @Override
            public Color getForeground(Object e)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    long delta = element.getSecurityPosition().getDelta();

                    if (delta < 0)
                        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
                    else if (delta > 0)
                        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
                }
                return null;
            }
        });
        column.setVisible(false);
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
        Element element = (Element) ((IStructuredSelection) assets.getSelection()).getFirstElement();
        if (element == null || !element.isSecurity())
            return;

        final Security security = element.getSecurity();

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

    private static class Element
    {
        private AssetCategory category;
        private AssetPosition position;

        private Element(AssetCategory category)
        {
            this.category = category;
        }

        private Element(AssetPosition position)
        {
            this.position = position;
        }

        public boolean isCategory()
        {
            return category != null;
        }

        public boolean isPosition()
        {
            return position != null;
        }

        public boolean isSecurity()
        {
            return position != null && position.getSecurity() != null;
        }

        public AssetCategory getCategory()
        {
            return category;
        }

        public AssetPosition getPosition()
        {
            return position;
        }

        public SecurityPosition getSecurityPosition()
        {
            return position != null ? position.getPosition() : null;
        }

        public Security getSecurity()
        {
            return position != null ? position.getSecurity() : null;
        }
    }

    private static class StatementOfAssetsContentProvider implements IStructuredContentProvider
    {
        private Element[] elements;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.elements = new Element[0];
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

        private Element[] asList(List<AssetCategory> categories)
        {
            List<Element> answer = new ArrayList<Element>();
            for (AssetCategory cat : categories)
            {
                answer.add(new Element(cat));
                for (AssetPosition p : cat.getPositions())
                    answer.add(new Element(p));
            }
            return answer.toArray(new Element[0]);
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
