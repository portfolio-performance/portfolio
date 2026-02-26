package name.abuchen.portfolio.ui.views.trades;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCategory;
import name.abuchen.portfolio.snapshot.trades.TradeCategory.TradeAssignment;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.snapshot.trades.TradeTotals;
import name.abuchen.portfolio.snapshot.trades.TradesGroupedByTaxonomy;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.action.MenuContribution;
import name.abuchen.portfolio.ui.views.SecurityContextMenu;
import name.abuchen.portfolio.ui.views.TradesTableViewer;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.Interval;

public final class TradeDetailsView extends AbstractFinanceView
{
    public static class Input
    {
        private final Interval interval;
        private final List<Trade> trades;
        private final List<TradeCollectorException> errors;
        private final boolean useSecurityCurrency;

        public Input(Interval interval, List<Trade> trades, List<TradeCollectorException> errors,
                        boolean useSecurityCurrency)
        {
            this.interval = interval;
            this.trades = trades;
            this.errors = errors;
            this.useSecurityCurrency = useSecurityCurrency;
        }

        public Interval getInterval()
        {
            return interval;
        }

        public List<Trade> getTrades()
        {
            return trades;
        }

        public List<TradeCollectorException> getErrors()
        {
            return errors;
        }

        public boolean useSecurityCurrency()
        {
            return useSecurityCurrency;
        }
    }

    /*
     * We need a reference to the value in the buttons but the native Boolean
     * class is immutable.
     */
    private class MutableBoolean
    {
        private boolean value;

        public MutableBoolean(boolean value)
        {
            this.value = value;
        }

        public void setValue(boolean value)
        {
            this.value = value;
        }

        public void invert()
        {
            value = !value;
        }

        public boolean isTrue()
        {
            return value;
        }

        public boolean isFalse()
        {
            return !value;
        }
    }

    private class FilterAction extends Action
    {
        private MutableBoolean criterion;

        private DropDown theMenu;
        private FilterAction counterpart;

        public FilterAction(String label, MutableBoolean criterion, DropDown theMenu)
        {
            super(label);
            this.criterion = criterion;
            this.theMenu = theMenu;
            this.setChecked(criterion.isTrue());
        }

        public void setCounterpart(FilterAction counterpart)
        {
            this.counterpart = counterpart;
        }

        @Override
        public void run()
        {
            criterion.invert();

            if (counterpart != null && criterion.isTrue())
            {
                counterpart.criterion.setValue(false);
                counterpart.setChecked(false);
            }

            update();
            updateFilterButtonImage(theMenu);
        }
    }

    private class UpdateTradesJob extends Job
    {
        private final Input preselectedInput;
        private final boolean useSecCurrency;
        private final CurrencyConverter jobConverter;
        private final boolean onlyOpen;
        private final boolean onlyClosed;
        private final boolean onlyProfitable;
        private final boolean onlyLossMaking;
        private final Pattern jobFilterPattern;
        private final Taxonomy jobTaxonomy;
        private final boolean hideTotalsAtTheTopJob;
        private final boolean hideTotalsAtTheBottomJob;

        UpdateTradesJob(Input preselectedInput, boolean useSecCurrency, CurrencyConverter converter, boolean onlyOpen,
                        boolean onlyClosed, boolean onlyProfitable, boolean onlyLossMaking, Pattern filterPattern,
                        Taxonomy taxonomy, boolean hideTotalsAtTheTop, boolean hideTotalsAtTheBottom)
        {
            super(Messages.LabelTrades);
            this.preselectedInput = preselectedInput;
            this.useSecCurrency = useSecCurrency;
            this.jobConverter = converter;
            this.onlyOpen = onlyOpen;
            this.onlyClosed = onlyClosed;
            this.onlyProfitable = onlyProfitable;
            this.onlyLossMaking = onlyLossMaking;
            this.jobFilterPattern = filterPattern;
            this.jobTaxonomy = taxonomy;
            this.hideTotalsAtTheTopJob = hideTotalsAtTheTop;
            this.hideTotalsAtTheBottomJob = hideTotalsAtTheBottom;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            if (monitor != null)
                monitor.beginTask(Messages.LabelTrades, IProgressMonitor.UNKNOWN);

            try
            {
                Input data = preselectedInput != null ? preselectedInput
                                : collectAllTrades(useSecCurrency, jobConverter);

                if (monitor != null && monitor.isCanceled())
                    return Status.CANCEL_STATUS;

                Stream<Trade> filteredTrades = data.getTrades().stream();

                if (onlyClosed)
                    filteredTrades = filteredTrades.filter(Trade::isClosed);
                if (onlyOpen)
                    filteredTrades = filteredTrades.filter(t -> !t.isClosed());
                if (onlyLossMaking)
                    filteredTrades = filteredTrades.filter(Trade::isLoss);
                if (onlyProfitable)
                    filteredTrades = filteredTrades.filter(t -> t.getProfitLoss().isPositive());
                if (jobFilterPattern != null)
                    filteredTrades = filteredTrades.filter(t -> matchesFilter(t, jobFilterPattern));

                List<Trade> trades = filteredTrades.collect(Collectors.toList());

                if (monitor != null && monitor.isCanceled())
                    return Status.CANCEL_STATUS;

                List<?> finalTableInput;
                if (jobTaxonomy != null)
                {
                    TradesGroupedByTaxonomy groupedTrades = new TradesGroupedByTaxonomy(jobTaxonomy, trades,
                                    jobConverter);
                    finalTableInput = flattenToElements(groupedTrades, hideTotalsAtTheTopJob, hideTotalsAtTheBottomJob);
                }
                else
                {
                    finalTableInput = trades;
                }

                List<TradeCollectorException> errors = data.getErrors();

                if (monitor != null && monitor.isCanceled())
                    return Status.CANCEL_STATUS;

                Display display = Display.getDefault();
                if (display == null || display.isDisposed())
                    return Status.CANCEL_STATUS;

                display.asyncExec(() -> applyJobResult(this, finalTableInput, errors));

                return Status.OK_STATUS;
            }
            catch (Exception e)
            {
                PortfolioPlugin.log(e);
                scheduleCursorReset(this);
                return new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, "Failed to update trades", e); //$NON-NLS-1$
            }
            finally
            {
                if (monitor != null)
                    monitor.done();
            }
        }
    }

    private static final String PREF_USE_SECURITY_CURRENCY = "useSecurityCurrency"; //$NON-NLS-1$
    private static final String PREF_HIDE_TOTALS_TOP = TradeDetailsView.class.getSimpleName() + "@hideTotalsTop"; //$NON-NLS-1$
    private static final String PREF_HIDE_TOTALS_BOTTOM = TradeDetailsView.class.getSimpleName() + "@hideTotalsBottom"; //$NON-NLS-1$

    private static final String PREF_TAXONOMY = TradeDetailsView.class.getSimpleName() + "-taxonomy"; //$NON-NLS-1$
    private static final String PREF_TAXONOMY_NONE = "@none"; //$NON-NLS-1$

    private static final String ID_WARNING_TOOL_ITEM = "warning"; //$NON-NLS-1$

    @Inject
    private SelectionService selectionService;

    private Input input;

    private CurrencyConverter converter;
    private TradesTableViewer table;
    private Taxonomy taxonomy;
    private Job currentUpdateJob;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelTrades;
    }

    /**
     * Indicates whether to calculate the trade in the currency of the security
     */
    private boolean useSecurityCurrency = false;

    private MutableBoolean usePreselectedTrades = new MutableBoolean(false);

    private MutableBoolean onlyOpen = new MutableBoolean(false);
    private MutableBoolean onlyClosed = new MutableBoolean(false);

    private boolean hideTotalsAtTheTop;
    private boolean hideTotalsAtTheBottom;

    private MutableBoolean onlyProfitable = new MutableBoolean(false);
    private MutableBoolean onlyLossMaking = new MutableBoolean(false);

    private Pattern filterPattern;

    @Inject
    @Optional
    public void setTrades(@Named(UIConstants.Parameter.VIEW_PARAMETER) Input input)
    {
        if (input != null)
        {
            this.input = input;
            this.usePreselectedTrades.setValue(true);
            this.useSecurityCurrency = input.useSecurityCurrency();
        }
    }

    @PostConstruct
    protected void construct(ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
    }

    @PostConstruct
    private void readPreferences(IPreferenceStore preferences)
    {
        // read preferences only if not preselected by the setTrades method
        if (usePreselectedTrades.isFalse())
            useSecurityCurrency = preferences.getBoolean(this.getClass().getSimpleName() + PREF_USE_SECURITY_CURRENCY);

        hideTotalsAtTheTop = preferences.getBoolean(PREF_HIDE_TOTALS_TOP);
        hideTotalsAtTheBottom = preferences.getBoolean(PREF_HIDE_TOTALS_BOTTOM);
    }

    @PostConstruct
    private void loadTaxonomy() // NOSONAR
    {
        String taxonomyId = getPreferenceStore().getString(PREF_TAXONOMY);

        if (PREF_TAXONOMY_NONE.equals(taxonomyId))
            return;

        if (taxonomyId != null)
        {
            for (Taxonomy t : getClient().getTaxonomies())
            {
                if (taxonomyId.equals(t.getId()))
                {
                    this.taxonomy = t;
                    break;
                }
            }
        }

        if (this.taxonomy == null && !getClient().getTaxonomies().isEmpty())
            this.taxonomy = getClient().getTaxonomies().get(0);
    }

    @Override
    public void notifyModelUpdated()
    {
        // the base currency might have changed
        this.converter = this.converter.with(getClient().getBaseCurrency());

        // only update the trades if it is *not* based on a pre-calculated set
        // of trades, for example when the user navigates from the dashboard to
        // the detailed trades
        if (input == null || !input.getTrades().equals(table.getInput()))
            update();

        if (!table.getTableViewer().getTable().isDisposed())
            table.getTableViewer().refresh(true);
    }

    @Override
    protected void addButtons(ToolBarManager toolBarManager)
    {
        addSearchButton(toolBarManager);

        toolBarManager.add(new Separator());

        addFilterButton(toolBarManager);

        toolBarManager.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE,
                        manager -> manager.add(new SimpleAction(Messages.LabelTrades + " (CSV)", //$NON-NLS-1$
                                        a -> new TableViewerCSVExporter(table.getTableViewer())
                                                        .export(Messages.LabelTrades + ".csv"))) //$NON-NLS-1$
        ));

        toolBarManager.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE, manager -> {

            manager.add(new LabelOnly(Messages.LabelTaxonomies));

            var noneAction = new SimpleAction(Messages.LabelUseNoTaxonomy, a -> {
                taxonomy = null;
                getPreferenceStore().setValue(PREF_TAXONOMY, PREF_TAXONOMY_NONE);
                update();
            });
            noneAction.setChecked(taxonomy == null);
            manager.add(noneAction);

            for (final Taxonomy t : getClient().getTaxonomies())
            {
                manager.add(new MenuContribution(t.getName(), () -> {
                    taxonomy = t;
                    getPreferenceStore().setValue(PREF_TAXONOMY, t.getId());
                    update();
                }, t.equals(taxonomy)));
            }

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.LabelColumns));

            table.getShowHideColumnHelper().menuAboutToShow(manager);

            manager.add(new Separator());
            var action = new SimpleAction(Messages.LabelUseSecurityCurrency, a -> {
                useSecurityCurrency = !useSecurityCurrency;
                getPreferenceStore().setValue(this.getClass().getSimpleName() + PREF_USE_SECURITY_CURRENCY,
                                useSecurityCurrency);
                update();
            });
            action.setChecked(useSecurityCurrency);

            // enable the action to use the security currency only the user is
            // not using the preselected trades (because only then we can
            // recalculate the trades)
            action.setEnabled(usePreselectedTrades.isFalse());

            manager.add(action);
        }));
    }

    private void updateFilterButtonImage(DropDown dropDown)
    {
        boolean isOn = usePreselectedTrades.isTrue() || onlyOpen.isTrue() || onlyClosed.isTrue()
                        || onlyProfitable.isTrue() || onlyLossMaking.isTrue();
        dropDown.setImage(isOn ? Images.FILTER_ON : Images.FILTER_OFF);
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

                search.addModifyListener(e -> {
                    String filterText = search.getText().trim();
                    if (filterText.isEmpty())
                    {
                        filterPattern = null;
                        TradeDetailsView.this.update();
                    }
                    else
                    {
                        filterPattern = Pattern.compile(".*" + Pattern.quote(filterText) + ".*", //$NON-NLS-1$ //$NON-NLS-2$
                                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                        TradeDetailsView.this.update();
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

    private void addFilterButton(ToolBarManager manager)
    {
        boolean hasPreselectedTrades = input != null;

        // retrieve existing filter
        IPreferenceStore preferenceStore = getPreferenceStore();
        var savedFilters = preferenceStore.getInt(this.getClass().getSimpleName() + "-filterSettingsTrade"); //$NON-NLS-1$
        if ((savedFilters & (1 << 1)) != 0)
            onlyOpen.setValue(true);
        if ((savedFilters & (1 << 2)) != 0)
            onlyClosed.setValue(true);
        if ((savedFilters & (1 << 3)) != 0)
            onlyProfitable.setValue(true);
        if ((savedFilters & (1 << 4)) != 0)
            onlyLossMaking.setValue(true);

        DropDown filterDropDowMenu = new DropDown(Messages.MenuFilterTrades, Images.FILTER_OFF, SWT.NONE);
        updateFilterButtonImage(filterDropDowMenu);

        filterDropDowMenu.setMenuListener(mgr -> {

            if (hasPreselectedTrades)
            {
                mgr.add(new FilterAction(input.getInterval().toString(), usePreselectedTrades, filterDropDowMenu));
                mgr.add(new Separator());
            }

            FilterAction onlyOpenAction = new FilterAction(Messages.FilterOnlyOpenTrades, onlyOpen, filterDropDowMenu);
            FilterAction onlyClosedAction = new FilterAction(Messages.FilterOnlyClosedTrades, onlyClosed,
                            filterDropDowMenu);

            onlyOpenAction.setCounterpart(onlyClosedAction);
            onlyClosedAction.setCounterpart(onlyOpenAction);

            FilterAction onlyProfitableAction = new FilterAction(Messages.FilterOnlyProfitableTrades, onlyProfitable,
                            filterDropDowMenu);
            FilterAction onlyLossMakingAction = new FilterAction(Messages.FilterOnlyLossMakingTrades, onlyLossMaking,
                            filterDropDowMenu);

            onlyProfitableAction.setCounterpart(onlyLossMakingAction);
            onlyLossMakingAction.setCounterpart(onlyProfitableAction);

            mgr.add(onlyOpenAction);
            mgr.add(onlyClosedAction);
            mgr.add(new Separator());
            mgr.add(onlyProfitableAction);
            mgr.add(onlyLossMakingAction);
        });

        filterDropDowMenu.addDisposeListener(e -> {
            int savedFilter = 0;
            if (onlyOpen.isTrue())
                savedFilter += (1 << 1);
            if (onlyClosed.isTrue())
                savedFilter += (1 << 2);
            if (onlyProfitable.isTrue())
                savedFilter += (1 << 3);
            if (onlyLossMaking.isTrue())
                savedFilter += (1 << 4);
            preferenceStore.setValue(this.getClass().getSimpleName() + "-filterSettingsTrade", savedFilter); //$NON-NLS-1$
        });

        manager.add(filterDropDowMenu);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        table = new TradesTableViewer(this);

        Control control = table.createViewControl(parent, TradesTableViewer.ViewMode.MULTIPLE_SECURITIES);

        table.getTableViewer().addSelectionChangedListener(event -> {
            var structured = event.getStructuredSelection();
            if (structured.isEmpty())
            {
                selectionService.setSelection(null);
            }
            else
            {
                var securitySelection = SecuritySelection.from(getClient(), structured);
                selectionService.setSelection(securitySelection.isEmpty() ? null : securitySelection);
            }
        });

        table.getTableViewer().addSelectionChangedListener(e -> {
            Object first = e.getStructuredSelection().getFirstElement();
            if (first instanceof TradeElement && ((TradeElement) first).isTotal())
                setInformationPaneInput(null);
            else
                setInformationPaneInput(first);
        });

        update();

        new ContextMenu(table.getTableViewer().getControl(), this::fillContextMenu).hook();
        hookKeyListener();

        return control;
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(SecurityEventsPane.class));
    }

    private void fillContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = table.getTableViewer().getStructuredSelection();

        if (!selection.isEmpty() && selection.size() == 1)
        {
            Object element = selection.getFirstElement();
            Trade trade = element instanceof Trade ? (Trade) element
                            : element instanceof TradeElement ? ((TradeElement) element).getTrade() : null;

            if (trade != null)
                new SecurityContextMenu(this).menuAboutToShow(manager, trade.getSecurity(), trade.getPortfolio());
        }

        if (taxonomy == null)
            return;

        if (!manager.isEmpty())
            manager.add(new Separator());

        MenuManager submenu = new MenuManager(Messages.PrefTitlePresentation);
        manager.add(submenu);

        var action = new SimpleAction(Messages.LabelTotalsAtTheTop, a -> {
            hideTotalsAtTheTop = !hideTotalsAtTheTop;
            getPreferenceStore().setValue(PREF_HIDE_TOTALS_TOP, hideTotalsAtTheTop);
            update();
        });
        action.setChecked(!hideTotalsAtTheTop);
        submenu.add(action);

        action = new SimpleAction(Messages.LabelTotalsAtTheBottom, a -> {
            hideTotalsAtTheBottom = !hideTotalsAtTheBottom;
            getPreferenceStore().setValue(PREF_HIDE_TOTALS_BOTTOM, hideTotalsAtTheBottom);
            update();
        });
        action.setChecked(!hideTotalsAtTheBottom);
        submenu.add(action);
    }

    private void hookKeyListener()
    {
        table.getTableViewer().getControl().addKeyListener(KeyListener.keyPressedAdapter(e -> {
            var selection = table.getTableViewer().getStructuredSelection();
            if (selection.isEmpty() || selection.size() > 1)
                return;

            Object element = selection.getFirstElement();
            Trade trade = element instanceof Trade ? (Trade) element
                            : element instanceof TradeElement ? ((TradeElement) element).getTrade() : null;

            if (trade != null)
                new SecurityContextMenu(TradeDetailsView.this).handleEditKey(e, trade.getSecurity());
        }));
    }

    private void update()
    {
        if (table == null)
            return;

        var control = table.getTableViewer().getControl();
        if (control == null || control.isDisposed())
            return;

        if (currentUpdateJob != null)
            currentUpdateJob.cancel();

        control.setCursor(control.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

        Input preselectedInput = usePreselectedTrades.isTrue() ? input : null;

        currentUpdateJob = new UpdateTradesJob(preselectedInput, this.useSecurityCurrency, this.converter,
                        this.onlyOpen.isTrue(), this.onlyClosed.isTrue(), this.onlyProfitable.isTrue(),
                        this.onlyLossMaking.isTrue(), this.filterPattern, this.taxonomy, this.hideTotalsAtTheTop,
                        this.hideTotalsAtTheBottom);
        currentUpdateJob.setSystem(true);
        currentUpdateJob.schedule();
    }

    private void applyJobResult(Job job, List<?> finalTableInput, List<TradeCollectorException> errors)
    {
        if (currentUpdateJob != job)
            return;

        if (table == null)
            return;

        var viewer = table.getTableViewer();
        if (viewer == null)
            return;

        var control = viewer.getControl();
        if (control == null || control.isDisposed())
            return;

        table.setInput(finalTableInput);
        updateToolbarErrors(errors);
        control.setCursor(null);
        currentUpdateJob = null;
    }

    private void scheduleCursorReset(Job job)
    {
        Display display = Display.getDefault();
        if (display == null || display.isDisposed())
            return;

        display.asyncExec(() -> {
            if (currentUpdateJob != job)
                return;

            if (table == null)
                return;

            var viewer = table.getTableViewer();
            if (viewer == null)
                return;

            var control = viewer.getControl();
            if (control == null || control.isDisposed())
                return;

            control.setCursor(null);
            currentUpdateJob = null;
        });
    }

    private void updateToolbarErrors(List<TradeCollectorException> errors)
    {
        ToolBarManager toolBar = getToolBarManager();
        if (toolBar == null)
            return;

        if (!errors.isEmpty())
        {
            toolBar.remove(ID_WARNING_TOOL_ITEM);

            Action warning = new SimpleAction(Messages.MsgErrorTradeCollectionWithErrors,
                            Images.ERROR_NOTICE.descriptor(), a -> {
                                String message = errors.stream().map(TradeCollectorException::getMessage)
                                                .collect(Collectors.joining("\n\n")); //$NON-NLS-1$
                                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                                message);
                            });
            warning.setId(ID_WARNING_TOOL_ITEM);
            toolBar.insert(0, new ActionContributionItem(warning));
            toolBar.update(true);
        }
        else
        {
            if (toolBar.remove(ID_WARNING_TOOL_ITEM) != null)
                toolBar.update(true);
        }
    }

    @PreDestroy
    private void disposeView()
    {
        if (currentUpdateJob != null)
        {
            currentUpdateJob.cancel();
            currentUpdateJob = null;
        }
    }

    private static boolean matchesFilter(Trade trade, Pattern pattern)
    {
        if (pattern == null)
            return true;

        if (trade == null)
            return false;

        Security security = trade.getSecurity();

        String[] properties = new String[] { security.getName(), //
                        security.getIsin(), //
                        security.getTickerSymbol(), //
                        security.getWkn() //
        };

        for (String property : properties)
        {
            if (property != null && pattern.matcher(property).matches())
                return true;
        }

        return false;
    }

    private Input collectAllTrades(boolean useSecCurrency, CurrencyConverter currentConverter)
    {
        List<Trade> trades = new ArrayList<>();
        List<TradeCollectorException> errors = new ArrayList<>();
        getClient().getSecurities().forEach(s -> {
            try
            {
                CurrencyConverter converterToUse = useSecCurrency && s.getCurrencyCode() != null
                                ? currentConverter.with(s.getCurrencyCode())
                                : currentConverter;
                var collector = new TradeCollector(getClient(), converterToUse);
                trades.addAll(collector.collect(s));
            }
            catch (TradeCollectorException e)
            {
                errors.add(e);
            }
        });

        return new Input(null, trades, errors, useSecCurrency);
    }

    /**
     * Flattens the taxonomy-grouped trades into a list of TradeElements for
     * display in the table. Category rows are interleaved with trade rows. Uses
     * sortOrder to keep categories as headers - category gets sortOrder N, then
     * all its trades get sortOrder N+1 (same for all trades in that category).
     */
    private List<TradeElement> flattenToElements(TradesGroupedByTaxonomy groupedTrades, boolean hideTotalsAtTheTop,
                    boolean hideTotalsAtTheBottom)
    {
        List<TradeElement> elements = new ArrayList<>();
        TradeTotals totals = new TradeTotals(groupedTrades);

        if (!hideTotalsAtTheTop)
            elements.add(new TradeElement(totals, 0));

        int sortOrder = 1;

        for (TradeCategory category : groupedTrades.asList())
        {
            // Do not show categories that have no matching trades
            if (category.getTradeAssignments().isEmpty())
                continue;

            // Add category row with current sortOrder
            elements.add(new TradeElement(category, sortOrder));
            sortOrder++;

            // Add all trades in this category with the SAME sortOrder
            // This keeps them grouped together during sorting
            for (TradeAssignment assignment : category.getTradeAssignments())
            {
                elements.add(new TradeElement(assignment.getTrade(), sortOrder, assignment.getWeight()));
            }

            // Increment sortOrder for next category
            sortOrder++;
        }

        if (!hideTotalsAtTheBottom)
            elements.add(new TradeElement(totals, Integer.MAX_VALUE));

        return elements;
    }
}
