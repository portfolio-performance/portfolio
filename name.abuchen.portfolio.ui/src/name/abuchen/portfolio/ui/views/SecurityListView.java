package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.wizards.EditSecurityWizard;
import name.abuchen.portfolio.ui.wizards.ImportQuotesWizard;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;

public class SecurityListView extends AbstractListView
{
    private SecuritiesTable securities;
    private TableViewer prices;
    private TableViewer transactions;
    private TimelineChart chart;
    private LatestQuoteTable latest;

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

    protected void setWeights(SashForm sash)
    {
        sash.setWeights(new int[] { 50, 50 });
    }

    @Override
    public void notifyModelUpdated()
    {
        if (securities != null)
            setSecurityTableInput();
    }

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);

        if (parameter instanceof Watchlist)
            this.watchlist = (Watchlist) parameter;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addSearchButton(toolBar);

        new ToolItem(toolBar, SWT.SEPARATOR | SWT.VERTICAL).setWidth(20);

        addCreateSecurityButton(toolBar);
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

    private void addCreateSecurityButton(ToolBar toolBar)
    {
        ToolItem button = new ToolItem(toolBar, SWT.PUSH);
        button.setImage(PortfolioPlugin.image(PortfolioPlugin.IMG_PLUS));
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Security newSecurity = new Security();
                newSecurity.setFeed(QuoteFeed.MANUAL);
                newSecurity.setType(AssetClass.EQUITY);
                Dialog dialog = new WizardDialog(getClientEditor().getSite().getShell(), new EditSecurityWizard(
                                getClient(), newSecurity));
                if (dialog.open() == Dialog.OK)
                {
                    markDirty();
                    getClient().getSecurities().add(newSecurity);

                    if (watchlist != null)
                        watchlist.getSecurities().add(newSecurity);

                    setSecurityTableInput();
                    securities.updateQuotes(newSecurity);
                }
            }
        });
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

                if (security.getName() != null && filterPattern.matcher(security.getName()).matches())
                    return true;

                if (security.getIsin() != null && filterPattern.matcher(security.getIsin()).matches())
                    return true;

                if (security.getTickerSymbol() != null && filterPattern.matcher(security.getTickerSymbol()).matches())
                    return true;

                if (security.getType() != null && filterPattern.matcher(security.getType().name()).matches())
                    return true;

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

        transactions.setInput(security != null ? Transaction.sortByDate(security.getTransactions(getClient()))
                        : new ArrayList<Transaction>(0));

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
        latest = new LatestQuoteTable(sash);

        ViewerHelper.pack(latest.getTable());
        int width = latest.getTable().getBounds().width;
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
        prices = createPricesTable(folder);
        item.setControl(prices.getTable());

        // tab 3: transactions
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabTransactions);
        transactions = createTransactionTable(folder);
        item.setControl(transactions.getTable());

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

    protected TableViewer createPricesTable(Composite parent)
    {
        final TableViewer prices = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI);

        TableColumn column = new TableColumn(prices.getTable(), SWT.None);
        column.setText(Messages.ColumnDate);
        column.setWidth(80);

        column = new TableColumn(prices.getTable(), SWT.None);
        column.setText(Messages.ColumnAmount);
        column.setAlignment(SWT.RIGHT);
        column.setWidth(80);

        prices.getTable().setHeaderVisible(true);
        prices.getTable().setLinesVisible(true);

        prices.setLabelProvider(new PriceLabelProvider());
        prices.setContentProvider(new SimpleListContentProvider(true));

        new CellEditorFactory(prices, SecurityPrice.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();

                                Security security = (Security) prices.getData(Security.class.toString());

                                securities.refresh(security);
                                prices.refresh(element);
                                latest.setInput(security);
                                transactions.setInput(Transaction.sortByDate(security.getTransactions(getClient())));
                                updateChart(security);
                            }
                        }) //
                        .editable("time") // //$NON-NLS-1$
                        .amount("value") // //$NON-NLS-1$
                        .apply();

        hookContextMenu(prices.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPricesContextMenu(manager);
            }
        });

        return prices;
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
                    transactions.setInput(Transaction.sortByDate(security.getTransactions(getClient())));
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
                    transactions.setInput(Transaction.sortByDate(security.getTransactions(getClient())));
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
                    transactions.setInput(Transaction.sortByDate(security.getTransactions(getClient())));
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

                    Dialog dialog = new WizardDialog(getClientEditor().getSite().getShell(), new ImportQuotesWizard(
                                    security));
                    if (dialog.open() != Dialog.OK)
                        return;

                    markDirty();
                    securities.refresh(security);
                    onSecurityChanged(security);
                }
            });
        }
    }

    static class PriceLabelProvider extends LabelProvider implements ITableLabelProvider
    {

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        public String getColumnText(Object element, int columnIndex)
        {
            SecurityPrice p = (SecurityPrice) element;
            switch (columnIndex)
            {
                case 0:
                    return Values.Date.format(p.getTime());
                case 1:
                    return Values.Quote.format(p.getValue());
            }
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////
    // tab item: chart
    // //////////////////////////////////////////////////////////////

    private void updateChart(Security security)
    {
        ISeries series = chart.getSeriesSet().getSeries("prices"); //$NON-NLS-1$
        if (series != null)
            chart.getSeriesSet().deleteSeries("prices"); //$NON-NLS-1$
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

        ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "prices"); //$NON-NLS-1$
        lineSeries.setXDateSeries(dates);
        lineSeries.setLineWidth(2);
        lineSeries.enableArea(true);
        lineSeries.setSymbolType(PlotSymbolType.NONE);
        lineSeries.setYSeries(values);

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
                            chart.addMarkerLine(t.getDate(), new RGB(0, 128, 0), label);
                            break;
                        case SELL:
                        case TRANSFER_OUT:
                            chart.addMarkerLine(t.getDate(), new RGB(128, 0, 0), "-" + label); //$NON-NLS-1$
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            }

        }

        chart.redraw();
    }

    // //////////////////////////////////////////////////////////////
    // tab item: transactions
    // //////////////////////////////////////////////////////////////

    protected TableViewer createTransactionTable(Composite parent)
    {
        final TableViewer table = new TableViewer(parent, SWT.FULL_SELECTION);

        TableViewerColumn column = new TableViewerColumn(table, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        column.getColumn().setWidth(80);
        column.getColumn().setMoveable(true);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((Transaction) element).getDate());
            }
        });
        ColumnViewerSorter.create(Transaction.class, "date").attachTo(table, column, true); //$NON-NLS-1$

        column = new TableViewerColumn(table, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTransactionType);
        column.getColumn().setWidth(80);
        column.getColumn().setMoveable(true);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof PortfolioTransaction)
                    return ((PortfolioTransaction) element).getType().toString();
                else if (element instanceof AccountTransaction)
                    return ((AccountTransaction) element).getType().toString();
                else
                    return null;
            }
        });

        column = new TableViewerColumn(table, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnShares);
        column.getColumn().setWidth(80);
        column.getColumn().setMoveable(true);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object element)
            {
                return (element instanceof PortfolioTransaction) ? ((PortfolioTransaction) element).getShares() : null;
            }
        });

        column = new TableViewerColumn(table, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAmount);
        column.getColumn().setWidth(80);
        column.getColumn().setMoveable(true);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((Transaction) element).getAmount());
            }
        });
        ColumnViewerSorter.create(PortfolioTransaction.class, "amount").attachTo(table, column); //$NON-NLS-1$

        column = new TableViewerColumn(table, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnQuote);
        column.getColumn().setWidth(80);
        column.getColumn().setMoveable(true);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return (element instanceof PortfolioTransaction) ? Values.Amount
                                .format(((PortfolioTransaction) element).getActualPurchasePrice()) : null;
            }
        });

        table.getTable().setHeaderVisible(true);
        table.getTable().setLinesVisible(true);

        table.setContentProvider(new SimpleListContentProvider(true));

        return table;
    }

    // //////////////////////////////////////////////////////////////
    // tab item: latest quote
    // //////////////////////////////////////////////////////////////

    static class LatestQuoteTable
    {
        private Table table;

        public LatestQuoteTable(Composite parent)
        {
            table = new Table(parent, SWT.BORDER);
            table.setHeaderVisible(true);

            TableColumn column = new TableColumn(table, SWT.NO_SCROLL | SWT.NO_FOCUS);
            column.setWidth(80);
            column.setResizable(true);

            column = new TableColumn(table, SWT.NONE);
            column.setWidth(80);
            column.setResizable(true);
            column.setAlignment(SWT.RIGHT);

            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, Messages.ColumnLatestPrice);

            item = new TableItem(table, SWT.NONE);
            item.setText(0, Messages.ColumnLatestTrade);

            item = new TableItem(table, SWT.NONE);
            item.setText(0, Messages.ColumnDaysHigh);

            item = new TableItem(table, SWT.NONE);
            item.setText(0, Messages.ColumnDaysLow);

            item = new TableItem(table, SWT.NONE);
            item.setText(0, Messages.ColumnVolume);

            item = new TableItem(table, SWT.NONE);
            item.setText(0, Messages.ColumnPreviousClose);
        }

        public Table getTable()
        {
            return table;
        }

        public void setInput(Security security)
        {
            if (security == null || security.getLatest() == null)
            {
                for (int ii = 0; ii < 6; ii++)
                    table.getItem(ii).setText(1, ""); //$NON-NLS-1$
            }
            else
            {
                LatestSecurityPrice p = security.getLatest();
                table.getItem(0).setText(1, Values.Amount.format(p.getValue()));

                table.getItem(1).setText(1, Values.Date.format(p.getTime()));

                long daysHigh = p.getHigh();
                table.getItem(2).setText(1, daysHigh == -1 ? "n/a" : Values.Amount.format(daysHigh)); //$NON-NLS-1$

                long daysLow = p.getLow();
                table.getItem(3).setText(1, daysLow == -1 ? "n/a" : Values.Amount.format(daysLow)); //$NON-NLS-1$

                long volume = p.getVolume();
                table.getItem(4).setText(1, volume == -1 ? "n/a" : String.format("%,d", volume)); //$NON-NLS-1$ //$NON-NLS-2$

                long prevClose = p.getPreviousClose();
                table.getItem(5).setText(1, prevClose == -1 ? "n/a" : Values.Amount.format(prevClose)); //$NON-NLS-1$
            }
        }
    }

}
