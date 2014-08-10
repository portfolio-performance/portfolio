package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.DateEditingSupport;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.ValueEditingSupport;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportQuotesWizard;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;
import name.abuchen.portfolio.ui.wizards.security.SearchYahooWizard;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;

public class SecurityListView extends AbstractListView implements ModificationListener
{
    private class CreateSecurityDropDown extends AbstractDropDown
    {
        public CreateSecurityDropDown(ToolBar toolBar)
        {
            super(toolBar, Messages.SecurityMenuAddNewSecurity, //
                            PortfolioPlugin.image(PortfolioPlugin.IMG_PLUS), SWT.NONE);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new Action(Messages.SecurityMenuNewSecurity)
            {
                @Override
                public void run()
                {
                    Security newSecurity = new Security();
                    newSecurity.setFeed(QuoteFeed.MANUAL);
                    openEditDialog(newSecurity);
                }
            });

            manager.add(new Action(Messages.SecurityMenuSearchYahoo)
            {
                @Override
                public void run()
                {
                    SearchYahooWizard wizard = new SearchYahooWizard(getClient());
                    Dialog dialog = new WizardDialog(getToolBar().getShell(), wizard);

                    if (dialog.open() == Dialog.OK)
                    {
                        Security newSecurity = wizard.getSecurity();
                        openEditDialog(newSecurity);
                    }
                }
            });
        }

        private void openEditDialog(Security newSecurity)
        {
            Dialog dialog = new EditSecurityDialog(getToolBar().getShell(), getClient(), newSecurity);

            if (dialog.open() == Dialog.OK)
            {
                markDirty();
                getClient().addSecurity(newSecurity);

                if (watchlist != null)
                    watchlist.getSecurities().add(newSecurity);

                setSecurityTableInput();
                securities.updateQuotes(newSecurity);
            }
        }
    }

    private SecuritiesTable securities;
    private TableViewer prices;
    private TableViewer transactions;
    private TableViewer events;
    private TimelineChart chart;
    private SecurityDetailsViewer latest;

    private Date chartPeriod;

    private Watchlist watchlist;

    private Pattern filterPattern;

    public SecurityListView()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -2);
        chartPeriod = cal.getTime();
    }

    @Override
    protected String getTitle()
    {
        return watchlist == null ? Messages.LabelSecurities : Messages.LabelSecurities + " " + watchlist.getName(); //$NON-NLS-1$
    }

    @Override
    protected int[] getDefaultWeights(Control[] children)
    {
        return new int[] { 50, 50 };
    }

    @Override
    public void notifyModelUpdated()
    {
        if (securities != null)
            setSecurityTableInput();
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        // called from prices table
        Security security = (Security) prices.getData(Security.class.toString());

        // if the date changed, the prices must be reordered --> binary search
        if (newValue instanceof Date)
        {
            SecurityPrice price = (SecurityPrice) element;
            security.removePrice(price);
            security.addPrice(price);
        }

        securities.refresh(security);
        prices.refresh(element);
        latest.setInput(security);
        transactions.setInput(security.getTransactions(getClient()));
        events.setInput(security.getEvents());
        updateChart(security);

        markDirty();
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        if (parameter instanceof Watchlist)
            this.watchlist = (Watchlist) parameter;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addSearchButton(toolBar);

        new ToolItem(toolBar, SWT.SEPARATOR | SWT.VERTICAL).setWidth(20);

        new CreateSecurityDropDown(toolBar);
        addExportButton(toolBar);
        addSaveButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addSearchButton(ToolBar toolBar)
    {
        final Text search = new Text(toolBar, SWT.SEARCH | SWT.ICON_CANCEL);
        search.setSize(100, SWT.DEFAULT);
        search.setMessage(Messages.LabelSearch);

        ToolItem toolItem = new ToolItem(toolBar, SWT.SEPARATOR);
        toolItem.setWidth(search.getSize().x);
        toolItem.setControl(search);

        search.addModifyListener(new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                String filter = search.getText().trim();
                if (filter.length() == 0)
                {
                    filterPattern = null;
                    securities.refresh();
                }
                else
                {
                    filterPattern = Pattern.compile(".*" + filter + ".*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
                    securities.refresh();
                }
            }
        });
    }

    private void addExportButton(ToolBar toolBar)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(securities.getTableViewer()) //
                                .export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_EXPORT));
        export.setToolTipText(Messages.MenuExportData);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    private void addSaveButton(ToolBar toolBar)
    {
        Action save = new Action()
        {
            @Override
            public void run()
            {
                securities.getColumnHelper().showSaveMenu(getActiveShell());
            }
        };
        save.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_SAVE));
        save.setToolTipText(Messages.MenuConfigureChart);
        new ActionContributionItem(save).fill(toolBar, -1);
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action config = new Action()
        {
            @Override
            public void run()
            {
                securities.getColumnHelper().showHideShowColumnsMenu(getActiveShell());
            }
        };
        config.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_CONFIG));
        config.setToolTipText(Messages.MenuShowHideColumns);

        new ActionContributionItem(config).fill(toolBar, -1);
    }

    // //////////////////////////////////////////////////////////////
    // top table: securities
    // //////////////////////////////////////////////////////////////

    protected void createTopTable(Composite parent)
    {
        securities = new SecuritiesTable(parent, this);

        securities.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                onSecurityChanged((Security) ((IStructuredSelection) event.getSelection()).getFirstElement());
            }
        });

        securities.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (filterPattern == null)
                    return true;

                Security security = (Security) element;

                String[] properties = new String[] { security.getName(), //
                                security.getIsin(), //
                                security.getTickerSymbol(), //
                                security.getWkn(), //
                                security.getNote() //
                };

                for (String property : properties)
                {
                    if (property != null && filterPattern.matcher(property).matches())
                        return true;
                }

                return false;
            }
        });

        setSecurityTableInput();
    }

    private void setSecurityTableInput()
    {
        if (watchlist != null)
            securities.setInput(watchlist);
        else
            securities.setInput(getClient().getSecurities());
    }

    private void onSecurityChanged(Security security)
    {
        prices.setData(Security.class.toString(), security);
        prices.setInput(security != null ? security.getPrices() : new ArrayList<SecurityPrice>(0));
        prices.refresh();

        latest.setInput(security);

        transactions.setInput(security != null ? security.getTransactions(getClient()) : new ArrayList<Transaction>(0));

        events.setInput(security != null ? security.getEvents() : Collections.emptyList());

        updateChart(security);
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: folder
    // //////////////////////////////////////////////////////////////

    protected void createBottomTable(Composite parent)
    {
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);

        // folder
        CTabFolder folder = new CTabFolder(sash, SWT.BORDER);

        // latest
        latest = new SecurityDetailsViewer(sash, SWT.BORDER, getClient());
        latest.getControl().pack();
        int width = latest.getControl().getBounds().width;
        sash.setWeights(new int[] { parent.getParent().getParent().getBounds().width - width, width });

        // tab 1: chart
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabChart);

        Composite chartComposite = new Composite(folder, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).applyTo(chartComposite);
        item.setControl(chartComposite);

        chart = new TimelineChart(chartComposite);
        chart.getTitle().setText("..."); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

        Composite buttons = new Composite(chartComposite, SWT.NONE);
        buttons.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridDataFactory.fillDefaults().grab(false, true).applyTo(buttons);
        RowLayoutFactory.fillDefaults().type(SWT.VERTICAL).spacing(2).fill(true).applyTo(buttons);

        addButton(buttons, Messages.SecurityTabChart1M, Calendar.MONTH, -1);
        addButton(buttons, Messages.SecurityTabChart2M, Calendar.MONTH, -2);
        addButton(buttons, Messages.SecurityTabChart6M, Calendar.MONTH, -6);
        addButton(buttons, Messages.SecurityTabChart1Y, Calendar.YEAR, -1);
        addButton(buttons, Messages.SecurityTabChart2Y, Calendar.YEAR, -2);
        addButton(buttons, Messages.SecurityTabChart3Y, Calendar.YEAR, -3);
        addButton(buttons, Messages.SecurityTabChart5Y, Calendar.YEAR, -5);
        addButton(buttons, Messages.SecurityTabChart10Y, Calendar.YEAR, -10);

        Button button = new Button(buttons, SWT.FLAT);
        button.setText(Messages.SecurityTabChartAll);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                chartPeriod = null;

                Security security = (Security) prices.getData(Security.class.toString());
                updateChart(security);
            }
        });

        // tab 2: historical quotes
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabHistoricalQuotes);
        item.setControl(createPricesTable(folder));

        // tab 3: transactions
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabTransactions);
        item.setControl(createTransactionTable(folder));

        // tab 4: event
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabEvents);
        item.setControl(createEventsTable(folder));

        folder.setSelection(0);
    }

    private void addButton(Composite buttons, String label, final int field, final int amount)
    {
        Button b = new Button(buttons, SWT.FLAT);
        b.setText(label);
        b.addSelectionListener(new ChartPeriodSelectionListener()
        {
            @Override
            protected void roll(Calendar cal)
            {
                cal.add(field, amount);
            }
        });
    }

    private abstract class ChartPeriodSelectionListener implements SelectionListener
    {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
            Calendar cal = Calendar.getInstance();
            roll(cal);
            chartPeriod = cal.getTime();

            Security security = (Security) prices.getData(Security.class.toString());
            updateChart(security);
        }

        protected abstract void roll(Calendar cal);

        @Override
        public void widgetDefaultSelected(SelectionEvent e)
        {}
    }

    // //////////////////////////////////////////////////////////////
    // tab item: prices
    // //////////////////////////////////////////////////////////////

    protected Composite createPricesTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        prices = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnEditingSupport.prepare(prices);
        ShowHideColumnHelper support = new ShowHideColumnHelper(SecurityListView.class.getSimpleName() + "@prices", //$NON-NLS-1$
                        getPreferenceStore(), prices, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((SecurityPrice) element).getTime());
            }
        });
        ColumnViewerSorter.create(SecurityPrice.class, "time").attachTo(column, SWT.UP); //$NON-NLS-1$
        new DateEditingSupport(SecurityPrice.class, "time").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Quote.format(((SecurityPrice) element).getValue());
            }
        });
        ColumnViewerSorter.create(SecurityPrice.class, "value").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(SecurityPrice.class, "value", Values.Quote).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        prices.getTable().setHeaderVisible(true);
        prices.getTable().setLinesVisible(true);

        prices.setContentProvider(new SimpleListContentProvider(true));

        hookContextMenu(prices.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPricesContextMenu(manager);
            }
        });

        return container;
    }

    private void fillPricesContextMenu(IMenuManager manager)
    {
        boolean isSecuritySelected = prices.getData(Security.class.toString()) != null;
        if (isSecuritySelected)
        {
            manager.add(new Action(Messages.SecurityMenuAddPrice)
            {
                @Override
                public void run()
                {
                    Security security = (Security) prices.getData(Security.class.toString());
                    if (security == null)
                        return;

                    SecurityPrice price = new SecurityPrice();
                    price.setTime(Dates.today());

                    security.addPrice(price);

                    markDirty();

                    prices.setInput(security.getPrices());
                    latest.setInput(security);
                    transactions.setInput(security.getTransactions(getClient()));
                    events.setInput(security.getEvents());
                    updateChart(security);

                    prices.setSelection(new StructuredSelection(price), true);
                    prices.editElement(price, 0);
                }
            });
            manager.add(new Separator());
        }

        if (((IStructuredSelection) prices.getSelection()).getFirstElement() != null)
        {
            manager.add(new Action(Messages.SecurityMenuDeletePrice)
            {
                @Override
                public void run()
                {
                    Security security = (Security) prices.getData(Security.class.toString());
                    if (security == null)
                        return;

                    Iterator<?> iter = ((IStructuredSelection) prices.getSelection()).iterator();
                    while (iter.hasNext())
                    {
                        SecurityPrice price = (SecurityPrice) iter.next();
                        if (price == null)
                            continue;

                        security.removePrice(price);
                    }

                    markDirty();

                    prices.setInput(security.getPrices());
                    latest.setInput(security);
                    transactions.setInput(security.getTransactions(getClient()));
                    events.setInput(security.getEvents());
                    updateChart(security);
                }
            });
        }

        if (prices.getTable().getItemCount() > 0)
        {
            manager.add(new Action(Messages.SecurityMenuDeleteAllPrices)
            {
                @Override
                public void run()
                {
                    Security security = (Security) prices.getData(Security.class.toString());
                    if (security == null)
                        return;

                    security.removeAllPrices();

                    markDirty();

                    prices.setInput(security.getPrices());
                    latest.setInput(security);
                    transactions.setInput(security.getTransactions(getClient()));
                    events.setInput(security.getEvents());
                    updateChart(security);
                }
            });
        }

        if (isSecuritySelected)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.SecurityMenuUpdateQuotes)
            {
                @Override
                public void run()
                {
                    Security security = (Security) prices.getData(Security.class.toString());
                    if (security != null)
                        securities.updateQuotes(security);
                }
            });
            manager.add(new Action(Messages.SecurityMenuImportQuotes)
            {
                @Override
                public void run()
                {
                    Security security = (Security) prices.getData(Security.class.toString());
                    if (security == null)
                        return;

                    Dialog dialog = new WizardDialog(getActiveShell(), new ImportQuotesWizard(security));
                    if (dialog.open() != Dialog.OK)
                        return;

                    markDirty();
                    securities.refresh(security);
                    onSecurityChanged(security);
                }
            });
        }
    }

    // //////////////////////////////////////////////////////////////
    // tab item: chart
    // //////////////////////////////////////////////////////////////

    private void updateChart(Security security)
    {
        ISeries series = chart.getSeriesSet().getSeries(Messages.ColumnQuote);
        if (series != null)
            chart.getSeriesSet().deleteSeries(Messages.ColumnQuote);
        chart.clearMarkerLines();

        if (security == null || security.getPrices().isEmpty())
        {
            chart.getTitle().setText(security == null ? "..." : security.getName()); //$NON-NLS-1$
            chart.redraw();
            return;
        }

        chart.getTitle().setText(security.getName());

        List<SecurityPrice> prices = security.getPrices();

        int index;
        Date[] dates;
        double[] values;

        if (chartPeriod == null)
        {
            index = 0;
            dates = new Date[prices.size()];
            values = new double[prices.size()];
        }
        else
        {
            index = Math.abs(Collections.binarySearch(prices, new SecurityPrice(chartPeriod, 0),
                            new SecurityPrice.ByDate()));

            if (index >= prices.size())
            {
                // no data available
                chart.redraw();
                return;
            }

            dates = new Date[prices.size() - index];
            values = new double[prices.size() - index];
        }

        for (int ii = 0; index < prices.size(); index++, ii++)
        {
            SecurityPrice p = prices.get(index);
            dates[ii] = p.getTime();
            values[ii] = p.getValue() / Values.Quote.divider();
        }

        ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, Messages.ColumnQuote);
        lineSeries.setXDateSeries(dates);
        lineSeries.setLineWidth(2);
        lineSeries.enableArea(true);
        lineSeries.setSymbolType(PlotSymbolType.NONE);
        lineSeries.setYSeries(values);
        lineSeries.setAntialias(SWT.ON);

        chart.getAxisSet().adjustRange();

        for (Portfolio portfolio : getClient().getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getSecurity() == security && (chartPeriod == null || chartPeriod.before(t.getDate())))
                {
                    String label = Values.Share.format(t.getShares());
                    switch (t.getType())
                    {
                        case BUY:
                        case TRANSFER_IN:
                        case DELIVERY_INBOUND:
                            chart.addMarkerLine(t.getDate(), new RGB(0, 128, 0), label);
                            break;
                        case SELL:
                        case TRANSFER_OUT:
                        case DELIVERY_OUTBOUND:
                            chart.addMarkerLine(t.getDate(), new RGB(128, 0, 0), "-" + label); //$NON-NLS-1$
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            }
        }

        for (SecurityEvent event : security.getEvents())
        {
            if (chartPeriod == null || chartPeriod.before(event.getDate()))
                chart.addMarkerLine(event.getDate(), new RGB(255, 140, 0), event.getDetails());
        }

        chart.redraw();
    }

    // //////////////////////////////////////////////////////////////
    // tab item: transactions
    // //////////////////////////////////////////////////////////////

    protected Composite createTransactionTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(transactions, ToolTip.NO_RECREATE);

        ShowHideColumnHelper support = new ShowHideColumnHelper(SecurityListView.class.getSimpleName()
                        + "@transactions2", getPreferenceStore(), transactions, layout); //$NON-NLS-1$

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((TransactionPair<?>) element).getTransaction().getDate());
            }
        });
        column.setSorter(ColumnViewerSorter.create(new Comparator<Object>()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                return ((TransactionPair<?>) o1).getTransaction().getDate()
                                .compareTo((((TransactionPair<?>) o2).getTransaction().getDate()));
            }
        }), SWT.UP);
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                if (t instanceof PortfolioTransaction)
                    return ((PortfolioTransaction) t).getType().toString();
                else if (t instanceof AccountTransaction)
                    return ((AccountTransaction) t).getType().toString();
                else
                    return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                if (t instanceof PortfolioTransaction)
                {
                    return ((PortfolioTransaction) t).getShares();
                }
                else if (t instanceof AccountTransaction)
                {
                    long shares = ((AccountTransaction) t).getShares();
                    return shares != 0 ? shares : null;
                }
                return null;
            }

            @Override
            public String getToolTipText(Object element)
            {
                Long v = getValue(element);
                return v != null ? Values.Share.format(v) : null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                return Values.Amount.format(t.getAmount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(new Comparator<Object>()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                long a1 = ((TransactionPair<?>) o1).getTransaction().getAmount();
                long a2 = ((TransactionPair<?>) o2).getTransaction().getAmount();
                return a1 > a2 ? 1 : a1 < a2 ? -1 : 0;
            }
        }));
        support.addColumn(column);

        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                if (t instanceof PortfolioTransaction)
                {
                    return Values.Amount.format(((PortfolioTransaction) t).getActualPurchasePrice());
                }
                else if (t instanceof AccountTransaction)
                {
                    long shares = ((AccountTransaction) t).getShares();
                    if (shares != 0)
                        return Values.Amount.format(Math.round(t.getAmount() * Values.Share.divider() / shares));
                }
                return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnPortfolio, SWT.NONE, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TransactionOwner<?> owner = ((TransactionPair<?>) element).getOwner();
                if (owner instanceof Portfolio)
                    return owner.toString();
                return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.NONE, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                TransactionPair<?> pair = (TransactionPair<?>) element;
                Transaction t = pair.getTransaction();
                if (t instanceof PortfolioTransaction)
                    return t.getCrossEntry() != null ? t.getCrossEntry().getCrossEntity(t).toString() : null;
                else
                    return pair.getOwner().toString();
            }
        });
        support.addColumn(column);

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider(true));

        return container;
    }

    // //////////////////////////////////////////////////////////////
    // tab item: transactions
    // //////////////////////////////////////////////////////////////

    protected Composite createEventsTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        events = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(SecurityListView.class.getSimpleName() + "@events", //$NON-NLS-1$
                        getPreferenceStore(), events, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((SecurityEvent) element).getDate());
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((SecurityEvent) element).getType().toString();
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnDetails, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((SecurityEvent) element).getDetails();
            }
        });
        support.addColumn(column);

        support.createColumns();

        events.getTable().setHeaderVisible(true);
        events.getTable().setLinesVisible(true);

        events.setContentProvider(new SimpleListContentProvider(true));

        return container;
    }
}
