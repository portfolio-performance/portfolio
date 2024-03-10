package name.abuchen.portfolio.ui.views.payments;

import java.util.function.Consumer;

import org.eclipse.swtchart.Chart;

import name.abuchen.portfolio.ui.util.TabularDataSource;

public interface PaymentsChartBuilder
{
    String getLabel();

    int getTabIndex();

    void configure(Chart chart, Consumer<TabularDataSource> selectionListener);

    void createSeries(Chart chart, PaymentsViewModel model);
}
