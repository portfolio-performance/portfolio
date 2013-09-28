package name.abuchen.portfolio.ui.views;

import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DivRecord;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DividendInitialTransaction;
import name.abuchen.portfolio.snapshot.DividendPerformanceSnapshot.DividendTransaction;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;

public class DividendsPerformanceView extends AbstractHistoricView
{
    public static Color getColor(int color)
    {
        return Display.getCurrent().getSystemColor(color);
    }

    public static Color getMixedColor(int color1, int color2, double f2)
    {
        Color col1 = getColor(color1);
        Color col2 = getColor(color2);

        double f1 = 1 - f2;

        double dr = col1.getRed() * f1 + col2.getRed() * f2;
        double dg = col1.getGreen() * f1 + col2.getGreen() * f2;
        double db = col1.getBlue() * f1 + col2.getBlue() * f2;

        return new Color(Display.getCurrent(), (int) dr, (int) dg, (int) db);
    }

    public static Color getMixedColor(int color1, int color2, double f2, int color3, double f3)
    {
        Color col1 = getColor(color1);
        Color col2 = getColor(color2);
        Color col3 = getColor(color3);

        double f1 = 1 - f2 - f3;

        double dr = col1.getRed() * f1 + col2.getRed() * f2 + col3.getRed() * f3;
        double dg = col1.getGreen() * f1 + col2.getGreen() * f2 + col3.getGreen() * f3;
        double db = col1.getBlue() * f1 + col2.getBlue() * f2 + col3.getBlue() * f3;

        return new Color(Display.getCurrent(), (int) dr, (int) dg, (int) db);
    }

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

    private TreeViewer tree;

    @Override
    protected String getTitle()
    {
        return Messages.LabelDividendPerformance;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        tree = createTreeViewer(parent);

        reportingPeriodUpdated();
        ViewerHelper.pack(tree);

        return tree.getControl();
    }

    @Override
    public void notifyModelUpdated()
    {
        reportingPeriodUpdated();
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        ReportingPeriod period = getReportingPeriod();
        tree.setInput(DividendPerformanceSnapshot.create(getClient(), period).getRecords());
        tree.refresh();
    }

    private TreeViewer createTreeViewer(Composite parent)
    {
        TreeViewer tree = new TreeViewer(parent, SWT.FULL_SELECTION);
        TreeViewerColumn tvcol;
        TreeColumn column;

        // enforce correct column sequence
        int cc = 0;

        // Name
        assert cc == colA_Name;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.None);
        column = tvcol.getColumn();
        column.setText(Messages.ColumnName);
        column.setWidth(150);
        ColumnViewerSorter.create(DivRecord.class, "SecurityName").attachTo(tree, tvcol, true); //$NON-NLS-1$
        // ColumnViewerSorter: es muss eine getXxx-Methode in der
        // DivRecord-Klasse existieren, sonst crasht es !!!

        assert cc == colB_InternalRateOfReturn;
        cc++;
        // IZF über alles
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText(Messages.ColumnIRR);
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "irr").attachTo(tree, tvcol); //$NON-NLS-1$

        // Anzahl aktuell
        assert cc == colC_Shares;
        cc++;
        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText("Stück");
        column.setWidth(50);

        // Einstandspreis für Dividendenberechnung
        assert cc == colD_Cost;
        cc++;
        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText("Einstand");
        column.setWidth(75);

        // Gesamtsumme der erhaltenen Dividenden
        assert cc == colE_DividendSum;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("∑Div");
        column.setWidth(75);
        ColumnViewerSorter.create(DivRecord.class, "divAmount").attachTo(tree, tvcol); //$NON-NLS-1$

        // Rendite insgesamt
        assert cc == colF_TotalRateOfDividendReturn;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "totalRateOfReturnDiv").attachTo(tree, tvcol); //$NON-NLS-1$

        // jährliche Dividendenrendite bezogen auf den Einstandspreis (izf-div)
        assert cc == colG_InternalRateOfDivReturn;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("øDiv%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "irrDiv").attachTo(tree, tvcol); //$NON-NLS-1$

        // Anzahl der Dividendenereignisse | zugehörige Id
        assert cc == colH_DivEventCount;
        cc++;
        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText("#Div");
        column.setWidth(25);

        // Datum der letzten Dividendenzahlung | Vorgangsdatum
        assert cc == colI_LastReturn;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.None);
        column = tvcol.getColumn();
        column.setText("zuletzt am");
        column.setWidth(75);
        ColumnViewerSorter.create(DivRecord.class, "dateTo").attachTo(tree, tvcol); //$NON-NLS-1$

        // Periodizität der Dividendenzahlungen | Vorgangstyp
        assert cc == colJ_Periodicity;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.None);
        column = tvcol.getColumn();
        column.setText("Periodiziät");
        column.setWidth(100);
        ColumnViewerSorter.create(DivRecord.class, "periodicitySort").attachTo(tree, tvcol); //$NON-NLS-1$

        // durchschnittliche Stückzahl in den letzten 12 Monaten
        assert cc == colK_Shares12;
        cc++;
        column = new TreeColumn(tree.getTree(), SWT.None);
        column.setText("øStck¹²");
        column.setWidth(50);

        // Summe der Zahlungen in den letzten 12 Monaten
        assert cc == colL_Dividends12;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("∑Div¹²"); //$NON-NLS-1$
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "div12Amount").attachTo(tree, tvcol); //$NON-NLS-1$

        // Dividende pro Stück in den letzten 12 Monaten
        assert cc == colM_Dividends12PerShare;
        cc++;
        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText("øDiv¹²");
        column.setWidth(50);

        // Einstand pro Stück
        assert cc == colN_Cost12Amount;
        cc++;
        column = new TreeColumn(tree.getTree(), SWT.RIGHT);
        column.setText("Einstand¹²");
        column.setWidth(50);

        // persönliche Dividende im letzen Jahr
        assert cc == colO_PersonalRateOfDividendReturn;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("pers.Div%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "personalDiv").attachTo(tree, tvcol); //$NON-NLS-1$

        // erwartete Dividende im nächsten Jahr
        assert cc == colP_Dividends12Expected;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div¹²e");
        column.setWidth(75);
        ColumnViewerSorter.create(DivRecord.class, "expectedDiv12Amount").attachTo(tree, tvcol); //$NON-NLS-1$

        // mittlere Dividendensteigerung der letzten Jahre
        assert cc == colQ_DividendIncreasingRate;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("DSR%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "divIncreasingRate").attachTo(tree, tvcol); //$NON-NLS-1$

        // Zuverlässigkeit der Dividendensteigerung der letzten Jahre
        assert cc == colR_DividendIncreasingReliabilty;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Zuverl.%");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "divIncreasingReliability").attachTo(tree, tvcol); //$NON-NLS-1$

        // Anzahl der Jahre mit Dividendensteigerung
        assert cc == colS_DividendIncreasingYears;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Jahre");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "divIncreasingYears").attachTo(tree, tvcol); //$NON-NLS-1$

        // erwartete Dividende in 5 Jahren
        assert cc == colT_DividendExpectedLongTerm5;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div¹² 5J.");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "div60Amount").attachTo(tree, tvcol); //$NON-NLS-1$

        // erwartete Dividende in 10 Jahren
        assert cc == colU_DividendExpectedLongTerm10;
        cc++;
        tvcol = new TreeViewerColumn(tree, SWT.RIGHT);
        column = tvcol.getColumn();
        column.setText("Div¹² 10J.");
        column.setWidth(50);
        ColumnViewerSorter.create(DivRecord.class, "div120Amount").attachTo(tree, tvcol); //$NON-NLS-1$

        tree.getTree().setHeaderVisible(true);
        tree.getTree().setLinesVisible(true);

        tree.setLabelProvider(new DividendPerformanceLabelProvider());
        tree.setContentProvider(new DividendPerformanceContentProvider());

        tree.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(tree));

        hookContextMenu(tree.getTree(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        return tree;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Object selection = ((IStructuredSelection) tree.getSelection()).getFirstElement();
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

            if (element instanceof DivRecord)
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
            else if (element instanceof Transaction)
            {
                Transaction t = (Transaction) element;

                switch (columnIndex)
                {
                    case colH_DivEventId: // Datum
                        if (t instanceof DividendTransaction)
                            return Values.Id.formatNonZero(((DividendTransaction) t).getDivEventId());
                        else
                            return null;
                    case colI_TransactionDate: // Datum
                        return Values.Date.format(t.getDate());
                    case colJ_TransactionType: // Vorgang
                        if (t instanceof PortfolioTransaction)
                            return ((PortfolioTransaction) t).getType().toString();
                        else if (t instanceof AccountTransaction)
                            return ((AccountTransaction) t).getType().toString();
                        else if (t instanceof DividendTransaction)
                            return Messages.LabelDividends;
                        else
                            return Messages.LabelQuote;
                    case colK_Shares:
                        if (t instanceof PortfolioTransaction)
                            return Values.Share.format(((PortfolioTransaction) t).getShares());
                        else if (t instanceof DividendInitialTransaction)
                            return Values.Share.format(((DividendInitialTransaction) t).getPosition().getShares());
                        else if (t instanceof DividendTransaction)
                            return Values.Share.formatNonZero(((DividendTransaction) t).getShares());
                        else
                            return null;
                    case colL_DividendAmount:
                        if (t instanceof DividendTransaction)
                            return Values.Amount.format(((DividendTransaction) t).getAmount());
                        else
                            return null;
                    case colM_DividensPerShare:
                        if (t instanceof DividendTransaction)
                            return Values.Quote.formatNonZero(((DividendTransaction) t).getDividendPerShare());
                        else
                            return null;
                    case colN_Amount:
                        if (t instanceof PortfolioTransaction)
                            return Values.Amount.format(Math.abs(t.getAmount()));
                        else if (t instanceof DividendInitialTransaction)
                            return Values.Amount.format(((DividendInitialTransaction) t).getAmount());
                        else
                            return null;
                    case colO_Account:
                        if (t instanceof PortfolioTransaction)
                            return ((PortfolioTransaction) t).getCrossEntry().getCrossEntity(t).toString();
                        else if (t instanceof DividendTransaction)
                            return ((DividendTransaction) t).getAccount().getName();
                        else
                            return null;
                }
            }
            return null;
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
                        return record.getIrr() >= 0 ? getColor(SWT.COLOR_DARK_GREEN) : getColor(SWT.COLOR_DARK_RED);
                    case colE_DividendSum:
                        return record.getDivAmount() >= 0 ? getColor(SWT.COLOR_DARK_GREEN)
                                        : getColor(SWT.COLOR_DARK_RED);
                }
            }
            else if (element instanceof PortfolioTransaction)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                switch (columnIndex)
                {
                    case colJ_Periodicity:
                        if (t.getType() == Type.BUY)
                            return getColor(SWT.COLOR_BLUE);
                        else if (t.getType() == Type.SELL)
                            return getColor(SWT.COLOR_RED);
                }
            }

            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex)
        {
            if ((element instanceof DividendTransaction) && (columnIndex >= colH_DivEventCount))
            {
                DividendTransaction dt = (DividendTransaction) element;
                if (dt.getIsDiv12()) // dt.getDate().before(this.))
                {

                    switch ((dt.getDivEventId() - 1) % 2)
                    {
                        case 0:
                            return getMixedColor(SWT.COLOR_GREEN, SWT.COLOR_WHITE, 0.80);
                        case 1:
                            return getMixedColor(SWT.COLOR_YELLOW, SWT.COLOR_WHITE, 0.60);
                    }

                }
                else
                {
                    switch ((dt.getDivEventId() - 1) % 2)
                    {
                        case 0:
                            return getMixedColor(SWT.COLOR_BLACK, SWT.COLOR_WHITE, 0.85);
                        case 1:
                            return getMixedColor(SWT.COLOR_BLACK, SWT.COLOR_WHITE, 0.90);
                    }
                }

            }
            return null;
        }
    }
}
