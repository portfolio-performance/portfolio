package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.MarkDirtyClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.MoneyTrailToolTipSupport;
import name.abuchen.portfolio.ui.util.viewers.NumberColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
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

public class SecuritiesPerformanceView extends AbstractFinanceView implements ReportingPeriodListener
{
    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private final Predicate<SecurityPerformanceRecord> sharesNotZero = record -> record.getSharesHeld() != 0;
        private final Predicate<SecurityPerformanceRecord> sharesEqualZero = record -> record.getSharesHeld() == 0;

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

            loadPreselectedClientFilter(preferenceStore);

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

        private void loadPreselectedClientFilter(IPreferenceStore preferenceStore)
        {
            String selection = preferenceStore.getString(
                            SecuritiesPerformanceView.class.getSimpleName() + ClientFilterMenu.PREF_KEY_POSTFIX);
            if (selection != null)
                clientFilterMenu.getAllItems().filter(item -> item.getUUIDs().equals(selection)).findAny()
                                .ifPresent(clientFilterMenu::select);

            clientFilterMenu.addListener(filter -> preferenceStore.putValue(
                            SecuritiesPerformanceView.class.getSimpleName() + ClientFilterMenu.PREF_KEY_POSTFIX,
                            clientFilterMenu.getSelectedItem().getUUIDs()));
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

        private Action createAction(String label, Predicate<SecurityPerformanceRecord> predicate)
        {
            Action action = new Action(label, Action.AS_CHECK_BOX)
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

    private ClientFilter clientFilter;
    private List<Predicate<SecurityPerformanceRecord>> recordFilter = new ArrayList<>();

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

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> recordColumns.menuAboutToShow(manager)));
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

        records = new TableViewer(container, SWT.FULL_SELECTION);
        recordColumns = new ShowHideColumnHelper(SecuritiesPerformanceView.class.getName(), getClient(),
                        getPreferenceStore(), records, layout);

        updateTitle(getDefaultTitle());
        recordColumns.addListener(() -> updateTitle(getDefaultTitle()));
        recordColumns.setToolBarManager(getViewToolBarManager());

        MoneyTrailToolTipSupport.enableFor(records, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(records);
        CopyPasteSupport.enableFor(records);

        createCommonColumns();
        createDividendColumns();
        addPerformanceColumns();
        addCapitalGainsColumns();
        createRiskColumns();
        createAdditionalColumns();

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
            SecurityPerformanceRecord record = (SecurityPerformanceRecord) ((IStructuredSelection) event.getSelection())
                            .getFirstElement();
            if (record != null)
                selectionService.setSelection(new SecuritySelection(getClient(), record.getSecurity()));
        });

        records.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (recordFilter.isEmpty())
                    return true;

                for (Predicate<SecurityPerformanceRecord> predicate : recordFilter)
                {
                    if (!predicate.test((SecurityPerformanceRecord) element))
                        return false;
                }

                return true;
            }
        });

        stylingEngine.style(records.getTable());

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
                return ((SecurityPerformanceRecord) e).getSharesHeld();
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "sharesHeld")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // security name
        column = new NameColumn(getClient());
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setSortDirction(SWT.UP);
        recordColumns.addColumn(column);

        // cost value - fifo
        column = new Column("pv", Messages.ColumnPurchaseValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchaseValue_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Money.format(((SecurityPerformanceRecord) r).getFifoCost(),
                                getClient().getBaseCurrency());
            }

            @Override
            public String getToolTipText(Object r)
            {
                return ((SecurityPerformanceRecord) r).explain(SecurityPerformanceRecord.Trails.FIFO_COST).isPresent()
                                ? SecurityPerformanceRecord.Trails.FIFO_COST
                                : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "fifoCost")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // cost value - moving average
        column = new Column("pvmvavg", Messages.ColumnPurchaseValueMovingAverage, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPurchaseValueMovingAverage_MenuLabel);
        column.setDescription(Messages.ColumnPurchaseValueMovingAverage_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Money.format(((SecurityPerformanceRecord) r).getMovingAverageCost(),
                                getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "movingAverageCost")); //$NON-NLS-1$
        column.setVisible(false);
        recordColumns.addColumn(column);

        // cost value per share - fifo
        column = new Column("pp", Messages.ColumnPurchasePrice, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchasePrice_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.CalculatedQuote.format(((SecurityPerformanceRecord) r).getFifoCostPerSharesHeld(),
                                getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "fifoCostPerSharesHeld")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // cost value per share - moving average
        column = new Column("ppmvavg", Messages.ColumnPurchasePriceMovingAverage, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnPurchasePriceMovingAverage_MenuLabel);
        column.setDescription(Messages.ColumnPurchasePriceMovingAverage_Description + TextUtil.PARAGRAPH_BREAK
                        + Messages.DescriptionDataRelativeToReportingPeriod);
        column.setImage(Images.INTERVAL);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.CalculatedQuote.format(
                                ((SecurityPerformanceRecord) r).getMovingAverageCostPerSharesHeld(),
                                getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "movingAverageCostPerSharesHeld")); //$NON-NLS-1$
        column.setVisible(false);
        recordColumns.addColumn(column);

        // latest / current quote
        column = new Column("quote", Messages.ColumnQuote, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnQuote_DescriptionEndOfReportingPeriod);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                SecurityPerformanceRecord record = (SecurityPerformanceRecord) element;
                return Values.Quote.format(record.getQuote(), getClient().getBaseCurrency());
            }

            @Override
            public String getToolTipText(Object element)
            {
                SecurityPerformanceRecord record = (SecurityPerformanceRecord) element;

                return MessageFormat.format(Messages.TooltipQuoteAtDate, getText(element),
                                Values.Date.format(record.getLatestSecurityPrice().getDate()));
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((SecurityPerformanceRecord) e).getQuote()));
        recordColumns.addColumn(column);

        // change to previous day percent value
        column = new Column("5", Messages.ColumnChangeOnPrevious, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnChangeOnPrevious_MenuLabel);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2, element -> {
            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((SecurityPerformanceRecord) element).getSecurity()
                            .getLatestTwoSecurityPrices();
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
            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((SecurityPerformanceRecord) element).getSecurity()
                            .getLatestTwoSecurityPrices();
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
        }));
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> { // NOSONAR

            Optional<Pair<SecurityPrice, SecurityPrice>> previous1 = ((SecurityPerformanceRecord) o1).getSecurity()
                            .getLatestTwoSecurityPrices();
            Optional<Pair<SecurityPrice, SecurityPrice>> previous2 = ((SecurityPerformanceRecord) o2).getSecurity()
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

        column.setLabelProvider(new NumberColorLabelProvider<>(Values.CalculatedQuote, element -> {
            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((SecurityPerformanceRecord) element).getSecurity()
                            .getLatestTwoSecurityPrices();
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
            Optional<Pair<SecurityPrice, SecurityPrice>> previous = ((SecurityPerformanceRecord) element).getSecurity()
                            .getLatestTwoSecurityPrices();
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
        }));
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> { // NOSONAR

            Optional<Pair<SecurityPrice, SecurityPrice>> previous1 = ((SecurityPerformanceRecord) o1).getSecurity()
                            .getLatestTwoSecurityPrices();
            Optional<Pair<SecurityPrice, SecurityPrice>> previous2 = ((SecurityPerformanceRecord) o2).getSecurity()
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
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Money.format(((SecurityPerformanceRecord) r).getMarketValue(),
                                getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "marketValue")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // fees paid
        column = new Column("fees", Messages.ColumnFees, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnFees_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Money.format(((SecurityPerformanceRecord) r).getFees(), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "fees")); //$NON-NLS-1$
        column.setVisible(false);
        recordColumns.addColumn(column);

        // taxes paid
        column = new Column("taxes", Messages.ColumnTaxes, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Money.format(((SecurityPerformanceRecord) r).getTaxes(), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "taxes")); //$NON-NLS-1$
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
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((SecurityPerformanceRecord) r).getTrueTimeWeightedRateOfReturn()));
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "trueTimeWeightedRateOfReturn")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("ttwror_pa", Messages.ColumnTTWRORpa, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.LabelTTWROR_Annualized);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((SecurityPerformanceRecord) r).getTrueTimeWeightedRateOfReturnAnnualized()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class,
                        "trueTimeWeightedRateOfReturnAnnualized")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // internal rate of return
        column = new Column("izf", Messages.ColumnIRR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        column.setLabelProvider(
                        new NumberColorLabelProvider<>(Values.Percent2, r -> ((SecurityPerformanceRecord) r).getIrr()));
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "irr")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("capitalgains", Messages.ColumnCapitalGains, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setDescription(Messages.ColumnCapitalGains_Description);
        column.setLabelProvider(new MoneyColorLabelProvider(
                        element -> ((SecurityPerformanceRecord) element).getCapitalGainsOnHoldings(), getClient()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "capitalGainsOnHoldings")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("capitalgains%", Messages.ColumnCapitalGainsPercent, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setDescription(Messages.ColumnCapitalGainsPercent_Description);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((SecurityPerformanceRecord) r).getCapitalGainsOnHoldingsPercent()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "capitalGainsOnHoldingsPercent")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("capitalgainsmvavg", Messages.ColumnCapitalGainsMovingAverage, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnCapitalGainsMovingAverage_MenuLabel);
        column.setDescription(Messages.ColumnCapitalGainsMovingAverage_Description);
        column.setLabelProvider(new MoneyColorLabelProvider(
                        element -> ((SecurityPerformanceRecord) element).getCapitalGainsOnHoldingsMovingAverage(),
                        getClient()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class,
                        "capitalGainsOnHoldingsMovingAverage")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("capitalgainsmvavg%", Messages.ColumnCapitalGainsMovingAveragePercent, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnCapitalGainsMovingAveragePercent_MenuLabel);
        column.setDescription(Messages.ColumnCapitalGainsMovingAveragePercent_Description);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((SecurityPerformanceRecord) r).getCapitalGainsOnHoldingsMovingAveragePercent()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class,
                        "capitalGainsOnHoldingsMovingAveragePercent")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // delta
        column = new Column("delta", Messages.ColumnAbsolutePerformance, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnAbsolutePerformance_Description);
        column.setMenuLabel(Messages.ColumnAbsolutePerformance_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setLabelProvider(new MoneyColorLabelProvider(element -> ((SecurityPerformanceRecord) element).getDelta(),
                        getClient()));
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "delta")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // delta percent
        column = new Column("delta%", Messages.ColumnAbsolutePerformancePercent, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnAbsolutePerformancePercent_Description);
        column.setMenuLabel(Messages.ColumnAbsolutePerformancePercent_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((SecurityPerformanceRecord) r).getDeltaPercent()));
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "deltaPercent")); //$NON-NLS-1$
        column.setVisible(false);
        recordColumns.addColumn(column);
    }

    private void addCapitalGainsColumns()
    {
        Column column = new Column("cg", //$NON-NLS-1$
                        Messages.ColumnRealizedCapitalGains, SWT.RIGHT, 80);
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(new MoneyColorLabelProvider(
                        element -> ((SecurityPerformanceRecord) element).getRealizedCapitalGains().getCapitalGains(),
                        element -> SecurityPerformanceRecord.Trails.REALIZED_CAPITAL_GAINS, getClient()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(
                        element -> ((SecurityPerformanceRecord) element).getRealizedCapitalGains().getCapitalGains()));
        recordColumns.addColumn(column);

        column = new Column("cgforex", //$NON-NLS-1$
                        Messages.ColumnCurrencyGains + " / " + Messages.ColumnRealizedCapitalGains, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(new MoneyColorLabelProvider(
                        element -> ((SecurityPerformanceRecord) element).getRealizedCapitalGains()
                                        .getForexCaptialGains(),
                        element -> SecurityPerformanceRecord.Trails.REALIZED_CAPITAL_GAINS_FOREX, getClient()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(
                        element -> ((SecurityPerformanceRecord) element).getRealizedCapitalGains().getCapitalGains()));
        recordColumns.addColumn(column);

        column = new Column("ucg", //$NON-NLS-1$
                        Messages.ColumnUnrealizedCapitalGains, SWT.RIGHT, 80);
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(new MoneyColorLabelProvider(
                        element -> ((SecurityPerformanceRecord) element).getUnrealizedCapitalGains().getCapitalGains(),
                        element -> SecurityPerformanceRecord.Trails.UNREALIZED_CAPITAL_GAINS, getClient()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(element -> ((SecurityPerformanceRecord) element)
                        .getUnrealizedCapitalGains().getCapitalGains()));
        recordColumns.addColumn(column);

        column = new Column("ucgforex", //$NON-NLS-1$
                        Messages.ColumnCurrencyGains + " / " + Messages.ColumnUnrealizedCapitalGains, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelCapitalGains);
        column.setLabelProvider(new MoneyColorLabelProvider(
                        element -> ((SecurityPerformanceRecord) element).getUnrealizedCapitalGains()
                                        .getForexCaptialGains(),
                        element -> SecurityPerformanceRecord.Trails.UNREALIZED_CAPITAL_GAINS_FOREX, getClient()));
        column.setVisible(false);
        column.setSorter(ColumnViewerSorter.create(element -> ((SecurityPerformanceRecord) element)
                        .getUnrealizedCapitalGains().getCapitalGains()));
        recordColumns.addColumn(column);
    }

    private void createDividendColumns()
    {
        // Gesamtsumme der erhaltenen Dividenden
        Column column = new Column("sumdiv", Messages.ColumnDividendSum, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendSum_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Money.format(((SecurityPerformanceRecord) r).getSumOfDividends(),
                                getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "sumOfDividends")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Rendite insgesamt
        column = new Column("d%", Messages.ColumnDividendTotalRateOfReturn, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setDescription(Messages.ColumnDividendTotalRateOfReturn_Description);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.formatNonZero(((SecurityPerformanceRecord) r).getTotalRateOfReturnDiv());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "totalRateOfReturnDiv")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Rendite insgesamt, nach gleitendem Durchschnitt
        column = new Column("d%mvavg", Messages.ColumnDividendMovingAverageTotalRateOfReturn, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setMenuLabel(Messages.ColumnDividendMovingAverageTotalRateOfReturn_MenuLabel);
        column.setDescription(Messages.ColumnDividendMovingAverageTotalRateOfReturn_Description);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2
                                .formatNonZero(((SecurityPerformanceRecord) r).getTotalRateOfReturnDivMovingAverage());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class,
                        "totalRateOfReturnDivMovingAverage")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Rendite pro Jahr
        column = new Column("d%peryear", Messages.ColumnDividendRateOfReturnPerYear, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setDescription(Messages.ColumnDividendRateOfReturnPerYear_Description);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.formatNonZero(((SecurityPerformanceRecord) r).getRateOfReturnPerYear());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "rateOfReturnPerYear")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Anzahl der Dividendenereignisse
        column = new Column("dcount", Messages.ColumnDividendPaymentCount, SWT.RIGHT, 25); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setMenuLabel(Messages.ColumnDividendPaymentCount_MenuLabel);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Id.format(((SecurityPerformanceRecord) r).getDividendEventCount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "dividendEventCount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Datum der letzten Dividendenzahlung
        column = new Column("dlast", Messages.ColumnLastDividendPayment, SWT.None, 75); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnLastDividendPayment_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);
        column.setLabelProvider(new DateLabelProvider(r -> ((SecurityPerformanceRecord) r).getLastDividendPayment()));
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "lastDividendPayment")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Periodizität der Dividendenzahlungen
        column = new Column("dperiod", Messages.ColumnDividendPeriodicity, SWT.None, 100); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setDescription(Messages.ColumnDividendPeriodicity_Description);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return ((SecurityPerformanceRecord) r).getPeriodicity().toString();
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "periodicitySort")); //$NON-NLS-1$
        recordColumns.addColumn(column);
    }

    private void createRiskColumns()
    {
        Column column = new Column("mdd", Messages.ColumnMaxDrawdown, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.LabelMaxDrawdown);
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.formatNonZero(((SecurityPerformanceRecord) r).getMaxDrawdown());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "maxDrawdown")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("mddduration", Messages.ColumnMaxDrawdownDuration, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.LabelMaxDrawdownDuration);
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return String.valueOf(((SecurityPerformanceRecord) r).getMaxDrawdownDuration());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "maxDrawdownDuration")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("vola", Messages.LabelVolatility, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.format(((SecurityPerformanceRecord) r).getVolatility());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "volatility")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        column = new Column("semivola", Messages.LabelSemiVolatility, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.LabelRiskIndicators);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.format(((SecurityPerformanceRecord) r).getSemiVolatility());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "semiVolatility")); //$NON-NLS-1$
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
            oldSelection.add(((SecurityPerformanceRecord) iter.next()).getSecurity());

        reportingPeriodUpdated();
        updateTitle(getDefaultTitle());

        List<SecurityPerformanceRecord> newSelection = ((List<?>) records.getInput()).stream()
                        .map(e -> (SecurityPerformanceRecord) e) //
                        .filter(e -> oldSelection.contains(e.getSecurity())) //
                        .collect(Collectors.toList());
        records.setSelection(new StructuredSelection(newSelection));
    }

    @Override
    public void reportingPeriodUpdated()
    {
        Client filteredClient = clientFilter.filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);

        Interval period = dropDown.getSelectedPeriod().toInterval(LocalDate.now());
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        records.setInput(SecurityPerformanceSnapshot.create(filteredClient, converter, period).getRecords());
        records.refresh();
    }

    private void fillContextMenu(IMenuManager manager) // NOSONAR
    {
        Object selection = ((IStructuredSelection) records.getSelection()).getFirstElement();
        if (!(selection instanceof SecurityPerformanceRecord))
            return;

        Security security = ((SecurityPerformanceRecord) selection).getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
    }
}
