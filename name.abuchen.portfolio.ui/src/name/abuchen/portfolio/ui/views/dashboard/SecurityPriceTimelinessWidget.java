package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.util.TextUtil;

public class SecurityPriceTimelinessWidget extends WidgetDelegate<Number>
{
    /**
     * @see name.abuchen.portfolio.ui.views.SecuritiesTable#addColumnDateOfLatestPrice()
     */
    private static final int CONSIDER_AS_OLD_AFTER_DAYS = 7;
    protected Label title;
    protected ColoredLabel indicator;
    private LocalDate daysAgo;
    private long oldSecuritiesCount;
    private long allSecuritiesCount;

    protected SecurityPriceTimelinessWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        this.daysAgo = LocalDate.now().minusDays(CONSIDER_AS_OLD_AFTER_DAYS);

        this.oldSecuritiesCount = this.getClient().getSecurities().stream()
                        .filter(s -> !s.isRetired()
                                        && (s.getLatest() == null || s.getLatest().getDate().isBefore(this.daysAgo)))
                        .count();

        this.allSecuritiesCount = this.getClient().getSecurities().stream().filter(s -> !s.isRetired()).count();
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

        InfoToolTip.attach(indicator, () -> {
            return MessageFormat.format(Messages.TooltipSecurityPriceTimeliness, this.oldSecuritiesCount, this.allSecuritiesCount,
                            CONSIDER_AS_OLD_AFTER_DAYS);
        });

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
        return () -> 1 - (double) this.oldSecuritiesCount / this.allSecuritiesCount;

    }
}
