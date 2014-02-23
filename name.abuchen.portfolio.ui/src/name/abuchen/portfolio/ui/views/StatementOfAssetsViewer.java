package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.GroupByTaxonomy;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.OptionLabelProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

public class StatementOfAssetsViewer
{
    private Composite container;
    private TableViewer assets;

    private Font boldFont;
    private Menu contextMenu;

    private AbstractFinanceView owner;
    private ShowHideColumnHelper support;

    private final Client client;
    private ClientSnapshot clientSnapshot;
    private PortfolioSnapshot portfolioSnapshot;
    private Taxonomy taxonomy;

    public StatementOfAssetsViewer(Composite parent, AbstractFinanceView owner, Client client)
    {
        this.owner = owner;
        this.client = client;

        loadTaxonomy(client);

        createColumns(parent);

        this.assets.getTable().addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                StatementOfAssetsViewer.this.widgetDisposed();
            }
        });
    }

    private void loadTaxonomy(Client client)
    {
        String taxonomyId = owner.getClientEditor().getPreferenceStore().getString(this.getClass().getSimpleName());

        if (taxonomyId != null)
        {
            for (Taxonomy t : client.getTaxonomies())
            {
                if (taxonomyId.equals(t.getId()))
                {
                    this.taxonomy = t;
                    break;
                }
            }
        }

        if (this.taxonomy == null && !client.getTaxonomies().isEmpty())
            this.taxonomy = client.getTaxonomies().get(0);
    }

    private void createColumns(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        assets = new TableViewer(container, SWT.FULL_SELECTION);

        support = new ShowHideColumnHelper(StatementOfAssetsViewer.class.getName(), assets, layout);

        Column column = new Column(Messages.ColumnSharesOwned, SWT.RIGHT, 80);
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
                if (element.isGroupByTaxonomy())
                    return Messages.LabelTotalSum;
                else if (element.isCategory())
                    return element.getCategory().getClassification().toString();
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
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
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
                return Values.Amount.format(element.getValuation());
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
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
                if (element.isGroupByTaxonomy())
                    return Values.Percent.format(1d);
                if (element.isCategory())
                    return Values.Percent.format(element.getCategory().getShare());
                else
                    return Values.Percent.format(element.getPosition().getShare());
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
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
                long purchaseValue = element.getFIFOPurchaseValue();
                return purchaseValue == 0 ? null : Values.Amount.format(purchaseValue);
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
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
                long profitLoss = element.getProfitLoss();
                return profitLoss == 0 ? null : Values.Amount.format(profitLoss);
            }

            @Override
            public Color getForeground(Object e)
            {
                Element element = (Element) e;
                long profitLoss = element.getProfitLoss();

                if (profitLoss < 0)
                    return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
                else if (profitLoss > 0)
                    return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
                else
                    return null;
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnIRRPerformance, SWT.RIGHT, 80);
        column.setOptions(Messages.LabelReportingYears, Messages.ColumnIRRPerformanceOption, 1, 2, 3, 4, 5, 10);
        column.setLabelProvider(new OptionLabelProvider()
        {
            @Override
            public String getText(Object e, Integer option)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    calculatePerformance(element, option);
                    SecurityPerformanceRecord record = element.getPerformance(option);
                    return Values.Percent.format(record.getIrr());
                }
                return null;
            }

            @Override
            public Color getForeground(Object e, Integer option)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    calculatePerformance(element, option);
                    SecurityPerformanceRecord record = element.getPerformance(option);
                    double irr = record.getIrr();

                    if (irr < 0)
                        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
                    else if (irr > 0)
                        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
                }
                return null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnTotalProfitLoss, SWT.RIGHT, 80);
        column.setOptions(Messages.LabelReportingYears, Messages.ColumnTotalProfitLossOption, 1, 2, 3, 4, 5, 10);
        column.setLabelProvider(new OptionLabelProvider()
        {
            @Override
            public String getText(Object e, Integer option)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    calculatePerformance(element, option);
                    SecurityPerformanceRecord record = element.getPerformance(option);
                    return Values.Amount.format(record.getDelta());
                }
                return null;
            }

            @Override
            public Color getForeground(Object e, Integer option)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    calculatePerformance(element, option);
                    SecurityPerformanceRecord record = element.getPerformance(option);
                    long delta = record.getDelta();

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

        column = new Column(Messages.ColumnWKN, SWT.None, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                return element.isSecurity() ? element.getSecurity().getWkn() : null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 22); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                return element.isSecurity() ? element.getSecurity().getNote() : null;
            }

            @Override
            public Image getImage(Object e)
            {
                Element element = (Element) e;
                if (!element.isSecurity())
                    return null;

                String note = element.getSecurity().getNote();
                return note != null && note.length() > 0 ? PortfolioPlugin.image(PortfolioPlugin.IMG_NOTE) : null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        support.createColumns();

        assets.getTable().setHeaderVisible(true);
        assets.getTable().setLinesVisible(true);

        assets.setContentProvider(new StatementOfAssetsContentProvider());

        assets.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(assets));

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), assets.getTable());
        boldFont = resources.createFont(FontDescriptor.createFrom(assets.getTable().getFont()).setStyle(SWT.BOLD));
    }

    public void hookMenuListener(IMenuManager manager, final AbstractFinanceView view)
    {
        Element element = (Element) ((IStructuredSelection) assets.getSelection()).getFirstElement();
        if (element == null)
            return;

        if (element.isAccount())
        {
            new AccountContextMenu(view).menuAboutToShow(manager, element.getAccount());
        }
        else if (element.isSecurity())
        {
            new SecurityContextMenu(view).menuAboutToShow(manager, element.getSecurity());
        }
    }

    public void pack()
    {
        if (!support.isUserConfigured())
            ViewerHelper.pack(assets);
    }

    public TableViewer getTableViewer()
    {
        return assets;
    }

    public Control getControl()
    {
        return container;
    }

    public void showConfigMenu(Shell shell)
    {
        if (contextMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    StatementOfAssetsViewer.this.menuAboutToShow(manager);
                }
            });

            contextMenu = menuMgr.createContextMenu(shell);
        }

        contextMenu.setVisible(true);
    }

    private void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new LabelOnly(Messages.LabelTaxonomies));
        for (final Taxonomy t : client.getTaxonomies())
        {
            Action action = new Action(t.getName())
            {
                @Override
                public void run()
                {
                    taxonomy = t;

                    if (clientSnapshot != null)
                        internalSetInput(clientSnapshot.groupByTaxonomy(taxonomy));
                    else
                        internalSetInput(portfolioSnapshot.groupByTaxonomy(taxonomy));
                }
            };
            action.setChecked(t.equals(taxonomy));
            manager.add(action);
        }

        manager.add(new Separator());

        manager.add(new LabelOnly(Messages.LabelColumns));
        support.menuAboutToShow(manager);
    }

    public void setInput(ClientSnapshot snapshot)
    {
        this.clientSnapshot = snapshot;
        this.portfolioSnapshot = null;
        internalSetInput(snapshot.groupByTaxonomy(taxonomy));
    }

    public void setInput(PortfolioSnapshot snapshot)
    {
        this.clientSnapshot = null;
        this.portfolioSnapshot = snapshot;
        internalSetInput(snapshot.groupByTaxonomy(taxonomy));
    }

    private void internalSetInput(GroupByTaxonomy grouping)
    {
        assets.getTable().setRedraw(false);
        try
        {
            assets.setInput(grouping);
            assets.refresh();
        }
        finally
        {
            assets.getTable().setRedraw(true);
        }
    }

    private void calculatePerformance(Element element, Integer option)
    {
        // already calculated?
        if (element.getPerformance(option) != null)
            return;

        if (clientSnapshot == null && portfolioSnapshot == null)
            return;

        // start date
        Date endDate = clientSnapshot != null ? clientSnapshot.getTime() : portfolioSnapshot.getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);
        cal.add(Calendar.YEAR, -option.intValue());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DATE, -1);

        SecurityPerformanceSnapshot sps = null;

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(cal.getTime(), endDate);
        if (clientSnapshot != null)
            sps = SecurityPerformanceSnapshot.create(client, period);
        else
            sps = SecurityPerformanceSnapshot.create(client, portfolioSnapshot.getSource(), period);

        StatementOfAssetsContentProvider contentProvider = (StatementOfAssetsContentProvider) assets
                        .getContentProvider();

        for (Element e : contentProvider.elements)
        {
            if (e.isSecurity())
            {
                for (SecurityPerformanceRecord r : sps.getRecords())
                {
                    if (r.getSecurity().equals(e.getSecurity()))
                    {
                        e.setPerformance(option, r);
                        break;
                    }
                }
            }
        }
    }

    private void widgetDisposed()
    {
        if (taxonomy != null)
            owner.getClientEditor().getPreferenceStore().setValue(this.getClass().getSimpleName(), taxonomy.getId());

        if (contextMenu != null)
            contextMenu.dispose();
    }

    private static class Element implements Adaptable
    {
        private GroupByTaxonomy groupByTaxonomy;
        private AssetCategory category;
        private AssetPosition position;

        private transient Map<Integer, SecurityPerformanceRecord> performance = new HashMap<Integer, SecurityPerformanceRecord>();

        private Element(AssetCategory category)
        {
            this.category = category;
        }

        private Element(AssetPosition position)
        {
            this.position = position;
        }

        private Element(GroupByTaxonomy groupByTaxonomy)
        {
            this.groupByTaxonomy = groupByTaxonomy;
        }

        public void setPerformance(Integer year, SecurityPerformanceRecord record)
        {
            performance.put(year, record);
        }

        public SecurityPerformanceRecord getPerformance(Integer year)
        {
            return performance.get(year);
        }

        public boolean isGroupByTaxonomy()
        {
            return groupByTaxonomy != null;
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

        public boolean isAccount()
        {
            return position != null && position.getInvestmentVehicle() instanceof Account;
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

        public Account getAccount()
        {
            return isAccount() ? (Account) position.getInvestmentVehicle() : null;
        }

        public Long getValuation()
        {
            if (position != null)
                return position.getValuation();
            else if (category != null)
                return category.getValuation();
            else
                return groupByTaxonomy.getValuation();
        }

        public long getFIFOPurchaseValue()
        {
            if (position != null)
                return position.getFIFOPurchaseValue();
            else if (category != null)
                return category.getFIFOPurchaseValue();
            else
                return groupByTaxonomy.getFIFOPurchaseValue();
        }

        public long getProfitLoss()
        {
            if (position != null)
                return position.getProfitLoss();
            else if (category != null)
                return category.getProfitLoss();
            else
                return groupByTaxonomy.getProfitLoss();
        }

        @Override
        public <T> T adapt(Class<T> type)
        {
            return type == Security.class ? type.cast(getSecurity()) : null;
        }
    }

    private static class StatementOfAssetsContentProvider implements IStructuredContentProvider
    {
        private Element[] elements;

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.elements = new Element[0];
            }
            else if (newInput instanceof GroupByTaxonomy)
            {
                this.elements = flatten((GroupByTaxonomy) newInput);
            }
            else
            {
                throw new RuntimeException("Unsupported type: " + newInput.getClass().getName()); //$NON-NLS-1$
            }
        }

        private Element[] flatten(GroupByTaxonomy categories)
        {
            List<Element> answer = new ArrayList<Element>();
            for (AssetCategory cat : categories.asList())
            {
                answer.add(new Element(cat));
                for (AssetPosition p : cat.getPositions())
                    answer.add(new Element(p));
            }
            answer.add(new Element(categories));
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
