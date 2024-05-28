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
        this.setTooltip((ds, period) -> Messages.TooltipSharpeRatio);
        this.setProvider((ds, period) -> {
            PerformanceIndex index = dashboardData.calculate(ds, period);
            double performanceIRR = index.getPerformanceIRR();
            double riskFreeIRR = ((double)configuration.getRiskFreeIRR()) / 10000;
            double volatility = index.getVolatility().getStandardDeviation();

            if (Double.isNaN(riskFreeIRR) || volatility == 0)
                return Double.NaN; // Handle invalid risk-free IRR or zero volatility

            double excessReturn = performanceIRR - riskFreeIRR;
            return excessReturn / volatility;
        });
    }
}
