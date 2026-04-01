package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.dashboard.lists.AbstractSecurityListWidget;
import name.abuchen.portfolio.util.TextUtil;

public class TopFlopWidget extends AbstractSecurityListWidget<TopFlopWidget.PerformanceItem>
{
    // Holds either a percentage (perf) or a Money, depending on the performance
    // metric.
    public static class PerformanceItem extends AbstractSecurityListWidget.Item
    {
        private final double perf;
        private final Money money;

        private PerformanceItem(Security security, double perf, Money money)
        {
            super(security);
            this.perf = perf;
            this.money = money;
        }

        public static PerformanceItem ofPercentage(Security security, double perf)
        {
            return new PerformanceItem(security, perf, null);
        }

        public static PerformanceItem ofDelta(Security security, Money money)
        {
            return new PerformanceItem(security, 0, money);
        }

        public double getSortableAmount()
        {
            return money != null ? money.getAmount() : perf;
        }

        public Money getMoney()
        {
            return money;
        }

        public double getPerf()
        {
            return perf;
        }
    }

    public enum PerformanceType
    {
        TWR_CUMULATED(Messages.LabelTTWROR), TWR_ANNUALIZED(Messages.LabelTTWROR_Annualized), IRR(
                        Messages.LabelIRR), DELTA(Messages.ColumnAbsolutePerformance);

        private final String label;

        PerformanceType(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class PerformanceListConfig extends EnumBasedConfig<PerformanceType>
    {
        public PerformanceListConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelPerformanceMetric, PerformanceType.class,
                            Dashboard.Config.PERFORMANCE, Policy.EXACTLY_ONE);
        }
    }

    public enum TopFlopList
    {
        TOP(Messages.LabelWinner), FLOP(Messages.LabelLoser);

        private final String label;

        TopFlopList(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class TopFlopConfig extends EnumBasedConfig<TopFlopList>
    {
        public TopFlopConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelWinnerLoserList, TopFlopList.class,
                            Dashboard.Config.TOPFLOP, Policy.EXACTLY_ONE);
        }
    }

    protected StyledLabel subtitle;

    public TopFlopWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ClientFilterConfig(this));
        addConfig(new PerformanceListConfig(this));
        addConfig(new TopFlopConfig(this));
    }

    @Override
    public Supplier<List<PerformanceItem>> getUpdateTask()
    {
        var perfType = get(PerformanceListConfig.class).getValue();
        var interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());
        var filteredClient = get(ClientFilterConfig.class).getSelectedFilter().filter(getClient());
        var topFlop = get(TopFlopConfig.class).getValue();

        var snapshot = LazySecurityPerformanceSnapshot.create(filteredClient,
                        getDashboardData().getCurrencyConverter().with(getClient().getBaseCurrency()), interval);

        List<PerformanceItem> items = snapshot.getRecords().stream().map(r -> toPerformanceItem(r, perfType))
                        .collect(Collectors.toList());

        return () -> sortAndLimit(items, topFlop);
    }

    private PerformanceItem toPerformanceItem(LazySecurityPerformanceRecord securityRecord, PerformanceType perfType)
    {
        var security = securityRecord.getSecurity();
        return switch (perfType)
        {
            case DELTA -> PerformanceItem.ofDelta(security, securityRecord.getDelta().get());
            case IRR -> PerformanceItem.ofPercentage(security, securityRecord.getIrr().get());
            case TWR_ANNUALIZED -> PerformanceItem.ofPercentage(security,
                            securityRecord.getTrueTimeWeightedRateOfReturnAnnualized().get());
            case TWR_CUMULATED -> PerformanceItem.ofPercentage(security,
                            securityRecord.getTrueTimeWeightedRateOfReturn().get());
        };
    }

    private List<PerformanceItem> sortAndLimit(List<PerformanceItem> items, TopFlopList topFlop)
    {
        var comparator = Comparator.comparingDouble(PerformanceItem::getSortableAmount);
        if (topFlop == TopFlopList.TOP)
            comparator = comparator.reversed();

        return items.stream().sorted(comparator).limit(5).toList();
    }

    private String buildSubtitleText()
    {
        return get(TopFlopConfig.class).getValue() + " | " //$NON-NLS-1$
                        + get(PerformanceListConfig.class).getValue();
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        var composite = super.createControl(parent, resources);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(composite);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(list);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(list);
        return composite;
    }

    @Override
    protected void createTitleArea(Composite container)
    {
        super.createTitleArea(container);

        subtitle = new StyledLabel(container, SWT.NONE);
        subtitle.setBackground(container.getBackground());
        subtitle.setText(TextUtil.tooltip(buildSubtitleText()));
        subtitle.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(subtitle);
    }

    @Override
    public void update(List<PerformanceItem> items)
    {
        subtitle.setText(TextUtil.tooltip(buildSubtitleText()));
        super.update(items);
    }

    @Override
    protected Composite createItemControl(Composite parent, PerformanceItem item, PerformanceItem previous)
    {
        var security = item.getSecurity();

        var composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        var image = LogoManager.instance().getDefaultColumnImage(security, getClient().getSettings());
        var logo = createLabel(composite, image);
        var name = createLabel(composite, security.getName());
        var amount = createColoredLabel(composite, item, SWT.RIGHT);

        addListener(mouseUpAdapter, composite, name, amount);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(amount, -5, SWT.LEFT));
        FormDataFactory.startingWith(amount).right(new FormAttachment(100));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
        return composite;
    }

    private String buildAmountLabel(PerformanceItem item)
    {
        return switch (get(PerformanceListConfig.class).getValue()) // $NON-NLS-1$
        {
            case IRR, TWR_ANNUALIZED -> Values.AnnualizedPercent2.format(item.getPerf());
            case TWR_CUMULATED -> Values.Percent2.format(item.getPerf());
            case DELTA -> Values.Money.format(item.getMoney(), getClient().getBaseCurrency());
        };
    }

    protected Label createColoredLabel(Composite composite, PerformanceItem item, int style)
    {
        double amount = item.getSortableAmount();
        var color = amount > 0 ? ValueColorScheme.current().positiveForeground()
                        : amount < 0 ? ValueColorScheme.current().negativeForeground() : null;

        var label = new Label(composite, style);
        label.setText(TextUtil.tooltip(buildAmountLabel(item)));
        label.setForeground(color);
        return label;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        // nothing to do
    }
}
