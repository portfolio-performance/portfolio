package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.PortfolioTransaction;
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

public class InvestmentHeatmapWidget extends AbstractHeatmapWidget<Long>
{
    enum InvestmentType
    {

        INVESTMENTS_MINUS_REVENUE(Messages.LabelHeatmapInvestmentsNoSellings,
                        t -> t.getType() == PortfolioTransaction.Type.BUY
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                        || t.getType() == PortfolioTransaction.Type.SELL
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND), //
        INVESTMENTS(Messages.LabelHeatmapInvestmentsDirect, //
                        t -> t.getType() == PortfolioTransaction.Type.BUY
                                        || t.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND);

        private String label;
        private Predicate<PortfolioTransaction> predicate;

        private InvestmentType(String label, Predicate<PortfolioTransaction> predicate)
        {
            this.label = label;
            this.predicate = predicate;
        }

        @Override
        public String toString()
        {
            return label;
        }

        public boolean isIncluded(PortfolioTransaction t)
        {
            return predicate.test(t);
        }
    }

    static class EarningsConfig extends EnumBasedConfig<InvestmentType>
    {
        public EarningsConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelHeatmapInvestmentsDirect, InvestmentType.class, Dashboard.Config.EARNING_TYPE,
                            Policy.EXACTLY_ONE);
        }
    }

    enum GrossNetType
    {
        NET(Messages.LabelAfterTaxAndFees, t -> t.getMonetaryAmount()), //
        GROSS(Messages.LabelBeforeTaxAndFees, t -> t.getGrossValue());

        private String label;
        private Function<PortfolioTransaction, Money> valueExtractor;

        private GrossNetType(String label, Function<PortfolioTransaction, Money> valueExtractor)
        {
            this.label = label;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public String toString()
        {
            return label;
        }

        public Money getValue(PortfolioTransaction t)
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

    public InvestmentHeatmapWidget(Widget widget, DashboardData data)
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

        CurrencyConverter converter = getDashboardData().getCurrencyConverter();
        InvestmentType type = get(EarningsConfig.class).getValue();
        GrossNetType grossNet = get(GrossNetConfig.class).getValue();

        // iterate over transactions and add to model

        Client filteredClient = get(ClientFilterConfig.class).getSelectedFilter()
                        .filter(getDashboardData().getClient());

        filteredClient.getPortfolios().stream() //
                        .flatMap(a -> a.getTransactions().stream()) //
                        .filter(type::isIncluded) //
                        .filter(t -> calcInterval.contains(t.getDateTime())).forEach(t -> {
                            int row = t.getDateTime().getYear() - startYear;
                            int col = t.getDateTime().getMonth().getValue() - 1;

                            Long value = converter.convert(t.getDateTime(), grossNet.getValue(t)).getAmount();
                            if (t.getType().isLiquidation())
                            {
                                value = -value;
                            }

                            Long oldValue = model.getRow(row).getData(col);

                            model.getRow(row).setData(col, oldValue + value);
                        });

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
}
