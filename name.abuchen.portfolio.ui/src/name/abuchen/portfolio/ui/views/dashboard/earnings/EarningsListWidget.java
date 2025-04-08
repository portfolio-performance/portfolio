package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DashboardView;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dashboard.earnings.EarningsListWidget.Model;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class EarningsListWidget extends WidgetDelegate<Model>
{

    public enum ExpansionSetting
    {
        EXPAND_ALL(Messages.LabelExpandAll), //
        EXPAND_CURRENT_MONTH(Messages.LabelExpandCurrentMonth), //
        COLLAPSE_ALL(Messages.LabelCollapseAll), //
        ;

        private ExpansionSetting(String label)
        {
            this.label = label;
        }

        private String label;

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class ExpansionSettingConfig extends EnumBasedConfig<ExpansionSetting>
    {
        public ExpansionSettingConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelTreeExpansionConfig, ExpansionSetting.class, Dashboard.Config.LAYOUT,
                            Policy.EXACTLY_ONE);
        }
    }

    public static class Model
    {
        private final GrossNetType grossNetType;
        private final int year;
        private final Money sum;
        private final List<MonthData> months;

        public Model(GrossNetType grossNetType, int year, Money sum, List<MonthData> months)
        {
            this.grossNetType = grossNetType;
            this.year = year;
            this.sum = sum;
            this.months = months;
        }

        public int getYear()
        {
            return year;
        }

        public List<MonthData> getMonths()
        {
            return months;
        }

    }

    public static class MonthData
    {
        private final int monthOfYear;
        private Money sum;
        private final List<TransactionPair<AccountTransaction>> transactions = new ArrayList<>();

        public MonthData(int monthOfYear, Money sum)
        {
            this.monthOfYear = monthOfYear;
            this.sum = sum;
        }
    }

    @Inject
    private AbstractFinanceView view;

    private final String[] monthLabels = new DateFormatSymbols().getMonths();

    private int selectedYear = LocalDate.now().getYear();

    private Label labelTitle;
    private Label labelYear;
    private Label labelSum;
    private Composite list;
    private boolean[] collapsed;
    private ExpansionSetting previousExpansion;

    public EarningsListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ClientFilterConfig(this));
        addConfig(new EarningTypeConfig(this));
        addConfig(new GrossNetTypeConfig(this));
        addConfig(new ExpansionSettingConfig(this));

        String year = widget.getConfiguration().get(Dashboard.Config.START_YEAR.name());
        if (year != null)
        {
            try
            {
                selectedYear = Integer.parseInt(year);
            }
            catch (NumberFormatException ignore)
            {
                // fall back to current year
            }
        }
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        labelTitle = new Label(container, SWT.NONE);
        labelTitle.setBackground(container.getBackground());
        labelTitle.setText(TextUtil.tooltip(getWidget().getLabel()));
        labelTitle.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(labelTitle);

        Composite heading = new Composite(container, SWT.NONE);
        heading.setBackground(container.getBackground());
        heading.setLayout(new FormLayout());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(heading);

        ImageHyperlink previous = new ImageHyperlink(heading, SWT.NONE);
        previous.setImage(Images.PREVIOUS.image());
        previous.addHyperlinkListener(new HyperlinkAdapter()
        {
            @Override
            public void linkActivated(HyperlinkEvent e)
            {
                selectedYear--;
                getWidget().getConfiguration().put(Dashboard.Config.START_YEAR.name(), String.valueOf(selectedYear));
                update();
            }
        });

        labelYear = new Label(heading, SWT.CENTER);
        labelYear.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        labelYear.setText(String.valueOf(selectedYear));

        ImageHyperlink next = new ImageHyperlink(heading, SWT.NONE);
        next.setImage(Images.NEXT.image());
        next.addHyperlinkListener(new HyperlinkAdapter()
        {
            @Override
            public void linkActivated(HyperlinkEvent e)
            {
                int current = selectedYear;
                selectedYear = Math.min(selectedYear + 1, LocalDate.now().getYear());

                if (current != selectedYear)
                {
                    getWidget().getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                                    String.valueOf(selectedYear));
                    update();
                }
            }
        });

        labelSum = new Label(heading, SWT.RIGHT);
        labelSum.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);

        FormDataFactory.startingWith(previous).thenRight(labelYear).width(SWTHelper.stringWidth(next, "XXXXX")) //$NON-NLS-1$
                        .thenRight(next).thenRight(labelSum).right(new FormAttachment(100));

        list = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(list);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(list);

        return container;
    }

    @Override
    public Supplier<Model> getUpdateTask()
    {
        ClientFilter clientFilter = get(ClientFilterConfig.class).getSelectedFilter();
        EarningType earningsType = get(EarningTypeConfig.class).getValue();
        GrossNetType grossNetType = get(GrossNetTypeConfig.class).getValue();

        final int calculationYear = selectedYear;

        return () -> {

            CurrencyConverter converter = getDashboardData().getCurrencyConverter();

            List<MonthData> months = new ArrayList<>();
            for (int ii = 0; ii < 12; ii++)
                months.add(new MonthData(ii + 1, Money.of(converter.getTermCurrency(), 0)));

            Client filteredClient = clientFilter.filter(getClient());
            Interval interval = new ReportingPeriod.YearX(calculationYear).toInterval(LocalDate.now());

            for (Account account : filteredClient.getAccounts())
            {
                for (AccountTransaction tx : account.getTransactions()) // NOSONAR
                {
                    if (!earningsType.isIncluded(tx))
                        continue;

                    if (!interval.contains(tx.getDateTime()))
                        continue;

                    int monthOfYear = tx.getDateTime().getMonthValue();
                    final MonthData monthData = months.get(monthOfYear - 1);

                    Money value = grossNetType == GrossNetType.GROSS ? tx.getGrossValue() : tx.getMonetaryAmount();

                    monthData.sum = tx.getType().isCredit()
                                    ? monthData.sum.add(value.with(converter.at(tx.getDateTime())))
                                    : monthData.sum.subtract(value.with(converter.at(tx.getDateTime())));

                    monthData.transactions.add(new TransactionPair<>(account, tx));
                }
            }

            Money sum = months.stream().map(m -> m.sum).collect(MoneyCollectors.sum(converter.getTermCurrency()));

            return new Model(grossNetType, calculationYear, sum, months);
        };
    }

    @Override
    public void update(Model model)
    {
        this.labelTitle.setText(getWidget().getLabel());
        this.labelYear.setText(String.valueOf(model.getYear()));
        this.labelSum.setText(Values.Money.format(model.sum, getClient().getBaseCurrency()));
        ExpansionSetting expansion = get(ExpansionSettingConfig.class).getValue();
        if (collapsed == null || previousExpansion != expansion)
        {
            createCollapsedArray();
        }
        previousExpansion = expansion;

        Control[] children = list.getChildren();
        for (Control child : children)
            if (!child.isDisposed())
                child.dispose();

        for (MonthData month : model.months)
        {
            Composite header = createMonthHeader(list, month);
            GridDataFactory.fillDefaults().grab(true, false).indent(0, 7).applyTo(header);

            Label separator = new Label(list, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

            if (collapsed[month.monthOfYear - 1])
                continue;

            for (TransactionPair<AccountTransaction> tx : month.transactions)
            {
                Composite item = createItem(list, tx, model.grossNetType);
                GridDataFactory.fillDefaults().grab(true, false).applyTo(item);
            }
        }

        list.setData(model);
        list.getParent().getParent().layout(true);
        list.layout();

        ((DashboardView) this.view).updateScrolledCompositeMinSize();
    }

    private void createCollapsedArray()
    {
        ExpansionSetting expansion = get(ExpansionSettingConfig.class).getValue();
        collapsed = new boolean[12];

        if (expansion.equals(ExpansionSetting.EXPAND_ALL))
            return;

        Arrays.fill(collapsed, true);
        if (expansion.equals(ExpansionSetting.COLLAPSE_ALL))
            return;

        LocalDate now = LocalDate.now();
        collapsed[now.getMonthValue() - 1] = false;
    }

    protected Composite createMonthHeader(Composite parent, MonthData month)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label label = new Label(composite, SWT.NONE);
        label.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        int mVal = month.monthOfYear - 1;
        label.setText(monthLabels[mVal]);
        label.setCursor(new HyperlinkSettings(parent.getDisplay()).getHyperlinkCursor());
        label.addMouseListener(MouseListener.mouseUpAdapter(e -> {
            collapsed[mVal] = !collapsed[mVal];
            update();
        }));

        Label sum = new Label(composite, SWT.RIGHT);
        sum.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        sum.setText(Values.Money.format(month.sum, getClient().getBaseCurrency()));

        FormDataFactory.startingWith(label).left(new FormAttachment(0)).right(new FormAttachment(sum, -5, SWT.LEFT));
        FormDataFactory.startingWith(sum).right(new FormAttachment(100));

        return composite;
    }

    protected Composite createItem(Composite parent, TransactionPair<AccountTransaction> pair,
                    GrossNetType grossNetType)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Security security = pair.getTransaction().getSecurity();
        Account account = (Account) pair.getOwner();

        Label logo = new Label(composite, SWT.NONE);
        logo.setImage(LogoManager.instance().getDefaultColumnImage(security != null ? security : account,
                        getClient().getSettings()));

        Label name = new Label(composite, SWT.NONE);
        name.setText(TextUtil.tooltip(security != null ? security.getName() : account.getName()));
        name.addListener(SWT.MouseUp, event -> view.setInformationPaneInput(pair.getTransaction().getSecurity()));

        Label earning = new Label(composite, SWT.RIGHT);
        Money value = grossNetType == GrossNetType.GROSS ? pair.getTransaction().getGrossValue()
                        : pair.getTransaction().getMonetaryAmount();
        if (pair.getTransaction().getType().isDebit())
            value = value.multiply(-1);
        earning.setText(Values.Money.format(value, getClient().getBaseCurrency()));

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(earning, -5, SWT.LEFT));
        FormDataFactory.startingWith(earning).right(new FormAttachment(100));

        return composite;
    }

    @Override
    public Control getTitleControl()
    {
        return labelTitle;
    }

}
