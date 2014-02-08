package name.abuchen.portfolio.ui.views;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DivRecord;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DividendFinalTransaction;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DividendInitialTransaction;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DividendTransaction;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
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
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

public class DividendsPerformanceView extends AbstractListView implements ReportingPeriodListener
{
    static final int colA_Name = 0;
    static final int colB_InternalRateOfReturn = 1; // internal rate of return
    static final int colC_Shares = 2;
    static final int colD_Cost = 3;
    static final int colE_DividendSum = 4;
    static final int colF_TotalRateOfDividendReturn = 5;
    static final int colG_InternalRateOfDivReturn = 6;
    static final int colH_DivEventCount = 7;
    static final int colI_LastReturn = 8;
    static final int colJ_Periodicity = 9;
    static final int colK_Shares12 = 10;
    static final int colL_Dividends12 = 11;
    static final int colM_Dividends12PerShare = 12;
    static final int colN_Cost12Amount = 13;
    static final int colO_PersonalRateOfDividendReturn = 14;
    static final int colP_Dividends12Expected = 15;
    static final int colQ_DividendIncreasingRate = 16;
    static final int colR_DividendIncreasingReliabilty = 17;
    static final int colS_DividendIncreasingYears = 18;
    static final int colT_DividendExpectedLongTerm5 = 19;
    static final int colU_DividendExpectedLongTerm10 = 20;

    static final int colH_DivEventId = colH_DivEventCount;
    static final int colI_TransactionDate = colI_LastReturn;
    static final int colJ_TransactionType = colJ_Periodicity;
    static final int colK_Shares = colK_Shares12;
    static final int colL_DividendAmount = colL_Dividends12;
    static final int colM_DividensPerShare = colM_Dividends12PerShare;
    static final int colN_Amount = colN_Cost12Amount;
    static final int colO_Account = colO_PersonalRateOfDividendReturn;

    private TableViewer records;
    private TableViewer transactions;
    private ReportingPeriodDropDown dropDown;

    @Override
    protected String getTitle()
    {
        return Messages.LabelDividendPerformance;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        dropDown = new ReportingPeriodDropDown(toolBar, getClientEditor(), this);
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        records = createTreeViewer(parent);

        reportingPeriodUpdated();
        ViewerHelper.pack(records);
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
                    return Values.Amount.formatNonZero(((DividendTransaction) t).getDividendPerShare());
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
        records.setInput(DividendPerformanceSnapshot.create(getClient(), period).getRecords());
        records.refresh();
    }

    private TableViewer createTreeViewer(Composite parent)
    {
        TableViewer table = new TableViewer(parent, SWT.FULL_SELECTION);
        TableViewerColumn tvcol;
        TableColumn column;

        // enforce correct column sequence
        int cc = 0;

        // Name
        assert cc == colA_Name;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.None);
        column = tvcol.getColumn();
        column.setText(Messages.ColumnName);
        column.setWidth(150);
        ColumnViewerSorter.create(DivRecord.class, "SecurityName").attachTo(table, tvcol, true); //$NON-NLS-1$
        // ColumnViewerSorter: es muss eine getXxx-Methode in der
        // DivRecord-Klasse existieren, sonst crasht es !!!

        assert cc == colB_InternalRateOfReturn;
        cc++;
        // IZF über alles
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText(Messages.ColumnIRR);
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "irr").attachTo(table, tvcol); //$NON-NLS-1$

        // Anzahl aktuell
        assert cc == colC_Shares;
        cc++;
        column = new TableColumn(table.getTable(), SWT.RIGHT);
        column.setText("Stück");
        column.setWidth(50);

        // Einstandspreis für Dividendenberechnung
        assert cc == colD_Cost;
        cc++;
        column = new TableColumn(table.getTable(), SWT.RIGHT);
        column.setText("Einstand");
        column.setWidth(75);

        // Gesamtsumme der erhaltenen Dividenden
        assert cc == colE_DividendSum;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("∑Div");
        column.setWidth(75);
        ColumnViewerSorter.create(DivRecord.class, "divAmount").attachTo(table, tvcol); //$NON-NLS-1$

        // Rendite insgesamt
        assert cc == colF_TotalRateOfDividendReturn;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "totalRateOfReturnDiv").attachTo(table, tvcol); //$NON-NLS-1$

        // jährliche Dividendenrendite bezogen auf den Einstandspreis (izf-div)
        assert cc == colG_InternalRateOfDivReturn;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("øDiv%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "irrDiv").attachTo(table, tvcol); //$NON-NLS-1$

        // Anzahl der Dividendenereignisse | zugehörige Id
        assert cc == colH_DivEventCount;
        cc++;
        column = new TableColumn(table.getTable(), SWT.RIGHT);
        column.setText("#Div");
        column.setWidth(25);

        // Datum der letzten Dividendenzahlung | Vorgangsdatum
        assert cc == colI_LastReturn;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.None);
        column = tvcol.getColumn();
        column.setText("zuletzt am");
        column.setWidth(75);
        ColumnViewerSorter.create(DivRecord.class, "dateTo").attachTo(table, tvcol); //$NON-NLS-1$

        // Periodizität der Dividendenzahlungen | Vorgangstyp
        assert cc == colJ_Periodicity;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.None);
        column = tvcol.getColumn();
        column.setText("Periodiziät");
        column.setWidth(100);
        ColumnViewerSorter.create(DivRecord.class, "periodicitySort").attachTo(table, tvcol); //$NON-NLS-1$

        // durchschnittliche Stückzahl in den letzten 12 Monaten
        assert cc == colK_Shares12;
        cc++;
        column = new TableColumn(table.getTable(), SWT.None);
        column.setText("øStck¹²");
        column.setWidth(50);

        // Summe der Zahlungen in den letzten 12 Monaten
        assert cc == colL_Dividends12;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("∑Div¹²"); //$NON-NLS-1$
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "div12Amount").attachTo(table, tvcol); //$NON-NLS-1$

        // Dividende pro Stück in den letzten 12 Monaten
        assert cc == colM_Dividends12PerShare;
        cc++;
        column = new TableColumn(table.getTable(), SWT.RIGHT);
        column.setText("øDiv¹²");
        column.setWidth(50);

        // Einstand pro Stück
        assert cc == colN_Cost12Amount;
        cc++;
        column = new TableColumn(table.getTable(), SWT.RIGHT);
        column.setText("Einstand¹²");
        column.setWidth(50);

        // persönliche Dividende im letzen Jahr
        assert cc == colO_PersonalRateOfDividendReturn;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("pers.Div%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "personalDiv").attachTo(table, tvcol); //$NON-NLS-1$

        // erwartete Dividende im nächsten Jahr
        assert cc == colP_Dividends12Expected;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div¹²e");
        column.setWidth(75);
        ColumnViewerSorter.create(DivRecord.class, "expectedDiv12Amount").attachTo(table, tvcol); //$NON-NLS-1$

        // mittlere Dividendensteigerung der letzten Jahre
        assert cc == colQ_DividendIncreasingRate;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("DSR%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "divIncreasingRate").attachTo(table, tvcol); //$NON-NLS-1$

        // Zuverlässigkeit der Dividendensteigerung der letzten Jahre
        assert cc == colR_DividendIncreasingReliabilty;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Zuverl.%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "divIncreasingReliability").attachTo(table, tvcol); //$NON-NLS-1$

        // Anzahl der Jahre mit Dividendensteigerung
        assert cc == colS_DividendIncreasingYears;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Jahre");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "divIncreasingYears").attachTo(table, tvcol); //$NON-NLS-1$

        // erwartete Dividende in 5 Jahren
        assert cc == colT_DividendExpectedLongTerm5;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div¹² 5J.");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "div60Amount").attachTo(table, tvcol); //$NON-NLS-1$

        // erwartete Dividende in 10 Jahren
        assert cc == colU_DividendExpectedLongTerm10;
        cc++;
        tvcol = new TableViewerColumn(table, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div¹² 10J.");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "div120Amount").attachTo(table, tvcol); //$NON-NLS-1$

        table.getTable().setHeaderVisible(true);
        table.getTable().setLinesVisible(true);

        table.setLabelProvider(new DividendPerformanceLabelProvider());
        table.setContentProvider(new DividendPerformanceContentProvider());

        table.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(table));

        hookContextMenu(table.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        table.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                DivRecord record = (DivRecord) ((IStructuredSelection) event.getSelection()).getFirstElement();
                transactions.setInput(record != null ? record.getTransactions() : Collections.emptyList());
                transactions.refresh();
            }
        });

        return table;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Object selection = ((IStructuredSelection) records.getSelection()).getFirstElement();
        if (!(selection instanceof DivRecord))
            return;

        Security security = ((DivRecord) selection).getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
    }

    private static class DividendPerformanceContentProvider implements ITreeContentProvider
    {
        private DivRecord[] records;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            @SuppressWarnings("unchecked")
            List<DivRecord> r = (List<DivRecord>) newInput;
            this.records = r != null ? r.toArray(new DivRecord[0]) : new DivRecord[0];
        }

        public Object[] getElements(Object inputElement)
        {
            return this.records;
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof DivRecord)
                return ((DivRecord) parentElement).getTransactions().toArray();
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof Transaction)
            {
                Transaction t = (Transaction) element;
                for (DivRecord r : records)
                {
                    if (t.getSecurity().equals(r.getSecurity()))
                        return r;
                }

            }
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof DivRecord;
        }

        public void dispose()
        {}

    }

    private static class DividendPerformanceLabelProvider extends LabelProvider implements ITableLabelProvider,
                    ITableColorProvider
    {
        public Image getColumnImage(Object element, int columnIndex)
        {
            if ((columnIndex == colA_Name) && (element instanceof DivRecord))
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            }
            else
            {
                return null;
            }
        }

        public String getColumnText(Object element, int columnIndex)
        {
            DivRecord divRecord = (DivRecord) element;

            if (!divRecord.getHasDiv12() && (columnIndex > colJ_Periodicity))
                return null;

            switch (columnIndex)
            {
                case colA_Name: // Wertpapier
                    return divRecord.getSecurityName();
                case colB_InternalRateOfReturn: // IZF Gesamt
                    return Values.Percent2.format(divRecord.getIrr());
                case colC_Shares: // aktuelle Stücke
                    return Values.Share.format(divRecord.getStockShares());
                case colD_Cost: // Kaufpreis
                    return Values.Amount.format(divRecord.getStockAmount());
                case colE_DividendSum: // Gewinn
                    return Values.Amount.format(divRecord.getDivAmount());
                case colF_TotalRateOfDividendReturn: // Rendite insgesamt
                    return Values.Percent2.format(divRecord.getTotalRateOfReturnDiv());
                case colG_InternalRateOfDivReturn: // IZF Dividenden
                    return Values.Percent2.format(divRecord.getIrrDiv());
                case colH_DivEventCount: // Anzahl Dividendentermine
                    return Values.Id.format(divRecord.getDivEventCount());
                case colI_LastReturn: // Datum letzte Zahlung
                    return Values.Date.format(divRecord.getDateTo());
                case colJ_Periodicity: // Typ J/H/Q
                    return divRecord.getPeriodicity().toString();
                case colK_Shares12: // Rendite insgesamt
                    return Values.Share.format(divRecord.getDiv12MeanShares());
                case colL_Dividends12: // Summe der Dividenden der letzen 12
                                       // Monate
                    return Values.Amount.format(divRecord.getDiv12Amount());
                case colM_Dividends12PerShare: // Dividenden pro Stück der
                                               // letzten 12 Monate
                    return Values.Amount.format(divRecord.getDiv12PerShare());
                case colN_Cost12Amount:
                    return Values.Amount.format(divRecord.getCost12Amount());
                case colO_PersonalRateOfDividendReturn: // persönliche
                                                        // Rendite
                    return Values.Percent2.format(divRecord.getPersonalDiv());
                case colP_Dividends12Expected:
                    return Values.Amount.formatNonZero(divRecord.getExpectedDiv12Amount());
                case colQ_DividendIncreasingRate:
                    return Values.Percent0.formatNonZero(divRecord.getDivIncreasingRate(), 0.01);
                case colR_DividendIncreasingReliabilty:
                    return Values.Percent0.formatNonZero(divRecord.getDivIncreasingReliability(), 0.01);
                case colS_DividendIncreasingYears:
                    return Values.Integer.formatNonZero(divRecord.getDivIncreasingYears());
                case colT_DividendExpectedLongTerm5:
                    return Values.Amount.formatNonZero(divRecord.getDiv60Amount());
                case colU_DividendExpectedLongTerm10:
                    return Values.Amount.formatNonZero(divRecord.getDiv120Amount());
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public Color getForeground(Object element, int columnIndex)
        {
            if (element instanceof DivRecord)
            {
                DivRecord record = (DivRecord) element;

                switch (columnIndex)
                {
                    case colB_InternalRateOfReturn:
                        return Display.getCurrent().getSystemColor(
                                        record.getIrr() >= 0 ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
                    case colE_DividendSum:
                        return Display.getCurrent().getSystemColor(
                                        record.getDivAmount() >= 0 ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
                }
            }
            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex)
        {
            return null;
        }
    }
}
