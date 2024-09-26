package name.abuchen.portfolio.ui.views;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailProvider;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.ClientFilterMenu.Item;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.ClientFilterColumnOptions;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.MarkDirtyClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.NumberColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.OptionLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ToolTipCustomProviderSupport;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.DividendPaymentColumns;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.TaxonomyColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;
import name.abuchen.portfolio.ui.views.panes.CalculationLineItemPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

/**
 * @implNote The RowElement separates aggregate rows from the security rows. The
 *           columns must be able to handle row elements, EXCEPT for the sorter.
 *           The sorter must handle LazySecurityPerformanceRecord (and is
 *           centrally wrapped to handle the RowElement).
 */
public class SecuritiesPerformanceView extends AbstractFinanceView implements ReportingPeriodListener
{
    /**
     * Enables aggregate operations (sum) on a list of records.
     */
    private static class AggregateRow
    {
        private final List<LazySecurityPerformanceRecord> records;
        private final Map<Security, LazySecurityPerformanceRecord> security2record;

        public AggregateRow(List<LazySecurityPerformanceRecord> records)
        {
            this.records = records;
            this.security2record = records.stream().collect(Collectors.toMap(r -> r.getSecurity(), r -> r));
        }

        public List<LazySecurityPerformanceRecord> getRecords()
        {
            return records;
        }

        public LazySecurityPerformanceRecord getRecord(Security security)
        {
            return security2record.get(security);
        }

        public Money sum(String currencyCode, Function<LazySecurityPerformanceRecord, Money> value)
        {
            var sum = MutableMoney.of(currencyCode);

            for (var r : records)
            {
                var v = value.apply(r);
                if (v != null)
                    sum.add(v);
            }

            return sum.toMoney();
        }

        public Integer sum(ToIntFunction<LazySecurityPerformanceRecord> value)
        {
            var sum = 0;

            for (var r : records)
            {
                var v = value.applyAsInt(r);
                sum += v;
            }

            return sum;
        }
    }

    /**
     * Extends the aggregate row with a) caching of computed snapshots as
     * derived aggregate rows and b) caching of the filtered aggregate rows. The
     * two level of caching are needed in order to not compute all records only
     * because the display predicate changed.
     */
    private class Model extends AggregateRow
    {
        private static final String TOP = SecuritiesPerformanceView.class.getSimpleName() + "@top"; //$NON-NLS-1$
        private static final String BOTTOM = SecuritiesPerformanceView.class.getSimpleName() + "@bottom"; //$NON-NLS-1$

        private final Map<String, AggregateRow> filter2aggregates = new HashMap<>();
        private final Map<String, AggregateRow> predicate2cache = new HashMap<>();

        private boolean hideTotalsAtTheTop;
        private boolean hideTotalsAtTheBottom;

        public Model(LazySecurityPerformanceSnapshot snapshot)
        {
            super(snapshot.getRecords());

            this.hideTotalsAtTheTop = getPreferenceStore().getBoolean(TOP);
            this.hideTotalsAtTheBottom = getPreferenceStore().getBoolean(BOTTOM);
        }

        /**
         * Invalidates the predicate cache.
         */
        public void invalidatePredicateCache()
        {
            predicate2cache.clear();
        }

        /**
         * Returns the security performance records for a given filter. If the
         * snapshot has not been computed yet, it is computed first.
         */
        private AggregateRow getAggregate(ClientFilterMenu.Item filterItem)
        {
            return filter2aggregates.computeIfAbsent(filterItem.getId(), id -> {
                var filter = filterItem.getFilter();
                var period = dropDown.getSelectedPeriod().toInterval(LocalDate.now());
                var converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
                var snapshot = LazySecurityPerformanceSnapshot.create(filter.filter(getClient()), converter, period);
                return new AggregateRow(snapshot.getRecords());
            });
        }

        /**
         * Returns the security performance records for a given filter and the
         * current set of predicates applied to the view.
         */
        private AggregateRow getFilteredAggregate(ClientFilterMenu.Item filterItem)
        {
            return predicate2cache.computeIfAbsent(filterItem.getId(), id -> {

                var aggregate = getAggregate(filterItem);

                // remove records for which there is not corresponding record in
                // the base set

                List<LazySecurityPerformanceRecord> answer = new ArrayList<>();
                loop: for (var r : aggregate.getRecords()) // NOSONAR
                {
                    var baseRecord = getRecord(r.getSecurity());
                    if (baseRecord == null)
                        continue loop;

                    for (Predicate<LazySecurityPerformanceRecord> predicate : recordFilter)
                    {
                        if (!predicate.test(baseRecord))
                            continue loop;
                    }

                    answer.add(r);
                }

                return new AggregateRow(answer);
            });
        }

        /**
         * Returns the security performance records filtered for the current set
         * of predicates.
         */
        public AggregateRow getFilteredAggregate()
        {
            return predicate2cache.computeIfAbsent("$base$", id -> { //$NON-NLS-1$

                if (recordFilter.isEmpty())
                    return Model.this;

                List<LazySecurityPerformanceRecord> answer = new ArrayList<>();
                loop: for (var r : getRecords()) // NOSONAR
                {
                    for (Predicate<LazySecurityPerformanceRecord> predicate : recordFilter)
                    {
                        if (!predicate.test(r))
                            continue loop;
                    }

                    answer.add(r);
                }

                return new AggregateRow(answer);
            });
        }

        /**
         * Retrieves the Security performance record for the given filter.
         */
        public LazySecurityPerformanceRecord getRecord(Security security, ClientFilterMenu.Item filterItem)
        {
            return getAggregate(filterItem).getRecord(security);
        }

        public boolean isHideTotalsAtTheTop()
        {
            return hideTotalsAtTheTop;
        }

        public void setHideTotalsAtTheTop(boolean hideTotalsAtTheTop)
        {
            this.hideTotalsAtTheTop = hideTotalsAtTheTop;
            getPreferenceStore().setValue(TOP, hideTotalsAtTheTop);
        }

        public boolean isHideTotalsAtTheBottom()
        {
            return hideTotalsAtTheBottom;
        }

        public void setHideTotalsAtTheBottom(boolean hideTotalsAtTheBottom)
        {
            this.hideTotalsAtTheBottom = hideTotalsAtTheBottom;
            getPreferenceStore().setValue(BOTTOM, hideTotalsAtTheBottom);
        }

        public List<RowElement> getRows()
        {
            var rows = new ArrayList<RowElement>();

            if (!model.hideTotalsAtTheTop)
                rows.add(new RowElement(model, -1));

            model.getRecords().stream().map(r -> new RowElement(model, r)).forEach(rows::add);

            if (!model.hideTotalsAtTheBottom)
                rows.add(new RowElement(model, 1));

            return rows;
        }
    }

    /**
     * Wrapper to distinguish between a security performance record and the
     * aggregate rows.
     */
    private static class RowElement implements Adaptable, TrailProvider
    {
        /**
         * The sortOrder is used to enable a stable sorting positioning for the
         * aggregate rows (either on top or on the bottom or both).
         */
        private final int sortOrder;

        private final Model model;
        private final LazySecurityPerformanceRecord performanceRecord;

        public RowElement(Model model, LazySecurityPerformanceRecord performanceRecord)
        {
            this.sortOrder = 0;
            this.performanceRecord = performanceRecord;
            this.model = model;
        }

        public RowElement(Model model, int sortOrder)
        {
            this.sortOrder = sortOrder;
            this.performanceRecord = null;
            this.model = model;
        }

        public int getSortOrder()
        {
            return sortOrder;
        }

        public boolean isRecord()
        {
            return performanceRecord != null;
        }

        @Override
        public <T> T adapt(Class<T> type) // NOSONAR
        {
            if (type == BaseSecurityPerformanceRecord.class && isRecord())
                return type.cast(performanceRecord);
            else if (performanceRecord != null)
                return performanceRecord.adapt(type);
            else
                return null;
        }

        @Override
        public Optional<Trail> explain(String key)
        {
            if (performanceRecord == null)
                return Optional.empty();
            return performanceRecord.explain(key);
        }
    }

    /**
     * Comparator that compares row elements and delegates to the security
     * performance record when necessary.
     */
    private static class RowElementComparator implements Comparator<Object>
    {
        private Comparator<Object> comparator;

        public RowElementComparator(Comparator<Object> wrapped)
        {
            this.comparator = wrapped;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            var a = (RowElement) o1;
            var b = (RowElement) o2;

            if (a.getSortOrder() != b.getSortOrder())
            {
                int direction = ColumnViewerSorter.SortingContext.getSortDirection();
                return direction == SWT.UP ? a.getSortOrder() - b.getSortOrder() : b.getSortOrder() - a.getSortOrder();
            }

            return comparator.compare(a.performanceRecord, b.performanceRecord);
        }
    }

    /**
     * Label provider that is delegating to the underlying label provider that
     * handles LazySecurityPerformnceRecord objects.
     */
    private class RowElementLabelProvider extends ColumnLabelProvider
    {
        private final ColumnLabelProvider recordLabelProvider;
        private final Function<AggregateRow, String> aggregateLabelProvider;

        public RowElementLabelProvider(ColumnLabelProvider recordLabelProvider,
                        Function<AggregateRow, String> aggregateLabelProvider)
        {
            this.recordLabelProvider = recordLabelProvider;
            this.aggregateLabelProvider = aggregateLabelProvider;
        }

        public RowElementLabelProvider(ColumnLabelProvider recordLabelProvider)
        {
            this.recordLabelProvider = recordLabelProvider;
            this.aggregateLabelProvider = null;
        }

        public RowElementLabelProvider(Function<LazySecurityPerformanceRecord, String> recordLabelProvider)
        {
            this(ColumnLabelProvider.createTextProvider(
                            object -> recordLabelProvider.apply((LazySecurityPerformanceRecord) object)));
        }

        public RowElementLabelProvider(Function<LazySecurityPerformanceRecord, String> recordLabelProvider,
                        Function<AggregateRow, String> aggregateLabelProvider)
        {
            this(ColumnLabelProvider.createTextProvider(
                            object -> recordLabelProvider.apply((LazySecurityPerformanceRecord) object)),
                            aggregateLabelProvider);
        }

        public RowElementLabelProvider(Column column)
        {
            this((ColumnLabelProvider) column.getLabelProvider().get(), null);
        }

        public RowElementLabelProvider(Column column, Function<AggregateRow, String> aggregateLabelProvider)
        {
            this((ColumnLabelProvider) column.getLabelProvider().get(), aggregateLabelProvider);
        }

        @Override
        public Font getFont(Object element)
        {
            var row = (RowElement) element;
            if (row.performanceRecord != null)
                return recordLabelProvider.getFont(row.performanceRecord);
            else
                return boldFont;
        }

        @Override
        public Image getImage(Object element)
        {
            var row = (RowElement) element;
            if (row.performanceRecord != null)
                return recordLabelProvider.getImage(row.performanceRecord);
            else
                return null;
        }

        @Override
        public Color getForeground(Object element)
        {
            var row = (RowElement) element;
            if (row.performanceRecord != null)
                return recordLabelProvider.getForeground(row.performanceRecord);
            else
                return null;
        }

        @Override
        public String getText(Object element)
        {
            var row = (RowElement) element;
            if (row.performanceRecord != null)
                return recordLabelProvider.getText(row.performanceRecord);
            else if (aggregateLabelProvider != null)
                return aggregateLabelProvider.apply(row.model.getFilteredAggregate());
            else
                return null;
        }

        @Override
        public String getToolTipText(Object element)
        {
            var row = (RowElement) element;
            if (row.performanceRecord != null)
                return recordLabelProvider.getToolTipText(row.performanceRecord);
            else
                return null;
        }

        @Override
        public boolean useNativeToolTip(Object element)
        {
            var row = (RowElement) element;
            if (row.performanceRecord != null)
                return recordLabelProvider.useNativeToolTip(row.performanceRecord);
            else
                return true;
        }

    }

    /**
     * Label provider supporting options that is delegating to the underlying
     * label provider that handles LazySecurityPerformnceRecord objects.
     */
    private class RowElementOptionLabelProvider extends OptionLabelProvider<ClientFilterMenu.Item>
    {
        private final Function<LazySecurityPerformanceRecord, String> recordLabelProvider;
        private final Function<AggregateRow, String> aggregateLabelProvider;

        public RowElementOptionLabelProvider(Function<LazySecurityPerformanceRecord, String> recordLabelProvider,
                        Function<AggregateRow, String> aggregateLabelProvider)
        {
            this.recordLabelProvider = recordLabelProvider;
            this.aggregateLabelProvider = aggregateLabelProvider;
        }

        public RowElementOptionLabelProvider(Function<LazySecurityPerformanceRecord, String> recordLabelProvider)
        {
            this(recordLabelProvider, null);
        }

        @Override
        public String getText(Object element, ClientFilterMenu.Item filterItem)
        {
            var row = (RowElement) element;

            if (row.isRecord())
            {
                var dataRecord = row.model.getRecord(row.performanceRecord.getSecurity(), filterItem);
                return dataRecord == null ? null : recordLabelProvider.apply(dataRecord);
            }
            else if (aggregateLabelProvider != null)
            {
                return aggregateLabelProvider.apply(row.model.getFilteredAggregate(filterItem));
            }
            else
            {
                return null;
            }
        }

        @Override
        public Font getFont(Object element, Item option)
        {
            var row = (RowElement) element;
            return row.isRecord() ? null : boldFont;
        }
    }

    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private final Predicate<LazySecurityPerformanceRecord> sharesNotZero = r -> r.getSharesHeld().get() != 0;
        private final Predicate<LazySecurityPerformanceRecord> sharesEqualZero = r -> r.getSharesHeld().get() == 0;

        private ClientFilterMenu clientFilterMenu;

        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityFilter, Images.FILTER_OFF, SWT.NONE);

            if (preferenceStore.getBoolean(SecuritiesPerformanceView.class.getSimpleName() + "-sharesGreaterZero")) //$NON-NLS-1$
                recordFilter.add(sharesNotZero);

            if (preferenceStore.getBoolean(SecuritiesPerformanceView.class.getSimpleName() + "-sharesEqualZero")) //$NON-NLS-1$
                recordFilter.add(sharesEqualZero);

            clientFilterMenu = new ClientFilterMenu(getClient(), preferenceStore, f -> {
                setImage(recordFilter.isEmpty() && !clientFilterMenu.hasActiveFilter() ? Images.FILTER_OFF
                                : Images.FILTER_ON);
                clientFilter = f;
                notifyModelUpdated();
            });

            clientFilterMenu.trackSelectedFilterConfigurationKey(SecuritiesPerformanceView.class.getSimpleName());

            clientFilter = clientFilterMenu.getSelectedFilter();

            if (!recordFilter.isEmpty() || clientFilterMenu.hasActiveFilter())
                setImage(Images.FILTER_ON);

            setMenuListener(this);

            addDisposeListener(e -> {
                preferenceStore.setValue(SecuritiesPerformanceView.class.getSimpleName() + "-sharesGreaterZero", //$NON-NLS-1$
                                recordFilter.contains(sharesNotZero));
                preferenceStore.setValue(SecuritiesPerformanceView.class.getSimpleName() + "-sharesEqualZero", //$NON-NLS-1$
                                recordFilter.contains(sharesEqualZero));
            });
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(createAction(Messages.SecurityFilterSharesHeldNotZero, sharesNotZero));
            manager.add(createAction(Messages.SecurityFilterSharesHeldEqualZero, sharesEqualZero));

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.MenuChooseClientFilter));
            clientFilterMenu.menuAboutToShow(manager);
        }

        private Action createAction(String label, Predicate<LazySecurityPerformanceRecord> predicate)
        {
            Action action = new Action(label, IAction.AS_CHECK_BOX)
            {
                @Override
                public void run()
                {
                    boolean isChecked = recordFilter.contains(predicate);

                    if (isChecked)
                        recordFilter.remove(predicate);
                    else
                        recordFilter.add(predicate);

                    // uncheck mutually exclusive actions if new filter is added
                    if (!isChecked)
                    {
                        if (predicate == sharesNotZero)
                            recordFilter.remove(sharesEqualZero);
                        else if (predicate == sharesEqualZero)
                            recordFilter.remove(sharesNotZero);
                    }

                    setImage(recordFilter.isEmpty() && !clientFilterMenu.hasActiveFilter() ? Images.FILTER_OFF
                                    : Images.FILTER_ON);

                    model.invalidatePredicateCache();
                    records.refresh();
                }
            };
            action.setChecked(recordFilter.contains(predicate));
            return action;
        }
    }

    @Inject
    private SelectionService selectionService;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Inject
    private IStylingEngine stylingEngine;

    private ShowHideColumnHelper recordColumns;

    private TableViewer records;
    private ReportingPeriodDropDown dropDown;
    private Font boldFont;

    private ClientFilter clientFilter;
    private List<Predicate<LazySecurityPerformanceRecord>> recordFilter = new ArrayList<>();

    private Model model;

    @Override
    protected String getDefaultTitle()
    {
        return recordColumns == null ? Messages.LabelSecurityPerformance
                        : Messages.LabelSecurityPerformance + " (" + recordColumns.getConfigurationName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        dropDown = new ReportingPeriodDropDown(getPart(), this);
        toolBar.add(dropDown);

        toolBar.add(new FilterDropDown(getPreferenceStore()));
        addExportButton(toolBar);

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE, manager -> {
            recordColumns.menuAboutToShow(manager);

            manager.add(new Separator());
            MenuManager submenu = new MenuManager(Messages.PrefTitlePresentation);
            manager.add(submenu);

            Action action = new SimpleAction(Messages.LabelTotalsAtTheTop, a -> {
                model.setHideTotalsAtTheTop(!model.isHideTotalsAtTheTop());
                records.setInput(model.getRows());
                records.refresh();
            });
            action.setChecked(!model.isHideTotalsAtTheTop());
            submenu.add(action);

            action = new SimpleAction(Messages.LabelTotalsAtTheBottom, a -> {
                model.setHideTotalsAtTheBottom(!model.isHideTotalsAtTheBottom());
                records.setInput(model.getRows());
                records.refresh();
            });
            action.setChecked(!model.isHideTotalsAtTheBottom());
            submenu.add(action);
        }));

    }

    private void addExportButton(ToolBarManager manager)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(records).export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);

        manager.add(new ActionContributionItem(export));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        records = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        recordColumns = new ShowHideColumnHelper(SecuritiesPerformanceView.class.getName(), getClient(),
                        getPreferenceStore(), records, layout);

        updateTitle(getDefaultTitle());
        recordColumns.addListener(() -> updateTitle(getDefaultTitle()));
        recordColumns.setToolBarManager(getViewToolBarManager());

        ToolTipCustomProviderSupport.enableFor(records, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(records);
        CopyPasteSupport.enableFor(records);

        createCommonColumns();
        createDividendColumns();
        addPerformanceColumns();
        addCapitalGainsColumns();
        createRiskColumns();
        createAdditionalColumns();
        createClientFilteredColumns();
        createExperimentalEDivColumn();

        // wrap all underlying sorter with the element comparator to handle the
        // aggregate rows

        recordColumns.getColumns().forEach(c -> {
            var sorter = c.getSorter();
            if (sorter != null)
                sorter.wrap(RowElementComparator::new);
        });

        recordColumns.createColumns(true);

        records.getTable().setHeaderVisible(true);
        records.getTable().setLinesVisible(true);

        records.setContentProvider(ArrayContentProvider.getInstance());

        records.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(records));

        hookContextMenu(records.getTable(), this::fillContextMenu);

        records.addSelectionChangedListener(event -> setInformationPaneInput(
                        ((IStructuredSelection) event.getSelection()).getFirstElement()));

        records.addSelectionChangedListener(event -> {
            List<Security> securities = event.getStructuredSelection().stream().filter(e -> ((RowElement) e).isRecord())
                            .map(e -> ((RowElement) e).performanceRecord.getSecurity()).toList();
            if (!securities.isEmpty())
                selectionService.setSelection(new SecuritySelection(getClient(), securities));
            else
                selectionService.setSelection(null);

        });

        records.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (recordFilter.isEmpty())
                    return true;

                var row = (RowElement) element;
                if (!row.isRecord())
                    return true;

                for (Predicate<LazySecurityPerformanceRecord> predicate : recordFilter)
                {
                    if (!predicate.test(row.performanceRecord))
                        return false;
                }

                return true;
            }
        });

        stylingEngine.style(records.getTable());

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), records.getTable());
        boldFont = resources.create(FontDescriptor.createFrom(records.getTable().getFont()).setStyle(SWT.BOLD));

        reportingPeriodUpdated();

        return container;
    }

    private void createCommonColumns()
    {
        // shares held
        Column column = new Column("shares", Messages.ColumnSharesOwned, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            public Long getValue(Object e)
            {
                var row = (RowElement) e;
                return row.performanceRecord != null ? row.performanceRecord.getSharesHeld().get() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getSharesHeld().get()));
        recordColumns.addColumn(column);

        // security name
        column = new NameColumn(getClient());
        column.setLabelProvider(new RowElementLabelProvider(column, aggregate -> Messages.ColumnSum));
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setSortDirction(SWT.UP);
        recordColumns.addColumn(column);

        // cost value - fifo
        column = new Column("pv", Messages.ColumnPurchaseValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchaseValue_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Money.format(r.getFifoCost().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getFifoCost().get()),
                                        getClient().getBaseCurrency())));
        column.setToolTipProvider(
                        element -> ((RowElement) element).explain(LazySecurityPerformanceRecord.Trails.FIFO_COST));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getFifoCost().get()));
        recordColumns.addColumn(column);

        // cost value - moving average
        column = new Column("pvmvavg", Messages.ColumnPurchaseValueMovingAverage, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPurchaseValueMovingAverage_MenuLabel);
        column.setDescription(Messages.ColumnPurchaseValueMovingAverage_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Money.format(r.getMovingAverageCost().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(),
                                                        r -> r.getMovingAverageCost().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getMovingAverageCost().get()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // cost value per share - fifo
        column = new Column("pp", Messages.ColumnPurchasePrice, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchasePrice_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new RowElementLabelProvider(r -> Values.CalculatedQuote
                        .format(r.getFifoCostPerSharesHeld().get(), getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getFifoCostPerSharesHeld().get()));
        recordColumns.addColumn(column);

        // cost value per share - moving average
        column = new Column("ppmvavg", Messages.ColumnPurchasePriceMovingAverage, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPurchasePriceMovingAverage_MenuLabel);
        column.setDescription(Messages.ColumnPurchasePriceMovingAverage_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new RowElementLabelProvider(r -> Values.CalculatedQuote
                        .format(r.getMovingAverageCostPerSharesHeld().get(), getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getMovingAverageCostPerSharesHeld().get()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // latest / current quote
        column = new Column("quote", Messages.ColumnQuote, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnQuote_DescriptionEndOfReportingPeriod);
        column.setLabelProvider(new RowElementLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                LazySecurityPerformanceRecord entry = (LazySecurityPerformanceRecord) element;
                return Values.Quote.format(entry.getQuote().get(), getClient().getBaseCurrency());
            }

            @Override
            public String getToolTipText(Object element)
            {
                LazySecurityPerformanceRecord entry = (LazySecurityPerformanceRecord) element;

                return MessageFormat.format(Messages.TooltipQuoteAtDate, getText(element),
                                Values.Date.format(entry.getLatestSecurityPrice().get().getDate()));
            }
        }));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getQuote().get()));
        recordColumns.addColumn(column);

        // change to previous day percent value
        column = new Column("5", Messages.ColumnChangeOnPrevious, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnChangeOnPrevious_MenuLabel);
        column.setLabelProvider(new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.Percent2, element -> {
            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((LazySecurityPerformanceRecord) element)
                            .getSecurity().getLatestTwoSecurityPrices();
            if (previous.isPresent())
            {
                double latestQuote = previous.get().getLeft().getValue();
                double previousQuote = previous.get().getRight().getValue();
                return (latestQuote - previousQuote) / previousQuote;
            }
            else
            {
                return null;
            }
        }, element -> {
            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((LazySecurityPerformanceRecord) element)
                            .getSecurity().getLatestTwoSecurityPrices();
            if (previous.isPresent())
            {
                return Messages.ColumnLatestPrice + ": " //$NON-NLS-1$
                                + MessageFormat.format(Messages.TooltipQuoteAtDate,
                                                Values.Quote.format(previous.get().getLeft().getValue()),
                                                Values.Date.format(previous.get().getLeft().getDate()))
                                + "\n" // //$NON-NLS-1$
                                + Messages.ColumnPreviousPrice + ": " //$NON-NLS-1$
                                + MessageFormat.format(Messages.TooltipQuoteAtDate,
                                                Values.Quote.format(previous.get().getRight().getValue()),
                                                Values.Date.format(previous.get().getRight().getDate()));
            }
            else
            {
                return null;
            }
        })));
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> { // NOSONAR

            Optional<Pair<SecurityPrice, SecurityPrice>> previous1 = ((LazySecurityPerformanceRecord) o1).getSecurity()
                            .getLatestTwoSecurityPrices();
            Optional<Pair<SecurityPrice, SecurityPrice>> previous2 = ((LazySecurityPerformanceRecord) o2).getSecurity()
                            .getLatestTwoSecurityPrices();

            if (!previous1.isPresent() && !previous2.isPresent())
                return 0;
            if (!previous1.isPresent() && previous2.isPresent())
                return -1;
            if (previous1.isPresent() && !previous2.isPresent())
                return 1;

            double latestQuote1 = previous1.get().getLeft().getValue();
            double previousQuote1 = previous1.get().getRight().getValue();
            double v1 = (latestQuote1 - previousQuote1) / previousQuote1;

            double latestQuote2 = previous2.get().getLeft().getValue();
            double previousQuote2 = previous2.get().getRight().getValue();
            double v2 = (latestQuote2 - previousQuote2) / previousQuote2;

            return Double.compare(v1, v2);
        }));
        recordColumns.addColumn(column);

        // change to previous day absolute value
        column = new Column("changeonpreviousamount", Messages.ColumnChangeOnPreviousAmount, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnChangeOnPrevious_MenuLabelAmount);

        column.setLabelProvider(
                        new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.CalculatedQuote, element -> {
                            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((LazySecurityPerformanceRecord) element)
                                            .getSecurity().getLatestTwoSecurityPrices();
                            if (previous.isPresent())
                            {
                                double latestQuote = previous.get().getLeft().getValue();
                                double previousQuote = previous.get().getRight().getValue();
                                return (long) (latestQuote - previousQuote);
                            }
                            else
                            {
                                return null;
                            }
                        }, element -> {
                            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((LazySecurityPerformanceRecord) element)
                                            .getSecurity().getLatestTwoSecurityPrices();
                            if (previous.isPresent())
                            {
                                return Messages.ColumnLatestPrice + ": " //$NON-NLS-1$
                                                + MessageFormat.format(Messages.TooltipQuoteAtDate,
                                                                Values.Quote.format(
                                                                                previous.get().getLeft().getValue()),
                                                                Values.Date.format(previous.get().getLeft().getDate()))
                                                + "\n" // //$NON-NLS-1$
                                                + Messages.ColumnPreviousPrice + ": " //$NON-NLS-1$
                                                + MessageFormat.format(Messages.TooltipQuoteAtDate,
                                                                Values.Quote.format(
                                                                                previous.get().getRight().getValue()),
                                                                Values.Date.format(
                                                                                previous.get().getRight().getDate()));
                            }
                            else
                            {
                                return null;
                            }
                        })));
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> { // NOSONAR

            Optional<Pair<SecurityPrice, SecurityPrice>> previous1 = ((LazySecurityPerformanceRecord) o1).getSecurity()
                            .getLatestTwoSecurityPrices();
            Optional<Pair<SecurityPrice, SecurityPrice>> previous2 = ((LazySecurityPerformanceRecord) o2).getSecurity()
                            .getLatestTwoSecurityPrices();

            if (!previous1.isPresent() && !previous2.isPresent())
                return 0;
            if (!previous1.isPresent() && previous2.isPresent())
                return -1;
            if (previous1.isPresent() && !previous2.isPresent())
                return 1;

            double latestQuote1 = previous1.get().getLeft().getValue();
            double previousQuote1 = previous1.get().getRight().getValue();
            double v1 = latestQuote1 - previousQuote1;

            double latestQuote2 = previous2.get().getLeft().getValue();
            double previousQuote2 = previous2.get().getRight().getValue();
            double v2 = latestQuote2 - previousQuote2;

            return Double.compare(v1, v2);
        }));
        recordColumns.addColumn(column);

        // market value
        column = new Column("mv", Messages.ColumnMarketValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Money.format(r.getMarketValue().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getMarketValue().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getMarketValue().get()));
        recordColumns.addColumn(column);

        // fees paid
        column = new Column("fees", Messages.ColumnFees, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnFees_Description);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Money.format(r.getFees().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getFees().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getFees().get()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // taxes paid
        column = new Column("taxes", Messages.ColumnTaxes, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Money.format(r.getTaxes().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getTaxes().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getTaxes().get()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // isin
        column = new IsinColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // ticker
        column = new SymbolColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // wkn
        column = new WknColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // note
        column = new NoteColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        recordColumns.addColumn(column);
    }

    private void addPerformanceColumns()
    {
        Column column = new Column("twror", Messages.ColumnTTWROR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.LabelTTWROR);
        column.setLabelProvider(new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((LazySecurityPerformanceRecord) r).getTrueTimeWeightedRateOfReturn().get())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getTrueTimeWeightedRateOfReturn().get()));
        recordColumns.addColumn(column);

        column = new Column("ttwror_pa", Messages.ColumnTTWRORpa, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.LabelTTWROR_Annualized);
        column.setLabelProvider(new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((LazySecurityPerformanceRecord) r).getTrueTimeWeightedRateOfReturnAnnualized().get())));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(
                        e -> ((LazySecurityPerformanceRecord) e).getTrueTimeWeightedRateOfReturnAnnualized().get()));
        recordColumns.addColumn(column);

        // internal rate of return
        column = new Column("izf", Messages.ColumnIRR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        column.setLabelProvider(new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((LazySecurityPerformanceRecord) r).getIrr().get())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getIrr().get()));
        recordColumns.addColumn(column);

        column = new Column("capitalgains", Messages.ColumnCapitalGains, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setDescription(Messages.ColumnCapitalGains_Description);
        column.setLabelProvider(
                        new RowElementLabelProvider(
                                        new MoneyColorLabelProvider(
                                                        element -> ((LazySecurityPerformanceRecord) element)
                                                                        .getCapitalGainsOnHoldings().get(),
                                                        getClient()),
                                        aggregate -> Values.Money.format(
                                                        aggregate.sum(getClient().getBaseCurrency(),
                                                                        r -> r.getCapitalGainsOnHoldings().get()),
                                                        getClient().getBaseCurrency())));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getCapitalGainsOnHoldings().get()));
        recordColumns.addColumn(column);

        column = new Column("capitalgains%", Messages.ColumnCapitalGainsPercent, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setDescription(Messages.ColumnCapitalGainsPercent_Description);
        column.setLabelProvider(new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((LazySecurityPerformanceRecord) r).getCapitalGainsOnHoldingsPercent().get())));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getCapitalGainsOnHoldingsPercent().get()));
        recordColumns.addColumn(column);

        column = new Column("capitalgainsmvavg", Messages.ColumnCapitalGainsMovingAverage, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnCapitalGainsMovingAverage_MenuLabel);
        column.setDescription(Messages.ColumnCapitalGainsMovingAverage_Description);
        column.setLabelProvider(
                        new RowElementLabelProvider(
                                        new MoneyColorLabelProvider(
                                                        element -> ((LazySecurityPerformanceRecord) element)
                                                                        .getCapitalGainsOnHoldingsMovingAverage().get(),
                                                        getClient()),
                                        aggregate -> Values.Money.format(
                                                        aggregate.sum(getClient().getBaseCurrency(),
                                                                        r -> r.getCapitalGainsOnHoldingsMovingAverage()
                                                                                        .get()),
                                                        getClient().getBaseCurrency())));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(
                        e -> ((LazySecurityPerformanceRecord) e).getCapitalGainsOnHoldingsMovingAverage().get()));
        recordColumns.addColumn(column);

        column = new Column("capitalgainsmvavg%", Messages.ColumnCapitalGainsMovingAveragePercent, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnCapitalGainsMovingAveragePercent_MenuLabel);
        column.setDescription(Messages.ColumnCapitalGainsMovingAveragePercent_Description);
        column.setLabelProvider(new RowElementLabelProvider(
                        new NumberColorLabelProvider<>(Values.Percent2, r -> ((LazySecurityPerformanceRecord) r)
                                        .getCapitalGainsOnHoldingsMovingAveragePercent().get())));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e)
                        .getCapitalGainsOnHoldingsMovingAveragePercent().get()));
        recordColumns.addColumn(column);

        // delta
        column = new Column("delta", Messages.ColumnAbsolutePerformance, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnAbsolutePerformance_Description);
        column.setMenuLabel(Messages.ColumnAbsolutePerformance_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setLabelProvider(new RowElementLabelProvider(new MoneyColorLabelProvider(
                        element -> ((LazySecurityPerformanceRecord) element).getDelta().get(), getClient()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getDelta().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getDelta().get()));
        recordColumns.addColumn(column);

        // delta percent
        column = new Column("delta%", Messages.ColumnAbsolutePerformancePercent, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnAbsolutePerformancePercent_Description);
        column.setMenuLabel(Messages.ColumnAbsolutePerformancePercent_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setLabelProvider(new RowElementLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((LazySecurityPerformanceRecord) r).getDeltaPercent().get())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getDeltaPercent().get()));
        column.setVisible(false);
        recordColumns.addColumn(column);
    }

    private void addCapitalGainsColumns()
    {
        Column column = new Column("cg", //$NON-NLS-1$
                        Messages.ColumnRealizedCapitalGains, SWT.RIGHT, 80);
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(
                        new RowElementLabelProvider(
                                        new MoneyColorLabelProvider(element -> ((LazySecurityPerformanceRecord) element)
                                                        .getRealizedCapitalGains().get().getCapitalGains(),
                                                        getClient()),
                                        aggregate -> Values.Money.format(
                                                        aggregate.sum(getClient().getBaseCurrency(),
                                                                        r -> r.getRealizedCapitalGains().get()
                                                                                        .getCapitalGains()),
                                                        getClient().getBaseCurrency())));
        column.setToolTipProvider(element -> ((RowElement) element)
                        .explain(LazySecurityPerformanceRecord.Trails.REALIZED_CAPITAL_GAINS));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(element -> ((LazySecurityPerformanceRecord) element)
                        .getRealizedCapitalGains().get().getCapitalGains()));
        recordColumns.addColumn(column);

        column = new Column("cgforex", //$NON-NLS-1$
                        Messages.ColumnCurrencyGains + " / " + Messages.ColumnRealizedCapitalGains, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(
                        new RowElementLabelProvider(
                                        new MoneyColorLabelProvider(element -> ((LazySecurityPerformanceRecord) element)
                                                        .getRealizedCapitalGains().get().getForexCaptialGains(),
                                                        getClient()),
                                        aggregate -> Values.Money.format(
                                                        aggregate.sum(getClient().getBaseCurrency(),
                                                                        r -> r.getRealizedCapitalGains().get()
                                                                                        .getForexCaptialGains()),
                                                        getClient().getBaseCurrency())));
        column.setToolTipProvider(element -> ((RowElement) element)
                        .explain(LazySecurityPerformanceRecord.Trails.REALIZED_CAPITAL_GAINS_FOREX));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(element -> ((LazySecurityPerformanceRecord) element)
                        .getRealizedCapitalGains().get().getCapitalGains()));
        recordColumns.addColumn(column);

        column = new Column("ucg", //$NON-NLS-1$
                        Messages.ColumnUnrealizedCapitalGains, SWT.RIGHT, 80);
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(
                        new RowElementLabelProvider(
                                        new MoneyColorLabelProvider(element -> ((LazySecurityPerformanceRecord) element)
                                                        .getUnrealizedCapitalGains().get().getCapitalGains(),
                                                        getClient()),
                                        aggregate -> Values.Money.format(
                                                        aggregate.sum(getClient().getBaseCurrency(),
                                                                        r -> r.getUnrealizedCapitalGains().get()
                                                                                        .getCapitalGains()),
                                                        getClient().getBaseCurrency())));
        column.setToolTipProvider(element -> ((RowElement) element)
                        .explain(LazySecurityPerformanceRecord.Trails.UNREALIZED_CAPITAL_GAINS));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(element -> ((LazySecurityPerformanceRecord) element)
                        .getUnrealizedCapitalGains().get().getCapitalGains()));
        recordColumns.addColumn(column);

        column = new Column("ucgforex", //$NON-NLS-1$
                        Messages.ColumnCurrencyGains + " / " + Messages.ColumnUnrealizedCapitalGains, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(new RowElementLabelProvider(
                        new MoneyColorLabelProvider(element -> ((LazySecurityPerformanceRecord) element)
                                        .getUnrealizedCapitalGains().get().getForexCaptialGains(), getClient()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(),
                                                        r -> r.getUnrealizedCapitalGains().get()
                                                                        .getForexCaptialGains()),
                                        getClient().getBaseCurrency())));
        column.setToolTipProvider(element -> ((RowElement) element)
                        .explain(LazySecurityPerformanceRecord.Trails.UNREALIZED_CAPITAL_GAINS_FOREX));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(element -> ((LazySecurityPerformanceRecord) element)
                        .getUnrealizedCapitalGains().get().getCapitalGains()));
        recordColumns.addColumn(column);
    }

    private void createDividendColumns()
    {
        // Gesamtsumme der erhaltenen Dividenden
        Column column = new Column("sumdiv", Messages.ColumnDividendSum, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendSum_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Money.format(r.getSumOfDividends().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getSumOfDividends().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getSumOfDividends().get()));
        recordColumns.addColumn(column);

        // Rendite insgesamt
        column = new Column("d%", Messages.ColumnDividendTotalRateOfReturn, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setDescription(Messages.ColumnDividendTotalRateOfReturn_Description);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Percent2.formatNonZero(r.getTotalRateOfReturnDiv().get())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getTotalRateOfReturnDiv().get()));
        recordColumns.addColumn(column);

        // Rendite insgesamt, nach gleitendem Durchschnitt
        column = new Column("d%mvavg", Messages.ColumnDividendMovingAverageTotalRateOfReturn, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setMenuLabel(Messages.ColumnDividendMovingAverageTotalRateOfReturn_MenuLabel);
        column.setDescription(Messages.ColumnDividendMovingAverageTotalRateOfReturn_Description);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Percent2.formatNonZero(r.getTotalRateOfReturnDivMovingAverage().get())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getTotalRateOfReturnDivMovingAverage().get()));
        recordColumns.addColumn(column);

        // Rendite pro Jahr
        column = new Column("d%peryear", Messages.ColumnDividendRateOfReturnPerYear, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setDescription(Messages.ColumnDividendRateOfReturnPerYear_Description);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Percent2.formatNonZero(r.getRateOfReturnPerYear().get())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getRateOfReturnPerYear().get()));
        recordColumns.addColumn(column);

        // Anzahl der Dividendenereignisse
        column = new Column("dcount", Messages.ColumnDividendPaymentCount, SWT.RIGHT, 25); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setMenuLabel(Messages.ColumnDividendPaymentCount_MenuLabel);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(r -> Values.Id.format(r.getDividendEventCount().get()),
                        aggregate -> Values.Id.format(aggregate.sum(r -> r.getDividendEventCount().get()))));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getDividendEventCount().get()));
        recordColumns.addColumn(column);

        // Datum der letzten Dividendenzahlung
        column = new Column("dlast", Messages.ColumnLastDividendPayment, SWT.None, 75); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnLastDividendPayment_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(new DateLabelProvider(
                        r -> ((LazySecurityPerformanceRecord) r).getLastDividendPayment().get())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getLastDividendPayment().get()));
        recordColumns.addColumn(column);

        // Periodizität der Dividendenzahlungen
        column = new Column("dperiod", Messages.ColumnDividendPeriodicity, SWT.None, 100); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setDescription(Messages.ColumnDividendPeriodicity_Description);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(r -> r.getPeriodicity().get().toString()));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getPeriodicity().get().ordinal()));
        recordColumns.addColumn(column);

        DividendPaymentColumns.createFor(getClient()).forEach(c -> {
            c.setLabelProvider(new RowElementLabelProvider(c));
            recordColumns.addColumn(c);
        });
    }

    private void createRiskColumns()
    {
        Column column = new Column("mdd", Messages.ColumnMaxDrawdown, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.LabelMaxDrawdown);
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Percent2.formatNonZero(r.getDrawdown().get().getMaxDrawdown())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getDrawdown().get().getMaxDrawdown()));
        recordColumns.addColumn(column);

        column = new Column("mddduration", Messages.ColumnMaxDrawdownDuration, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.LabelMaxDrawdownDuration);
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return String.valueOf(((LazySecurityPerformanceRecord) r).getDrawdown().get().getMaxDrawdownDuration()
                                .getDays());
            }

            @Override
            public String getToolTipText(Object r)
            {
                return ((LazySecurityPerformanceRecord) r).getDrawdown().get().getMaxDrawdownDuration().toString();
            }
        }));
        column.setSorter(ColumnViewerSorter.create(e -> ((LazySecurityPerformanceRecord) e).getDrawdown().get()
                        .getMaxDrawdownDuration().getDays()));
        recordColumns.addColumn(column);

        column = new Column("vola", Messages.LabelVolatility, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Percent2.format(r.getVolatility().get().getStandardDeviation())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getVolatility().get().getStandardDeviation()));
        recordColumns.addColumn(column);

        column = new Column("semivola", Messages.LabelSemiVolatility, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new RowElementLabelProvider(
                        r -> Values.Percent2.format(r.getVolatility().get().getSemiDeviation())));
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((LazySecurityPerformanceRecord) e).getVolatility().get().getSemiDeviation()));
        recordColumns.addColumn(column);
    }

    private void createAdditionalColumns()
    {
        for (Taxonomy taxonomy : getClient().getTaxonomies())
        {
            Column column = new TaxonomyColumn(taxonomy);
            column.setVisible(false);
            recordColumns.addColumn(column);
        }

        AttributeColumn.createFor(getClient(), Security.class) //
                        .forEach(column -> {
                            column.getEditingSupport().addListener(new MarkDirtyClientListener(getClient()));
                            recordColumns.addColumn(column);
                        });
    }

    private void createClientFilteredColumns()
    {
        // the suffix added to the column label so that the message format can
        // print the filter name
        var suffix = " [{0}]"; //$NON-NLS-1$

        // shares held
        Column column = new Column("filter:shares", Messages.ColumnSharesOwned, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setOptions(new ClientFilterColumnOptions(Messages.ColumnSharesOwned + suffix,
                        new ClientFilterMenu(getClient(), getPreferenceStore())));
        column.setGroupLabel(Messages.LabelClientFilterMenu);
        column.setLabelProvider(
                        new RowElementOptionLabelProvider(r -> Values.Share.formatNonZero(r.getSharesHeld().get())));
        column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
            // important: because we wrap all sorters centrally, we only have
            // the unwrapped record here -> use the model to resolve the correct
            // record
            var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                            (ClientFilterMenu.Item) option);
            return dataRecord == null ? null : dataRecord.getSharesHeld().get();
        }));

        column.setVisible(false);
        recordColumns.addColumn(column);

        // cost value - fifo
        column = new Column("filter:pv", Messages.ColumnPurchaseValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setOptions(new ClientFilterColumnOptions(Messages.ColumnPurchaseValue + suffix,
                        new ClientFilterMenu(getClient(), getPreferenceStore())));
        column.setDescription(Messages.ColumnPurchaseValue_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setGroupLabel(Messages.LabelClientFilterMenu);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new RowElementOptionLabelProvider(
                        r -> Values.Money.format(r.getFifoCost().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getFifoCost().get()),
                                        getClient().getBaseCurrency())));
        column.setToolTipProvider((element, option) -> {
            var row = (RowElement) element;
            if (!row.isRecord())
                return null;
            var dataRecord = row.model.getRecord(row.performanceRecord.getSecurity(), (ClientFilterMenu.Item) option);
            return dataRecord == null ? "" : dataRecord.explain(LazySecurityPerformanceRecord.Trails.FIFO_COST); //$NON-NLS-1$
        });
        column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
            var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                            (ClientFilterMenu.Item) option);
            return dataRecord == null ? null : dataRecord.getFifoCost().get();
        }));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // market value
        column = new Column("filter:mv", Messages.ColumnMarketValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setOptions(new ClientFilterColumnOptions(Messages.ColumnMarketValue + suffix,
                        new ClientFilterMenu(getClient(), getPreferenceStore())));
        column.setGroupLabel(Messages.LabelClientFilterMenu);
        column.setLabelProvider(new RowElementOptionLabelProvider(
                        r -> Values.Money.format(r.getMarketValue().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getMarketValue().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
            var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                            (ClientFilterMenu.Item) option);
            return dataRecord == null ? null : dataRecord.getMarketValue().get();
        }));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // sum of dividends
        column = new Column("filter:sumdiv", Messages.ColumnDividendSum, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setOptions(new ClientFilterColumnOptions(Messages.ColumnDividendSum + suffix,
                        new ClientFilterMenu(getClient(), getPreferenceStore())));
        column.setMenuLabel(Messages.ColumnDividendSum_MenuLabel);
        column.setGroupLabel(Messages.LabelClientFilterMenu);
        column.setLabelProvider(new RowElementOptionLabelProvider(
                        r -> Values.Money.format(r.getSumOfDividends().get(), getClient().getBaseCurrency()),
                        aggregate -> Values.Money.format(
                                        aggregate.sum(getClient().getBaseCurrency(), r -> r.getSumOfDividends().get()),
                                        getClient().getBaseCurrency())));
        column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
            var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                            (ClientFilterMenu.Item) option);
            return dataRecord == null ? null : dataRecord.getSumOfDividends().get();
        }));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // true time weighted rate of return
        column = new Column("filter:twror", Messages.LabelTTWROR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setOptions(new ClientFilterColumnOptions(Messages.ColumnTTWROR + suffix,
                        new ClientFilterMenu(getClient(), getPreferenceStore())));
        column.setGroupLabel(Messages.LabelClientFilterMenu);
        column.setLabelProvider(new RowElementOptionLabelProvider(
                        r -> Values.Percent2.format(r.getTrueTimeWeightedRateOfReturn().get())));
        column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
            var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                            (ClientFilterMenu.Item) option);
            return dataRecord == null ? null : dataRecord.getTrueTimeWeightedRateOfReturn().get();
        }));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // internal rate of erturn
        column = new Column("filter:izf", Messages.ColumnIRR_MenuLabel, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setOptions(new ClientFilterColumnOptions(Messages.ColumnIRR + suffix,
                        new ClientFilterMenu(getClient(), getPreferenceStore())));
        column.setGroupLabel(Messages.LabelClientFilterMenu);
        column.setLabelProvider(new RowElementOptionLabelProvider(r -> Values.Percent2.format(r.getIrr().get())));
        column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
            var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                            (ClientFilterMenu.Item) option);
            return dataRecord == null ? null : dataRecord.getIrr().get();
        }));
        column.setVisible(false);
        recordColumns.addColumn(column);
    }

    private void createExperimentalEDivColumn()
    {
        // retrieve attribute

        Optional<AttributeType> attribute = getClient().getSettings().getAttributeTypes()
                        .filter(a -> a.supports(Security.class)) //
                        .filter(a -> a.getType() == Double.class) //
                        .filter(a -> "ediv".equalsIgnoreCase(a.getSource())) //$NON-NLS-1$
                        .findAny();

        if (attribute.isPresent())
        {
            var suffix = " [{0}]"; //$NON-NLS-1$
            var edivAttribute = attribute.get();

            // market value * expected dividend yield = expected dividends
            Function<LazySecurityPerformanceRecord, Money> valueProvider = dataRecord -> {
                var dive = (Double) dataRecord.getSecurity().getAttributes().get(edivAttribute);
                if (dive == null)
                    return null;

                var marketValue = dataRecord.getMarketValue().get();

                double expected = marketValue.getAmount() * dive;
                if (attribute.get().getConverter().getClass() == AttributeType.PercentPlainConverter.class)
                    expected /= 100;

                return Money.of(marketValue.getCurrencyCode(), Math.round(expected));
            };

            Column column = new Column("filter:expecteddividends", //$NON-NLS-1$
                            Messages.ExperimentalColumnExpectedDividends_MenuLabel, SWT.RIGHT, 80);
            column.setOptions(new ClientFilterColumnOptions(Messages.ExperimentalColumnExpectedDividends + suffix,
                            new ClientFilterMenu(getClient(), getPreferenceStore())));
            column.setGroupLabel(Messages.LabelClientFilterMenu);
            column.setLabelProvider(new RowElementOptionLabelProvider(
                            r -> Values.Money.formatNonZero(valueProvider.apply(r), getClient().getBaseCurrency()),
                            aggregate -> Values.Money.formatNonZero(
                                            aggregate.sum(getClient().getBaseCurrency(), valueProvider::apply),
                                            getClient().getBaseCurrency())));
            column.setSorter(ColumnViewerSorter.createWithOption((element, option) -> {
                var dataRecord = model.getRecord(((LazySecurityPerformanceRecord) element).getSecurity(),
                                (ClientFilterMenu.Item) option);
                return dataRecord == null ? null : valueProvider.apply(dataRecord);
            }));

            column.setVisible(false);
            recordColumns.addColumn(column);
        }
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(CalculationLineItemPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
    }

    @Override
    public void notifyModelUpdated()
    {
        // keep security selected even though SecurityPerformacneRecord changes
        Set<Security> oldSelection = new HashSet<>();
        for (Iterator<?> iter = records.getStructuredSelection().iterator(); iter.hasNext();)
        {
            var row = (RowElement) iter.next();
            if (row.isRecord())
                oldSelection.add(row.performanceRecord.getSecurity());
        }

        reportingPeriodUpdated();
        updateTitle(getDefaultTitle());

        List<RowElement> newSelection = ((List<?>) records.getInput()).stream() //
                        .map(e -> (RowElement) e) //
                        .filter(e -> e.isRecord()) //
                        .filter(e -> oldSelection.contains(e.performanceRecord.getSecurity())) //
                        .collect(toMutableList());
        records.setSelection(new StructuredSelection(newSelection));
    }

    @Override
    public void reportingPeriodUpdated()
    {
        Client filteredClient = clientFilter.filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);

        Interval period = dropDown.getSelectedPeriod().toInterval(LocalDate.now());
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

        model = new Model(LazySecurityPerformanceSnapshot.create(filteredClient, converter, period));

        records.setInput(model.getRows());
        records.refresh();
    }

    private void fillContextMenu(IMenuManager manager) // NOSONAR
    {
        RowElement row = (RowElement) ((IStructuredSelection) records.getSelection()).getFirstElement();
        if (row == null || !row.isRecord())
            return;

        Security security = row.performanceRecord.getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
    }
}
