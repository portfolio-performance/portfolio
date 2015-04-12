package name.abuchen.portfolio.ui.views;

import java.util.Collections;
import java.util.Date;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeTypes;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.DividendFinalTransaction;
import name.abuchen.portfolio.snapshot.security.DividendInitialTransaction;
import name.abuchen.portfolio.snapshot.security.DividendTransaction;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.MarkDirtyListener;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.TaxonomyColumn;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

public class DividendsPerformanceView extends AbstractListView implements ReportingPeriodListener
{
    private ShowHideColumnHelper recordColumns;

    private TableViewer records;
    private TableViewer transactions;
    private ReportingPeriodDropDown dropDown;

    @Override
    protected String getTitle()
    {
        return Messages.LabelSecurityPerformance;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        dropDown = new ReportingPeriodDropDown(toolBar, getPart(), this);
        addExportButton(toolBar);
        addSaveButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addExportButton(ToolBar toolBar)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(records).export(getTitle() + ".csv"); //$NON-NLS-1$
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
                recordColumns.showSaveMenu(getActiveShell());
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
                recordColumns.showHideShowColumnsMenu(getActiveShell());
            }
        };
        config.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_CONFIG));
        config.setToolTipText(Messages.MenuShowHideColumns);

        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        records = new TableViewer(container, SWT.FULL_SELECTION);
        recordColumns = new ShowHideColumnHelper(DividendsPerformanceView.class.getName(), getClient(),
                        getPreferenceStore(), records, layout);
        ColumnViewerToolTipSupport.enableFor(records, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(records);

        createCommonColumns();
        createDividendColumns();
        createRiskColumns();
        createAdditionalColumns();

        recordColumns.createColumns();

        records.getTable().setHeaderVisible(true);
        records.getTable().setLinesVisible(true);

        records.setContentProvider(new SimpleListContentProvider());

        ViewerHelper.pack(records);

        records.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(records));

        hookContextMenu(records.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        records.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                SecurityPerformanceRecord record = (SecurityPerformanceRecord) ((IStructuredSelection) event
                                .getSelection()).getFirstElement();
                transactions.setInput(record != null ? record.getTransactions() : Collections.emptyList());
                transactions.refresh();
            }
        });

        reportingPeriodUpdated();
    }

    private void createCommonColumns()
    {
        // shares held
        Column column = new Column("shares", Messages.ColumnSharesOwned, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object e)
            {
                return ((SecurityPerformanceRecord) e).getSharesHeld();
            }

            @Override
            public String getToolTipText(Object e)
            {
                return Values.Share.format(((SecurityPerformanceRecord) e).getSharesHeld());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "sharesHeld")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // security name
        column = new NameColumn();
        recordColumns.addColumn(column);

        // True time-weighted rate of return
        column = new Column("twror", Messages.ColumnTWROR, SWT.RIGHT, 50); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnTWROR_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.format(((SecurityPerformanceRecord) r).getTrueTimeWeightedRateOfReturn());
            }

            @Override
            public Color getForeground(Object e)
            {
                return getColor(((SecurityPerformanceRecord) e).getTrueTimeWeightedRateOfReturn());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "trueTimeWeightedRateOfReturn")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // internal rate of return
        column = new Column("izf", Messages.ColumnIRR, SWT.RIGHT, 50); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnIRR_MenuLabel);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.format(((SecurityPerformanceRecord) r).getIrr());
            }

            @Override
            public Color getForeground(Object e)
            {
                return getColor(((SecurityPerformanceRecord) e).getIrr());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "irr")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // cost value - fifo
        column = new Column("pv", Messages.ColumnPurchaseValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchaseValue_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getFifoCost());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "fifoCost")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // cost value per share - fifo
        column = new Column("pp", Messages.ColumnPurchasePrice, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setDescription(Messages.ColumnPurchasePrice_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getFifoCostPerSharesHeld());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "fifoCostPerSharesHeld")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Gesamtsumme der erhaltenen Dividenden
        column = new Column("sumdiv", Messages.ColumnDividendSum, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendSum_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getSumOfDividends());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "sumOfDividends")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // market value
        column = new Column("mv", Messages.ColumnMarketValue, SWT.RIGHT, 75); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getMarketValue());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "marketValue")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // delta
        column = new Column("delta", Messages.ColumnDelta, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setDescription(Messages.ColumnDelta_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getDelta());
            }

            @Override
            public Color getForeground(Object e)
            {
                return getColor(((SecurityPerformanceRecord) e).getDelta());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "delta")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // fees paid
        column = new Column("fees", Messages.ColumnFees, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setDescription(Messages.ColumnFees_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getFees());
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
                return Values.Amount.format(((SecurityPerformanceRecord) r).getTaxes());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "taxes")); //$NON-NLS-1$
        column.setVisible(false);
        recordColumns.addColumn(column);

        // isin
        column = new IsinColumn();
        column.getEditingSupport().addListener(new MarkDirtyListener(this));
        column.setVisible(false);
        recordColumns.addColumn(column);

        // note
        column = new NoteColumn();
        column.getEditingSupport().addListener(new MarkDirtyListener(this));
        column.setVisible(false);
        recordColumns.addColumn(column);
    }

    private void createDividendColumns()
    {
        // Rendite insgesamt
        Column column = new Column("d%", Messages.ColumnDividendTotalRateOfReturn, SWT.RIGHT, 80); //$NON-NLS-1$
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
                Date date = ((SecurityPerformanceRecord) r).getLastDividendPayment();
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

        for (final AttributeType attribute : AttributeTypes.available(Security.class))
        {
            Column column = new AttributeColumn(attribute);
            column.setVisible(false);
            column.setEditingSupport(null);
            recordColumns.addColumn(column);
        }
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(DividendsPerformanceView.class.getSimpleName()
                        + "@bottom3", getPreferenceStore(), transactions, layout); //$NON-NLS-1$

        // date
        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Transaction t = (Transaction) e;
                return Values.Date.format(t.getDate());
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "date"), SWT.DOWN); //$NON-NLS-1$
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
                    return Messages.LabelDividends;
                else
                    return Messages.LabelQuote;
            }
        });
        support.addColumn(column);

        // shares
        column = new Column(Messages.ColumnShares, SWT.None, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object t)
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
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof DividendTransaction)
                    return Values.Amount.format(((DividendTransaction) t).getAmount());
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

        // einstandskurs / bewertung
        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object t)
            {
                if (t instanceof PortfolioTransaction)
                    return Values.Amount.format(((PortfolioTransaction) t).getAmount());
                else if (t instanceof DividendInitialTransaction)
                    return Values.Amount.format(((DividendInitialTransaction) t).getAmount());
                else if (t instanceof DividendFinalTransaction)
                    return Values.Amount.format(((DividendFinalTransaction) t).getAmount());
                else if (t instanceof AccountTransaction)
                    return Values.Amount.format(((AccountTransaction) t).getAmount());
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
                    return p.getCrossEntry() != null ? p.getCrossEntry().getCrossEntity(p).toString() : null;
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
                return note != null && note.length() > 0 ? PortfolioPlugin.image(PortfolioPlugin.IMG_NOTE) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(Transaction.class, "note")); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider());

        ViewerHelper.pack(transactions);
    }

    @Override
    public void notifyModelUpdated()
    {
        reportingPeriodUpdated();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        ReportingPeriod period = dropDown.getPeriods().getFirst();
        records.setInput(SecurityPerformanceSnapshot.create(getClient(), period).getRecords());
        records.refresh();
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Object selection = ((IStructuredSelection) records.getSelection()).getFirstElement();
        if (!(selection instanceof SecurityPerformanceRecord))
            return;

        Security security = ((SecurityPerformanceRecord) selection).getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
    }

    private static Color getColor(double value)
    {
        return Display.getCurrent().getSystemColor(value >= 0 ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
    }

}
