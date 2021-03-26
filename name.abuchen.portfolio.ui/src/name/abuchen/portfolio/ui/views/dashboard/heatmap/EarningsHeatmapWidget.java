package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.util.Interval;

public class EarningsHeatmapWidget extends AbstractHeatmapWidget<Long>
{
    enum EarningType
    {
        EARNINGS(Messages.LabelDividends + " + " + Messages.LabelInterest, //$NON-NLS-1$
                        t -> t.getType() == AccountTransaction.Type.DIVIDENDS
                                        || t.getType() == AccountTransaction.Type.INTEREST
                                        || t.getType() == AccountTransaction.Type.INTEREST_CHARGE), //
        DIVIDENDS(Messages.LabelDividends, t -> t.getType() == AccountTransaction.Type.DIVIDENDS), //
        INTEREST(Messages.LabelInterest, t -> t.getType() == AccountTransaction.Type.INTEREST
                        || t.getType() == AccountTransaction.Type.INTEREST_CHARGE);

        private String label;
        private Predicate<AccountTransaction> predicate;

        private EarningType(String label, Predicate<AccountTransaction> predicate)
        {
            this.label = label;
            this.predicate = predicate;
        }

        @Override
        public String toString()
        {
            return label;
        }

        public boolean isIncluded(AccountTransaction t)
        {
            return predicate.test(t);
        }
    }

    static class EarningsConfig extends EnumBasedConfig<EarningType>
    {
        public EarningsConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelEarnings, EarningType.class, Dashboard.Config.EARNING_TYPE,
                            Policy.EXACTLY_ONE);
        }
    }

    enum GrossNetType
    {
        NET(Messages.LabelNet, t -> t.getMonetaryAmount()), //
        GROSS(Messages.LabelGross, t -> t.getGrossValue());

        private String label;
        private Function<AccountTransaction, Money> valueExtractor;

        private GrossNetType(String label, Function<AccountTransaction, Money> valueExtractor)
        {
            this.label = label;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public String toString()
        {
            return label;
        }

        public Money getValue(AccountTransaction t)
        {
            return valueExtractor.apply(t);
        }
    }

    static class GrossNetConfig extends EnumBasedConfig<GrossNetType>
    {
        public GrossNetConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelGrossNetCalculation, GrossNetType.class, Dashboard.Config.NET_GROSS,
                            Policy.EXACTLY_ONE);
        }
    }

    public enum Average
    {
        AVERAGE(Messages.HeatmapOrnamentAverage);

        private String label;

        private Average(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class AverageConfig extends EnumBasedConfig<Average>
    {
        public AverageConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.HeatmapOrnamentAverage, Average.class, Dashboard.Config.LAYOUT, Policy.MULTIPLE);
        }
    }

    public EarningsHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ClientFilterConfig(this));
        addConfig(new EarningsConfig(this));
        addConfig(new GrossNetConfig(this));
        addConfig(new AverageConfig(this));
    }

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

            for (YearMonth month = YearMonth.of(year.getValue(), 1);
                 month.getYear() == year.getValue();
                 month = month.plusMonths(1))
            {
                if (calcInterval.intersects(Interval.of(month.atDay(1).minusDays(1), month.atEndOfMonth())))
                    row.addData(0L);
                else
                    row.addData(null);
            }
            model.addRow(row);
        }

        CurrencyConverter converter = getDashboardData().getCurrencyConverter();
        EarningType type = get(EarningsConfig.class).getValue();
        GrossNetType grossNet = get(GrossNetConfig.class).getValue();

        // iterate over transactions and add to model

        Client filteredClient = get(ClientFilterConfig.class).getSelectedFilter()
                        .filter(getDashboardData().getClient());

        filteredClient.getAccounts().stream() //
                        .flatMap(a -> a.getTransactions().stream()) //
                        .filter(type::isIncluded) //
                        .filter(t -> calcInterval.contains(t.getDateTime())).forEach(t -> {
                            int row = t.getDateTime().getYear() - startYear;
                            int col = t.getDateTime().getMonth().getValue() - 1;

                            Long value = converter.convert(t.getDateTime(), grossNet.getValue(t)).getAmount();
                            if (t.getType().isDebit())
                                value = -value;

                            Long oldValue = model.getRow(row).getData(col);

                            model.getRow(row).setData(col, oldValue + value);
                        });

        // sum
        model.getRows().forEach(row -> row.addData(row.getData().mapToLong(l -> l == null ? 0L : l.longValue()).sum()));

        // average
        if (showAverage)
        {
            model.getRows().forEach(
                            row -> row.addData((long) row.getDataSubList(0, 12).stream().filter(Objects::nonNull)
                                            .mapToLong(l -> l == null ? 0L : l.longValue()).average().getAsDouble()));
        }

        return model;
    }
}
