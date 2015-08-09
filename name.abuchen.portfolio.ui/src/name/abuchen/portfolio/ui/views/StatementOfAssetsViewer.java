package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeTypes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
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
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.MarkDirtyListener;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.OptionLabelProvider;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.StringEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.TaxonomyColumn;

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
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
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
        String taxonomyId = owner.getPart().getPreferenceStore().getString(this.getClass().getSimpleName());

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
        ColumnViewerToolTipSupport.enableFor(assets, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(assets);

        support = new ShowHideColumnHelper(StatementOfAssetsViewer.class.getName(), client, owner.getPreferenceStore(),
                        assets, layout);

        Column column = new Column("0", Messages.ColumnSharesOwned, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object e)
            {
                Element element = (Element) e;
                return element.isSecurity() ? element.getSecurityPosition().getShares() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                Element element = (Element) e;
                return element.isSecurity() ? Values.Share.format(element.getSecurityPosition().getShares()) : null;
            }
        });
        support.addColumn(column);

        column = new NameColumn("1"); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (((Element) e).isGroupByTaxonomy())
                    return Messages.LabelTotalSum;
                return super.getText(e);
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
            }

            @Override
            public Image getImage(Object e)
            {
                if (((Element) e).isCategory())
                    return null;
                return super.getImage(e);
            }
        });
        column.setEditingSupport(new StringEditingSupport(Named.class, "name") //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                boolean isCategory = ((Element) element).isCategory();
                boolean isUnassignedCategory = isCategory
                                && Classification.UNASSIGNED_ID.equals(((Element) element).getCategory()
                                                .getClassification().getId());

                return !isUnassignedCategory ? super.canEdit(element) : false;
            }

        }.setMandatory(true).addListener(new MarkDirtyListener(this.owner)));
        column.setSorter(null);
        support.addColumn(column);

        column = new Column("2", Messages.ColumnTicker, SWT.None, 60); //$NON-NLS-1$
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

        column = new Column("12", Messages.ColumnWKN, SWT.None, 60); //$NON-NLS-1$
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

        column = new IsinColumn("3"); //$NON-NLS-1$
        column.getEditingSupport().addListener(new MarkDirtyListener(this.owner));
        column.setSorter(null);
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("4", Messages.ColumnQuote, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isSecurity())
                    return null;

                Money money = Money.of(element.getSecurity().getCurrencyCode(), element.getSecurityPosition()
                                .getPrice().getValue());
                return Values.Money.format(money, client.getBaseCurrency());
            }
        });
        support.addColumn(column);

        column = new Column("5", Messages.ColumnMarketValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                return Values.Money.format(element.getValuation(), client.getBaseCurrency());
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
            }
        });
        support.addColumn(column);

        column = new Column("6", Messages.ColumnShareInPercent, SWT.RIGHT, 80); //$NON-NLS-1$
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

        column = new Column("7", Messages.ColumnPurchasePrice, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchasePrice_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (element.isSecurity())
                {
                    Money purchasePrice = element.getSecurityPosition().getFIFOPurchasePrice();
                    return Values.Money.formatNonZero(purchasePrice, client.getBaseCurrency());
                }
                return null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("8", Messages.ColumnPurchaseValue, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                Money purchaseValue = element.getFIFOPurchaseValue();
                return Values.Money.formatNonZero(purchaseValue, client.getBaseCurrency());
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("9", Messages.ColumnProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                Money profitLoss = element.getProfitLoss();
                return Values.Money.formatNonZero(profitLoss, client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object e)
            {
                Element element = (Element) e;
                Money profitLoss = element.getProfitLoss();
                return Display.getCurrent().getSystemColor(
                                profitLoss.isNegative() ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN);
            }

            @Override
            public Font getFont(Object e)
            {
                return ((Element) e).isGroupByTaxonomy() || ((Element) e).isCategory() ? boldFont : null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(new MarkDirtyListener(this.owner));
        column.setSorter(null);
        support.addColumn(column);

        column = new Column("10", Messages.ColumnIRRPerformance, SWT.RIGHT, 80); //$NON-NLS-1$
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

        column = new Column("11", Messages.ColumnTotalProfitLoss, SWT.RIGHT, 80); //$NON-NLS-1$
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
                    return Values.Money.format(record.getDelta(), client.getBaseCurrency());
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
                    Money delta = record.getDelta();

                    if (delta.isNegative())
                        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
                    else if (delta.isPositive())
                        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
                }
                return null;
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        addTaxonomyColumns();
        addAttributeColumns();
        addCurrencyColumns();

        support.createColumns();

        assets.getTable().setHeaderVisible(true);
        assets.getTable().setLinesVisible(true);

        assets.setContentProvider(new StatementOfAssetsContentProvider());

        ViewerHelper.pack(assets);

        assets.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(assets));

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), assets.getTable());
        boldFont = resources.createFont(FontDescriptor.createFrom(assets.getTable().getFont()).setStyle(SWT.BOLD));
    }

    private void addAttributeColumns()
    {
        for (final AttributeType attribute : AttributeTypes.available(Security.class))
        {
            Column column = new AttributeColumn(attribute);
            column.setVisible(false);
            column.setSorter(null);
            column.getEditingSupport().addListener(new MarkDirtyListener(this.owner));
            support.addColumn(column);
        }
    }

    private void addTaxonomyColumns()
    {
        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            Column column = new TaxonomyColumn(taxonomy);
            column.setVisible(false);
            column.setSorter(null);
            support.addColumn(column);
        }
    }

    private void addCurrencyColumns()
    {
        Column column = new Column("baseCurrency", Messages.ColumnCurrency, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isPosition())
                    return null;

                return element.getPosition().getInvestmentVehicle().getCurrencyCode();
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("exchangeRate", Messages.ColumnExchangeRate, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isPosition())
                    return null;

                String baseCurrency = element.getPosition().getInvestmentVehicle().getCurrencyCode();
                CurrencyConverter converter = getCurrencyConverter();
                return Values.ExchangeRate.format(converter.getRate(getDate(), baseCurrency).getValue());
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("marketValueBaseCurrency", //$NON-NLS-1$
                        Messages.ColumnMarketValue + Messages.BaseCurrencyCue, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnMarketValueBaseCurrency);
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isPosition())
                    return null;

                return Values.Money.format(element.getPosition().getPosition().calculateValue(),
                                client.getBaseCurrency());
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("purchaseValueBaseCurrency", //$NON-NLS-1$
                        Messages.ColumnPurchaseValue + Messages.BaseCurrencyCue, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPurchaseValueBaseCurrency);
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isPosition())
                    return null;

                return Values.Money.formatNonZero(element.getPosition().getPosition().getFIFOPurchaseValue(),
                                client.getBaseCurrency());
            }
        });
        column.setVisible(false);
        support.addColumn(column);

        column = new Column("profitLossBaseCurrency", //$NON-NLS-1$
                        Messages.ColumnProfitLoss + Messages.BaseCurrencyCue, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnProfitLossBaseCurrency);
        column.setGroupLabel(Messages.ColumnForeignCurrencies);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Element element = (Element) e;
                if (!element.isPosition())
                    return null;

                return Values.Money.formatNonZero(element.getPosition().getPosition().getProfitLoss(),
                                client.getBaseCurrency());
            }
        });
        column.setVisible(false);
        support.addColumn(column);
    }

    public void hookMenuListener(IMenuManager manager, final AbstractFinanceView view)
    {
        Element element = (Element) ((IStructuredSelection) assets.getSelection()).getFirstElement();
        if (element == null)
            return;

        if (element.isAccount())
        {
            new AccountContextMenu(view).menuAboutToShow(manager, element.getAccount(), null);
        }
        else if (element.isSecurity())
        {
            Portfolio portfolio = portfolioSnapshot != null ? portfolioSnapshot.getSource() : null;
            new SecurityContextMenu(view).menuAboutToShow(manager, element.getSecurity(), portfolio);
        }
    }

    public void pack()
    {
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

    public void showSaveMenu(Shell shell)
    {
        support.showSaveMenu(shell);
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
        internalSetInput(snapshot != null ? snapshot.groupByTaxonomy(taxonomy) : null);
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

    private CurrencyConverter getCurrencyConverter()
    {
        if (clientSnapshot != null)
            return clientSnapshot.getCurrencyConverter();
        else if (portfolioSnapshot != null)
            return portfolioSnapshot.getCurrencyConverter();
        else
            return null;
    }

    private LocalDate getDate()
    {
        if (clientSnapshot != null)
            return clientSnapshot.getTime();
        else if (portfolioSnapshot != null)
            return portfolioSnapshot.getTime();
        else
            return null;
    }

    private void calculatePerformance(Element element, Integer option)
    {
        // already calculated?
        if (element.getPerformance(option) != null)
            return;

        if (clientSnapshot == null && portfolioSnapshot == null)
            return;

        // start date
        LocalDate endDate = clientSnapshot != null ? clientSnapshot.getTime() : portfolioSnapshot.getTime();
        LocalDate startDate = endDate.minusYears(option.intValue()).withDayOfMonth(1).minusDays(1);

        SecurityPerformanceSnapshot sps = null;

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(startDate, endDate);
        if (clientSnapshot != null)
        {
            sps = SecurityPerformanceSnapshot.create(client, clientSnapshot.getCurrencyConverter(), period);
        }
        else
        {
            sps = SecurityPerformanceSnapshot.create(client, portfolioSnapshot.getCurrencyConverter(),
                            portfolioSnapshot.getSource(), period);
        }

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
            owner.getPart().getPreferenceStore().setValue(this.getClass().getSimpleName(), taxonomy.getId());

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

        public Money getValuation()
        {
            if (position != null)
                return position.getValuation();
            else if (category != null)
                return category.getValuation();
            else
                return groupByTaxonomy.getValuation();
        }

        public Money getFIFOPurchaseValue()
        {
            if (position != null)
                return position.getFIFOPurchaseValue();
            else if (category != null)
                return category.getFIFOPurchaseValue();
            else
                return groupByTaxonomy.getFIFOPurchaseValue();
        }

        public Money getProfitLoss()
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
            if (type == Security.class)
            {
                return type.cast(getSecurity());
            }
            else if (type == Attributable.class)
            {
                return type.cast(getSecurity());
            }
            else if (type == Named.class || type == Annotated.class)
            {
                if (isSecurity())
                    return type.cast(getSecurity());
                else if (isAccount())
                    return type.cast(getAccount());
                else if (isCategory())
                    return type.cast(getCategory().getClassification());
                else
                    return null;
            }
            else if (type == InvestmentVehicle.class)
            {
                if (isSecurity())
                    return type.cast(getSecurity());
                else if (isAccount())
                    return type.cast(getAccount());
                else
                    return null;
            }
            else
            {
                return null;
            }
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
