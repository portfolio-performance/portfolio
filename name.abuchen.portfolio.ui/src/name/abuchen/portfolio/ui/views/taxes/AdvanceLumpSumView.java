package name.abuchen.portfolio.ui.views.taxes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.advancelumpsum.AdvanceLumpSumCalculation;
import name.abuchen.portfolio.snapshot.advancelumpsum.AdvanceLumpSumCalculation.Item;
import name.abuchen.portfolio.snapshot.advancelumpsum.AdvanceLumpSumCalculation.Result;
import name.abuchen.portfolio.snapshot.advancelumpsum.BaseInterestRate;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.NumberVerifyListener;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TreeViewerCSVExporter;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.util.text.DecimalKeypadSupport;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.SafeTreeColumnLayout;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.TextUtil;

public class AdvanceLumpSumView extends AbstractFinanceView
{
    private enum TaxRate
    {
        WITHOUT_CHURCH_TAX(new BigDecimal("0.26375"), null), //$NON-NLS-1$
        CHURCH_TAX_8(new BigDecimal("0.278186"), new BigDecimal("0.08")), //$NON-NLS-1$ //$NON-NLS-2$
        CHURCH_TAX_9(new BigDecimal("0.279951"), new BigDecimal("0.09")); //$NON-NLS-1$ //$NON-NLS-2$

        private final BigDecimal rate;
        private final BigDecimal churchTax;

        TaxRate(BigDecimal rate, BigDecimal churchTax)
        {
            this.rate = rate;
            this.churchTax = churchTax;
        }

        String getLabel()
        {
            return churchTax == null ? MessageFormat.format(Messages.LabelTaxRateWithoutChurchTax, percent(rate))
                            : MessageFormat.format(Messages.LabelTaxRateWithChurchTax, percent(rate),
                                            percent(churchTax));
        }
    }

    private record PortfolioNode(Portfolio portfolio, List<Item> items, Money advanceLumpSum, Money taxableAmount,
                    Money usedLoss, Money usedExemption, Money tax)
    {
    }

    private static final String PREF_PREFIX = AdvanceLumpSumView.class.getSimpleName();
    private static final String PREF_YEAR = PREF_PREFIX + "-year"; //$NON-NLS-1$
    private static final String PREF_TAX_RATE = PREF_PREFIX + "-tax-rate"; //$NON-NLS-1$
    private static final String SUMMARY_SEPARATOR = " | "; //$NON-NLS-1$

    /** first year with a Vorabpauschale (InvStG 2018) */
    private static final int FIRST_YEAR = 2018;

    @Inject
    private Client client;

    @Inject
    private ExchangeRateProviderFactory factory;

    private CurrencyConverter converter;

    private int year;
    private TaxRate taxRate;
    private ManualBaseInterestRates manualRates;
    private final Map<Portfolio, Long> lossBalances = new HashMap<>();
    private final Map<Portfolio, Long> exemptionOrders = new HashMap<>();
    private Result result;

    private Composite taxHeadline;
    private StyledLabel taxAmount;
    private StyledLabel summary;
    private TreeViewer viewer;
    private ShowHideColumnHelper columns;
    private DropDown yearDropDown;
    private DropDown taxRateDropDown;
    private DropDown baseInterestRateDropDown;

    @PostConstruct
    public void setup()
    {
        converter = new CurrencyConverterImpl(factory, CurrencyUnit.EUR);

        var preferences = getPreferenceStore();
        var currentYear = LocalDate.now().getYear();
        year = preferences.getInt(PREF_YEAR);
        if (year < FIRST_YEAR || year > currentYear)
            year = currentYear;

        taxRate = TaxRate.WITHOUT_CHURCH_TAX;
        var storedTaxRate = preferences.getString(PREF_TAX_RATE);
        for (TaxRate rate : TaxRate.values())
        {
            if (rate.name().equals(storedTaxRate))
                taxRate = rate;
        }

        manualRates = new ManualBaseInterestRates(preferences);
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAdvanceLumpSum;
    }

    @Override
    public void notifyModelUpdated()
    {
        recalculate();
    }

    @Override
    protected void notifyViewCreationCompleted()
    {
        recalculate();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        yearDropDown = new DropDown(String.valueOf(year), Images.CALENDAR_OFF, SWT.DROP_DOWN, this::fillYearMenu);
        yearDropDown.setToolTip(Messages.LabelYear);
        toolBar.add(yearDropDown);

        baseInterestRateDropDown = new DropDown(Messages.LabelBaseInterestRate, Images.EDIT, SWT.PUSH);
        baseInterestRateDropDown.setDefaultAction(new SimpleAction(a -> editBaseInterestRate()));
        toolBar.add(baseInterestRateDropDown);

        taxRateDropDown = new DropDown(taxRate.getLabel(), Images.EURO, SWT.DROP_DOWN,
                        this::fillTaxRateMenu);
        toolBar.add(taxRateDropDown);

        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TreeViewerCSVExporter(viewer).export(getTitle() + " " + year + ".csv"))); //$NON-NLS-1$ //$NON-NLS-2$

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> columns.menuAboutToShow(manager)));
    }

    private void fillYearMenu(IMenuManager manager)
    {
        for (int y = LocalDate.now().getYear(); y >= FIRST_YEAR; y--)
        {
            final int selected = y;
            var action = new SimpleAction(String.valueOf(y), IAction.AS_CHECK_BOX, a -> {
                if (year != selected)
                {
                    lossBalances.clear();
                    exemptionOrders.clear();
                }
                year = selected;
                getPreferenceStore().setValue(PREF_YEAR, year);
                yearDropDown.setLabel(String.valueOf(year));
                recalculate();
            });
            action.setChecked(y == year);
            manager.add(action);
        }
    }

    private void fillTaxRateMenu(IMenuManager manager)
    {
        for (TaxRate rate : TaxRate.values())
        {
            var action = new SimpleAction(rate.getLabel(), IAction.AS_CHECK_BOX, a -> {
                taxRate = rate;
                getPreferenceStore().setValue(PREF_TAX_RATE, rate.name());
                taxRateDropDown.setLabel(rate.getLabel());
                recalculate();
            });
            action.setChecked(rate == taxRate);
            manager.add(action);
        }
    }

    private void resetBaseInterestRate()
    {
        manualRates.remove(year);
        recalculate();
    }

    /**
     * A manual rate can be dropped only if one is stored for the year.
     */
    private boolean canResetBaseInterestRate()
    {
        return manualRates.get(year).isPresent();
    }

    private void editBaseInterestRate()
    {
        var current = resolveBaseInterestRate()
                        .map(r -> Values.PercentPlain.format(r.rate().doubleValue())).orElse(""); //$NON-NLS-1$

        var dialog = new BaseInterestRateDialog(getActiveShell(), year, current, canResetBaseInterestRate());

        var returnCode = dialog.open();
        if (returnCode == BaseInterestRateDialog.RESET_ID)
        {
            resetBaseInterestRate();
            return;
        }

        if (returnCode != Window.OK)
            return;

        manualRates.put(year, ManualBaseInterestRates.parsePercent(dialog.getValue()));
        recalculate();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        var container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 5).applyTo(container);

        taxHeadline = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().spacing(0, 8).applyTo(taxHeadline);
        var headlineLayout = GridDataFactory.fillDefaults().grab(true, false).indent(5, 5).create();
        headlineLayout.exclude = true;
        taxHeadline.setLayoutData(headlineLayout);
        taxHeadline.setVisible(false);

        var taxRow = new Composite(taxHeadline, SWT.NONE);
        var rowLayout = new RowLayout();
        rowLayout.marginLeft = rowLayout.marginRight = rowLayout.marginTop = rowLayout.marginBottom = 0;
        rowLayout.spacing = 12;
        rowLayout.center = true;
        taxRow.setLayout(rowLayout);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(taxRow);

        var taxLabel = new Label(taxRow, SWT.NONE);
        taxLabel.setText(Messages.LabelTotalEstimatedTax);
        taxAmount = new StyledLabel(taxRow, SWT.NONE);

        var separator = new Label(taxHeadline, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

        summary = new StyledLabel(container, SWT.WRAP);
        GridDataFactory.fillDefaults().grab(true, false).indent(5, 5).applyTo(summary);

        var treeContainer = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(treeContainer);
        TreeColumnLayout layout = new SafeTreeColumnLayout();
        treeContainer.setLayout(layout);

        viewer = new TreeViewer(treeContainer, SWT.FULL_SELECTION);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        CopyPasteSupport.enableFor(viewer);
        ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(getEditorActivationState(), viewer);

        columns = new ShowHideColumnHelper(AdvanceLumpSumView.class.getSimpleName(), //$NON-NLS-1$
                        getPreferenceStore(), viewer, layout);
        createColumns();
        columns.createColumns(true);

        viewer.setContentProvider(new ContentProvider());
        viewer.addSelectionChangedListener(event -> {
            var element = event.getStructuredSelection().getFirstElement();
            if (element instanceof Item item)
                setInformationPaneInput(item.security());
            else if (element instanceof PortfolioNode node)
                setInformationPaneInput(node.portfolio());
            else
                setInformationPaneInput(null);
        });

        return container;
    }

    private void createColumns()
    {
        Function<Object, String> nameOf = element -> {
            if (element instanceof PortfolioNode node)
                return node.portfolio().getName();
            return element instanceof Item item ? item.security().getName() : null;
        };

        var column = new Column("name", Messages.ColumnName, SWT.NONE, 460); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return nameOf.apply(element);
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof PortfolioNode node)
                    return LogoManager.instance().getDefaultColumnImage(node.portfolio(), client.getSettings());
                return element instanceof Item item
                                ? LogoManager.instance().getDefaultColumnImage(item.security(), client.getSettings())
                                : null;
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(nameOf));
        column.setRemovable(false);
        columns.addColumn(column);

        column = new Column("shares", Messages.ColumnShares, SWT.RIGHT, 70); //$NON-NLS-1$
        column.setLabelProvider(itemLabel(item -> Values.Share.format(item.shares())));
        column.setSorter(ColumnViewerSorter.create(e -> e instanceof Item item ? item.shares() : null));
        columns.addColumn(column);

        column = new Column("partialExemption", //$NON-NLS-1$
                        name.abuchen.portfolio.Messages.AttributesPartialExemptionColumn, SWT.RIGHT, 110);
        column.setLabelProvider(itemLabel(item -> Values.Percent2.format(item.partialExemption())));
        column.setSorter(ColumnViewerSorter.create(e -> e instanceof Item item ? item.partialExemption() : null));
        columns.addColumn(column);

        addQuoteColumn("priceStart", Messages.ColumnPriceStartOfYear, Item::priceStart, Item::priceStartDate, //$NON-NLS-1$
                        false);
        addQuoteColumn("priceEnd", Messages.ColumnPriceEndOfYear, Item::priceEnd, Item::priceEndDate, false); //$NON-NLS-1$
        addQuoteColumn("distributions", Messages.ColumnDistributionsPerShare, estimate(Item::distributionsPerShare), //$NON-NLS-1$
                        false);
        addQuoteColumn("baseYield", Messages.ColumnBaseYieldPerShare, estimate(Item::baseYieldPerShare), false); //$NON-NLS-1$
        addQuoteColumn("advanceLumpSumPerShare", Messages.ColumnAdvanceLumpSumPerShare, //$NON-NLS-1$
                        estimate(Item::advanceLumpSumPerShare), true);

        addMoneyColumn("advanceLumpSum", Messages.LabelAdvanceLumpSum, 130, Item::advanceLumpSum, //$NON-NLS-1$
                        PortfolioNode::advanceLumpSum);
        addMoneyColumn("taxableAmount", Messages.ColumnTaxableAmount, 170, Item::taxableAmount, //$NON-NLS-1$
                        PortfolioNode::taxableAmount);
        addMoneyColumn("tax", Messages.ColumnEstimatedTax, 130, Item::tax, PortfolioNode::tax); //$NON-NLS-1$

        addDeductionColumn("generalLossBalance", Messages.ColumnGeneralLossBalance, //$NON-NLS-1$
                        Messages.ColumnGeneralLossBalance_Description, Messages.MsgAdvanceLumpSumLossUsed,
                        lossBalances, PortfolioNode::usedLoss, 155);
        addDeductionColumn("exemptionOrder", Messages.ColumnExemptionOrder, //$NON-NLS-1$
                        Messages.ColumnExemptionOrder_Description, Messages.MsgAdvanceLumpSumExemptionOrderUsed,
                        exemptionOrders, PortfolioNode::usedExemption, 140);

        column = new Column("balance", Messages.ColumnBalance, SWT.RIGHT, 110); //$NON-NLS-1$
        column.setDescription(Messages.ColumnAdvanceLumpSumBalance_Description);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var account = element instanceof PortfolioNode node ? node.portfolio().getReferenceAccount() : null;
                if (account == null)
                    return null;
                return referenceBalance(account, converter, balanceDate())
                                .map(balance -> CurrencyUnit.EUR.equals(client.getBaseCurrency())
                                                ? Values.Money.format(balance, CurrencyUnit.EUR)
                                                : Values.Money.format(balance))
                                .orElse(null);
            }
        });
        columns.addColumn(column);
    }

    /**
     * Returns the balance in the term currency of the converter, or empty if
     * no exchange rate is available at the given date.
     */
    /* package */ static Optional<Money> referenceBalance(Account account, CurrencyConverter converter,
                    LocalDate date)
    {
        var funds = AccountSnapshot.create(account, converter, date).getUnconvertedFunds();
        return converter.findRate(date, funds.getCurrencyCode())
                        .map(rate -> Money.of(converter.getTermCurrency(), rate.getValue()
                                        .multiply(BigDecimal.valueOf(funds.getAmount()))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue()));
    }

    /**
     * The tax is collected at the beginning of the following year, therefore
     * the balance of that day is the relevant one. For the current year that
     * date is still in the future and today's balance is shown.
     */
    /* package */ static LocalDate balanceDate(int year, LocalDate today)
    {
        var date = LocalDate.of(year + 1, 1, 1);
        return date.isAfter(today) ? today : date;
    }

    private LocalDate balanceDate()
    {
        return balanceDate(year, LocalDate.now());
    }

    private static ColumnLabelProvider itemLabel(Function<Item, String> text)
    {
        return new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return element instanceof Item item ? text.apply(item) : null;
            }
        };
    }

    private void addQuoteColumn(String id, String label, Function<Item, Quote> value, boolean visible)
    {
        addQuoteColumn(id, label, value, item -> null, visible);
    }

    private void addQuoteColumn(String id, String label, Function<Item, Quote> value, Function<Item, LocalDate> date,
                    boolean visible)
    {
        var column = new Column(id, label, SWT.RIGHT, 180);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var quote = element instanceof Item item ? value.apply(item) : null;
                return quote == null ? null : Values.CalculatedQuote.format(quote, client.getBaseCurrency());
            }

            @Override
            public String getToolTipText(Object element)
            {
                var priceDate = element instanceof Item item ? date.apply(item) : null;
                return priceDate == null ? null
                                : MessageFormat.format(Messages.TooltipDateOfPrice, Values.Date.format(priceDate));
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> e instanceof Item item ? value.apply(item) : null));
        column.setVisible(visible);
        columns.addColumn(column);
    }

    private static <T> Function<Item, T> estimate(Function<Item, T> value)
    {
        return item -> item.hasMissingExchangeRates() || item.hasUnresolvedDistributions() ? null : value.apply(item);
    }

    private void addMoneyColumn(String id, String label, int width, Function<Item, Money> itemValue,
                    Function<PortfolioNode, Money> nodeValue)
    {
        var estimate = estimate(itemValue);
        Function<Object, Money> value = element -> {
            if (element instanceof Item item)
                return estimate.apply(item);
            return element instanceof PortfolioNode node ? nodeValue.apply(node) : null;
        };

        var column = new Column(id, label, SWT.RIGHT, width);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var money = value.apply(element);
                return money != null ? Values.Money.format(money, client.getBaseCurrency()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(value::apply));
        columns.addColumn(column);
    }

    private void addDeductionColumn(String id, String label, String description, String usedMessage,
                    Map<Portfolio, Long> amounts, Function<PortfolioNode, Money> usedAmount, int width)
    {
        var column = new Column(id, label, SWT.RIGHT, width);
        column.setDescription(description);
        Function<PortfolioNode, Long> enteredAmount = node -> amounts.getOrDefault(node.portfolio(), 0L);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return element instanceof PortfolioNode node ? Values.Amount.format(enteredAmount.apply(node)) : null;
            }

            @Override
            public String getToolTipText(Object element)
            {
                if (!(element instanceof PortfolioNode node))
                    return null;

                return description + "\n" //$NON-NLS-1$
                                + MessageFormat.format(usedMessage, Values.Money.format(usedAmount.apply(node)));
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> e instanceof PortfolioNode node ? enteredAmount.apply(node) : null));
        column.setEditingSupport(new ColumnEditingSupport()
        {
            @Override
            public CellEditor createEditor(Composite composite)
            {
                var editor = new TextCellEditor(composite);
                var text = (Text) editor.getControl();
                text.addVerifyListener(new NumberVerifyListener(false));
                DecimalKeypadSupport.configure(text);
                return editor;
            }

            @Override
            public boolean canEdit(Object element)
            {
                return element instanceof PortfolioNode;
            }

            @Override
            public Object getValue(Object element)
            {
                return element instanceof PortfolioNode node ? Values.Amount.format(enteredAmount.apply(node)) : ""; //$NON-NLS-1$
            }

            @Override
            public void setValue(Object element, Object value)
            {
                if (!(element instanceof PortfolioNode node))
                    return;

                var amount = parseAmount(String.valueOf(value));
                if (amount == null)
                    return;

                if (amount <= 0)
                    amounts.remove(node.portfolio());
                else
                    amounts.put(node.portfolio(), amount);

                recalculate();
            }
        });
        columns.addColumn(column);
    }

    /**
     * Parses an amount typed by the user. Returns {@code null} for input that
     * is not a plain amount.
     */
    /* package */ static Long parseAmount(String text)
    {
        if (text == null || text.isBlank())
            return null;

        try
        {
            var position = new ParsePosition(0);
            var number = NumberFormat.getNumberInstance().parse(text.trim(), position);
            if (number == null || position.getIndex() != text.trim().length())
                return null;
            return Values.Amount.factorize(number.doubleValue());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private void recalculate()
    {
        if (viewer == null || viewer.getTree().isDisposed())
            return;

        var baseInterestRate = resolveBaseInterestRate();

        if (baseInterestRateDropDown != null)
        {
            baseInterestRateDropDown.setLabel(baseInterestRate.map(this::baseInterestRateLabel)
                            .orElse(Messages.LabelBaseInterestRate));
            baseInterestRateDropDown.setToolTip(
                            MessageFormat.format(Messages.MenuEditBaseInterestRate, String.valueOf(year)));
        }

        if (baseInterestRate.isEmpty())
        {
            result = null;
        }
        else
        {
            var valuationDate = LocalDate.of(year, 12, 31);
            if (valuationDate.isAfter(LocalDate.now()))
                valuationDate = LocalDate.now();

            result = new AdvanceLumpSumCalculation(client, converter).calculate(baseInterestRate.get(), taxRate.rate,
                            valuationDate, lossBalances, exemptionOrders);
        }

        viewer.setInput(buildNodes());
        viewer.expandAll();
        columns.invalidateCache();

        var hasResult = result != null;
        taxAmount.setText(hasResult ? formatBoldMoney(result.getTotalTax()) : ""); //$NON-NLS-1$
        ((GridData) taxHeadline.getLayoutData()).exclude = !hasResult;
        taxHeadline.setVisible(hasResult);
        summary.setText(buildSummary(year, baseInterestRate.orElse(null), result));
        summary.getParent().layout(true);
    }

    private List<PortfolioNode> buildNodes()
    {
        var nodes = new ArrayList<PortfolioNode>();
        if (result == null)
            return nodes;

        var currencyCode = result.currencyCode();
        result.getItemsByPortfolio().forEach((portfolio, items) -> nodes.add(new PortfolioNode(portfolio, items,
                        Result.sum(items, Item::advanceLumpSum, currencyCode),
                        Result.sum(items, Item::taxableAmount, currencyCode),
                        Result.sum(items, Item::usedLoss, currencyCode),
                        Result.sum(items, Item::usedExemption, currencyCode),
                        Result.sum(items, Item::tax, currencyCode))));
        return nodes;
    }

    @SuppressWarnings("nls")
    /* package */ static String buildSummary(int year, BaseInterestRate baseInterestRate, Result result)
    {
        var text = new StringBuilder();

        if (baseInterestRate == null)
        {
            appendWarning(text, MessageFormat.format(Messages.MsgAdvanceLumpSumMissingBaseInterestRate,
                            String.valueOf(year)));
        }
        else
        {
            var source = switch (baseInterestRate.source())
            {
                case OFFICIAL -> Messages.LabelBaseInterestRateSourceOfficial;
                case MANUAL -> Messages.LabelBaseInterestRateSourceManual;
            };

            text.append(TextUtil.escapeHtml(Messages.LabelBaseInterestRate)).append(' ').append(year)
                            .append(": <strong>")
                            .append(Values.PercentPlain.format(baseInterestRate.rate().doubleValue()))
                            .append(" %</strong> (").append(TextUtil.escapeHtml(source)).append(')');

            if (result != null)
            {
                text.append(SUMMARY_SEPARATOR).append(TextUtil.escapeHtml(Messages.LabelAdvanceLumpSum)).append(": ")
                                .append(formatBoldMoney(result.getTotalAdvanceLumpSum()));
                text.append(formatTaxableSummary(result.getTotalTaxableAmount(), result.getTotalUsedLoss(),
                                result.getTotalUsedExemption()));
            }
            text.append("\n\n");
        }

        if (result != null)
        {
            appendSecurityWarning(text, result, Item::hasMissingPrices, Messages.MsgAdvanceLumpSumMissingPrices);
            appendSecurityWarning(text, result, Item::hasOutdatedPrices, Messages.MsgAdvanceLumpSumOutdatedPrices);
            appendSecurityWarning(text, result, Item::hasMissingExchangeRates,
                            Messages.MsgAdvanceLumpSumMissingExchangeRates);
            appendSecurityWarning(text, result, Item::hasUnresolvedDistributions,
                            Messages.MsgAdvanceLumpSumUnresolvedDistributions);
        }

        if (year == LocalDate.now().getYear())
            text.append(TextUtil.escapeHtml(Messages.MsgAdvanceLumpSumCurrentYear)).append('\n');

        text.append("<em>").append(TextUtil.escapeHtml(Messages.MsgAdvanceLumpSumInfo)).append(' ')
                        .append(TextUtil.escapeHtml(Messages.MsgAdvanceLumpSumPriceSource)).append('\n')
                        .append(TextUtil.escapeHtml(Messages.MsgAdvanceLumpSumTaxCollection)).append("</em>");

        return text.toString();
    }

    @SuppressWarnings("nls")
    /* package */ static String formatTaxableSummary(Money taxableAfterDeductions, Money usedLoss, Money usedExemption)
    {
        var taxableBeforeDeductions = taxableAfterDeductions.add(usedLoss).add(usedExemption);
        var text = new StringBuilder(SUMMARY_SEPARATOR).append(TextUtil.escapeHtml(Messages.LabelTaxableBeforeDeductions))
                        .append(": ").append(formatBoldMoney(taxableBeforeDeductions));
        appendUsedDeduction(text, usedLoss, Messages.MsgAdvanceLumpSumLossUsed);
        appendUsedDeduction(text, usedExemption, Messages.MsgAdvanceLumpSumExemptionOrderUsed);
        return text.toString();
    }

    private static void appendUsedDeduction(StringBuilder text, Money amount, String message)
    {
        if (amount.getAmount() <= 0)
            return;

        text.append(SUMMARY_SEPARATOR).append(MessageFormat.format(TextUtil.escapeHtml(message), formatBoldMoney(amount)));
    }

    private static String formatBoldMoney(Money amount)
    {
        return "<strong>" + TextUtil.escapeHtml(Values.Money.format(amount)) + "</strong>"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static void appendSecurityWarning(StringBuilder text, Result result, Predicate<Item> filter,
                    String message)
    {
        var names = new StringJoiner(", "); //$NON-NLS-1$
        result.items().stream().filter(filter).map(i -> i.security().getName()).distinct().forEach(names::add);
        if (names.length() > 0)
            appendWarning(text, MessageFormat.format(message, names.toString()));
    }

    @SuppressWarnings("nls")
    private static void appendWarning(StringBuilder text, String message)
    {
        text.append("<warning>").append(TextUtil.escapeHtml(message)).append("</warning>\n");
    }

    private Optional<BaseInterestRate> resolveBaseInterestRate()
    {
        return BaseInterestRate.resolve(year, manualRates.get(year));
    }

    private String baseInterestRateLabel(BaseInterestRate rate)
    {
        return Messages.LabelBaseInterestRate + " " + //$NON-NLS-1$
                        Values.PercentPlain.format(rate.rate().doubleValue()) + " %"; //$NON-NLS-1$
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
    }

    private static String percent(BigDecimal fraction)
    {
        var format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(4);
        return format.format(fraction);
    }

    /* package */ static class ContentProvider implements ITreeContentProvider
    {
        private Object[] roots = new Object[0];

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            roots = newInput == null ? new Object[0] : ((List<?>) newInput).toArray();
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return roots;
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return parentElement instanceof PortfolioNode node ? node.items().toArray() : new Object[0];
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return element instanceof PortfolioNode node && !node.items().isEmpty();
        }
    }
}
