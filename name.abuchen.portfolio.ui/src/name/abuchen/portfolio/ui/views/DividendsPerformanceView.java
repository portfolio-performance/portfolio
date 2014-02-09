package name.abuchen.portfolio.ui.views;

import java.util.Collections;
import java.util.Date;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
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
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
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
        dropDown = new ReportingPeriodDropDown(toolBar, getClientEditor(), this);
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
                recordColumns.showSaveMenu(getClientEditor().getSite().getShell());
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
                recordColumns.showHideShowColumnsMenu(getClientEditor().getSite().getShell());
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
        recordColumns = new ShowHideColumnHelper(DividendsPerformanceView.class.getName(), getClient(), records, layout);

        createCommonColumns();
        createDividendColumns();
        createDividendProjectionColumns();

        recordColumns.createColumns();

        records.getTable().setHeaderVisible(true);
        records.getTable().setLinesVisible(true);

        records.setContentProvider(new SimpleListContentProvider());

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
        ViewerHelper.pack(records);
    }

    private void createCommonColumns()
    {
        // security name
        Column column = new Column(Messages.ColumnName, SWT.None, 300);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return ((SecurityPerformanceRecord) r).getSecurityName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "SecurityName"), SWT.DOWN); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // internal rate of return
        column = new Column(Messages.ColumnIRR, SWT.RIGHT, 50);
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

        // shares held
        column = new Column(Messages.ColumnSharesOwned, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object e)
            {
                return ((SecurityPerformanceRecord) e).getStockShares();
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "stockShares")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // cost value
        column = new Column("Einstand", SWT.RIGHT, 75);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getStockAmount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "stockAmount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Gesamtsumme der erhaltenen Dividenden
        column = new Column("∑Div", SWT.RIGHT, 80);
        column.setGroupLabel("Dividenden");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.format(((SecurityPerformanceRecord) r).getDivAmount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "divAmount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // market value
        column = new Column(Messages.ColumnMarketValue, SWT.RIGHT, 75);
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
        column = new Column(Messages.ColumnDelta, SWT.RIGHT, 100);
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
    }

    private void createDividendColumns()
    {
        // Rendite insgesamt
        Column column = new Column("Div%", SWT.RIGHT, 80);
        column.setGroupLabel("Dividenden");
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

        // jährliche Dividendenrendite bezogen auf den Einstandspreis (izf-div)
        column = new Column("øDiv%", SWT.RIGHT, 80);
        column.setGroupLabel("Dividenden");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.formatNonZero(((SecurityPerformanceRecord) r).getIrrDiv(), 0.001d);
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "irrDiv")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Anzahl der Dividendenereignisse
        column = new Column("#Div", SWT.RIGHT, 25);
        column.setGroupLabel("Dividenden");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Id.format(((SecurityPerformanceRecord) r).getDivEventCount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "divEventCount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Datum der letzten Dividendenzahlung
        column = new Column("zuletzt am", SWT.None, 75);
        column.setGroupLabel("Dividenden");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                Date date = ((SecurityPerformanceRecord) r).getDateTo();
                return date != null ? Values.Date.format(date) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "dateTo")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Periodizität der Dividendenzahlungen
        column = new Column("Periodiziät", SWT.None, 100);
        column.setGroupLabel("Dividenden");
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

    private void createDividendProjectionColumns()
    {
        // durchschnittliche Stückzahl in den letzten 12 Monaten
        Column column = new Column("øStck¹²", SWT.None, 80);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object r)
            {
                long shares = ((SecurityPerformanceRecord) r).getDiv12MeanShares();
                return shares != 0 ? shares : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "div12MeanShares")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Summe der Zahlungen in den letzten 12 Monaten
        column = new Column("∑Div¹²", SWT.RIGHT, 75);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.formatNonZero(((SecurityPerformanceRecord) r).getDiv12Amount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "div12Amount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Dividende pro Stück in den letzten 12 Monaten
        column = new Column("øDiv¹²", SWT.RIGHT, 50);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.formatNonZero(((SecurityPerformanceRecord) r).getDiv12PerShare());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "div12PerShare")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Einstand pro Stück
        column = new Column("Einstand¹²", SWT.RIGHT, 75);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.formatNonZero(((SecurityPerformanceRecord) r).getCost12Amount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "cost12Amount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Einstand pro Stück
        column = new Column("pers.Div%", SWT.RIGHT, 50);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent2.formatNonZero(((SecurityPerformanceRecord) r).getPersonalDiv());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "personalDiv")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // erwartete Dividende im nächsten Jahr
        column = new Column("Div¹²e", SWT.RIGHT, 75);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.formatNonZero(((SecurityPerformanceRecord) r).getExpectedDiv12Amount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "expectedDiv12Amount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // mittlere Dividendensteigerung der letzten Jahre
        column = new Column("DSR%", SWT.RIGHT, 50);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent0.formatNonZero(((SecurityPerformanceRecord) r).getDivIncreasingRate(), 0.01);
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "divIncreasingRate")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Zuverlässigkeit der Dividendensteigerung der letzten Jahre
        column = new Column("Zuverl.%", SWT.RIGHT, 50);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Percent0.formatNonZero(((SecurityPerformanceRecord) r).getDivIncreasingReliability(),
                                0.01);
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "divIncreasingReliability")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // Anzahl der Jahre mit Dividendensteigerung
        column = new Column("Jahre", SWT.RIGHT, 50);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Integer.formatNonZero(((SecurityPerformanceRecord) r).getDivIncreasingYears());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "divIncreasingYears")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // erwartete Dividende in 5 Jahren
        column = new Column("Div¹² 5J.", SWT.RIGHT, 75);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.formatNonZero(((SecurityPerformanceRecord) r).getDiv60Amount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "div60Amount")); //$NON-NLS-1$
        recordColumns.addColumn(column);

        // erwartete Dividende in 10 Jahren
        column = new Column("Div¹² 10J.", SWT.RIGHT, 75);
        column.setGroupLabel("Dividenden Prognose");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object r)
            {
                return Values.Amount.formatNonZero(((SecurityPerformanceRecord) r).getDiv120Amount());
            }
        });
        column.setSorter(ColumnViewerSorter.create(SecurityPerformanceRecord.class, "div120Amount")); //$NON-NLS-1$
        recordColumns.addColumn(column);
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(DividendsPerformanceView.class.getSimpleName()
                        + "@bottom1", transactions, layout); //$NON-NLS-1$

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
        column = new Column("Dividende", SWT.RIGHT, 80);
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
        column = new Column("Dividende/Anteil", SWT.RIGHT, 80);
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

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider());
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
