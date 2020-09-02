package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.util.TextUtil;

public class ExchangeRateWidget extends WidgetDelegate<Object>
{
    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                    .withZone(ZoneId.systemDefault());

    private Label title;
    private Label indicator;

    public ExchangeRateWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ExchangeRateSeriesConfig(this));
        addConfig(new ReportingPeriodConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        indicator = new Label(container, SWT.NONE);
        indicator.setFont(resources.getKpiFont());
        indicator.setBackground(container.getBackground());
        indicator.setText(""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);

        InfoToolTip.attach(indicator, () -> {
            ReportingPeriod period = get(ReportingPeriodConfig.class).getReportingPeriod();
            ExchangeRateTimeSeries series = get(ExchangeRateSeriesConfig.class).getSeries();
            Optional<ExchangeRate> rate = series.lookupRate(period.toInterval(LocalDate.now()).getEnd());
            return rate.isPresent() ? MessageFormat.format(Messages.TooltipDateOfExchangeRate,
                            formatter.format(rate.get().getTime())) : ""; //$NON-NLS-1$
        });

        update(null);

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> null;
    }

    @Override
    public void update(Object data)
    {
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));

        ReportingPeriod period = get(ReportingPeriodConfig.class).getReportingPeriod();
        ExchangeRateTimeSeries series = get(ExchangeRateSeriesConfig.class).getSeries();
        Optional<ExchangeRate> rate = series.lookupRate(period.toInterval(LocalDate.now()).getEnd());

        this.indicator.setText(series.getBaseCurrency() + '/' + series.getTermCurrency() + ' '
                        + (rate.isPresent() ? Values.ExchangeRate.format(rate.get().getValue()) : '-'));
    }
}
