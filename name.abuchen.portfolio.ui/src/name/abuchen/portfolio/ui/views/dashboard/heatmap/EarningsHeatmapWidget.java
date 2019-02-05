package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
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

    public EarningsHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new EarningsConfig(this));
        addConfig(new GrossNetConfig(this));
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
                        interval.getEnd().withDayOfMonth(interval.getEnd().lengthOfMonth()));

        // build model
        HeatmapModel<Long> model = new HeatmapModel<>(numDashboardColumns <= 1 ? Values.Amount : Values.AmountShort);
        model.setCellToolTip(value -> value != null ? Values.Amount.format(value) : ""); //$NON-NLS-1$
        addMonthlyHeader(model, numDashboardColumns, true, false);
        int startYear = calcInterval.getStart().plusDays(1).getYear();

        // prepare data
        for (Integer year : calcInterval.iterYears())
        {
            String label = numDashboardColumns > 2 ? String.valueOf(year % 100) : String.valueOf(year);
            HeatmapModel.Row<Long> row = new HeatmapModel.Row<>(label);

            for (LocalDate month = LocalDate.of(year, 1, 1); month.getYear() == year; month = month.plusMonths(1))
                row.addData(calcInterval.contains(month) ? 0L : null);
            model.addRow(row);
        }

        CurrencyConverter converter = getDashboardData().getCurrencyConverter();
        EarningType type = get(EarningsConfig.class).getValue();
        GrossNetType grossNet = get(GrossNetConfig.class).getValue();

        // iterate over transactions and add to model
        getDashboardData().getClient().getAccounts().stream() //
                        .flatMap(a -> a.getTransactions().stream()) //
                        .filter(t -> type.isIncluded(t)) //
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

        return model;
    }
}
