package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.math.Correlation;
import name.abuchen.portfolio.math.Correlation.CorrelationNotDefinedException;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.util.Interval;

public class CorrelationWidget extends AbstractIndicatorWidget<Object>
{
    public static class SecondDataSeriesConfig extends DataSeriesConfig
    {
        public SecondDataSeriesConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, true, false, null, Messages.LabelDataSeries + " 2", Dashboard.Config.SECONDARY_DATA_SERIES); //$NON-NLS-1$
        }

    }

    public CorrelationWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData, true, false, null, Messages.LabelDataSeries + " 1"); //$NON-NLS-1$

        addConfigAfter(DataSeriesConfig.class, new SecondDataSeriesConfig(this));
    }

    @Override
    public void onWidgetConfigEdited(Class<? extends WidgetConfig> type)
    {
        // construct label to indicate the data series (user can manually
        // change the label later)
        getWidget().setLabel(WidgetFactory.valueOf(getWidget().getType()).getLabel() + ", " //$NON-NLS-1$
                        + get(DataSeriesConfig.class).getDataSeries().getLabel() + " & " //$NON-NLS-1$
                        + get(SecondDataSeriesConfig.class).getDataSeries().getLabel());
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> {

            Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            PerformanceIndex index1 = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                            interval);

            PerformanceIndex index2 = getDashboardData().calculate(get(SecondDataSeriesConfig.class).getDataSeries(),
                            interval);
            
            double correlation = Double.NaN;
            
            try
            {
                correlation = new Correlation(index1, index2).calculateCorrelationCoefficient();
            }
            catch (CorrelationNotDefinedException e)
            {
                return e.getMessage();
            }

            return correlation;
        };
    }

    @Override
    public void update(Object correlationOrError)
    {
        super.update(correlationOrError);

        if(correlationOrError instanceof String) // error message
        {
            this.indicator.setText((String) correlationOrError);
        }
        else
        {
            Double correlation = (Double) correlationOrError;
            this.indicator.setText(Values.Percent2.format(correlation));
        }
    }


    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = super.createControl(parent, resources);

        InfoToolTip.attach(indicator, Messages.TooltipCorrelation);

        return container;
    }
}
