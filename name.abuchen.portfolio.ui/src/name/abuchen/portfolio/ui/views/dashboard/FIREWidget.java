package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class FIREWidget extends WidgetDelegate<FIREWidget.FIREData>
{
    private static final double DEFAULT_RETURNS = 0.07;
    private static final double MAX_TIME_TO_FIRE_YEARS = 50;
    private static final double EPSILON_MONTHS = 0.01;
    private static final String DEFAULT_FIRE_NUMBER_INPUT = "1500000"; //$NON-NLS-1$
    private static final String DEFAULT_MONTHLY_SAVINGS_INPUT = "5000"; //$NON-NLS-1$

    public record FIREData(Money fireNumber, Money currentValue, Money monthlySavings, double twror, double timeToFire,
                    LocalDate targetDate)
    {
        public FIREData()
        {
            this(null, null, null, Double.NaN, Double.NaN, null);
        }

        public Money getFireNumber()
        {
            return fireNumber;
        }

        public Money getCurrentValue()
        {
            return currentValue;
        }

        public Money getMonthlySavings()
        {
            return monthlySavings;
        }

        public double getTwror()
        {
            return twror;
        }

        public double getTimeToFire()
        {
            return timeToFire;
        }

        public LocalDate getTargetDate()
        {
            return targetDate;
        }
    }

    private static class FIRENumberConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private Money fireNumber;

        public FIRENumberConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String fireNumberStr = delegate.getWidget().getConfiguration().get(Dashboard.Config.FIRE_NUMBER.name());
            if (fireNumberStr != null && !fireNumberStr.isEmpty())
            {
                try
                {
                    long amount = Long.parseLong(fireNumberStr);
                    this.fireNumber = Money.of(delegate.getClient().getBaseCurrency(), amount);
                }
                catch (NumberFormatException e)
                {
                    this.fireNumber = null; // No default, show placeholder
                }
            }
            else
            {
                this.fireNumber = null; // No default, show placeholder
            }
        }

        public Money getFireNumber()
        {
            return fireNumber;
        }

        public void setFireNumber(Money fireNumber)
        {
            this.fireNumber = fireNumber;
            delegate.getWidget().getConfiguration().put(Dashboard.Config.FIRE_NUMBER.name(),
                            String.valueOf(fireNumber.getAmount()));
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            String display = fireNumber != null
                            ? Values.MoneyShort.format(fireNumber,
                                            delegate.getClient().getBaseCurrency())
                            : Messages.LabelFIREClickToSet;
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                            new LabelOnly(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIRENumber,
                                            display)));
        }

        @Override
        public String getLabel()
        {
            String display = fireNumber != null
                            ? Values.MoneyShort.format(fireNumber,
                                            delegate.getClient().getBaseCurrency())
                            : Messages.LabelFIREClickToSet;
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIRENumber, display);
        }

    }

    private static class FIREMonthlySavingsConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private Money monthlySavings;

        public FIREMonthlySavingsConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String monthlySavingsStr = delegate.getWidget().getConfiguration()
                            .get(Dashboard.Config.FIRE_MONTHLY_SAVINGS.name());
            if (monthlySavingsStr != null && !monthlySavingsStr.isEmpty())
            {
                try
                {
                    long amount = Long.parseLong(monthlySavingsStr);
                    this.monthlySavings = Money.of(delegate.getClient().getBaseCurrency(), amount);
                }
                catch (NumberFormatException e)
                {
                    this.monthlySavings = null;
                }
            }
            else
            {
                this.monthlySavings = null;
            }
        }

        public Money getMonthlySavings()
        {
            return monthlySavings;
        }

        public void setMonthlySavings(Money monthlySavings)
        {
            this.monthlySavings = monthlySavings;
            delegate.getWidget().getConfiguration().put(Dashboard.Config.FIRE_MONTHLY_SAVINGS.name(),
                            String.valueOf(monthlySavings.getAmount()));
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            String display = monthlySavings != null
                            ? Values.MoneyShort.format(monthlySavings,
                                            delegate.getClient().getBaseCurrency())
                            : Messages.LabelFIREClickToSet;
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                            new LabelOnly(MessageFormat.format(Messages.LabelColonSeparated,
                                            Messages.LabelFIREMonthlySavings, display)));
        }

        @Override
        public String getLabel()
        {
            String display = monthlySavings != null
                            ? Values.MoneyShort.format(monthlySavings,
                                            delegate.getClient().getBaseCurrency())
                            : Messages.LabelFIREClickToSet;
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIREMonthlySavings, display);
        }

    }

    private static class FIREReturnsConfig implements WidgetConfig
    {
        private final WidgetDelegate<?> delegate;
        private Double returns;

        public FIREReturnsConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String returnsStr = delegate.getWidget().getConfiguration().get(Dashboard.Config.FIRE_RETURNS.name());
            if (returnsStr != null && !returnsStr.isEmpty())
            {
                try
                {
                    this.returns = Double.parseDouble(returnsStr);
                }
                catch (NumberFormatException e)
                {
                    this.returns = null;
                }
            }
            else
            {
                this.returns = null;
            }
        }

        public Double getReturns()
        {
            return returns;
        }

        public void setReturns(Double returns)
        {
            this.returns = returns;
            delegate.getWidget().getConfiguration().put(Dashboard.Config.FIRE_RETURNS.name(), String.valueOf(returns));
            delegate.update();
            delegate.getClient().touch();
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            String display = returns != null ? Values.Percent2.format(returns) : Messages.LabelFIREClickToSet;
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                            new LabelOnly(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIREReturns,
                                            display)));
        }

        @Override
        public String getLabel()
        {
            String display = returns != null ? Values.Percent2.format(returns) : Messages.LabelFIREClickToSet;
            return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIREReturns, display);
        }
    }

    private Composite container;
    private Label title;
    private ColoredLabel fireNumberLabel;
    private Text fireNumberInput;
    private ColoredLabel currentValueLabel;
    private ColoredLabel monthlySavingsLabel;
    private Text monthlySavingsInput;
    private ColoredLabel twrorLabel;
    private Text twrorInput;
    private ColoredLabel timeToFireLabel;
    private ColoredLabel targetDateLabel;
    private Listener clickOutsideCancelListener;

    public FIREWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new FIRENumberConfig(this));
        addConfig(new FIREMonthlySavingsConfig(this));
        addConfig(new FIREReturnsConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).spacing(3, 3).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(Colors.theme().defaultBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(title);

        // Current Net Worth (first row)
        new Label(container, SWT.NONE); // Empty sign column
        Label currentValueLbl = new Label(container, SWT.NONE);
        currentValueLbl.setText(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIRECurrentNetWorth,
                        "")); //$NON-NLS-1$
        currentValueLbl.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(currentValueLbl);

        currentValueLabel = new ColoredLabel(container, SWT.RIGHT);
        currentValueLabel.setBackground(Colors.theme().defaultBackground());
        currentValueLabel.setText("");
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(currentValueLabel);

        // FIRE Number (second row, editable when clicked)
        new Label(container, SWT.NONE); // Empty sign column
        Label fireNumberLbl = new Label(container, SWT.NONE);
        fireNumberLbl.setText(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIRENumber, "")); //$NON-NLS-1$
        fireNumberLbl.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(fireNumberLbl);

        // Create both label and text field, initially show only label
        fireNumberLabel = new ColoredLabel(container, SWT.RIGHT);
        fireNumberLabel.setBackground(Colors.theme().defaultBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(fireNumberLabel);

        fireNumberInput = new Text(container, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(fireNumberInput);
        fireNumberInput.setVisible(false);
        ((GridData) fireNumberInput.getLayoutData()).exclude = true;

        Money currentFireNumber = get(FIRENumberConfig.class).getFireNumber();
        String currency = getDashboardData().getClient().getBaseCurrency();
        if (currentFireNumber != null)
        {
            fireNumberLabel.setText(Values.MoneyShort.format(currentFireNumber, currency));
            fireNumberInput.setText(Values.Amount.format(currentFireNumber.getAmount()));
        }
        else
        {
            fireNumberLabel.setText(Messages.LabelFIREClickToSet);
            fireNumberInput.setText(DEFAULT_FIRE_NUMBER_INPUT); // Default for editing
        }

        // Click on label to edit
        fireNumberLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDown(MouseEvent e)
            {
                showInput(fireNumberLabel, fireNumberInput);
            }
        });

        // Focus lost - hide text input
        fireNumberInput.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                cancelFireNumberEditing();
                showLabel(fireNumberLabel, fireNumberInput);
            }
        });

        // Enter key - finish editing
        fireNumberInput.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
                {
                    commitFireNumber();
                    showLabel(fireNumberLabel, fireNumberInput);
                }
                else if (e.keyCode == SWT.ESC)
                {
                    cancelFireNumberEditing();
                    showLabel(fireNumberLabel, fireNumberInput);
                }
            }
        });

        // Est. Monthly Savings (editable when clicked)
        new Label(container, SWT.NONE); // Empty sign column
        Label monthlySavingsLbl = new Label(container, SWT.NONE);
        monthlySavingsLbl.setText(
                        MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIREMonthlySavings, "")); //$NON-NLS-1$
        monthlySavingsLbl.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(monthlySavingsLbl);

        // Create both label and text field, initially show only label
        monthlySavingsLabel = new ColoredLabel(container, SWT.RIGHT);
        monthlySavingsLabel.setBackground(Colors.theme().defaultBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(monthlySavingsLabel);

        monthlySavingsInput = new Text(container, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(monthlySavingsInput);
        monthlySavingsInput.setVisible(false);
        ((GridData) monthlySavingsInput.getLayoutData()).exclude = true;

        Money currentMonthlySavings = get(FIREMonthlySavingsConfig.class).getMonthlySavings();
        if (currentMonthlySavings != null)
        {
            monthlySavingsLabel.setText(Values.MoneyShort.format(currentMonthlySavings, currency));
            monthlySavingsInput.setText(Values.Amount.format(currentMonthlySavings.getAmount()));
        }
        else
        {
            monthlySavingsLabel.setText(Messages.LabelFIREClickToSet);
            monthlySavingsInput.setText(DEFAULT_MONTHLY_SAVINGS_INPUT); // Default $5000 for editing
        }

        // Click on label to edit
        monthlySavingsLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDown(MouseEvent e)
            {
                showInput(monthlySavingsLabel, monthlySavingsInput);
            }
        });

        // Focus lost - hide text input
        monthlySavingsInput.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                cancelMonthlySavingsEditing();
                showLabel(monthlySavingsLabel, monthlySavingsInput);
            }
        });

        // Enter key - finish editing
        monthlySavingsInput.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
                {
                    commitMonthlySavings();
                    showLabel(monthlySavingsLabel, monthlySavingsInput);
                }
                else if (e.keyCode == SWT.ESC)
                {
                    cancelMonthlySavingsEditing();
                    showLabel(monthlySavingsLabel, monthlySavingsInput);
                }
            }
        });

        // Est. Returns (editable when clicked)
        new Label(container, SWT.NONE); // Empty sign column
        Label twrorLbl = new Label(container, SWT.NONE);
        twrorLbl.setText(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIREReturns, "")); //$NON-NLS-1$
        twrorLbl.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(twrorLbl);

        // Create both label and text field, initially show only label
        twrorLabel = new ColoredLabel(container, SWT.RIGHT);
        twrorLabel.setBackground(Colors.theme().defaultBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(twrorLabel);

        twrorInput = new Text(container, SWT.BORDER | SWT.RIGHT);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(twrorInput);
        twrorInput.setVisible(false);
        ((GridData) twrorInput.getLayoutData()).exclude = true;

        Double currentReturns = get(FIREReturnsConfig.class).getReturns();
        if (currentReturns != null)
        {
            twrorLabel.setText(Values.Percent2.format(currentReturns));
            twrorInput.setText(Values.Percent.format(currentReturns));
        }
        else
        {
            twrorLabel.setText(Messages.LabelFIREClickToSet);
            twrorInput.setText(getDefaultReturnsInput());
        }

        // Click on label to edit
        twrorLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDown(MouseEvent e)
            {
                showInput(twrorLabel, twrorInput);
            }
        });

        // Focus lost - hide text input
        twrorInput.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                cancelReturnsEditing();
                showLabel(twrorLabel, twrorInput);
            }
        });

        // Enter key - finish editing
        twrorInput.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
                {
                    commitReturns();
                    showLabel(twrorLabel, twrorInput);
                }
                else if (e.keyCode == SWT.ESC)
                {
                    cancelReturnsEditing();
                    showLabel(twrorLabel, twrorInput);
                }
            }
        });

        // Time to FIRE
        new Label(container, SWT.NONE); // Empty sign column
        Label timeToFireLbl = new Label(container, SWT.NONE);
        timeToFireLbl.setText(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIRETimeToFIRE, "")); //$NON-NLS-1$
        timeToFireLbl.setBackground(container.getBackground());
        timeToFireLbl.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(timeToFireLbl);

        timeToFireLabel = new ColoredLabel(container, SWT.RIGHT);
        timeToFireLabel.setBackground(Colors.theme().defaultBackground());
        timeToFireLabel.setText("");
        timeToFireLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(timeToFireLabel);

        // FIRE Date
        new Label(container, SWT.NONE); // Empty sign column
        Label targetDateLbl = new Label(container, SWT.NONE);
        targetDateLbl.setText(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelFIRETargetDate, "")); //$NON-NLS-1$
        targetDateLbl.setBackground(container.getBackground());
        targetDateLbl.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(targetDateLbl);

        targetDateLabel = new ColoredLabel(container, SWT.RIGHT);
        targetDateLabel.setBackground(Colors.theme().defaultBackground());
        targetDateLabel.setText("");
        targetDateLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(targetDateLabel);

        clickOutsideCancelListener = this::handleClickOutsideActiveEditor;
        container.getDisplay().addFilter(SWT.MouseDown, clickOutsideCancelListener);
        container.addDisposeListener(e -> container.getDisplay().removeFilter(SWT.MouseDown, clickOutsideCancelListener));

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<FIREData> getUpdateTask()
    {
        return () -> {
            Money fireNumber = get(FIRENumberConfig.class).getFireNumber();

            // Calculate current portfolio value using the first (default) data
            // series
            Money currentValue = null;
            var availableSeries = getDashboardData().getDataSeriesSet().getAvailableSeries();
            if (!availableSeries.isEmpty())
            {
                // Use last 1 year for current value calculation
                LocalDate now = LocalDate.now();
                Interval interval = Interval.of(now.minusYears(1), now);
                PerformanceIndex index = getDashboardData().calculate(availableSeries.get(0), interval);

                long[] totals = index.getTotals();
                if (totals.length > 0)
                {
                    currentValue = Money.of(index.getCurrency(), totals[totals.length - 1]);
                }
            }

            // Get user input monthly savings and returns
            Money monthlySavings = get(FIREMonthlySavingsConfig.class).getMonthlySavings();
            Double userReturns = get(FIREReturnsConfig.class).getReturns();
            double twror = userReturns != null ? userReturns : Double.NaN;

            // Calculate years to FIRE and target date
            double timeToFire = Double.NaN;
            LocalDate targetDate = null;
            if (fireNumber != null && currentValue != null && monthlySavings != null && !Double.isNaN(twror))
            {
                timeToFire = calculateTimeToFIRE(currentValue.getAmount(), fireNumber.getAmount(),
                                monthlySavings.getAmount(), twror);

                if (timeToFire > 0 && timeToFire < MAX_TIME_TO_FIRE_YEARS - EPSILON_MONTHS / 12.0) // avoid setting a misleading capped date
                {
                    long daysToAdd = Math.round(timeToFire * 365.25);
                    targetDate = LocalDate.now().plusDays(daysToAdd);
                }
            }

            return new FIREData(fireNumber, currentValue, monthlySavings, twror, timeToFire, targetDate);
        };
    }

    static double calculateTimeToFIRE(long currentValue, long fireNumber, long monthlySavings,
                    double annualReturn)
    {
        if (currentValue >= fireNumber)
            return 0;

        if (monthlySavings < 0)
            return Double.POSITIVE_INFINITY;

        double monthlyReturn = Math.pow(1.0 + annualReturn, 1.0 / 12.0) - 1.0;

        if (monthlySavings == 0)
        {
            if (currentValue <= 0 || monthlyReturn <= 0)
                return Double.POSITIVE_INFINITY;

            double months = Math.log((double) fireNumber / currentValue) / Math.log(1 + monthlyReturn);
            return months / 12.0;
        }

        if (monthlyReturn <= 0)
        {
            // Simple linear calculation if no growth
            return (double) (fireNumber - currentValue) / (monthlySavings * 12);
        }

        // Future value formula: FV = PV × (1 + r)^n + PMT × [((1 + r)^n - 1) /
        // r]
        // This requires numerical solution since it can't be solved
        // algebraically
        double pv = currentValue;
        double fv = fireNumber;
        double pmt = monthlySavings;
        double r = monthlyReturn;

        // Use binary search to find n
        double low = 0;
        double high = MAX_TIME_TO_FIRE_YEARS * 12.0;

        while (high - low > EPSILON_MONTHS)
        {
            double mid = (low + high) / 2.0;
            double powerTerm = Math.pow(1 + r, mid);
            double calculatedFV = pv * powerTerm + pmt * ((powerTerm - 1) / r);

            if (calculatedFV < fv)
            {
                low = mid;
            }
            else
            {
                high = mid;
            }
        }

        return (low + high) / 2.0 / 12.0;
    }

    private void commitFireNumber()
    {
        commitMoneyField(fireNumberInput, fireNumberLabel, get(FIRENumberConfig.class).getFireNumber(),
                        newValue -> get(FIRENumberConfig.class).setFireNumber(newValue), DEFAULT_FIRE_NUMBER_INPUT);
    }

    private void commitMonthlySavings()
    {
        commitMoneyField(monthlySavingsInput, monthlySavingsLabel,
                        get(FIREMonthlySavingsConfig.class).getMonthlySavings(),
                        newValue -> get(FIREMonthlySavingsConfig.class).setMonthlySavings(newValue),
                        DEFAULT_MONTHLY_SAVINGS_INPUT);
    }

    private void cancelFireNumberEditing()
    {
        Money currentFireNumber = get(FIRENumberConfig.class).getFireNumber();
        if (currentFireNumber != null)
            fireNumberInput.setText(Values.Amount.format(currentFireNumber.getAmount()));
        else
            fireNumberInput.setText(DEFAULT_FIRE_NUMBER_INPUT);
    }

    private void cancelMonthlySavingsEditing()
    {
        Money currentMonthlySavings = get(FIREMonthlySavingsConfig.class).getMonthlySavings();
        if (currentMonthlySavings != null)
            monthlySavingsInput.setText(Values.Amount.format(currentMonthlySavings.getAmount()));
        else
            monthlySavingsInput.setText(DEFAULT_MONTHLY_SAVINGS_INPUT);
    }

    private void commitReturns()
    {
        Double currentReturns = get(FIREReturnsConfig.class).getReturns();

        try
        {
            String text = twrorInput.getText().replace("%", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
            StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.WeightPercent, true);
            long percentage = converter.convert(text);
            Double returns = (percentage / Values.WeightPercent.divider()) / 100.0;

            if (currentReturns == null || Double.compare(currentReturns, returns) != 0)
                get(FIREReturnsConfig.class).setReturns(returns);

            twrorLabel.setText(Values.Percent2.format(returns));
            twrorInput.setText(Values.Percent.format(returns));
        }
        catch (IllegalArgumentException e)
        {
            if (currentReturns != null)
            {
                twrorLabel.setText(Values.Percent2.format(currentReturns));
                twrorInput.setText(Values.Percent.format(currentReturns));
            }
            else
            {
                twrorLabel.setText(Messages.LabelFIREClickToSet);
                twrorInput.setText(getDefaultReturnsInput());
            }
        }
    }

    private void cancelReturnsEditing()
    {
        Double currentReturns = get(FIREReturnsConfig.class).getReturns();
        if (currentReturns != null)
            twrorInput.setText(Values.Percent.format(currentReturns));
        else
            twrorInput.setText(getDefaultReturnsInput());
    }

    private String getDefaultReturnsInput()
    {
        return Values.Percent.format(DEFAULT_RETURNS);
    }

    private void commitMoneyField(Text input, ColoredLabel label, Money currentValue, Consumer<Money> setter,
                    String defaultInput)
    {
        String currency = getDashboardData().getClient().getBaseCurrency();

        try
        {
            StringToCurrencyConverter converter = new StringToCurrencyConverter(Values.Amount);
            Long amount = converter.convert(input.getText());
            Money newValue = Money.of(currency, amount);

            if (currentValue == null || !currentValue.equals(newValue))
                setter.accept(newValue);

            label.setText(Values.MoneyShort.format(newValue, currency));
            input.setText(Values.Amount.format(newValue.getAmount()));
        }
        catch (IllegalArgumentException e)
        {
            if (currentValue != null)
            {
                label.setText(Values.MoneyShort.format(currentValue, currency));
                input.setText(Values.Amount.format(currentValue.getAmount()));
            }
            else
            {
                label.setText(Messages.LabelFIREClickToSet);
                input.setText(defaultInput);
            }
        }
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

        if (fireNumberInput.isVisible() && !isSameOrDescendant(clickedControl, fireNumberInput))
        {
            cancelFireNumberEditing();
            showLabel(fireNumberLabel, fireNumberInput);
        }

        if (monthlySavingsInput.isVisible() && !isSameOrDescendant(clickedControl, monthlySavingsInput))
        {
            cancelMonthlySavingsEditing();
            showLabel(monthlySavingsLabel, monthlySavingsInput);
        }

        if (twrorInput.isVisible() && !isSameOrDescendant(clickedControl, twrorInput))
        {
            cancelReturnsEditing();
            showLabel(twrorLabel, twrorInput);
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

    private boolean isTimeToFireCapped(FIREData data)
    {
        if (data.getCurrentValue() == null || data.getFireNumber() == null || data.getMonthlySavings() == null
                        || Double.isNaN(data.getTwror()) || data.getTimeToFire() < MAX_TIME_TO_FIRE_YEARS - EPSILON_MONTHS / 12.0)
            return false;

        long monthlySavings = data.getMonthlySavings().getAmount();
        if (monthlySavings <= 0)
            return false;

        double monthlyReturn = Math.pow(1.0 + data.getTwror(), 1.0 / 12.0) - 1.0;
        if (monthlyReturn <= 0)
            return false;

        double maxMonths = MAX_TIME_TO_FIRE_YEARS * 12.0;
        double powerTerm = Math.pow(1 + monthlyReturn, maxMonths);
        double projectedValue = data.getCurrentValue().getAmount() * powerTerm
                        + monthlySavings * ((powerTerm - 1) / monthlyReturn);

        return projectedValue < data.getFireNumber().getAmount();
    }

    @Override
    public void update(FIREData data)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        String currency = getDashboardData().getClient().getBaseCurrency();

        if (data.getCurrentValue() != null)
        {
            currentValueLabel.setText(Values.MoneyShort.format(data.getCurrentValue(), currency));
        }
        else
        {
            currentValueLabel.setText("-");
        }

        // Update monthly savings display only if user hasn't set a custom value
        Money userMonthlySavings = get(FIREMonthlySavingsConfig.class).getMonthlySavings();
        if (userMonthlySavings != null)
        {
            monthlySavingsLabel.setText(Values.MoneyShort.format(userMonthlySavings, currency));
            monthlySavingsLabel.setTextColor(userMonthlySavings.isNegative() ? Colors.theme().redForeground()
                            : Colors.theme().greenForeground());
        }
        else
        {
            monthlySavingsLabel.setText(Messages.LabelFIREClickToSet);
            monthlySavingsLabel.setTextColor(Colors.theme().defaultForeground());
        }

        // Update returns display only if user hasn't set a custom value
        Double userReturns = get(FIREReturnsConfig.class).getReturns();
        if (userReturns != null)
        {
            twrorLabel.setText(Values.Percent2.format(userReturns));
            twrorLabel.setTextColor(
                            userReturns < 0 ? Colors.theme().redForeground() : Colors.theme().greenForeground());
        }
        else
        {
            twrorLabel.setText(Messages.LabelFIREClickToSet);
            twrorLabel.setTextColor(Colors.theme().defaultForeground());
        }

        if (data.getFireNumber() == null || Double.isNaN(data.getTimeToFire()))
        {
            timeToFireLabel.setText("-");
            timeToFireLabel.setTextColor(Colors.theme().defaultForeground());
            targetDateLabel.setText("-");
            targetDateLabel.setTextColor(Colors.theme().defaultForeground());
        }
        else if (isTimeToFireCapped(data))
        {
            String cappedYears = MessageFormat.format(Messages.LabelMetricYearsFormatter, MAX_TIME_TO_FIRE_YEARS);
            timeToFireLabel.setText(cappedYears + "+"); //$NON-NLS-1$
            timeToFireLabel.setTextColor(Colors.theme().defaultForeground());
            targetDateLabel.setText("-");
            targetDateLabel.setTextColor(Colors.theme().defaultForeground());
        }
        else if (data.getTimeToFire() > 0 && data.getTimeToFire() < 100)
        {
            timeToFireLabel.setText(MessageFormat.format(Messages.LabelMetricYearsFormatter, data.getTimeToFire()));
            timeToFireLabel.setTextColor(Colors.theme().defaultForeground());

            if (data.getTargetDate() != null)
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                targetDateLabel.setText(data.getTargetDate().format(formatter));
                targetDateLabel.setTextColor(Colors.theme().defaultForeground());
            }
            else
            {
                targetDateLabel.setText("-");
            }
        }
        else if (data.getTimeToFire() == 0)
        {
            timeToFireLabel.setText(Messages.LabelFIREAchieved);
            timeToFireLabel.setTextColor(Colors.theme().greenForeground());
            targetDateLabel.setText(Messages.LabelToday);
            targetDateLabel.setTextColor(Colors.theme().greenForeground());
        }
        else
        {
            timeToFireLabel.setText("∞");
            timeToFireLabel.setTextColor(Colors.theme().redForeground());
            targetDateLabel.setText("-");
            targetDateLabel.setTextColor(Colors.theme().defaultForeground());
        }

        container.layout();
    }
}
