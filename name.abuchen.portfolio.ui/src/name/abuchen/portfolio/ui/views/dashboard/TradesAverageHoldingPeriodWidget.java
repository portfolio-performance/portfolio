package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class TradesAverageHoldingPeriodWidget extends AbstractTradesWidget
{
    private static final int DAYS_IN_YEAR = 365;

    public enum Metric
    {
        DAY(Messages.LabelMetricDays,
                        days -> MessageFormat.format(Messages.LabelMetricDaysFormatter, Math.round(days))), //
        YEAR(Messages.LabelMetricYears,
                        days -> MessageFormat.format(Messages.LabelMetricYearsFormatter, days / DAYS_IN_YEAR));

        private String label;
        private DoubleFunction<String> formatter;

        private Metric(String label, DoubleFunction<String> formatter)
        {
            this.label = label;
            this.formatter = formatter;
        }

        public String format(double days)
        {
            return formatter.apply(days);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    public static class MetricConfig extends EnumBasedConfig<Metric>
    {
        public MetricConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelMetric, Metric.class, Dashboard.Config.METRIC, Policy.EXACTLY_ONE);
        }
    }

    public TradesAverageHoldingPeriodWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new MetricConfig(this));
    }

    @Override
    public void update(TradeDetailsView.Input input)
    {
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));

        List<Trade> trades = input.getTrades();

        double totalPurchasePrice = trades.stream().map(Trade::getEntryValue)
                        .collect(MoneyCollectors.sum(getDashboardData().getCurrencyConverter().getTermCurrency()))
                        .getAmount();

        double averageHoldingDays = trades.stream()
                        .mapToDouble(t -> t.getHoldingPeriod() * t.getEntryValue().getAmount() / totalPurchasePrice)
                        .sum();

        this.indicator.setText(get(MetricConfig.class).getValue().format(averageHoldingDays));
        this.indicator.setToolTipText(Messages.TooltipAverageHoldingPeriod);
    }

    /**
     * Constructs a filter to include all trades (open or closed) which
     * intersect with the reporting interval.
     */
    @Override
    protected Predicate<Trade> getFilter(Interval interval)
    {
        return t -> {

            LocalDate start = t.getStart().toLocalDate();
            LocalDate end = t.getEnd().map(LocalDateTime::toLocalDate).orElseGet(LocalDate::now);

            return !start.isAfter(interval.getEnd()) && end.isAfter(interval.getStart());
        };
    }

}
