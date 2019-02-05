package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransactionDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferDialog;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.actions.ConvertBuySellToDeliveryAction;
import name.abuchen.portfolio.ui.views.actions.ConvertDeliveryToBuySellAction;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;
import name.abuchen.portfolio.ui.wizards.security.SearchSecurityWizardDialog;
import name.abuchen.portfolio.util.Dates;

public class SecurityListView extends AbstractListView implements ModificationListener
{
    private class CreateSecurityDropDown extends DropDown implements IMenuListener
    {
        public CreateSecurityDropDown()
        {
            super(Messages.SecurityMenuAddNewSecurity, Images.PLUS, SWT.NONE);
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new SimpleAction(Messages.SecurityMenuNewSecurity, a -> {
                Security newSecurity = new Security();
                newSecurity.setFeed(QuoteFeed.MANUAL);
                newSecurity.setCurrencyCode(getClient().getBaseCurrency());
                openEditDialog(newSecurity);
            }));

            manager.add(new SimpleAction(Messages.SecurityMenuNewExchangeRate, a -> {
                Security newSecurity = new Security();
                newSecurity.setFeed(QuoteFeed.MANUAL);
                newSecurity.setCurrencyCode(getClient().getBaseCurrency());
                newSecurity.setTargetCurrencyCode(getClient().getBaseCurrency());
                openEditDialog(newSecurity);
            }));

            manager.add(new Separator());

            manager.add(new SimpleAction(Messages.SecurityMenuSearch4Securities, a -> {
                SearchSecurityWizardDialog dialog = new SearchSecurityWizardDialog(
                                Display.getDefault().getActiveShell(), getClient());
                if (dialog.open() == Dialog.OK)
                    openEditDialog(dialog.getSecurity());
            }));
        }

        private void openEditDialog(Security newSecurity)
        {
            Dialog dialog = make(EditSecurityDialog.class, newSecurity);

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

    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private final Predicate<Security> securityIsNotInactive = record -> !record.isRetired();
        private final Predicate<Security> onlySecurities = record -> !record.isExchangeRate();
        private final Predicate<Security> onlyExchangeRates = record -> record.isExchangeRate();
        private final Predicate<Security> sharesGreaterZero = record -> getSharesHeld(getClient(), record) > 0;
        private final Predicate<Security> sharesEqualZero = record -> getSharesHeld(getClient(), record) == 0;

        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityListFilter, Images.FILTER_OFF, SWT.NONE);
            setMenuListener(this);

            int savedFilters = preferenceStore.getInt(this.getClass().getSimpleName() + "-filterSettings"); //$NON-NLS-1$

            if ((savedFilters & (1 << 1)) != 0)
                filter.add(securityIsNotInactive);
            if ((savedFilters & (1 << 2)) != 0)
                filter.add(onlySecurities);
            if ((savedFilters & (1 << 3)) != 0)
                filter.add(onlyExchangeRates);
            if ((savedFilters & (1 << 4)) != 0)
                filter.add(sharesGreaterZero);
            if ((savedFilters & (1 << 5)) != 0)
                filter.add(sharesEqualZero);

            if (!filter.isEmpty())
                setImage(Images.FILTER_ON);

            addDisposeListener(e -> {

                int savedFilter = 0;
                if (filter.contains(securityIsNotInactive))
                    savedFilter += (1 << 1);
                if (filter.contains(onlySecurities))
                    savedFilter += (1 << 2);
                if (filter.contains(onlyExchangeRates))
                    savedFilter += (1 << 3);
                if (filter.contains(sharesGreaterZero))
                    savedFilter += (1 << 4);
                if (filter.contains(sharesEqualZero))
                    savedFilter += (1 << 5);

                preferenceStore.setValue(this.getClass().getSimpleName() + "-filterSettings", savedFilter); //$NON-NLS-1$
            });
        }

        /**
         * Collects all shares held for the given security.
         * 
         * @param client
         *            {@link Client}
         * @param security
         *            {@link Security}
         * @return shares held on success, else 0
         */
        private long getSharesHeld(Client client, Security security)
        {
            // collect all shares and return a value greater 0
            return Math.max(security.getTransactions(client).stream()
                            .filter(t -> t.getTransaction() instanceof PortfolioTransaction) //
                            .map(t -> (PortfolioTransaction) t.getTransaction()) //
                            .mapToLong(t -> {
                                switch (t.getType())
                                {
                                    case BUY:
                                    case DELIVERY_INBOUND:
                                        return t.getShares();
                                    case SELL:
                                    case DELIVERY_OUTBOUND:
                                        return -t.getShares();
                                    default:
                                        return 0L;
                                }
                            }).sum(), 0);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(createAction(Messages.SecurityListFilterHideInactive, securityIsNotInactive));
            manager.add(createAction(Messages.SecurityListFilterOnlySecurities, onlySecurities));
            manager.add(createAction(Messages.SecurityListFilterOnlyExchangeRates, onlyExchangeRates));
            manager.add(createAction(Messages.SecurityFilterSharesHeldGreaterZero, sharesGreaterZero));
            manager.add(createAction(Messages.SecurityFilterSharesHeldEqualZero, sharesEqualZero));
        }

        private Action createAction(String label, Predicate<Security> predicate)
        {
            Action action = new Action(label, Action.AS_CHECK_BOX)
            {
                @Override
                public void run()
                {
                    boolean isChecked = filter.contains(predicate);

                    if (isChecked)
                        filter.remove(predicate);
                    else
                        filter.add(predicate);

                    // uncheck mutually exclusive actions if new filter is added
                    if (!isChecked)
                    {
                        if (predicate == onlySecurities)
                            filter.remove(onlyExchangeRates);
                        else if (predicate == onlyExchangeRates)
                            filter.remove(onlySecurities);
                        else if (predicate == sharesEqualZero)
                            filter.remove(sharesGreaterZero);
                        else if (predicate == sharesGreaterZero)
                            filter.remove(sharesEqualZero);
                    }

                    setImage(filter.isEmpty() ? Images.FILTER_OFF : Images.FILTER_ON);
                    securities.refresh(false);
                }
            };
            action.setChecked(filter.contains(predicate));
            return action;
        }
    }

    @Inject
    private SelectionService selectionService;

    @Inject
    private ExchangeRateProviderFactory factory;

    private SecuritiesTable securities;
    private TableViewer prices;
    private TableViewer transactions;
    private TableViewer events;
    private SecuritiesChart chart;
    private SecurityDetailsViewer latest;
    private SecurityQuoteQualityMetricsViewer metrics;

    private List<Predicate<Security>> filter = new ArrayList<>();

    private Watchlist watchlist;

    private Pattern filterPattern;

    @Override
    protected String getDefaultTitle()
    {
        StringBuilder title = new StringBuilder();
        if (watchlist == null)
            title.append(Messages.LabelSecurities);
        else
            title.append(Messages.LabelSecurities).append(' ').append(watchlist.getName());

        if (securities != null)
            title.append(" (").append(securities.getColumnHelper().getConfigurationName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$

        return title.toString();
    }

    @Override
    public void notifyModelUpdated()
    {
        if (securities != null && !securities.getTableViewer().getTable().isDisposed())
        {
            updateTitle(getDefaultTitle());
            securities.refresh(true);
        }
    }

    @Override
    public void markDirty()
    {
        super.markDirty();

        // see issue #448: if the note column is edited, the information area is
        // not updated accordingly. #markDirty is called by the SecuritiesTable
        // when any column is edited
        Security security = (Security) ((IStructuredSelection) securities.getTableViewer().getSelection())
                        .getFirstElement();
        latest.setInput(security);
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        // called from prices table
        Security security = (Security) prices.getData(Security.class.toString());

        // if the date changed, the prices must be reordered --> binary search
        if (newValue instanceof LocalDate)
        {
            SecurityPrice price = (SecurityPrice) element;
            security.removePrice(price);
            security.addPrice(price);
        }

        securities.refresh(security);
        prices.refresh(element);
        latest.setInput(security);
        metrics.setInput(security);
        transactions.setInput(security.getTransactions(getClient()));
        events.setInput(security.getEvents());
        chart.updateChart(security);

        markDirty();
    }

    @Inject
    @Optional
    public void setup(@Named(UIConstants.Parameter.VIEW_PARAMETER) Watchlist parameter)
    {
        this.watchlist = parameter;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addSearchButton(toolBar);

        toolBar.add(new Separator());

        toolBar.add(new CreateSecurityDropDown());
        toolBar.add(new FilterDropDown(getPreferenceStore()));
        addExportButton(toolBar);

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> securities.getColumnHelper().menuAboutToShow(manager)));
    }

    private void addSearchButton(ToolBarManager toolBar)
    {
        toolBar.add(new ControlContribution("searchbox") //$NON-NLS-1$
        {
            @Override
            protected Control createControl(Composite parent)
            {
                final Text search = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
                search.setMessage(Messages.LabelSearch);
                search.setSize(300, SWT.DEFAULT);

                search.addModifyListener(e -> {
                    String filterText = search.getText().trim();
                    if (filterText.length() == 0)
                    {
                        filterPattern = null;
                        securities.refresh(false);
                    }
                    else
                    {
                        filterPattern = Pattern.compile(".*" + filterText + ".*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
                        securities.refresh(false);
                    }
                });

                return search;
            }

            @Override
            protected int computeWidth(Control control)
            {
                return control.computeSize(100, SWT.DEFAULT, true).x;
            }
        });
    }

    private void addExportButton(ToolBarManager toolBar)
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
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);

        toolBar.add(export);
    }

    // //////////////////////////////////////////////////////////////
    // top table: securities
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createTopTable(Composite parent)
    {
        securities = new SecuritiesTable(parent, this);
        updateTitle(getDefaultTitle());
        securities.getColumnHelper().addListener(() -> updateTitle(getDefaultTitle()));
        securities.getColumnHelper().setToolBarManager(getViewToolBarManager());

        securities.addSelectionChangedListener(event -> onSecurityChanged(
                        (Security) ((IStructuredSelection) event.getSelection()).getFirstElement()));

        securities.addSelectionChangedListener(event -> {
            Security security = (Security) ((IStructuredSelection) event.getSelection()).getFirstElement();
            if (security != null)
                selectionService.setSelection(new SecuritySelection(getClient(), security));
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

        securities.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                for (Predicate<Security> predicate : filter)
                {
                    if (!predicate.test((Security) element))
                        return false;
                }

                return true;
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
        metrics.setInput(security);

        transactions.setInput(security != null ? security.getTransactions(getClient()) : new ArrayList<Transaction>(0));

        events.setInput(security != null ? security.getEvents() : Collections.emptyList());

        chart.updateChart(security);
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: folder
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createBottomTable(Composite parent)
    {
        Composite sash = new Composite(parent, SWT.NONE);

        sash.setLayout(new SashLayout(sash, SWT.HORIZONTAL | SWT.END));

        // folder
        CTabFolder folder = new CTabFolder(sash, SWT.BORDER);

        // latest
        latest = new SecurityDetailsViewer(sash, SWT.BORDER, getClient());
        latest.getControl().setLayoutData(new SashLayoutData(SWTHelper.getPackedWidth(latest.getControl())));

        // tab 1: chart
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabChart);

        Composite chartComposite = new Composite(folder, SWT.NONE);
        item.setControl(chartComposite);

        chart = new SecuritiesChart(chartComposite, getClient(),
                        new CurrencyConverterImpl(factory, getClient().getBaseCurrency()));

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

        // tab 5: data quality
        metrics = make(SecurityQuoteQualityMetricsViewer.class);
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.GroupLabelDataQuality);
        item.setControl(metrics.createViewControl(folder));

        folder.setSelection(0);
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
                return Values.Date.format(((SecurityPrice) element).getDate());
            }

            @Override
            public Color getBackground(Object element)
            {
                SecurityPrice current = (SecurityPrice) element;
                List<?> all = (List<?>) prices.getInput();
                int index = all.indexOf(current);

                if (index == 0)
                    return null;

                SecurityPrice previous = (SecurityPrice) all.get(index - 1);
                int days = Dates.daysBetween(previous.getDate(), current.getDate());
                return days > 3 ? Colors.WARNING : null;
            }
        });
        ColumnViewerSorter.create(SecurityPrice.class, "date").attachTo(column, SWT.UP); //$NON-NLS-1$
        new DateEditingSupport(SecurityPrice.class, "date").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Security security = (Security) prices.getData(Security.class.toString());
                SecurityPrice price = (SecurityPrice) element;
                return Values.Quote.format(security.getCurrencyCode(), price.getValue(), getClient().getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(SecurityPrice.class, "value").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(SecurityPrice.class, "value", Values.Quote).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        prices.getTable().setHeaderVisible(true);
        prices.getTable().setLinesVisible(true);

        prices.setContentProvider(ArrayContentProvider.getInstance());

        hookContextMenu(prices.getTable(), this::fillPricesContextMenu);

        return container;
    }

    private void fillPricesContextMenu(IMenuManager manager)
    {
        Security security = (Security) prices.getData(Security.class.toString());
        if (security != null)
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
                    price.setDate(LocalDate.now());

                    security.addPrice(price);

                    markDirty();

                    prices.setInput(security.getPrices());
                    latest.setInput(security);
                    metrics.setInput(security);
                    transactions.setInput(security.getTransactions(getClient()));
                    events.setInput(security.getEvents());
                    chart.updateChart(security);

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
                    metrics.setInput(security);
                    transactions.setInput(security.getTransactions(getClient()));
                    events.setInput(security.getEvents());
                    chart.updateChart(security);
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
                    metrics.setInput(security);
                    transactions.setInput(security.getTransactions(getClient()));
                    events.setInput(security.getEvents());
                    chart.updateChart(security);
                }
            });
        }

        if (security != null)
        {
            manager.add(new Separator());
            new QuotesContextMenu(this).menuAboutToShow(manager, security);
        }
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

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        SecurityListView.class.getSimpleName() + "@transactions4", getPreferenceStore(), transactions, //$NON-NLS-1$
                        layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.DateTime.format(((TransactionPair<?>) element).getTransaction().getDateTime());
            }
        });
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> ((TransactionPair<?>) o1).getTransaction().getDateTime()
                        .compareTo(((TransactionPair<?>) o2).getTransaction().getDateTime())), SWT.UP);
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
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
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
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                return Values.Money.format(t.getMonetaryAmount(), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            long a1 = ((TransactionPair<?>) o1).getTransaction().getAmount();
            long a2 = ((TransactionPair<?>) o2).getTransaction().getAmount();
            return Long.compare(a1, a2);
        }));
        support.addColumn(column);

        column = new Column(Messages.ColumnPerShare, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPerShare_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                if (t instanceof PortfolioTransaction)
                {
                    return Values.Quote.format(((PortfolioTransaction) t).getGrossPricePerShare(),
                                    getClient().getBaseCurrency());
                }
                else if (t instanceof AccountTransaction)
                {
                    long shares = ((AccountTransaction) t).getShares();
                    if (shares != 0)
                    {
                        long perShare = Math.round(((AccountTransaction) t).getGrossValueAmount()
                                        * Values.Share.divider() * Values.Quote.factorToMoney() / shares);
                        return Values.Quote.format(Quote.of(t.getCurrencyCode(), perShare));
                    }
                }
                return null;
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnFees_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                return Values.Money.format(t.getUnitSum(Unit.Type.FEE), getClient().getBaseCurrency());
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnTaxes, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnTaxes_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                return Values.Money.format(t.getUnitSum(Unit.Type.TAX), getClient().getBaseCurrency());
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
        new TransactionOwnerListEditingSupport(getClient(), TransactionOwnerListEditingSupport.EditMode.OWNER)
                        .addListener(this).attachTo(column);
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
                    return t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t).toString() : null;
                else
                    return pair.getOwner().toString();
            }
        });
        new TransactionOwnerListEditingSupport(getClient(), TransactionOwnerListEditingSupport.EditMode.CROSSOWNER)
                        .addListener(this).attachTo(column);
        support.addColumn(column);

        column = new NoteColumn();
        column.setEditingSupport(null);
        support.addColumn(column);

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(ArrayContentProvider.getInstance());

        hookContextMenu(transactions.getControl(), this::transactionMenuAboutToShow);

        hookKeyListener();
        return container;
    }

    private void hookKeyListener()
    {
        transactions.getControl().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == 'e' && e.stateMask == SWT.MOD1)
                {
                    TransactionPair<?> pair = (TransactionPair<?>) ((IStructuredSelection) transactions.getSelection())
                                    .getFirstElement();
                    if (pair != null)
                        createEditAction(pair).run();
                }
            }
        });
    }

    private void transactionMenuAboutToShow(IMenuManager manager) // NOSONAR
    {
        Security security = (Security) prices.getData(Security.class.toString());
        if (security == null)
            return;

        TransactionPair<?> pair = (TransactionPair<?>) ((IStructuredSelection) transactions.getSelection())
                        .getFirstElement();
        if (pair != null)
        {
            Action action = createEditAction(pair);
            action.setAccelerator(SWT.MOD1 | 'E');
            manager.add(action);
            manager.add(new Separator());

            if (pair.getTransaction() instanceof PortfolioTransaction)
            {
                Portfolio p = (Portfolio) pair.getOwner();
                PortfolioTransaction t = (PortfolioTransaction) pair.getTransaction();

                if (t.getType() == PortfolioTransaction.Type.BUY || t.getType() == PortfolioTransaction.Type.SELL)
                {
                    manager.add(new ConvertBuySellToDeliveryAction(getClient(), new TransactionPair<>(p, t)));
                    manager.add(new Separator());
                }

                if (t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
                {
                    manager.add(new ConvertDeliveryToBuySellAction(getClient(), new TransactionPair<>(p, t)));
                    manager.add(new Separator());
                }
            }
        }

        new SecurityContextMenu(SecurityListView.this).menuAboutToShow(manager, security);

        manager.add(new Separator());

        manager.add(new Action(Messages.MenuTransactionDelete)
        {
            @Override
            public void run()
            {
                TransactionPair<?> pair = (TransactionPair<?>) ((IStructuredSelection) transactions.getSelection())
                                .getFirstElement();
                if (pair == null)
                    return;

                pair.deleteTransaction(getClient());
                getClient().markDirty();
            }
        });

        manager.add(new Separator());
        manager.add(new ConfirmAction(Messages.MenuDeleteAllTransactions,
                        MessageFormat.format(Messages.MenuConfirmDeleteAllTransactions, security.getName()), //
                        a -> {

                            List<TransactionPair<?>> txs = security.getTransactions(getClient());

                            for (TransactionPair<?> tx : txs)
                            {
                                @SuppressWarnings("unchecked")
                                TransactionPair<Transaction> t = (TransactionPair<Transaction>) tx;
                                t.getOwner().deleteTransaction(t.getTransaction(), getClient());
                            }

                            getClient().markDirty();
                        }));
    }

    private Action createEditAction(TransactionPair<?> transactionPair)
    {
        if (transactionPair.getTransaction().getCrossEntry() instanceof BuySellEntry)
        {
            BuySellEntry entry = (BuySellEntry) transactionPair.getTransaction().getCrossEntry();
            return new OpenDialogAction(this, Messages.MenuEditTransaction)
                            .type(SecurityTransactionDialog.class, d -> d.setBuySellEntry(entry))
                            .parameters(entry.getPortfolioTransaction().getType());
        }
        else if (transactionPair.getTransaction().getCrossEntry() instanceof PortfolioTransferEntry)
        {
            PortfolioTransferEntry entry = (PortfolioTransferEntry) transactionPair.getTransaction().getCrossEntry();
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(SecurityTransferDialog.class, d -> d.setEntry(entry));
        }
        else if (transactionPair.getTransaction() instanceof PortfolioTransaction)
        {
            @SuppressWarnings("unchecked")
            TransactionPair<PortfolioTransaction> pair = (TransactionPair<PortfolioTransaction>) transactionPair;
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(SecurityTransactionDialog.class, d -> d.setDeliveryTransaction(pair)) //
                            .parameters(pair.getTransaction().getType());
        }
        else if (transactionPair.getTransaction() instanceof AccountTransaction)
        {
            @SuppressWarnings("unchecked")
            TransactionPair<AccountTransaction> pair = (TransactionPair<AccountTransaction>) transactionPair;
            return new OpenDialogAction(this, Messages.MenuEditTransaction) //
                            .type(AccountTransactionDialog.class,
                                            d -> d.setTransaction((Account) pair.getOwner(), pair.getTransaction())) //
                            .parameters(pair.getTransaction().getType());
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

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
        column.setSorter(ColumnViewerSorter.create(e -> ((SecurityEvent) e).getDate()), SWT.UP);
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

        events.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }
}
