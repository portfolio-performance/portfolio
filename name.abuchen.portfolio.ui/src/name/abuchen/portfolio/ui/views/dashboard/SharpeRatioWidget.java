package name.abuchen.portfolio.ui.views.dashboard;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;

/**
 * Display the current Sharpe Ratio of the Portfolio
 */
public class SharpeRatioWidget extends IndicatorWidget<Double>
{
    public SharpeRatioWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData, true, null);
        SharpeRatioConfig configuration = new SharpeRatioConfig(this);
        this.addConfig(configuration);

        this.setFormatter(Values.PercentPlain);
        this.setValueColored(false);
        this.setTooltip((ds, period) -> {
            return Messages.TooltipSharpeRatio;
        });
        this.setProvider((ds, period) -> {
            PerformanceIndex index = dashboardData.calculate(ds, period);
            double r = index.getPerformanceIRR();
            double rf = ((double)configuration.getRiskFreeIRR()) / 10000;
            double volatility = index.getVolatility().getStandardDeviation();

            if (Double.isNaN(rf))
                return Double.NaN; // Handle invalid rf value

            double excessReturn = r - rf;
            return excessReturn / volatility;
        });
    }
}
