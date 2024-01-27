package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.HoverButton;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.util.Interval;

public abstract class AbstractMonhtlyHeatmapWidget extends AbstractHeatmapWidget<Long>
{

    protected AbstractMonhtlyHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ClientFilterConfig(this));
        addConfig(new AverageConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        var composite = super.createControl(parent, resources);

        HoverButton.build(title, table).withListener(new HyperlinkAdapter()
        {
            @Override
            public void linkActivated(HyperlinkEvent e)
            {
                AbstractMonhtlyHeatmapWidget.this.linkActivated();
            }
        });

        return composite;
    }

    protected abstract void linkActivated();

    @Override
    protected HeatmapModel<Long> build()
    {
        int numDashboardColumns = getDashboardData().getDashboard().getColumns().size();

        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

        // adapt interval to include the first and last month fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfMonth() == interval.getStart().lengthOfMonth() ? interval.getStart()
                                        : interval.getStart().withDayOfMonth(1).minusDays(1),
                        interval.getEnd().with(TemporalAdjusters.lastDayOfMonth()));

        // build model
        HeatmapModel<Long> model = new HeatmapModel<>(numDashboardColumns <= 1 ? Values.Amount : Values.AmountShort);
        model.setCellToolTip(value -> value != null ? Values.Amount.format(value) : ""); //$NON-NLS-1$
        boolean showAverage = get(AverageConfig.class).getValues().contains(Average.AVERAGE);
        addMonthlyHeader(model, numDashboardColumns, true, false, showAverage);
        int startYear = calcInterval.getStart().plusDays(1).getYear();

        // prepare data
        for (Year year : calcInterval.getYears())
        {
            String label = numDashboardColumns > 2 ? String.valueOf(year.getValue() % 100) : String.valueOf(year);
            HeatmapModel.Row<Long> row = new HeatmapModel.Row<>(label);

            for (YearMonth month = YearMonth.of(year.getValue(), 1); month.getYear() == year.getValue(); month = month
                            .plusMonths(1))
            {
                if (calcInterval.intersects(Interval.of(month.atDay(1).minusDays(1), month.atEndOfMonth())))
                    row.addData(0L);
                else
                    row.addData(null);
            }
            model.addRow(row);
        }

        // iterate over transactions and add to model

        Client filteredClient = get(ClientFilterConfig.class).getSelectedFilter()
                        .filter(getDashboardData().getClient());

        processTransactions(startYear, calcInterval, model, filteredClient);

        // sum
        model.getRows().forEach(row -> row.addData(row.getData().mapToLong(l -> l == null ? 0L : l.longValue()).sum()));

        // average
        if (showAverage)
        {
            model.getRows().forEach(row -> {
                long average = Math.round(row.getDataSubList(0, 12).stream().filter(Objects::nonNull)
                                .mapToLong(l -> l == null ? 0L : l.longValue()).average().getAsDouble());
                row.addData(average);
            });
        }

        return model;
    }

    protected abstract void processTransactions(int startYear, Interval interval, HeatmapModel<Long> model,
                    Client filteredClient);

}
