package name.abuchen.portfolio.ui.views.payments;

import org.swtchart.Chart;

import name.abuchen.portfolio.ui.Messages;

public class PaymentsPerYearChartTab extends AbstractChartTab
{
    private PaymentsPerYearChartBuilder chartBuilder = new PaymentsPerYearChartBuilder();

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsPerYear;
    }

    @Override
    protected void attachTooltipTo(Chart chart)
    {
        chartBuilder.configure(chart);
    }

    @Override
    protected void createSeries()
    {
        chartBuilder.createSeries(getChart(), model);
    }
}
