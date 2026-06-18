package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Position;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.TextUtil;

public class TopContributorsWidget extends AbstractTopContributorsWidget<ClientPerformanceSnapshot>
{
    enum ContributorsLayout
    {
        ONLY_SECURITIES(Messages.SecurityListFilterOnlySecurities);

        private String label;

        private ContributorsLayout(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class ContributorsLayoutConfig extends EnumBasedConfig<ContributorsLayout>
    {
        public ContributorsLayoutConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelLayout, ContributorsLayout.class, Dashboard.Config.LAYOUT, Policy.MULTIPLE);
        }
    }

    private record ContributorRecord(String label, Security security, String categoryLabel, boolean needsExplanation,
                    Money value, boolean isPositiveContribution)
    {
    }

    public TopContributorsWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
        addConfig(new ContributorsLayoutConfig(this));
        addConfig(new CountConfig(this));
    }

    @Override
    public Supplier<ClientPerformanceSnapshot> getUpdateTask()
    {
        return () -> {
            PerformanceIndex index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));
            return index.getClientPerformanceSnapshot(true).orElseThrow(IllegalArgumentException::new);
        };
    }

    @Override
    protected List<DisplayRow> buildDisplayRows(ClientPerformanceSnapshot snapshot)
    {
        Set<ContributorsLayout> layouts = get(ContributorsLayoutConfig.class).getValues();
        boolean onlySecurities = layouts.contains(ContributorsLayout.ONLY_SECURITIES);

        var records = buildRecords(snapshot, onlySecurities);
        records.sort(Comparator.comparingLong((ContributorRecord r) -> r.value().getAmount()).reversed());

        return records.stream().map(r -> {
            var label = r.needsExplanation() ? String.format("%s <em>(%s)</em>", TextUtil.escapeHtml(r.label()), //$NON-NLS-1$
                            TextUtil.escapeHtml(r.categoryLabel())) : TextUtil.escapeHtml(r.label());
            return new DisplayRow(r.security(), label, Values.Money.format(r.value(), getClient().getBaseCurrency()),
                            r.isPositiveContribution());
        }).toList();
    }

    private List<ContributorRecord> buildRecords(ClientPerformanceSnapshot snapshot, boolean onlySecurities)
    {
        var records = new ArrayList<ContributorRecord>();

        var onlySecuritiesCategories = EnumSet.of(CategoryType.CAPITAL_GAINS, CategoryType.REALIZED_CAPITAL_GAINS);

        for (var type : CategoryType.values()) // NOSONAR
        {
            if (type == CategoryType.INITIAL_VALUE || type == CategoryType.FINAL_VALUE)
                continue;

            if (onlySecurities && !onlySecuritiesCategories.contains(type))
                continue;

            var category = snapshot.getCategoryByType(type);
            if (category == null)
                continue;

            var needsExplanation = type == CategoryType.EARNINGS || type == CategoryType.FEES
                            || type == CategoryType.TAXES || type == CategoryType.CURRENCY_GAINS;

            for (Position position : category.getPositions())
            {
                Money value = position.getValue();
                if (value.getAmount() == 0)
                    continue;

                boolean isPositive;
                if (value.getAmount() > 0)
                    isPositive = "+".equals(category.getSign()); //$NON-NLS-1$
                else
                    isPositive = "-".equals(category.getSign()); //$NON-NLS-1$

                records.add(new ContributorRecord(position.getLabel(), position.getSecurity(), category.getLabel(),
                                needsExplanation, value, isPositive));
            }
        }

        return records;
    }
}
