package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.util.SecurityTimeliness;
import name.abuchen.portfolio.util.TextUtil;

public class SecurityPriceTimelinessWidget extends WidgetDelegate<Number>
{
    protected Label title;
    protected ColoredLabel indicator;
    private List<Security> staleSecurities;
    private long allSecuritiesCount;

    @Preference(value = UIConstants.Preferences.QUOTES_STALE_AFTER_DAYS_PATH)
    @Inject
    private int numberOfTradeDaysToLookBack;

    protected SecurityPriceTimelinessWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(Colors.theme().defaultBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        indicator = new ColoredLabel(container, SWT.NONE);
        indicator.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.KPI);
        indicator.setBackground(Colors.theme().defaultBackground());
        indicator.setText(""); //$NON-NLS-1$

        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);

        this.update();

        InfoToolTip.attach(indicator, this::getTooltip);

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public void update(Number value)
    {
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));
        indicator.setText(Values.Percent2.format(value.doubleValue()));
    }

    @Override
    public Supplier<Number> getUpdateTask()
    {
        this.staleSecurities = this.getClient().getSecurities().stream().filter(
                        s -> (new SecurityTimeliness(s, this.numberOfTradeDaysToLookBack, Clock.systemDefaultZone()))
                                        .isStale())
                        .collect(Collectors.toList());

        this.allSecuritiesCount = this.getClient().getSecurities().stream().filter(s -> !s.isRetired()).count();

        return () -> this.allSecuritiesCount > 0 ? 1 - (double) this.staleSecurities.size() / this.allSecuritiesCount
                        : 0;
    }

    private String getTooltip()
    {
        if (this.staleSecurities == null)
            return ""; //$NON-NLS-1$
        
        String securities = this.staleSecurities.stream()
                        .map(s -> s.getName() + (s.getLatest() != null
                                        ? " (" + Values.Date.format(s.getLatest().getDate()) + ")" //$NON-NLS-1$//$NON-NLS-2$
                                        : "")) //$NON-NLS-1$
                        .sorted().collect(Collectors.joining("\n")); //$NON-NLS-1$

        return MessageFormat.format(Messages.TooltipSecurityPriceTimeliness, this.staleSecurities.size(),
                        this.allSecuritiesCount, this.numberOfTradeDaysToLookBack)
                        + (!securities.equals("") ? ":\n\n" + securities : ""); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
}
