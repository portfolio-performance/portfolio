package name.abuchen.portfolio.ui.views.payments;

import org.swtchart.Chart;

public interface PaymentsChartBuilder
{
    String getLabel();

    int getTabIndex();

    void configure(Chart chart);

    void createSeries(Chart chart, PaymentsViewModel model);
}
