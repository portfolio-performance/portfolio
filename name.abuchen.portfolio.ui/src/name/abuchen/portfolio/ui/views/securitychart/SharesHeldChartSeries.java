package name.abuchen.portfolio.ui.views.securitychart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;

import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

public class SharesHeldChartSeries
{

    public void configure(SecuritiesChart chart, TimelineChart timelineChart, ChartInterval chartInterval)
    {
        Client client = chart.getClient();
        Security security = chart.getSecurity();

        // securities w/o currency (e.g. index) cannot be bought and hence have
        // no purchase price
        if (security == null || security.getCurrencyCode() == null)
            return;

        // create a list of sorted transactions that change the number of pieces
        List<PortfolioTransaction> transactions = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .filter(t -> t.getSecurity().equals(security))
                        .filter(t -> !(t.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                        || t.getType() == PortfolioTransaction.Type.TRANSFER_OUT))
                        .sorted(Transaction.BY_DATE) //
                        .toList();

        var dates = new ArrayList<LocalDate>();
        var values = new ArrayList<Double>();

        collectData(chartInterval, transactions, dates, values);

        // create a new axis (if necessary)
        // we use the title to identify the axis

        IAxis axis = null;
        var axisSet = timelineChart.getAxisSet().getYAxes();

        for (int ii = 0; ii < axisSet.length; ii++)
        {
            if (Messages.ColumnSharesOwned.equals(axisSet[ii].getTitle().getText()))
            {
                axis = axisSet[ii];
                break;
            }
        }

        if (axis == null)
        {
            int id = timelineChart.getAxisSet().createYAxis();

            axis = timelineChart.getAxisSet().getYAxis(id);
            axis.getTitle().setText(Messages.ColumnSharesOwned);
            axis.getTitle().setVisible(false);
            axis.getTick().setVisible(false);
            axis.getGrid().setStyle(LineStyle.NONE);
            axis.setPosition(Position.Primary);
        }

        // create chart series

        ILineSeries series = (ILineSeries) timelineChart.getSeriesSet().createSeries(SeriesType.LINE,
                        Messages.ColumnSharesOwned);

        series.setSymbolType(PlotSymbolType.NONE);
        series.setYAxisId(axis.getId());
        series.enableStep(true);

        series.setLineWidth(2);
        series.setLineStyle(LineStyle.SOLID);
        series.enableArea(false);
        series.setAntialias(chart.getAntialias());
        series.setXDateSeries(TimelineChart.toJavaUtilDate(dates.toArray(new LocalDate[0])));
        series.setYSeries(Doubles.toArray(values));
        series.setLineColor(chart.getSharesHeldColor());
        series.setVisibleInLegend(true);
    }

    private void collectData(ChartInterval chartInterval, List<PortfolioTransaction> transactions,
                    ArrayList<LocalDate> dates, ArrayList<Double> values)
    {
        int index = 0;

        // collect changes to shares *before* chart interval
        var sharesAtStart = 0L;
        while (index < transactions.size()
                        && !transactions.get(index).getDateTime().toLocalDate().isAfter(chartInterval.getStart()))
        {
            var tx = transactions.get(index);
            sharesAtStart = sharesAtStart + (tx.getType().isPurchase() ? tx.getShares() : -tx.getShares());
            index++;
        }

        dates.add(chartInterval.getStart());
        values.add(sharesAtStart / Values.Share.divider());

        // iterate over remaining transactions

        LocalDate currentDate = null;
        long currentShares = sharesAtStart;

        while (index < transactions.size() && chartInterval.contains(transactions.get(index).getDateTime()))
        {
            var tx = transactions.get(index);

            var changeDate = tx.getDateTime().toLocalDate();
            var changeShares = tx.getType().isPurchase() ? tx.getShares() : -tx.getShares();

            if (currentDate != null && !changeDate.equals(currentDate))
            {
                dates.add(currentDate);
                values.add(currentShares / Values.Share.divider());
            }

            currentDate = changeDate;
            currentShares += changeShares;

            index++;
        }

        if (currentDate != null)
        {
            dates.add(currentDate);
            values.add(currentShares / Values.Share.divider());
        }

        // if necessary, add a data point for the last day of the interval

        if (!dates.get(dates.size() - 1).equals(chartInterval.getEnd()))
        {
            dates.add(chartInterval.getEnd());
            values.add(currentShares / Values.Share.divider());
        }
    }

}
