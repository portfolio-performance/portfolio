package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.math.Risk.Drawdown;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.util.Interval;

public class MaxDrawdownDurationWidget extends AbstractIndicatorWidget<PerformanceIndex>
{
    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withZone(ZoneId.systemDefault());

    private String toolTip = ""; //$NON-NLS-1$

    public MaxDrawdownDurationWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData, true, null);
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
    public Supplier<PerformanceIndex> getUpdateTask()
    {
        return () -> getDashboardData().getDataSeriesCache() //
                        .lookup(get(DataSeriesConfig.class).getDataSeries(), get(ReportingPeriodConfig.class)
                                        .getReportingPeriod().toInterval(LocalDate.now()));
    }

    @Override
    public void update(PerformanceIndex index)
    {
        super.update(index);

        Drawdown drawdown = index.getDrawdown();
        Interval maxDDDuration = drawdown.getMaxDrawdownDuration();

        boolean isUntilEndOfPeriod = maxDDDuration.getEnd().equals(index.getReportInterval().getEnd());
        String maxDDSupplement = isUntilEndOfPeriod ? Messages.TooltipMaxDrawdownDurationEndOfPeriod
                        : Messages.TooltipMaxDrawdownDurationFromXtoY;

        indicator.setText(MessageFormat.format(Messages.LabelXDays,
                        maxDDDuration.getDays() + (isUntilEndOfPeriod ? "+" : ""))); //$NON-NLS-1$ //$NON-NLS-2$

        // recovery time
        Interval recoveryTime = drawdown.getLongestRecoveryTime();
        isUntilEndOfPeriod = recoveryTime.getEnd().equals(index.getReportInterval().getEnd());
        String recoveryTimeSupplement = isUntilEndOfPeriod ? Messages.TooltipMaxDrawdownDurationEndOfPeriod
                        : Messages.TooltipMaxDrawdownDurationFromXtoY;

        this.toolTip = Messages.TooltipMaxDrawdownDuration + "\n\n" //$NON-NLS-1$
                        + MessageFormat.format(maxDDSupplement, formatter.format(maxDDDuration.getStart()),
                                        formatter.format(maxDDDuration.getEnd()))
                        + "\n\n" //$NON-NLS-1$
                        + MessageFormat.format(Messages.TooltipMaxDurationLowToHigh, recoveryTime.getDays()) //
                        + "\n" //$NON-NLS-1$
                        + MessageFormat.format(recoveryTimeSupplement, formatter.format(recoveryTime.getStart()),
                                        formatter.format(recoveryTime.getEnd()));
    }

}
