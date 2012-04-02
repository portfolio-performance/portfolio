package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TimelineChart;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.IAxis;
import org.swtchart.Range;

public class StatementOfAssetsHistoryView extends AbstractHistoricView
{
    // 10 days before and after
    private static final int OFFSET = 1000 * 60 * 60 * 24 * 10;

    private TimelineChart chart;

    public StatementOfAssetsHistoryView()
    {
        super(5);
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHistory;
    }

    @Override
    protected Composite buildBody(Composite parent)
    {
        return chart = buildChart(parent);
    }

    @Override
    protected void reportingPeriodUpdated()
    {
        Composite parent = chart.getParent();
        chart.dispose();
        chart = buildChart(parent);
        parent.layout(true);
    }

    protected TimelineChart buildChart(Composite parent)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(Dates.today());
        cal.add(Calendar.YEAR, -getReportingYears());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DATE, -1);

        Date startDate = cal.getTime();
        Date endDate = Dates.today();

        int noOfDays = Dates.daysBetween(startDate, endDate) + 1;

        // collect data for line series

        Date[] dates = new Date[noOfDays];
        double[] totals = new double[noOfDays];
        double[] cash = new double[noOfDays];
        double[] equity = new double[noOfDays];
        double[] debt = new double[noOfDays];
        double[] realEstate = new double[noOfDays];
        double[] commodity = new double[noOfDays];

        int index = 0;
        while (cal.getTimeInMillis() <= endDate.getTime())
        {

            ClientSnapshot snapshot = ClientSnapshot.create(getClient(), cal.getTime());
            dates[index] = cal.getTime();

            totals[index] = snapshot.getAssets() / 100d;

            List<AssetCategory> categories = snapshot.groupByCategory();
            cash[index] = categories.get(0).getValuation() / 100d;
            debt[index] = categories.get(1).getValuation() / 100d;
            equity[index] = categories.get(2).getValuation() / 100d;
            realEstate[index] = categories.get(3).getValuation() / 100d;
            commodity[index] = categories.get(4).getValuation() / 100d;

            cal.add(Calendar.DATE, 1);
            index++;
        }

        TimelineChart chart = new TimelineChart(parent);
        chart.getTitle().setVisible(false);
        chart.getLegend().setVisible(true);
        chart.getLegend().setPosition(SWT.BOTTOM);

        chart.addDateSeries(dates, totals, Colors.TOTALS);
        chart.addDateSeries(dates, cash, Colors.CASH);
        chart.addDateSeries(dates, debt, Colors.DEBT);
        chart.addDateSeries(dates, equity, Colors.EQUITY);
        chart.addDateSeries(dates, realEstate, Colors.REAL_ESTATE);
        chart.addDateSeries(dates, commodity, Colors.COMMODITY);

        // deposits & removals

        EnumSet<AccountTransaction.Type> relevantTypes = EnumSet.of(AccountTransaction.Type.DEPOSIT,
                        AccountTransaction.Type.REMOVAL);

        List<AccountTransaction> transactions = new ArrayList<AccountTransaction>();
        for (Account a : getClient().getAccounts())
        {
            for (AccountTransaction t : a.getTransactions())
                if (relevantTypes.contains(t.getType()) && !t.getDate().before(startDate))
                    transactions.add(t);
        }
        Collections.sort(transactions);

        List<Date> transferals_dates = new ArrayList<Date>();
        List<Integer> transferals = new ArrayList<Integer>();
        int tIndex = 0;

        cal.setTime(startDate);
        while (cal.getTimeInMillis() <= endDate.getTime())
        {
            int amount = 0;
            while (tIndex < transactions.size()
                            && transactions.get(tIndex).getDate().getTime() <= cal.getTimeInMillis())
            {
                AccountTransaction t = transactions.get(tIndex);
                switch (t.getType())
                {
                    case DEPOSIT:
                        amount += t.getAmount();
                        break;
                    case REMOVAL:
                        amount -= t.getAmount();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                tIndex++;
            }
            if (amount != 0)
            {
                transferals_dates.add(cal.getTime());
                transferals.add(amount);
            }

            cal.add(Calendar.DATE, 1);
        }

        double[] d_transferals = new double[transferals.size()];
        for (int ii = 0; ii < d_transferals.length; ii++)
            d_transferals[ii] = transferals.get(ii) / 100d;
        chart.addDateBarSeries(transferals_dates.toArray(new Date[0]), d_transferals, Messages.LabelTransferals);

        // for one reason or another, the ranges are not calculated properly if
        // done automatically
        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.setRange(new Range(startDate.getTime() - OFFSET, endDate.getTime() + OFFSET));

        double[] range = new double[] { 0, 0 };
        updateRange(range, totals);
        updateRange(range, d_transferals);
        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.setRange(new Range(range[0] - 3000, range[1] + 3000));

        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        return chart;
    }

    private void updateRange(double[] range, final double[] values)
    {
        for (int ii = 0; ii < values.length; ii++)
        {
            if (values[ii] < range[0])
                range[0] = values[ii];
            if (values[ii] > range[1])
                range[1] = values[ii];
        }
    }

}
