package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class CorrelationListView extends AbstractListView implements ModificationListener, ReportingPeriodListener
{
    private static final String FILTER_INACTIVE_SECURITIES = "filter-retired-securities"; //$NON-NLS-1$

    private TableViewer correlationAccount;
    private TableViewer correlationMatrix;
    private ShowHideColumnHelper accountColumns;
    private CurrencyConverter converter;
    protected Font boldFont;
    private Interval period1st;
    private Interval period2nd;
    private int noOfSecurities = 0;
    private List<Line> lines;
    private TableColumnLayout tableLayout = new TableColumnLayout();
    private boolean isFiltered = false;
    private ReportingPeriodDropDown periodDropDown1;
    private ReportingPeriodDropDown periodDropDown2;
    @SuppressWarnings("all")
    List correlationAccounts = new ArrayList<>();
    List<Security> correlationSecurities = new ArrayList<>();

    public static class Line
    {
        private InvestmentVehicle vehicle;
        private long[] values;

        public Line(InvestmentVehicle vehicle, int length)
        {
            this.vehicle = vehicle;
            this.values = new long[length];
        }

        public InvestmentVehicle getVehicle()
        {
            return vehicle;
        }

        public long getValue(int index)
        {
            return values[index];
        }

        public int getNoOfSecurities()
        {
            return values.length;
        }
    }

    public List<Line> getLines()
    {
        return lines;
    }

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelCorrelationMatrix;
    }

    @Override
    protected int getSashStyle()
    {
        return SWT.VERTICAL | SWT.BEGINNING;
    }

    @Override
    public void notifyModelUpdated()
    {
        correlationAccount.setInput(getAccounts());
        correlationAccount.setSelection(correlationAccount.getSelection());
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        markDirty();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        periodDropDown1 = new ReportingPeriodDropDown(getPart(), this);
        toolBar.add(periodDropDown1);
        periodDropDown2 = new ReportingPeriodDropDown(getPart(), this);
        toolBar.add(periodDropDown2);
        super.addButtons(toolBar);
        addFilterButton(toolBar);
        addExportButton(toolBar);

    }

    private void addFilterButton(ToolBarManager manager)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = !isFiltered;
                getPart().getPreferenceStore().setValue(FILTER_INACTIVE_SECURITIES, isFiltered);
                setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
                getSecurities(((IStructuredSelection) correlationAccount.getSelection()).getFirstElement());
                calculateCorrelationMatrix();
                updateBottomTableColumns(correlationMatrix, tableLayout);

            }
        };
        filter.setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
        filter.setToolTipText(Messages.AccountFilterRetiredAccounts);
        manager.add(filter);
    }

    private void addExportButton(ToolBarManager manager)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(correlationMatrix).export(getTitle() + ".csv"); //$NON-NLS-1$
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
        container.setLayout(tableLayout);

        correlationAccount = new TableViewer(container, SWT.FULL_SELECTION);

        accountColumns = new ShowHideColumnHelper(CorrelationListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        getPreferenceStore(), correlationAccount, tableLayout);

        createTopTableColumns(accountColumns);

        accountColumns.createColumns();
        correlationAccount.getTable().setHeaderVisible(true);
        correlationAccount.getTable().setLinesVisible(true);
        correlationAccount.setContentProvider(ArrayContentProvider.getInstance());
        correlationAccount.setInput(getAccounts());
        correlationAccount.addSelectionChangedListener(event -> {
            getSecurities(((IStructuredSelection) event.getSelection()).getFirstElement());
            calculateCorrelationMatrix();
            updateBottomTableColumns(correlationMatrix, tableLayout);

            correlationMatrix.refresh();
        });
    }

    private void createTopTableColumns(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnName, SWT.NONE, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                if (e instanceof String)
                    return e.toString();
                else if (e instanceof Portfolio)
                {
                    Portfolio pt = (Portfolio) e;
                    return pt.getName();
                }
                else if (e instanceof Watchlist)
                {
                    Watchlist wl = (Watchlist) e;
                    return wl.getName();
                }
                else
                    return null;
            }

            @Override
            public Image getImage(Object e)
            {
                if (e instanceof String)
                    return Images.SECURITY.image();
                else if (e instanceof Portfolio)
                    return Images.PORTFOLIO.image();
                else if (e instanceof Watchlist)
                    return Images.WATCHLIST.image();
                else
                    return null;
            }
        });
        support.addColumn(column);
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(tableLayout);

        correlationMatrix = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(correlationMatrix, ToolTip.NO_RECREATE);

        createBottomTableColumns(correlationMatrix, tableLayout);

        correlationMatrix.getTable().setHeaderVisible(true);
        correlationMatrix.getTable().setLinesVisible(true);

        correlationMatrix.setContentProvider(ArrayContentProvider.getInstance());

        correlationMatrix.setInput(null);

        for (TableColumn c : correlationMatrix.getTable().getColumns())
            c.pack();

    }

    protected void createBottomTableColumns(TableViewer records, TableColumnLayout layout)
    {
        createBottomTableVehicleColumn(records, layout);
        for (int index = 0; index < noOfSecurities; index++)
            createBottomTableCorrelationColumn(records, layout, correlationSecurities.get(index), index);
    }

    protected void createBottomTableVehicleColumn(TableViewer records, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return ((Line) element).getVehicle().isRetired() ? Images.SECURITY_RETIRED.image()
                                : Images.SECURITY.image();
            }

            @Override
            public String getText(Object element)
            {
                return ((Line) element).getVehicle().getName();
            }
        });

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

    private void createBottomTableCorrelationColumn(TableViewer records, TableColumnLayout layout, Security security,
                    int index)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(security.getName());
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Line line = (Line) element;
                if (line.getVehicle().equals(correlationSecurities.get(index)))
                    return null;
                else
                    return line.getVehicle() != null ? Values.Amount.formatNonZero(line.getValue(index))
                                    : Values.Amount.format(line.getValue(index));
            }

            @Override
            public String getToolTipText(Object element)
            {
                InvestmentVehicle vehicle1 = ((Line) element).getVehicle();
                InvestmentVehicle vehicle2 = ((Line) element).getVehicle();
                return TextUtil.tooltip(vehicle1 != null && vehicle2 != null
                                && !vehicle1.equals(correlationSecurities.get(index))
                                                ? "\u00AB <p>" + vehicle1.getName() + " \u00BB vs. \u00AB " //$NON-NLS-1$ //$NON-NLS-2$
                                                                + vehicle2.getName() + " \u00BB" //$NON-NLS-1$
                                                : null);
            }

            @Override
            public Color getBackground(Object element)
            {
                return ((Line) element).getVehicle().equals(correlationSecurities.get(index)) ? Colors.BLACK : null;
            }
        });

        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

    private void updateBottomTableColumns(TableViewer records, TableColumnLayout layout)
    {
        try
        {
            // first add, then remove columns
            // (otherwise rendering of first column is broken)
            records.getTable().setRedraw(false);

            int count = records.getTable().getColumnCount();

            createBottomTableColumns(records, layout);

            for (int ii = 0; ii < count; ii++)
                records.getTable().getColumn(0).dispose();

            records.setInput(this.getLines());

            for (TableColumn c : records.getTable().getColumns())
                c.pack();
        }
        finally
        {
            records.refresh();
            records.getTable().setRedraw(true);
        }
    }

    @SuppressWarnings("all")
    private List<?> getAccounts()
    {

        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

        correlationAccounts = new ArrayList<>();
        correlationAccounts.add(Messages.LabelAllSecurities);

        // collect watchlists with securities
        for (Watchlist watchlist : getClient().getWatchlists())
            if (!watchlist.getSecurities().isEmpty())
                correlationAccounts.add((Watchlist) watchlist);

        // collect portfolio with security holdings
        List<Portfolio> portfolios = getClient().getPortfolios();
        for (Portfolio portfolio : portfolios)
        {
            PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, LocalDate.now());
            if (!snapshot.getPositions().isEmpty())
                correlationAccounts.add(portfolio);
        }

        return correlationAccounts;
    }

    private List<Security> getSecurities(Object object)
    {
        this.correlationSecurities = new ArrayList<>();
        if (object instanceof String)
        {
            for (Security security : getClient().getSecurities())
                if (!isFiltered || isFiltered && !security.isRetired())
                    this.correlationSecurities.add(security);
        }
        else if (object instanceof Watchlist)
        {
            for (Security security : ((Watchlist) object).getSecurities())
                if (!isFiltered || isFiltered && !security.isRetired())
                    this.correlationSecurities.add(security);
        }
        else if (object instanceof Portfolio)
        {
            PortfolioSnapshot snapshot = PortfolioSnapshot.create(((Portfolio) object), converter, LocalDate.now());
            for (SecurityPosition security : snapshot.getPositions())
                this.correlationSecurities.add(security.getSecurity());
        }
        Collections.sort(this.correlationSecurities, new MySecuritySort());
        return correlationSecurities;
    }

    class MySecuritySort implements Comparator<Security>
    {

        @Override
        public int compare(Security e1, Security e2)
        {
            if (e1 == null)
                return e2 == null ? 0 : -1;
            return e1.getName().compareToIgnoreCase(e2.getName());
        }
    }

    private void calculateCorrelationMatrix()
    {
        period1st = periodDropDown1.getSelectedPeriod().toInterval(LocalDate.now());
        period2nd = periodDropDown2.getSelectedPeriod().toInterval(LocalDate.now());
        boolean onlyOnePeriod = period1st.equals(period2nd);

        Map<InvestmentVehicle, Line> vehicle2line = new LinkedHashMap<>();
        noOfSecurities = this.correlationSecurities.size();
        for (Security security1st : this.correlationSecurities)
        {
            Interval period = period1st;
            Map<LocalDate, Long> prices1st = getPricesIncludingLatestByInterval(security1st, period);
            Line line = vehicle2line.computeIfAbsent(security1st, s -> new Line(s, noOfSecurities));
            int counter = 0;
            for (Security security2nd : this.correlationSecurities)
            {
                if (security1st.equals(security2nd))
                {
                    if (onlyOnePeriod)
                        break;
                    period = period2nd;
                    prices1st = getPricesIncludingLatestByInterval(security1st, period);
                    counter += 1;
                    continue;
                }
                Map<LocalDate, Long> prices2nd = getPricesIncludingLatestByInterval(security2nd, period);
                if (!prices1st.isEmpty()  && !prices2nd.isEmpty() && (Math.min(prices1st.size(), prices2nd.size()) * 100
                                / Math.max(prices1st.size(), prices2nd.size()) > 80))
                    line.values[counter] = calculateCorrelation(prices1st, prices2nd);
                counter += 1;
            }
        }
        this.lines = new ArrayList<>(vehicle2line.values());
    }

    private long calculateCorrelation(Map<LocalDate, Long> securityPrice1st, Map<LocalDate, Long> securityPrice2nd)
    {
        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;
        int n = 0;

        for (LocalDate key : securityPrice1st.keySet())
        {
            if (securityPrice2nd.get(key) != null)
            {
                n++;
                double x = securityPrice1st.get(key) / Values.Quote.divider();
                double y = securityPrice2nd.get(key) / Values.Quote.divider();

                sx += x;
                sy += y;
                sxx += x * x;
                syy += y * y;
                sxy += x * y;
            }
        }
        // covariation
        double cov = sxy / n - sx * sy / n / n;
        // standard error of x
        double sigmax = Math.sqrt(sxx / n - sx * sx / n / n);
        // standard error of y
        double sigmay = Math.sqrt(syy / n - sy * sy / n / n);

        // correlation is just a normalized covariation
        return (long) ((cov / sigmax / sigmay) * Values.Quote.divider()) / 100;
    }

    private Map<LocalDate, Long> getPricesIncludingLatestByInterval(Security security, Interval interval)
    {
        return security.getPricesIncludingLatest().stream() //
                        .filter(t -> !t.getDate().isBefore(interval.getStart())
                                        && !t.getDate().isAfter(interval.getEnd()))
                        .collect(Collectors.toMap(SecurityPrice::getDate, SecurityPrice::getValue));
    }

    @Override
    public void reportingPeriodUpdated()
    {
        calculateCorrelationMatrix();
        updateBottomTableColumns(correlationMatrix, tableLayout);
    }
}