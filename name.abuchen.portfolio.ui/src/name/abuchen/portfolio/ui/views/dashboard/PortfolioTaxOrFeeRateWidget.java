package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;

public class PortfolioTaxOrFeeRateWidget extends AbstractIndicatorWidget<Object>
{
    private final Function<ClientPerformanceSnapshot, Object> valueProvider;
    private final String defaultToolTip;

    private String toolTip = ""; //$NON-NLS-1$

    public PortfolioTaxOrFeeRateWidget(Widget widget, DashboardData dashboardData,
                    Function<ClientPerformanceSnapshot, Object> valueProvider, String defaultToolTip)
    {
        super(widget, dashboardData, false);

        this.valueProvider = valueProvider;
        this.defaultToolTip = defaultToolTip;
    }

    public String getToolTip()
    {
        return toolTip;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite composite = super.createControl(parent, resources);

        InfoToolTip.attach(indicator, this::getToolTip);

        return composite;
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> {
            PerformanceIndex index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                            get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now()));

            ClientPerformanceSnapshot snapshot = index.getClientPerformanceSnapshot()
                            .orElseThrow(IllegalArgumentException::new);

            return valueProvider.apply(snapshot);
        };
    }

    @Override
    public void update(Object rateOrValue)
    {
        super.update(rateOrValue);

        if (rateOrValue instanceof Double)
        {
            Double rate = (Double) rateOrValue;
            this.indicator.setText(!rate.isInfinite() ? Values.Percent2.format(rate) : "-"); //$NON-NLS-1$
            this.toolTip = defaultToolTip;
        }
        else if (rateOrValue instanceof Money)
        {
            this.indicator.setText(Values.Money.format((Money) rateOrValue) + "*"); //$NON-NLS-1$
            this.toolTip = defaultToolTip + "\n\n* " + Messages.LabelPortfolioRateNotAvailable; //$NON-NLS-1$
        }
    }

}
