package name.abuchen.portfolio.ui.views.securitychart;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;

import com.google.common.primitives.Doubles;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineSeriesModel;
import name.abuchen.portfolio.ui.views.SecuritiesChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

public class SharesHeldChartSeries
{
    private record Data(List<LocalDate> dates, List<Double> values)
    {
    }

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

        var data = collectData(chartInterval, transactions);

        // if no shares where ever held, then do not create the axis
        if (data.isEmpty())
            return;

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

            // As we are dynamically adding the series, we have to check whether
            // the updates on the chart are suspended. Otherwise, the layout
            // information is not available for the axis and then #adjustRange
            // will fail

            if (timelineChart.isUpdateSuspended())
            {
                // resuming updates will trigger #updateLayout
                timelineChart.suspendUpdate(false);
                timelineChart.suspendUpdate(true);
            }
        }

        // create chart series

        for (int index = 0; index < data.size(); index++)
        {
            Data d = data.get(index);

            var label = index == 0 ? Messages.ColumnSharesOwned
                            : MessageFormat.format(Messages.ColumnSharesOwnedHoldingPeriod, index + 1);

            @SuppressWarnings("unchecked")
            var series = (ILineSeries<Integer>) timelineChart.getSeriesSet().createSeries(SeriesType.LINE, label);

            series.setDataModel(new TimelineSeriesModel(d.dates.toArray(new LocalDate[0]), Doubles.toArray(d.values)));

            series.setSymbolType(PlotSymbolType.NONE);
            series.setYAxisId(axis.getId());
            series.enableStep(true);

            series.setLineWidth(2);
            series.setLineStyle(LineStyle.SOLID);
            series.enableArea(false);
            series.setAntialias(chart.getAntialias());
            series.setLineColor(chart.getSharesHeldColor());
            series.setVisibleInLegend(index == 0);
        }
    }

    private List<Data> collectData(ChartInterval chartInterval, List<PortfolioTransaction> transactions)
    {
        var result = new ArrayList<Data>();

        int index = 0;

        // collect changes to shares *before* chart interval up until and
        // including to the end of the first day = first data point
        var sharesAtStart = 0L;
        while (index < transactions.size()
                        && !transactions.get(index).getDateTime().toLocalDate().isAfter(chartInterval.getStart()))
        {
            var tx = transactions.get(index);
            sharesAtStart = sharesAtStart + (tx.getType().isPurchase() ? tx.getShares() : -tx.getShares());
            index++;
        }

        Data current = null;

        if (sharesAtStart != 0)
        {
            current = new Data(new ArrayList<>(), new ArrayList<>());
            result.add(current);

            current.dates.add(chartInterval.getStart());
            current.values.add(sharesAtStart / Values.Share.divider());
        }

        // iterate over remaining transactions (if inside the chart interval,
        // i.e. relevant to render on the chart)

        long currentShares = sharesAtStart;

        while (index < transactions.size() && chartInterval.contains(transactions.get(index).getDateTime()))
        {
            var tx = transactions.get(index);

            var currentDate = tx.getDateTime().toLocalDate();
            currentShares += tx.getType().isPurchase() ? tx.getShares() : -tx.getShares();

            // if the next date is identical, include the next transaction in
            // the same data point. (there can be multiple purchase and sale
            // transactions on the same day, but we can have only one entry
            // in the data series)
            if (index + 1 < transactions.size()
                            && currentDate.equals(transactions.get(index + 1).getDateTime().toLocalDate()))
            {
                index++;
                continue;
            }

            if (currentShares == 0)
            {
                if (current != null)
                {
                    // end of a holding period
                    current.dates.add(currentDate);
                    current.values.add(current.values.get(current.values.size() - 1));
                    current = null;
                }
            }
            else
            {
                if (current == null)
                {
                    // create a new holding period
                    current = new Data(new ArrayList<>(), new ArrayList<>());
                    result.add(current);
                }

                current.dates.add(currentDate);
                current.values.add(currentShares / Values.Share.divider());
            }

            index++;
        }

        // if necessary, add a data point for the last day of the interval

        if (current != null && !current.dates.get(current.dates.size() - 1).equals(chartInterval.getEnd()))
        {
            current.dates.add(chartInterval.getEnd());
            current.values.add(currentShares / Values.Share.divider());
        }

        return result;
    }

}
