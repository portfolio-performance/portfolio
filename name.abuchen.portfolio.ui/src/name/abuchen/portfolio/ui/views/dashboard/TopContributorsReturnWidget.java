package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.PerformanceMetric;
import name.abuchen.portfolio.util.TextUtil;

public class TopContributorsReturnWidget extends AbstractTopContributorsWidget<List<LazySecurityPerformanceRecord>>
{
    private class PerformanceMetricConfig implements WidgetConfig
    {
        private PerformanceMetric metric = PerformanceMetric.TTWROR;

        public PerformanceMetricConfig()
        {
            try
            {
                String code = getWidget().getConfiguration().get(Dashboard.Config.METRIC.name());
                if (code != null)
                    metric = PerformanceMetric.valueOf(code);
            }
            catch (IllegalArgumentException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(metric.toString()));

            MenuManager subMenu = new MenuManager(Messages.LabelPerformanceMetric);
            Arrays.stream(PerformanceMetric.values()).forEach(m -> {
                Action action = new SimpleAction(m.toString(), a -> {
                    metric = m;
                    getWidget().getConfiguration().put(Dashboard.Config.METRIC.name(), m.name());
                    update();
                    getClient().touch();
                });
                action.setChecked(metric == m);
                subMenu.add(action);
            });

            manager.add(subMenu);
        }

        public PerformanceMetric getMetric()
        {
            return metric;
        }

        @Override
        public String getLabel()
        {
            return Messages.LabelPerformanceMetric + ": " + metric; //$NON-NLS-1$
        }
    }

    public TopContributorsReturnWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
        addConfig(new PerformanceMetricConfig());
        addConfig(new CountConfig(this));
    }

    @Override
    public Supplier<List<LazySecurityPerformanceRecord>> getUpdateTask()
    {
        return () -> {
            var interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            var index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(), interval);

            var snapshot = index.getClientPerformanceSnapshot().orElseThrow(IllegalArgumentException::new);
            var client = snapshot.getClient();
            var converter = getDashboardData().getCurrencyConverter();

            var secSnapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);
            return new ArrayList<>(secSnapshot.getRecords());
        };
    }

    @Override
    protected List<DisplayRow> buildDisplayRows(List<LazySecurityPerformanceRecord> records)
    {
        PerformanceMetric metric = get(PerformanceMetricConfig.class).getMetric();

        records.sort((a, b) -> Double.compare(getPerformance(b, metric), getPerformance(a, metric)));

        return records.stream().map(r -> {
            double performance = getPerformance(r, metric);
            return new DisplayRow(r.getSecurity(), TextUtil.escapeHtml(r.getSecurity().getName()),
                            Values.Percent2.format(performance), performance >= 0);
        }).toList();
    }

    private double getPerformance(LazySecurityPerformanceRecord record, PerformanceMetric metric)
    {
        return metric == PerformanceMetric.IRR ? record.getIrr() : record.getTrueTimeWeightedRateOfReturn();
    }
}
