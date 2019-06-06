package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.security.DividendFinalTransaction;
import name.abuchen.portfolio.snapshot.security.DividendInitialTransaction;
import name.abuchen.portfolio.snapshot.security.DividendTransaction;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.NumberColorLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.TaxonomyColumn;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class SecuritiesPerformanceView extends AbstractListView implements ReportingPeriodListener
{
    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private final Predicate<SecurityPerformanceRecord> sharesGreaterZero = record -> record.getSharesHeld() > 0;
        private final Predicate<SecurityPerformanceRecord> sharesEqualZero = record -> record.getSharesHeld() == 0;

        private ClientFilterMenu clientFilterMenu;

        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityFilter, Images.FILTER_OFF, SWT.NONE);

            if (preferenceStore.getBoolean(SecuritiesPerformanceView.class.getSimpleName() + "-sharesGreaterZero")) //$NON-NLS-1$
                recordFilter.add(sharesGreaterZero);

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
                                recordFilter.contains(sharesGreaterZero));
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
            manager.add(createAction(Messages.SecurityFilterSharesHeldGreaterZero, sharesGreaterZero));
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
                        if (predicate == sharesGreaterZero)
                            recordFilter.remove(sharesEqualZero);
                        else if (predicate == sharesEqualZero)
                            recordFilter.remove(sharesGreaterZero);
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

    private ShowHideColumnHelper recordColumns;

    private TableViewer records;
    private TableViewer transactions;
    private TradesTableViewer trades;
    private ReportingPeriodDropDown dropDown;

    private ClientFilter clientFilter;
    private List<Predicate<SecurityPerformanceRecord>> recordFilter = new ArrayList<>();

    private SecuritiesChart chart;
    private SecurityDetailsViewer latest;

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
    protected void createTopTable(Composite parent)
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

        ColumnViewerToolTipSupport.enableFor(records, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(records);

        createCommonColumns();
        createDividendColumns();
        addPerformanceColumns();
        createRiskColumns();
        createAdditionalColumns();

        recordColumns.createColumns();

        records.getTable().setHeaderVisible(true);
        records.getTable().setLinesVisible(true);

        records.setContentProvider(ArrayContentProvider.getInstance());

        records.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(records));

        hookContextMenu(records.getTable(), this::fillContextMenu);

        records.addSelectionChangedListener(event -> {
            SecurityPerformanceRecord record = (SecurityPerformanceRecord) ((IStructuredSelection) event.getSelection())
                            .getFirstElement();

            if (record != null)
            {
                transactions.setInput(record.getTransactions());
                transactions.refresh();
                chart.updateChart(record.getSecurity());
                Client filteredClient = clientFilter.filter(getClient());
                CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
                trades.setInput(new TradeCollector(filteredClient, converter).collect(record.getSecurity()));
                latest.setInput(record.getSecurity());
            }
            else
            {
                transactions.setInput(null);
                transactions.refresh();
                chart.updateChart(null);
                trades.setInput(null);
                latest.setInput(null);
            }
        });

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
        column = new NameColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
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
                return Values.Quote.format(((SecurityPerformanceRecord) r).getFifoCostPerSharesHeld(),
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
                return Values.Quote.format(((SecurityPerformanceRecord) r).getMovingAverageCostPerSharesHeld(),
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

        // note
        column = new NoteColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        recordColumns.addColumn(column);
    }

    private void addPerformanceColumns()
    {
        Column column = new Column("twror", Messages.ColumnTWROR, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelPerformance);
        column.setMenuLabel(Messages.ColumnTWROR_Description);
        column.setLabelProvider(new NumberColorLabelProvider<>(Values.Percent2,
                        r -> ((SecurityPerformanceRecord) r).getTrueTimeWeightedRateOfReturn()));
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "trueTimeWeightedRateOfReturn")); //$NON-NLS-1$
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
                        element -> ((SecurityPerformanceRecord) element).getCapitalGainsOnHoldings(),
                        getClient().getBaseCurrency()));
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
                        getClient().getBaseCurrency()));
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
                        getClient().getBaseCurrency()));
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
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                LocalDate date = ((SecurityPerformanceRecord) r).getLastDividendPayment();
                return date != null ? Values.Date.format(date) : null;
            }
        });
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

        getClient().getSettings() //
                        .getAttributeTypes() //
                        .filter(a -> a.supports(Security.class)) //
                        .forEach(attribute -> {
                            Column column = new AttributeColumn(attribute);
                            column.setVisible(false);
                            column.setEditingSupport(null);
                            recordColumns.addColumn(column);
                        });
    }

    @Override
    protected void createBottomTable(Composite parent) // NOSONAR
    {
        Composite sash = new Composite(parent, SWT.NONE);
        sash.setLayout(new SashLayout(sash, SWT.HORIZONTAL | SWT.END));

        // folder
        CTabFolder folder = new CTabFolder(sash, SWT.BORDER);

        // folder - chart
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabChart);
        Composite chartComposite = new Composite(folder, SWT.NONE);
        item.setControl(chartComposite);

        chart = new SecuritiesChart(chartComposite, clientFilter.filter(getClient()),
                        new CurrencyConverterImpl(factory, getClient().getBaseCurrency()));

        // folder - transactions

        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabTransactions);
        Composite container = new Composite(folder, SWT.NONE);
        item.setControl(container);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(transactions, ToolTip.NO_RECREATE);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        SecuritiesPerformanceView.class.getSimpleName() + "@bottom4", getPreferenceStore(), //$NON-NLS-1$
                        transactions, layout);
        createTransactionColumns(support);
        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);
        transactions.setContentProvider(ArrayContentProvider.getInstance());

        // folder - trades

        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.SecurityTabTrades);
        trades = new TradesTableViewer(this);
        item.setControl(trades.createViewControl(folder, TradesTableViewer.ViewMode.SINGLE_SECURITY));

        folder.setSelection(0);

        // latest quote information

        latest = new SecurityDetailsViewer(sash, SWT.BORDER, getClient());
        latest.getControl().setLayoutData(new SashLayoutData(SWTHelper.getPackedWidth(latest.getControl())));

        reportingPeriodUpdated();
    }

    private void createTransactionColumns(ShowHideColumnHelper support)
    {
        // date
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Transaction t = (Transaction) e;
                return Values.DateTime.format(t.getDateTime());
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "dateTime"), SWT.DOWN); //$NON-NLS-1$
        support.addColumn(column);

        // transaction type
        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof PortfolioTransaction)
                    return ((PortfolioTransaction) t).getType().toString();
                else if (t instanceof AccountTransaction)
                    return ((AccountTransaction) t).getType().toString();
                else if (t instanceof DividendTransaction)
                    return AccountTransaction.Type.DIVIDENDS.toString();
                else
                    return Messages.LabelQuote;
            }
        });
        support.addColumn(column);

        // shares
        column = new Column(Messages.ColumnShares, SWT.None, 80);
        column.setLabelProvider(new SharesLabelProvider() // NOSONAR
        {
            @Override
            public Long getValue(Object t) // NOSONAR
            {
                if (t instanceof PortfolioTransaction)
                    return ((PortfolioTransaction) t).getShares();
                else if (t instanceof DividendInitialTransaction)
                    return ((DividendInitialTransaction) t).getPosition().getShares();
                else if (t instanceof DividendFinalTransaction)
                    return ((DividendFinalTransaction) t).getPosition().getShares();
                else if (t instanceof DividendTransaction)
                    return ((DividendTransaction) t).getShares() != 0L ? ((DividendTransaction) t).getShares() : null;
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend amount
        column = new Column(Messages.ColumnDividendPayment, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnGrossDividend);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof DividendTransaction)
                    return Values.Money.format(((DividendTransaction) t).getGrossValue(),
                                    getClient().getBaseCurrency());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend per share
        column = new Column(Messages.ColumnDividendPerShare, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof DividendTransaction)
                    return Values.AmountFraction.formatNonZero(((DividendTransaction) t).getDividendPerShare());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend per share
        column = new Column(Messages.ColumnPersonalDividendYield, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPersonalDividendYield_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof DividendTransaction)
                    return Values.Percent2.formatNonZero(((DividendTransaction) t).getPersonalDividendYield());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // dividend per share (moving average)
        column = new Column(Messages.ColumnPersonalDividendYieldMovingAverage, SWT.RIGHT, 80);
        column.setDescription(Messages.ColumnPersonalDividendYieldMovingAverage_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof DividendTransaction)
                    return Values.Percent2
                                    .formatNonZero(((DividendTransaction) t).getPersonalDividendYieldMovingAverage());
                else
                    return null;
            }
        });
        support.addColumn(column);

        // einstandskurs / bewertung
        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof DividendTransaction)
                    return null;
                else
                    return Values.Money.format(((Transaction) t).getMonetaryAmount(), getClient().getBaseCurrency());
            }
        });
        support.addColumn(column);

        // purchase quote
        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction p = (PortfolioTransaction) t;
                    return Values.Quote.format(p.getGrossPricePerShare(), getClient().getBaseCurrency());
                }
                else
                    return null;
            }
        });
        support.addColumn(column);

        // gegenkonto
        column = new Column(Messages.ColumnAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction p = (PortfolioTransaction) t;
                    return p.getCrossEntry() != null ? p.getCrossEntry().getCrossOwner(p).toString() : null;
                }
                else if (t instanceof DividendTransaction)
                {
                    return ((DividendTransaction) t).getAccount().getName();
                }
                else
                {
                    return null;
                }
            }
        });
        support.addColumn(column);

        // note
        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 22); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return ((Transaction) r).getNote();
            }

            @Override
            public Image getImage(Object r)
            {
                String note = ((Transaction) r).getNote();
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "note")); //$NON-NLS-1$
        support.addColumn(column);
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
        Interval period = dropDown.getSelectedPeriod().toInterval(LocalDate.now());
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        Client filteredClient = clientFilter.filter(getClient());
        records.setInput(SecurityPerformanceSnapshot.create(filteredClient, converter, period).getRecords());
        records.refresh();
        chart.setClient(filteredClient);
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
