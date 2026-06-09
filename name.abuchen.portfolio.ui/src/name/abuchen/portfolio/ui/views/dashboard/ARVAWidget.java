package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class ARVAWidget extends WidgetDelegate<ARVAWidget.ARVAData>
{
    private static final double DEFAULT_REAL_RETURN = 0.03;
    private static final int DEFAULT_REMAINING_YEARS = 60;
    private static final double EPSILON_RETURN = 0.0000001;

    enum PaymentTiming
    {
        END_OF_PERIOD, BEGINNING_OF_PERIOD;

        public String getLabel()
        {
            return switch (this)
            {
                case END_OF_PERIOD -> Messages.LabelARVAEndOfPeriod;
                case BEGINNING_OF_PERIOD -> Messages.LabelARVABeginningOfPeriod;
            };
        }
    }

    public record ARVAData(Money currentValue, Money reservedAmount, Money arvaBasis, double realReturn,
                    int remainingYears, PaymentTiming paymentTiming, Money annualWithdrawal, Money monthlyWithdrawal,
                    double withdrawalRate, Money externalAnnualIncome, Money totalAnnualBudget,
                    Money totalMonthlyBudget)
    {
    }

    private record EditableRow(ColoredLabel valueLabel, Text input)
    {
    }

    private record ReadOnlyRow(Label rowLabel, ColoredLabel valueLabel)
    {
    }

    private abstract static class MoneyWidgetConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private final Dashboard.Config configurationKey;
        private final String label;
        private Money money;

        protected MoneyWidgetConfig(WidgetDelegate<?> delegate, Dashboard.Config configurationKey, String label)
        {
            this.delegate = delegate;
            this.configurationKey = configurationKey;
            this.label = label;

            String value = delegate.getWidget().getConfiguration().get(configurationKey.name());
            if (value != null && !value.isEmpty())
            {
                try
                {
                    long amount = Long.parseLong(value);
                    this.money = Money.of(delegate.getClient().getBaseCurrency(), amount);
                }
                catch (NumberFormatException e)
                {
                    this.money = null;
                }
            }
        }

        protected Money getMoney()
        {
            return money != null ? money : Money.of(delegate.getClient().getBaseCurrency(), 0);
        }

        protected void setMoney(Money money)
        {
            this.money = money;
            delegate.getWidget().getConfiguration().put(configurationKey.name(), String.valueOf(money.getAmount()));
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            var currency = delegate.getClient().getBaseCurrency();
            String display = Values.MoneyShort.format(getMoney(), currency);
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                            new LabelOnly(MessageFormat.format(Messages.LabelColonSeparated, label, display)));

            manager.add(new SimpleAction(label + "...", a -> { //$NON-NLS-1$
                var initial = Values.Amount.format(getMoney().getAmount());
                var dialog = new InputDialog(Display.getCurrent().getActiveShell(), label, label, initial, null);

                if (dialog.open() != Window.OK)
                    return;

                try
                {
                    var converter = new StringToCurrencyConverter(Values.Amount);
                    long amount = converter.convert(dialog.getValue());
                    setMoney(Money.of(currency, amount));
                }
                catch (IllegalArgumentException ignore)
                {
                    // ignore invalid input
                }
            }));
        }

        @Override
        public String getLabel()
        {
            String display = Values.MoneyShort.format(getMoney(), delegate.getClient().getBaseCurrency());
            return MessageFormat.format(Messages.LabelColonSeparated, label, display);
        }
    }

    private static class ReservedAmountConfig extends MoneyWidgetConfig
    {
        public ReservedAmountConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Dashboard.Config.ARVA_RESERVED_AMOUNT, Messages.LabelARVAReservedAmount);
        }

        public Money getReservedAmount()
        {
            return getMoney();
        }

        public void setReservedAmount(Money money)
        {
            setMoney(money);
        }
    }

    private static class ExternalAnnualIncomeConfig extends MoneyWidgetConfig
    {
        public ExternalAnnualIncomeConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Dashboard.Config.ARVA_EXTERNAL_ANNUAL_INCOME, Messages.LabelARVAExternalAnnualIncome);
        }

        public Money getExternalAnnualIncome()
        {
            return getMoney();
        }

        public void setExternalAnnualIncome(Money money)
        {
            setMoney(money);
        }
    }

    private static class RealReturnConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private Double realReturn;

        public RealReturnConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String value = delegate.getWidget().getConfiguration().get(Dashboard.Config.ARVA_REAL_RETURN.name());
            if (value != null && !value.isEmpty())
            {
                try
                {
                    realReturn = Double.parseDouble(value);
                }
                catch (NumberFormatException e)
                {
                    realReturn = null;
                }
            }
        }

        public double getRealReturn()
        {
            return realReturn != null ? realReturn : DEFAULT_REAL_RETURN;
        }

        public void setRealReturn(double realReturn)
        {
            this.realReturn = realReturn;
            delegate.getWidget().getConfiguration().put(Dashboard.Config.ARVA_REAL_RETURN.name(),
                            String.valueOf(realReturn));
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            String display = Values.Percent2.format(getRealReturn());
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(
                            MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelARVARealReturn, display)));

            manager.add(new SimpleAction(Messages.LabelARVARealReturn + "...", a -> { //$NON-NLS-1$
                var dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.LabelARVARealReturn,
                                Messages.LabelARVARealReturn, Values.Percent.format(getRealReturn()), null);

                if (dialog.open() != Window.OK)
                    return;

                try
                {
                    setRealReturn(parsePercent(dialog.getValue()));
                }
                catch (IllegalArgumentException ignore)
                {
                    // ignore invalid input
                }
            }));
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelARVARealReturn,
                            Values.Percent2.format(getRealReturn()));
        }
    }

    private static class RemainingYearsConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private Integer remainingYears;

        public RemainingYearsConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String value = delegate.getWidget().getConfiguration().get(Dashboard.Config.ARVA_REMAINING_YEARS.name());
            if (value != null && !value.isEmpty())
            {
                try
                {
                    remainingYears = Integer.parseInt(value);
                }
                catch (NumberFormatException e)
                {
                    remainingYears = null;
                }
            }
        }

        public int getRemainingYears()
        {
            return remainingYears != null && remainingYears > 0 ? remainingYears : DEFAULT_REMAINING_YEARS;
        }

        public void setRemainingYears(int remainingYears)
        {
            this.remainingYears = Math.max(1, remainingYears);
            delegate.getWidget().getConfiguration().put(Dashboard.Config.ARVA_REMAINING_YEARS.name(),
                            String.valueOf(this.remainingYears));
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            String display = String.valueOf(getRemainingYears());
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(MessageFormat
                            .format(Messages.LabelColonSeparated, Messages.LabelARVARemainingYears, display)));

            manager.add(new SimpleAction(Messages.LabelARVARemainingYears + "...", a -> { //$NON-NLS-1$
                var dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.LabelARVARemainingYears,
                                Messages.LabelARVARemainingYears, display, null);

                if (dialog.open() != Window.OK)
                    return;

                try
                {
                    setRemainingYears(Integer.parseInt(dialog.getValue()));
                }
                catch (NumberFormatException ignore)
                {
                    // ignore invalid input
                }
            }));
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelARVARemainingYears,
                            String.valueOf(getRemainingYears()));
        }
    }

    private static class PaymentTimingConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private PaymentTiming paymentTiming;

        public PaymentTimingConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String value = delegate.getWidget().getConfiguration().get(Dashboard.Config.ARVA_PAYMENT_TIMING.name());
            if (value != null && !value.isEmpty())
            {
                try
                {
                    paymentTiming = PaymentTiming.valueOf(value);
                }
                catch (IllegalArgumentException e)
                {
                    paymentTiming = null;
                }
            }
        }

        public PaymentTiming getPaymentTiming()
        {
            return paymentTiming != null ? paymentTiming : PaymentTiming.BEGINNING_OF_PERIOD;
        }

        public void setPaymentTiming(PaymentTiming paymentTiming)
        {
            this.paymentTiming = paymentTiming;
            delegate.getWidget().getConfiguration().put(Dashboard.Config.ARVA_PAYMENT_TIMING.name(),
                            paymentTiming.name());
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            var subMenu = new MenuManager(Messages.LabelARVAPaymentTiming);
            addPaymentTimingActions(subMenu);
            manager.add(subMenu);
        }

        public void addPaymentTimingActions(IMenuManager manager)
        {
            for (PaymentTiming value : PaymentTiming.values())
            {
                var action = new SimpleAction(value.getLabel(), a -> setPaymentTiming(value));
                action.setChecked(getPaymentTiming() == value);
                manager.add(action);
            }
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelARVAPaymentTiming,
                            getPaymentTiming().getLabel());
        }
    }

    private Composite container;
    private Label title;
    private ColoredLabel currentValueLabel;
    private ColoredLabel reservedAmountLabel;
    private Text reservedAmountInput;
    private ColoredLabel arvaBasisLabel;
    private ColoredLabel realReturnLabel;
    private Text realReturnInput;
    private ColoredLabel remainingYearsLabel;
    private Text remainingYearsInput;
    private ColoredLabel paymentTimingLabel;
    private ColoredLabel annualWithdrawalLabel;
    private ColoredLabel monthlyWithdrawalLabel;
    private ColoredLabel withdrawalRateLabel;
    private ColoredLabel externalAnnualIncomeLabel;
    private Text externalAnnualIncomeInput;
    private Label totalAnnualBudgetRowLabel;
    private ColoredLabel totalAnnualBudgetLabel;
    private Label totalMonthlyBudgetRowLabel;
    private ColoredLabel totalMonthlyBudgetLabel;

    public ARVAWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new RealReturnConfig(this));
        addConfig(new RemainingYearsConfig(this));
        addConfig(new ReservedAmountConfig(this));
        addConfig(new ExternalAnnualIncomeConfig(this));
        addConfig(new PaymentTimingConfig(this));
        addConfig(new DataSeriesConfig(this, false));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 2).spacing(3, 1).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(Colors.theme().defaultBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        InfoToolTip.attach(title, Messages.TooltipARVAWidget);
        GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(title);

        currentValueLabel = createReadOnlyRow(Messages.LabelARVACurrentValue, Messages.TooltipARVACurrentValue)
                        .valueLabel();

        EditableRow reservedAmountRow = createEditableRow(Messages.LabelARVAReservedAmount,
                        Messages.TooltipARVAReservedAmount, this::commitReservedAmount,
                        this::cancelReservedAmountEditing);
        reservedAmountLabel = reservedAmountRow.valueLabel();
        reservedAmountInput = reservedAmountRow.input();

        arvaBasisLabel = createReadOnlyRow(Messages.LabelARVABasis, Messages.TooltipARVABasis).valueLabel();

        EditableRow realReturnRow = createEditableRow(Messages.LabelARVARealReturn, Messages.TooltipARVARealReturn,
                        this::commitRealReturn, this::cancelRealReturnEditing);
        realReturnLabel = realReturnRow.valueLabel();
        realReturnInput = realReturnRow.input();

        EditableRow remainingYearsRow = createEditableRow(Messages.LabelARVARemainingYears,
                        Messages.TooltipARVARemainingYears, this::commitRemainingYears,
                        this::cancelRemainingYearsEditing);
        remainingYearsLabel = remainingYearsRow.valueLabel();
        remainingYearsInput = remainingYearsRow.input();

        paymentTimingLabel = createReadOnlyRow(Messages.LabelARVAPaymentTiming,
                        Messages.TooltipARVAPaymentTiming).valueLabel();
        paymentTimingLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                togglePaymentTiming();
            }
        });

        ReadOnlyRow annualWithdrawalRow = createReadOnlyRow(Messages.LabelARVAAnnualBudget,
                        Messages.TooltipARVAAnnualBudget);
        annualWithdrawalLabel = annualWithdrawalRow.valueLabel();
        monthlyWithdrawalLabel = createReadOnlyRow(Messages.LabelARVAMonthlyBudget, Messages.TooltipARVAMonthlyBudget)
                        .valueLabel();
        withdrawalRateLabel = createReadOnlyRow(Messages.LabelARVAWithdrawalRate, Messages.TooltipARVAWithdrawalRate)
                        .valueLabel();

        EditableRow externalIncomeRow = createEditableRow(Messages.LabelARVAExternalAnnualIncome,
                        Messages.TooltipARVAExternalAnnualIncome, this::commitExternalAnnualIncome,
                        this::cancelExternalAnnualIncomeEditing);
        externalAnnualIncomeLabel = externalIncomeRow.valueLabel();
        externalAnnualIncomeInput = externalIncomeRow.input();

        ReadOnlyRow totalAnnualBudgetRow = createReadOnlyRow(Messages.LabelARVATotalAnnualBudget,
                        Messages.TooltipARVATotalAnnualBudget);
        totalAnnualBudgetRowLabel = totalAnnualBudgetRow.rowLabel();
        totalAnnualBudgetLabel = totalAnnualBudgetRow.valueLabel();
        totalAnnualBudgetRowLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        totalAnnualBudgetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        ReadOnlyRow totalMonthlyBudgetRow = createReadOnlyRow(Messages.LabelARVATotalMonthlyBudget,
                        Messages.TooltipARVATotalMonthlyBudget);
        totalMonthlyBudgetRowLabel = totalMonthlyBudgetRow.rowLabel();
        totalMonthlyBudgetLabel = totalMonthlyBudgetRow.valueLabel();
        totalMonthlyBudgetRowLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        totalMonthlyBudgetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);

        resetInputsFromConfig();

        Listener clickOutsideCancelListener = this::handleClickOutsideActiveEditor;
        container.getDisplay().addFilter(SWT.MouseDown, clickOutsideCancelListener);
        container.addDisposeListener(
                        e -> container.getDisplay().removeFilter(SWT.MouseDown, clickOutsideCancelListener));

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<ARVAData> getUpdateTask()
    {
        return () -> {
            String currency = getDashboardData().getClient().getBaseCurrency();
            Money currentValue = null;
            var dataSeries = get(DataSeriesConfig.class).getDataSeries();
            if (dataSeries != null)
            {
                var now = LocalDate.now();
                var index = getDashboardData().calculate(dataSeries, Interval.of(now.minusYears(1), now));
                var totals = index.getTotals();
                if (totals.length > 0)
                    currentValue = Money.of(index.getCurrency(), totals[totals.length - 1]);
            }

            Money reservedAmount = get(ReservedAmountConfig.class).getReservedAmount();
            Money externalAnnualIncome = get(ExternalAnnualIncomeConfig.class).getExternalAnnualIncome();
            double realReturn = get(RealReturnConfig.class).getRealReturn();
            int remainingYears = get(RemainingYearsConfig.class).getRemainingYears();
            PaymentTiming paymentTiming = get(PaymentTimingConfig.class).getPaymentTiming();

            long currentAmount = currentValue != null ? currentValue.getAmount() : 0;
            long basisAmount = Math.max(0, currentAmount - Math.max(0, reservedAmount.getAmount()));
            long annualWithdrawalAmount = calculateAnnualWithdrawal(basisAmount, realReturn, remainingYears,
                            paymentTiming);
            long monthlyWithdrawalAmount = calculateMonthlyWithdrawal(basisAmount, realReturn, remainingYears,
                            paymentTiming);
            long totalAnnualBudgetAmount = annualWithdrawalAmount + externalAnnualIncome.getAmount();
            long totalMonthlyBudgetAmount = monthlyWithdrawalAmount + calculateMonthlyEquivalent(externalAnnualIncome);

            return new ARVAData(currentValue, reservedAmount, Money.of(currency, basisAmount), realReturn,
                            remainingYears, paymentTiming, Money.of(currency, annualWithdrawalAmount),
                            Money.of(currency, monthlyWithdrawalAmount),
                            calculateWithdrawalRate(annualWithdrawalAmount, basisAmount), externalAnnualIncome,
                            Money.of(currency, totalAnnualBudgetAmount), Money.of(currency, totalMonthlyBudgetAmount));
        };
    }

    static double calculateArvaFactor(double annualReturn, int remainingYears)
    {
        if (remainingYears <= 0)
            return 0.0;

        if (Math.abs(annualReturn) < EPSILON_RETURN)
            return 1.0 / remainingYears;

        double discount = 1.0 - Math.pow(1.0 + annualReturn, -remainingYears);
        if (Math.abs(discount) < EPSILON_RETURN)
            return 0.0;

        return annualReturn / discount;
    }

    static long calculateAnnualWithdrawal(long basisAmount, double annualReturn, int remainingYears, PaymentTiming timing)
    {
        if (basisAmount <= 0 || remainingYears <= 0)
            return 0;

        double factor = calculatePeriodicWithdrawalFactor(annualReturn, remainingYears, timing);

        return Math.max(0, Math.round(basisAmount * factor));
    }

    static long calculateMonthlyWithdrawal(long basisAmount, double annualReturn, int remainingYears,
                    PaymentTiming timing)
    {
        if (basisAmount <= 0 || remainingYears <= 0)
            return 0;

        double monthlyRate = calculateMonthlyRate(annualReturn);
        int months = remainingYears * 12;
        double factor = calculatePeriodicWithdrawalFactor(monthlyRate, months, timing);

        return Math.max(0, Math.round(basisAmount * factor));
    }

    static double calculateMonthlyRate(double annualReturn)
    {
        if (annualReturn <= -1.0)
            return Double.NaN;

        return Math.pow(1.0 + annualReturn, 1.0 / 12.0) - 1.0;
    }

    static double calculatePeriodicWithdrawalFactor(double periodicRate, int numberOfPeriods, PaymentTiming timing)
    {
        if (numberOfPeriods <= 0 || Double.isNaN(periodicRate) || Double.isInfinite(periodicRate))
            return 0.0;

        if (Math.abs(periodicRate) < EPSILON_RETURN)
            return 1.0 / numberOfPeriods;

        double discount = 1.0 - Math.pow(1.0 + periodicRate, -numberOfPeriods);
        if (Math.abs(discount) < EPSILON_RETURN || Double.isNaN(discount) || Double.isInfinite(discount))
            return 0.0;

        double factor = periodicRate / discount;
        if (timing == PaymentTiming.BEGINNING_OF_PERIOD)
        {
            double denominator = 1.0 + periodicRate;
            if (Math.abs(denominator) < EPSILON_RETURN)
                return 0.0;
            factor /= denominator;
        }

        return factor;
    }

    static long calculateMonthlyEquivalent(Money annualAmount)
    {
        return Math.round(annualAmount.getAmount() / 12.0);
    }

    static double calculateWithdrawalRate(long annualWithdrawal, long basisAmount)
    {
        return basisAmount > 0 ? (double) annualWithdrawal / basisAmount : Double.NaN;
    }

    private static double parsePercent(String input)
    {
        var text = input.replace("%", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
        var converter = new StringToCurrencyConverter(Values.WeightPercent, true);
        long percentage = converter.convert(text);
        return (percentage / Values.WeightPercent.divider()) / 100.0;
    }

    private Label createRowLabel(String labelText, String tooltip)
    {
        new Label(container, SWT.NONE);
        Label rowLabel = new Label(container, SWT.NONE);
        rowLabel.setText(MessageFormat.format(Messages.LabelColonSeparated, labelText, "")); //$NON-NLS-1$
        rowLabel.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(rowLabel);
        if (tooltip != null)
            InfoToolTip.attach(rowLabel, tooltip);
        return rowLabel;
    }

    private ReadOnlyRow createReadOnlyRow(String labelText, String tooltip)
    {
        Label rowLabel = createRowLabel(labelText, tooltip);

        ColoredLabel valueLabel = new ColoredLabel(container, SWT.RIGHT);
        valueLabel.setBackground(Colors.theme().defaultBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(valueLabel);
        if (tooltip != null)
            InfoToolTip.attach(valueLabel, tooltip);

        return new ReadOnlyRow(rowLabel, valueLabel);
    }

    private EditableRow createEditableRow(String rowLabelText, String toolTip, Runnable onCommit, Runnable onCancel)
    {
        createRowLabel(rowLabelText, toolTip);

        ColoredLabel valueLabel = new ColoredLabel(container, SWT.RIGHT);
        valueLabel.setBackground(Colors.theme().defaultBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(valueLabel);
        if (toolTip != null)
            InfoToolTip.attach(valueLabel, toolTip);

        Text input = new Text(container, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(input);
        input.setVisible(false);
        ((GridData) input.getLayoutData()).exclude = true;

        valueLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                showInput(valueLabel, input);
            }
        });

        input.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                if (!input.isVisible())
                    return;
                onCancel.run();
                showLabel(valueLabel, input);
            }
        });

        input.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
                {
                    onCommit.run();
                    showLabel(valueLabel, input);
                }
                else if (e.keyCode == SWT.ESC)
                {
                    onCancel.run();
                    showLabel(valueLabel, input);
                }
            }
        });

        return new EditableRow(valueLabel, input);
    }

    private void commitReservedAmount()
    {
        commitMoneyField(reservedAmountInput, reservedAmountLabel,
                        get(ReservedAmountConfig.class).getReservedAmount(),
                        value -> get(ReservedAmountConfig.class).setReservedAmount(value));
    }

    private void commitExternalAnnualIncome()
    {
        commitMoneyField(externalAnnualIncomeInput, externalAnnualIncomeLabel,
                        get(ExternalAnnualIncomeConfig.class).getExternalAnnualIncome(),
                        value -> get(ExternalAnnualIncomeConfig.class).setExternalAnnualIncome(value));
    }

    private void commitMoneyField(Text input, ColoredLabel label, Money currentValue, Consumer<Money> setter)
    {
        String currency = getDashboardData().getClient().getBaseCurrency();

        try
        {
            var converter = new StringToCurrencyConverter(Values.Amount);
            Long amount = converter.convert(input.getText());
            Money newValue = Money.of(currency, amount);

            if (!currentValue.equals(newValue))
                setter.accept(newValue);

            label.setText(Values.MoneyShort.format(newValue, currency));
            input.setText(Values.Amount.format(newValue.getAmount()));
        }
        catch (IllegalArgumentException e)
        {
            label.setText(Values.MoneyShort.format(currentValue, currency));
            input.setText(Values.Amount.format(currentValue.getAmount()));
        }
    }

    private void commitRealReturn()
    {
        double currentValue = get(RealReturnConfig.class).getRealReturn();

        try
        {
            double newValue = parsePercent(realReturnInput.getText());
            if (Double.compare(currentValue, newValue) != 0)
                get(RealReturnConfig.class).setRealReturn(newValue);

            realReturnLabel.setText(Values.Percent2.format(newValue));
            realReturnInput.setText(Values.Percent.format(newValue));
        }
        catch (IllegalArgumentException e)
        {
            realReturnLabel.setText(Values.Percent2.format(currentValue));
            realReturnInput.setText(Values.Percent.format(currentValue));
        }
    }

    private void commitRemainingYears()
    {
        int currentValue = get(RemainingYearsConfig.class).getRemainingYears();

        try
        {
            int newValue = Math.max(1, Integer.parseInt(remainingYearsInput.getText()));
            if (currentValue != newValue)
                get(RemainingYearsConfig.class).setRemainingYears(newValue);

            remainingYearsLabel.setText(String.valueOf(newValue));
            remainingYearsInput.setText(String.valueOf(newValue));
        }
        catch (NumberFormatException e)
        {
            remainingYearsLabel.setText(String.valueOf(currentValue));
            remainingYearsInput.setText(String.valueOf(currentValue));
        }
    }

    private void cancelReservedAmountEditing()
    {
        resetMoneyInput(reservedAmountInput, get(ReservedAmountConfig.class).getReservedAmount());
    }

    private void cancelExternalAnnualIncomeEditing()
    {
        resetMoneyInput(externalAnnualIncomeInput, get(ExternalAnnualIncomeConfig.class).getExternalAnnualIncome());
    }

    private void cancelRealReturnEditing()
    {
        realReturnInput.setText(Values.Percent.format(get(RealReturnConfig.class).getRealReturn()));
    }

    private void cancelRemainingYearsEditing()
    {
        remainingYearsInput.setText(String.valueOf(get(RemainingYearsConfig.class).getRemainingYears()));
    }

    private void togglePaymentTiming()
    {
        var config = get(PaymentTimingConfig.class);
        config.setPaymentTiming(config.getPaymentTiming() == PaymentTiming.END_OF_PERIOD
                        ? PaymentTiming.BEGINNING_OF_PERIOD
                        : PaymentTiming.END_OF_PERIOD);
    }

    private void resetInputsFromConfig()
    {
        String currency = getDashboardData().getClient().getBaseCurrency();
        var reservedAmount = get(ReservedAmountConfig.class).getReservedAmount();
        reservedAmountLabel.setText(Values.MoneyShort.format(reservedAmount, currency));
        resetMoneyInput(reservedAmountInput, reservedAmount);

        var externalAnnualIncome = get(ExternalAnnualIncomeConfig.class).getExternalAnnualIncome();
        externalAnnualIncomeLabel.setText(Values.MoneyShort.format(externalAnnualIncome, currency));
        resetMoneyInput(externalAnnualIncomeInput, externalAnnualIncome);

        double realReturn = get(RealReturnConfig.class).getRealReturn();
        realReturnLabel.setText(Values.Percent2.format(realReturn));
        realReturnInput.setText(Values.Percent.format(realReturn));

        int remainingYears = get(RemainingYearsConfig.class).getRemainingYears();
        remainingYearsLabel.setText(String.valueOf(remainingYears));
        remainingYearsInput.setText(String.valueOf(remainingYears));
    }

    private void resetMoneyInput(Text input, Money money)
    {
        input.setText(Values.Amount.format(money.getAmount()));
    }

    private void showInput(ColoredLabel label, Text input)
    {
        label.setVisible(false);
        ((GridData) label.getLayoutData()).exclude = true;

        input.setVisible(true);
        ((GridData) input.getLayoutData()).exclude = false;

        container.layout(true);
        input.setFocus();
        input.selectAll();
    }

    private void showLabel(ColoredLabel label, Text input)
    {
        input.setVisible(false);
        ((GridData) input.getLayoutData()).exclude = true;

        label.setVisible(true);
        ((GridData) label.getLayoutData()).exclude = false;

        container.layout(true);
    }

    private void handleClickOutsideActiveEditor(Event event)
    {
        if (!(event.widget instanceof Control clickedControl))
            return;

        if (reservedAmountInput.isVisible() && !isSameOrDescendant(clickedControl, reservedAmountInput))
        {
            cancelReservedAmountEditing();
            showLabel(reservedAmountLabel, reservedAmountInput);
        }

        if (realReturnInput.isVisible() && !isSameOrDescendant(clickedControl, realReturnInput))
        {
            cancelRealReturnEditing();
            showLabel(realReturnLabel, realReturnInput);
        }

        if (remainingYearsInput.isVisible() && !isSameOrDescendant(clickedControl, remainingYearsInput))
        {
            cancelRemainingYearsEditing();
            showLabel(remainingYearsLabel, remainingYearsInput);
        }

        if (externalAnnualIncomeInput.isVisible() && !isSameOrDescendant(clickedControl, externalAnnualIncomeInput))
        {
            cancelExternalAnnualIncomeEditing();
            showLabel(externalAnnualIncomeLabel, externalAnnualIncomeInput);
        }
    }

    private boolean isSameOrDescendant(Control control, Control possibleParent)
    {
        Control current = control;
        while (current != null)
        {
            if (current == possibleParent)
                return true;
            current = current.getParent();
        }
        return false;
    }

    @Override
    public void update(ARVAData data)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        String currency = getDashboardData().getClient().getBaseCurrency();

        currentValueLabel.setText(data.currentValue() != null ? Values.MoneyShort.format(data.currentValue(), currency)
                        : "-"); //$NON-NLS-1$
        reservedAmountLabel.setText(Values.MoneyShort.format(data.reservedAmount(), currency));
        arvaBasisLabel.setText(Values.MoneyShort.format(data.arvaBasis(), currency));
        arvaBasisLabel.setTextColor(data.arvaBasis().isZero() ? Colors.theme().redForeground()
                        : Colors.theme().defaultForeground());
        realReturnLabel.setText(Values.Percent2.format(data.realReturn()));
        realReturnLabel.setTextColor(data.realReturn() < 0 ? Colors.theme().redForeground()
                        : Colors.theme().greenForeground());
        remainingYearsLabel.setText(String.valueOf(data.remainingYears()));
        paymentTimingLabel.setText(data.paymentTiming().getLabel());
        annualWithdrawalLabel.setText(Values.MoneyShort.format(data.annualWithdrawal(), currency));
        monthlyWithdrawalLabel.setText(Values.MoneyShort.format(data.monthlyWithdrawal(), currency));
        withdrawalRateLabel.setText(Double.isNaN(data.withdrawalRate()) ? "-" //$NON-NLS-1$
                        : Values.Percent2.format(data.withdrawalRate()));
        externalAnnualIncomeLabel.setText(Values.MoneyShort.format(data.externalAnnualIncome(), currency));
        totalAnnualBudgetLabel.setText(Values.MoneyShort.format(data.totalAnnualBudget(), currency));
        totalMonthlyBudgetLabel.setText(Values.MoneyShort.format(data.totalMonthlyBudget(), currency));

        reservedAmountInput.setText(Values.Amount.format(data.reservedAmount().getAmount()));
        realReturnInput.setText(Values.Percent.format(data.realReturn()));
        remainingYearsInput.setText(String.valueOf(data.remainingYears()));
        externalAnnualIncomeInput.setText(Values.Amount.format(data.externalAnnualIncome().getAmount()));

        container.layout();
    }
}
