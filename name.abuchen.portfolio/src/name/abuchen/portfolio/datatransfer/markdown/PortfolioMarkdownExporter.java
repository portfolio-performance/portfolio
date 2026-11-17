package name.abuchen.portfolio.datatransfer.markdown;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Exporter;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

/**
 * Exports the portfolio as a single, self-contained Markdown document that is
 * designed to be given to a large language model (LLM) as context, for example
 * when asking an AI assistant for advice on the portfolio.
 * <p>
 * Design goals:
 * <ul>
 * <li><strong>Complete</strong>: valuation, allocation (including rebalancing
 * targets), holdings, cash, performance, security level results, market data,
 * investment plans, watchlists and transactions.</li>
 * <li><strong>Pre-calculated</strong>: all figures are produced by the same
 * calculations that drive the application, so that a model does not have to do
 * arithmetic on raw transactions.</li>
 * <li><strong>Self-describing</strong>: the document explains its own
 * conventions and metrics.</li>
 * <li><strong>Compact and unambiguous</strong>: tables instead of prose,
 * ISO dates, plain decimal numbers, explicit currencies. Historical quotes are
 * condensed to a few indicators.</li>
 * </ul>
 * The document is intentionally <em>not</em> localized: a stable structure and
 * vocabulary is more important for machine consumption than a translation.
 */
@SuppressWarnings("nls")
public class PortfolioMarkdownExporter implements Exporter
{
    /**
     * Determines which transactions are listed in the document.
     */
    public enum TransactionScope
    {
        ALL, LAST_12_MONTHS, NONE
    }

    private static final CostMethod COST_METHOD = CostMethod.FIFO;
    private static final int STALE_PRICE_DAYS = 7;
    private static final int DAYS_PER_YEAR = 365;
    private static final String TAXONOMY_ROOT = "(taxonomy root)";

    private record PeriodResult(String label, Interval interval, Money start, Money transfers, Money gains,
                    Money earnings, Money fees, Money taxes, Money end, Money absoluteGain, double ttwror,
                    double ttwrorAnnualized, double irr)
    {
        boolean isAnnualizable()
        {
            return ChronoUnit.DAYS.between(interval.getStart(), interval.getEnd()) >= DAYS_PER_YEAR;
        }
    }

    private record TransactionRow(LocalDateTime date, String type, String security, String shares, String price,
                    String gross, String fees, String taxes, String net, String currency, String portfolio,
                    String account, String counterparty, String note)
    {
    }

    private final Client client;
    private final CurrencyConverter converter;
    private final String name;

    private TransactionScope transactionScope = TransactionScope.ALL;
    private LocalDate valuationDate = LocalDate.now();
    private String generator;

    // state of one run
    private final Set<String> notes = new LinkedHashSet<>();
    private final Map<Security, String> securityLabels = new HashMap<>();
    private LocalDate firstTransactionDate;
    private ClientSnapshot snapshot;
    private LazySecurityPerformanceSnapshot securityPerformance;
    private PeriodResult sinceInception;
    private List<TransactionRow> transactionRows;

    /**
     * @param client
     *            the client to export
     * @param converter
     *            converts all values into the reporting currency
     * @param name
     *            name of the export, used as title and default file name
     */
    public PortfolioMarkdownExporter(Client client, CurrencyConverter converter, String name)
    {
        this.client = Objects.requireNonNull(client);
        this.converter = Objects.requireNonNull(converter);
        this.name = name;
    }

    public PortfolioMarkdownExporter withTransactionScope(TransactionScope scope)
    {
        this.transactionScope = Objects.requireNonNull(scope);
        return this;
    }

    /**
     * Sets the date used as "today" (valuation date). Defaults to the current
     * date.
     */
    public PortfolioMarkdownExporter withValuationDate(LocalDate date)
    {
        this.valuationDate = Objects.requireNonNull(date);
        return this;
    }

    /**
     * Optional description of the software that produced the file, e.g.
     * "Portfolio Performance 0.90.1".
     */
    public PortfolioMarkdownExporter withGenerator(String generator)
    {
        this.generator = generator;
        return this;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void export(OutputStream out) throws IOException
    {
        out.write(generate().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates the Markdown document.
     */
    public String generate()
    {
        notes.clear();
        sinceInception = null;
        transactionRows = null;

        initialize();

        var body = new StringBuilder();
        section(body, "Summary", this::writeSummary);
        section(body, "Asset allocation", this::writeAllocation);
        section(body, "Current holdings", this::writeHoldings);
        section(body, "Cash accounts", this::writeCashAccounts);
        section(body, "Portfolio breakdown", this::writePortfolioBreakdown);
        section(body, "Performance", this::writePerformance);
        section(body, "Security performance", this::writeSecurityPerformance);
        section(body, "Market data", this::writeMarketData);
        section(body, "Investment plans", this::writeInvestmentPlans);
        section(body, "Watchlists", this::writeWatchlists);
        section(body, "Transactions", this::writeTransactions);

        var document = new StringBuilder();
        writeHeader(document);
        writeReadingGuide(document);
        writeNotes(document);
        document.append(body);
        writeGlossary(document);
        return document.toString();
    }

    // ---------------------------------------------------------------------
    // infrastructure
    // ---------------------------------------------------------------------

    private void initialize()
    {
        // transactions after the valuation date (e.g. planned ones) do not
        // start the history
        firstTransactionDate = client.getAllTransactions().stream()
                        .map(pair -> pair.getTransaction().getDateTime().toLocalDate())
                        .filter(date -> !date.isAfter(valuationDate)).min(Comparator.naturalOrder()).orElse(null);

        snapshot = ClientSnapshot.create(client, converter, valuationDate);

        securityPerformance = firstTransactionDate == null ? null
                        : LazySecurityPerformanceSnapshot.create(client, converter, allTimeInterval());

        securityLabels.clear();
        var byName = client.getSecurities().stream().collect(Collectors.groupingBy(
                        s -> Objects.toString(s.getName(), ""), LinkedHashMap::new, Collectors.toList()));
        for (var entry : byName.entrySet())
        {
            var group = entry.getValue();
            for (int index = 0; index < group.size(); index++)
            {
                var security = group.get(index);
                securityLabels.put(security, group.size() == 1 ? entry.getKey()
                                : entry.getKey() + " [" + distinguishingSuffix(security, group, index) + "]");
            }
        }
    }

    /**
     * Returns a suffix that distinguishes securities with the same name: the
     * ticker symbol, else the ISIN, else the currency with a running number.
     */
    private static String distinguishingSuffix(Security security, List<Security> group, int index)
    {
        if (isUniqueIn(group, security.getTickerSymbol(), Security::getTickerSymbol))
            return security.getTickerSymbol();
        if (isUniqueIn(group, security.getIsin(), Security::getIsin))
            return security.getIsin();
        return security.getCurrencyCode() + " #" + (index + 1);
    }

    private static boolean isUniqueIn(List<Security> group, String value, Function<Security, String> attribute)
    {
        if (value == null || value.isBlank())
            return false;
        return group.stream().filter(s -> value.equals(attribute.apply(s))).count() == 1;
    }

    private Interval allTimeInterval()
    {
        // intervals exclude the start date
        return Interval.of(firstTransactionDate.minusDays(1), valuationDate);
    }

    private void section(StringBuilder out, String title, Consumer<StringBuilder> writer)
    {
        var buffer = new StringBuilder();
        try
        {
            writer.accept(buffer);
        }
        catch (RuntimeException e)
        {
            PortfolioLog.error(e);
            notes.add("The section '" + title + "' could not be created completely: " + e.getMessage());
        }

        if (buffer.length() > 0)
        {
            out.append("## ").append(title).append("\n\n").append(buffer);
        }
    }

    private String reportingCurrency()
    {
        return converter.getTermCurrency();
    }

    /**
     * Returns the name of the security, made unique if several securities have
     * the same name. Use it wherever a security is referenced so that rows of
     * different sections can be matched.
     */
    private String label(Security security)
    {
        return MarkdownFormat.text(securityLabels.getOrDefault(security, Objects.toString(security.getName(), "")));
    }

    /**
     * Sort key for securities. Independent of the default locale (e.g. the
     * Turkish dotless i) so that the output is identical everywhere.
     */
    private String sortKey(Security security)
    {
        return label(security).toLowerCase(Locale.ROOT);
    }

    private String label(InvestmentVehicle vehicle)
    {
        return vehicle instanceof Security security ? label(security) : MarkdownFormat.text(vehicle.getName());
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : MarkdownFormat.text(value);
    }

    private static String ownerName(TransactionOwner<?> owner)
    {
        if (owner instanceof Account account)
            return account.getName();
        else if (owner instanceof Portfolio portfolio)
            return portfolio.getName();
        else
            return "";
    }

    private Money convert(Money money)
    {
        return converter.convert(valuationDate, money);
    }

    // ---------------------------------------------------------------------
    // header, reading guide, notes
    // ---------------------------------------------------------------------

    private void writeHeader(StringBuilder out)
    {
        out.append("# Portfolio report");
        if (name != null && !name.isBlank())
            out.append(": ").append(MarkdownFormat.text(name));
        out.append("\n\n");

        out.append("- Valuation date: ").append(MarkdownFormat.date(valuationDate)).append('\n');
        out.append("- Reporting currency: ").append(reportingCurrency()).append('\n');
        if (firstTransactionDate != null)
            out.append("- First transaction: ").append(MarkdownFormat.date(firstTransactionDate)).append('\n');
        out.append("- Cost method: ").append(COST_METHOD.getLabel()).append(" (first in, first out)\n");
        out.append("- Contents: ").append(client.getActivePortfolios().size())
                        .append(" portfolios (securities accounts), ") //
                        .append(client.getActiveAccounts().size()).append(" cash accounts, ")
                        .append(client.getSecurities().size()).append(" securities, ")
                        .append(transactionRows().size()).append(" transactions\n");
        if (generator != null && !generator.isBlank())
            out.append("- Generated by: ").append(generator).append('\n');
        out.append('\n');
    }

    private void writeReadingGuide(StringBuilder out)
    {
        out.append("## How to read this document\n\n");
        out.append("This is a snapshot of an investment portfolio, exported from Portfolio Performance as context for an AI assistant. ");
        out.append("All figures were calculated by Portfolio Performance (the same calculations that are shown in the application). ");
        out.append("Use them as given instead of recalculating them from the transactions.\n\n");
        out.append("- Dates use the ISO format YYYY-MM-DD. Numbers use a dot as decimal separator and no thousands separator. Percentages carry a % sign.\n");
        out.append("- Amounts are in the reporting currency (").append(reportingCurrency())
                        .append(") unless a column, a heading or a currency column says otherwise. ");
        out.append("Prices in the holdings and market data tables are in the currency of the security. ");
        out.append("Amounts and prices in the transaction list are in the currency of the transaction (column 'Currency').\n");
        out.append("- Gains are positive, losses are negative. `n/a` means the value cannot be calculated; an empty cell means not applicable.\n");
        out.append("- 'Value' is the market value on the valuation date, based on the latest available price. 'Cost' is the purchase value of the shares that are currently held (including fees and taxes).\n");
        out.append("- Performance periods are measured from the close of the 'From' date to the close of the 'To' date. Annualized figures (p.a.) are only shown for periods of at least one year.\n");
        out.append("- Weights are relative to total assets (securities plus cash) unless stated otherwise.\n");
        out.append("- Terms such as TTWROR, IRR and the transaction types are explained in the glossary at the end.\n\n");
    }

    private void writeNotes(StringBuilder out)
    {
        if (notes.isEmpty())
            return;

        out.append("## Data quality notes\n\n");
        out.append("Consider these limitations when interpreting the figures:\n\n");
        for (var note : notes)
            out.append("- ").append(note).append('\n');
        out.append('\n');
    }

    // ---------------------------------------------------------------------
    // summary
    // ---------------------------------------------------------------------

    private void writeSummary(StringBuilder out)
    {
        var currency = reportingCurrency();

        var securities = snapshot.getJointPortfolio().getValue();
        var cash = Money.of(currency, snapshot.getAccounts().stream().mapToLong(a -> a.getFunds().getAmount()).sum());

        var table = new MarkdownTable("Item", "Value");
        table.row("Total assets (" + currency + ")", MarkdownFormat.money(snapshot.getMonetaryAssets()));
        table.row("Securities (" + currency + ")", MarkdownFormat.money(securities));
        table.row("Cash (" + currency + ")", MarkdownFormat.money(cash));
        table.row("Number of securities held", String.valueOf(
                        snapshot.getJointPortfolio().getPositions().stream().filter(p -> p.getShares() != 0).count()));

        var result = sinceInception();
        if (result != null)
        {
            table.row("Net deposits since first transaction (" + currency + ")",
                            MarkdownFormat.money(result.transfers()));
            table.row("Absolute gain since first transaction (" + currency + ")",
                            MarkdownFormat.money(result.absoluteGain()));
            table.row("Time-weighted return since first transaction (TTWROR)",
                            MarkdownFormat.percent(result.ttwror()));
            if (result.isAnnualizable())
            {
                table.row("Time-weighted return p.a.", MarkdownFormat.percent(result.ttwrorAnnualized()));
                table.row("Internal rate of return p.a. (IRR)", MarkdownFormat.percent(result.irr()));
            }
        }
        else
        {
            notes.add("The portfolio does not contain any transactions up to the valuation date; no performance figures are available.");
        }

        table.appendTo(out);
    }

    private PeriodResult sinceInception()
    {
        if (sinceInception == null && firstTransactionDate != null)
            sinceInception = calculate("Since first transaction", allTimeInterval());
        return sinceInception;
    }

    private PeriodResult calculate(String label, Interval interval)
    {
        var warnings = new ArrayList<Exception>();
        var index = PerformanceIndex.forClient(client, converter, interval, warnings);
        warnings.forEach(e -> notes.add(e.getMessage()));

        var snap = index.getClientPerformanceSnapshot(COST_METHOD.useFifo()).orElseThrow();

        return new PeriodResult(label, interval, //
                        snap.getValue(CategoryType.INITIAL_VALUE), //
                        snap.getValue(CategoryType.TRANSFERS), //
                        snap.getValue(CategoryType.CAPITAL_GAINS, CategoryType.REALIZED_CAPITAL_GAINS,
                                        CategoryType.CURRENCY_GAINS), //
                        snap.getValue(CategoryType.EARNINGS), //
                        snap.getValue(CategoryType.FEES), //
                        snap.getValue(CategoryType.TAXES), //
                        snap.getValue(CategoryType.FINAL_VALUE), //
                        snap.getAbsoluteDelta(), //
                        index.getFinalAccumulatedPercentage(), //
                        index.getFinalAccumulatedAnnualizedPercentage(), //
                        snap.getPerformanceIRR());
    }

    // ---------------------------------------------------------------------
    // asset allocation
    // ---------------------------------------------------------------------

    private void writeAllocation(StringBuilder out)
    {
        if (client.getTaxonomies().isEmpty())
            return;

        var total = snapshot.getMonetaryAssets().getAmount();
        if (total == 0)
            return;

        Map<InvestmentVehicle, Long> values = new HashMap<>();
        snapshot.getPositionsByVehicle().forEach((vehicle, position) -> values.put(vehicle,
                        position.getValuation().getAmount()));

        out.append("Share is relative to total assets. Nested categories are included in the value of their parent category.\n\n");

        for (var taxonomy : client.getTaxonomies())
        {
            var buffer = new StringBuilder();
            writeTaxonomy(buffer, taxonomy, values, total);
            if (buffer.length() > 0)
                out.append("### ").append(MarkdownFormat.text(taxonomy.getName())).append("\n\n").append(buffer);
        }
    }

    private void writeTaxonomy(StringBuilder out, Taxonomy taxonomy, Map<InvestmentVehicle, Long> values, long total)
    {
        var currency = reportingCurrency();

        // weight of each vehicle that is assigned within the taxonomy
        Map<InvestmentVehicle, Long> assignedWeight = new HashMap<>();
        taxonomy.foreach(new Taxonomy.Visitor()
        {
            @Override
            public void visit(Classification classification, Classification.Assignment assignment)
            {
                assignedWeight.merge(assignment.getInvestmentVehicle(), (long) assignment.getWeight(), Long::sum);
            }
        });

        long unassigned = 0;
        for (var entry : values.entrySet())
        {
            long weight = Math.min(assignedWeight.getOrDefault(entry.getKey(), 0L),
                            (long) Classification.ONE_HUNDRED_PERCENT);
            unassigned += Math.round(entry.getValue() * (double) (Classification.ONE_HUNDRED_PERCENT - weight)
                            / Classification.ONE_HUNDRED_PERCENT);
        }

        var root = taxonomy.getRoot();
        boolean hasTargets = root.getChildren().stream().anyMatch(c -> hasTarget(c));

        var allocation = hasTargets
                        ? new MarkdownTable("Category", "Value (" + currency + ")", "Share", "Target weight",
                                        "Target value (" + currency + ")", "Difference (" + currency + ")")
                        : new MarkdownTable("Category", "Value (" + currency + ")", "Share");
        var assignments = new MarkdownTable("Category", "Instrument", "Assigned weight", "Value (" + currency + ")");

        // assignments directly to the root are part of the taxonomy but of no
        // category; they have no target
        long rootValue = root.getAssignments().stream().mapToLong(a -> assignedValue(a, values)).sum();
        if (rootValue != 0)
        {
            var share = MarkdownFormat.percent(rootValue / (double) total);
            if (hasTargets)
                allocation.row(TAXONOMY_ROOT, MarkdownFormat.amount(rootValue), share, "", "", "");
            else
                allocation.row(TAXONOMY_ROOT, MarkdownFormat.amount(rootValue), share);
            writeAssignments(root, TAXONOMY_ROOT, values, assignments);
        }

        long rootTarget = total - unassigned;
        writeClassifications(root, "", rootTarget, hasTargets, values, total, allocation, assignments);

        if (unassigned != 0)
        {
            if (hasTargets)
                allocation.row("(unassigned)", MarkdownFormat.amount(unassigned),
                                MarkdownFormat.percent(unassigned / (double) total), "", "", "");
            else
                allocation.row("(unassigned)", MarkdownFormat.amount(unassigned),
                                MarkdownFormat.percent(unassigned / (double) total));
        }

        if (allocation.isEmpty())
            return;

        if (hasTargets)
        {
            out.append("Target weight is the target configured for the category (relative to its parent). ");
            out.append("As in the rebalancing view of the application, target values are derived from the part of the assets that is assigned to the taxonomy. ");
            out.append("Difference = value minus target value: a positive difference means the category is above its target.\n\n");
        }

        allocation.appendTo(out);

        if (!assignments.isEmpty())
        {
            out.append("Assignments of instruments to categories:\n\n");
            assignments.appendTo(out);
        }
    }

    private boolean hasTarget(Classification classification)
    {
        return classification.getWeight() > 0
                        || classification.getChildren().stream().anyMatch(this::hasTarget);
    }

    private void writeClassifications(Classification parent, String parentPath, long parentTarget,
                    boolean hasTargets, Map<InvestmentVehicle, Long> values, long total, MarkdownTable allocation,
                    MarkdownTable assignments)
    {
        long directAssignments = parent.getAssignments().stream().mapToLong(a -> assignedValue(a, values)).sum();

        var children = new ArrayList<>(parent.getChildren());
        children.sort(Comparator.comparingInt(Classification::getRank));

        for (var child : children)
        {
            var path = parentPath.isEmpty() ? child.getName() : parentPath + " > " + child.getName();

            long value = valueOf(child, values);
            long target = Math.round((parentTarget - directAssignments) * (double) child.getWeight()
                            / Classification.ONE_HUNDRED_PERCENT);

            if (value != 0 || (hasTargets && target != 0))
            {
                var share = MarkdownFormat.percent(value / (double) total);
                if (hasTargets)
                {
                    allocation.row(MarkdownFormat.text(path), MarkdownFormat.amount(value), share,
                                    MarkdownFormat.weight(child.getWeight()), MarkdownFormat.amount(target),
                                    MarkdownFormat.amount(value - target));
                }
                else
                {
                    allocation.row(MarkdownFormat.text(path), MarkdownFormat.amount(value), share);
                }
            }

            writeAssignments(child, MarkdownFormat.text(path), values, assignments);

            writeClassifications(child, path, target, hasTargets, values, total, allocation, assignments);
        }
    }

    private void writeAssignments(Classification classification, String category,
                    Map<InvestmentVehicle, Long> values, MarkdownTable assignments)
    {
        var list = new ArrayList<>(classification.getAssignments());
        list.sort(Comparator.comparingInt(Classification.Assignment::getRank));
        for (var assignment : list)
        {
            long assigned = assignedValue(assignment, values);
            if (assigned == 0)
                continue;

            assignments.row(category, label(assignment.getInvestmentVehicle()),
                            MarkdownFormat.weight(assignment.getWeight()), MarkdownFormat.amount(assigned));
        }
    }

    private long assignedValue(Classification.Assignment assignment, Map<InvestmentVehicle, Long> values)
    {
        var value = values.get(assignment.getInvestmentVehicle());
        if (value == null)
            return 0;

        return Math.round(value * (double) assignment.getWeight() / Classification.ONE_HUNDRED_PERCENT);
    }

    private long valueOf(Classification classification, Map<InvestmentVehicle, Long> values)
    {
        long sum = classification.getAssignments().stream().mapToLong(a -> assignedValue(a, values)).sum();
        for (var child : classification.getChildren())
            sum += valueOf(child, values);
        return sum;
    }

    // ---------------------------------------------------------------------
    // holdings
    // ---------------------------------------------------------------------

    private void writeHoldings(StringBuilder out)
    {
        var currency = reportingCurrency();
        var total = snapshot.getMonetaryAssets().getAmount();

        var table = new MarkdownTable("Security", "ISIN", "Ticker", "Currency", "Shares", "Price", "Price date",
                        "Value (" + currency + ")", "Weight", "Cost (" + currency + ")",
                        "Unrealized gain (" + currency + ")", "Unrealized gain %");

        var positions = new ArrayList<>(snapshot.getJointPortfolio().getPositions());
        positions.removeIf(p -> p.getShares() == 0 || p.getSecurity() == null);

        var rows = new ArrayList<Map.Entry<Long, String[]>>();
        for (var position : positions)
        {
            var security = position.getSecurity();
            var value = convert(position.calculateValue());
            var price = position.getPrice();

            checkPrice(security, price);

            var record = securityPerformance == null ? null : securityPerformance.getRecord(security).orElse(null);

            String cost = MarkdownFormat.NOT_AVAILABLE;
            String gain = MarkdownFormat.NOT_AVAILABLE;
            String gainPercent = MarkdownFormat.NOT_AVAILABLE;
            if (record != null)
            {
                var costBasis = record.getCost(COST_METHOD, TaxesAndFees.INCLUDED);
                cost = MarkdownFormat.money(costBasis);
                gain = MarkdownFormat.money(record.getCapitalGainsOnHoldings(COST_METHOD));
                gainPercent = costBasis.isZero() ? MarkdownFormat.NOT_AVAILABLE
                                : MarkdownFormat.percent(record.getCapitalGainsOnHoldingsPercent(COST_METHOD));
            }

            rows.add(Map.entry(value.getAmount(),
                            new String[] { label(security), nullToEmpty(security.getIsin()),
                                            nullToEmpty(security.getTickerSymbol()), security.getCurrencyCode(),
                                            MarkdownFormat.shares(position.getShares()),
                                            MarkdownFormat.quote(price.getValue()),
                                            MarkdownFormat.date(price.getDate()), MarkdownFormat.money(value),
                                            total == 0 ? MarkdownFormat.NOT_AVAILABLE
                                                            : MarkdownFormat.percent(
                                                                            value.getAmount() / (double) total),
                                            cost, gain, gainPercent }));
        }

        // largest holdings first
        rows.sort((a, b) -> Long.compare(b.getKey(), a.getKey()));
        rows.forEach(r -> table.row(r.getValue()));

        if (table.isEmpty())
        {
            out.append("No securities are currently held.\n\n");
            return;
        }

        out.append("Sorted by value, largest first. Cost and unrealized gain use the cost method stated in the header; ")
                        .append("price is in the currency of the security.\n\n");
        table.appendTo(out);

        var securityNotes = positions.stream().map(SecurityPosition::getSecurity)
                        .filter(s -> s.getNote() != null && !s.getNote().isBlank()).toList();
        if (!securityNotes.isEmpty())
        {
            out.append("Notes of the investor on individual securities:\n\n");
            securityNotes.forEach(s -> out.append("- ").append(label(s)).append(": ")
                            .append(MarkdownFormat.text(s.getNote())).append('\n'));
            out.append('\n');
        }
    }

    private void checkPrice(Security security, SecurityPrice price)
    {
        if (price.getValue() == 0)
        {
            notes.add("No price is available for the held security '" + label(security)
                            + "'; its value is shown as 0.");
        }
        else if (ChronoUnit.DAYS.between(price.getDate(), valuationDate) > STALE_PRICE_DAYS)
        {
            notes.add("The latest price of '" + label(security) + "' is from " + price.getDate() + " ("
                            + ChronoUnit.DAYS.between(price.getDate(), valuationDate) + " days old).");
        }
    }

    private void writeCashAccounts(StringBuilder out)
    {
        var currency = reportingCurrency();
        var table = new MarkdownTable("Account", "Currency", "Balance", "Value (" + currency + ")", "Status", "Note");

        for (var accountSnapshot : snapshot.getAccounts())
        {
            var account = accountSnapshot.getAccount();
            if (account.isRetired() && accountSnapshot.getUnconvertedFunds().isZero())
                continue;

            if (accountSnapshot.getUnconvertedFunds().isNegative())
                notes.add("The cash account '" + account.getName() + "' has a negative balance ("
                                + MarkdownFormat.money(accountSnapshot.getUnconvertedFunds()) + " "
                                + account.getCurrencyCode() + ").");

            table.row(MarkdownFormat.text(account.getName()), account.getCurrencyCode(),
                            MarkdownFormat.money(accountSnapshot.getUnconvertedFunds()),
                            MarkdownFormat.money(accountSnapshot.getFunds()),
                            account.isRetired() ? "retired" : "active", nullToEmpty(account.getNote()));
        }

        if (!table.isEmpty())
            table.appendTo(out);
    }

    private void writePortfolioBreakdown(StringBuilder out)
    {
        var portfolios = snapshot.getPortfolios().stream()
                        .filter(p -> !p.getPortfolio().isRetired() || !p.getPositions().isEmpty()).toList();

        if (portfolios.isEmpty())
            return;

        var currency = reportingCurrency();

        var overview = new MarkdownTable("Portfolio", "Reference account", "Value (" + currency + ")", "Status",
                        "Note");
        for (var portfolioSnapshot : portfolios)
        {
            var portfolio = portfolioSnapshot.getPortfolio();
            overview.row(MarkdownFormat.text(portfolio.getName()),
                            portfolio.getReferenceAccount() == null ? ""
                                            : MarkdownFormat.text(portfolio.getReferenceAccount().getName()),
                            MarkdownFormat.money(portfolioSnapshot.getValue()),
                            portfolio.isRetired() ? "retired" : "active", nullToEmpty(portfolio.getNote()));
        }
        overview.appendTo(out);

        if (portfolios.size() < 2)
            return;

        out.append("Positions per portfolio:\n\n");
        var positions = new MarkdownTable("Portfolio", "Security", "Shares", "Value (" + currency + ")");
        for (var portfolioSnapshot : portfolios)
        {
            for (SecurityPosition position : portfolioSnapshot.getPositions())
            {
                if (position.getShares() == 0 || position.getSecurity() == null)
                    continue;

                positions.row(MarkdownFormat.text(portfolioSnapshot.getPortfolio().getName()),
                                label(position.getSecurity()), MarkdownFormat.shares(position.getShares()),
                                MarkdownFormat.money(convert(position.calculateValue())));
            }
        }
        if (!positions.isEmpty())
            positions.appendTo(out);
    }

    // ---------------------------------------------------------------------
    // performance
    // ---------------------------------------------------------------------

    private void writePerformance(StringBuilder out)
    {
        if (firstTransactionDate == null)
            return;

        var allTime = allTimeInterval();

        var periods = new ArrayList<PeriodResult>();
        periods.add(sinceInception());

        var candidates = new ArrayList<Map.Entry<String, LocalDate>>();
        candidates.add(Map.entry("Last 5 years", valuationDate.minusYears(5)));
        candidates.add(Map.entry("Last 3 years", valuationDate.minusYears(3)));
        candidates.add(Map.entry("Last 12 months", valuationDate.minusYears(1)));
        candidates.add(Map.entry("Year to date", LocalDate.of(valuationDate.getYear() - 1, 12, 31)));

        // shorter periods only if they differ from the complete history
        for (var candidate : candidates)
        {
            if (candidate.getValue().isAfter(allTime.getStart()) && candidate.getValue().isBefore(valuationDate))
                periods.add(calculate(candidate.getKey(), Interval.of(candidate.getValue(), valuationDate)));
        }

        out.append("### By period\n\n");
        writePeriodTable(out, periods);

        var years = new ArrayList<PeriodResult>();
        for (int year = firstTransactionDate.getYear(); year <= valuationDate.getYear(); year++)
        {
            var end = year == valuationDate.getYear() ? valuationDate : LocalDate.of(year, 12, 31);
            var start = LocalDate.of(year - 1, 12, 31);
            if (start.isBefore(firstTransactionDate.minusDays(1)))
                start = firstTransactionDate.minusDays(1);

            var label = year == valuationDate.getYear() ? year + " (to date)" : String.valueOf(year);
            years.add(calculate(label, Interval.of(start, end)));
        }

        out.append("### By calendar year\n\n");
        writePeriodTable(out, years);
    }

    private void writePeriodTable(StringBuilder out, List<PeriodResult> periods)
    {
        var currency = reportingCurrency();

        out.append("Value columns are in ").append(currency).append(". End value = start value + net deposits + ")
                        .append("gains + earnings - fees - taxes. Gains include realized and unrealized capital gains and currency effects; ")
                        .append("earnings are dividends and interest.\n\n");

        var table = new MarkdownTable("Period", "From", "To", "Start value", "Net deposits", "Gains", "Earnings",
                        "Fees", "Taxes", "End value", "Absolute gain", "TTWROR", "TTWROR p.a.", "IRR p.a.");

        for (var period : periods)
        {
            table.row(period.label(), MarkdownFormat.date(period.interval().getStart()),
                            MarkdownFormat.date(period.interval().getEnd()), MarkdownFormat.money(period.start()),
                            MarkdownFormat.money(period.transfers()), MarkdownFormat.money(period.gains()),
                            MarkdownFormat.money(period.earnings()), MarkdownFormat.money(period.fees()),
                            MarkdownFormat.money(period.taxes()), MarkdownFormat.money(period.end()),
                            MarkdownFormat.money(period.absoluteGain()), MarkdownFormat.percent(period.ttwror()),
                            period.isAnnualizable() ? MarkdownFormat.percent(period.ttwrorAnnualized()) : "",
                            period.isAnnualizable() ? MarkdownFormat.percent(period.irr()) : "");
        }

        table.appendTo(out);
    }

    private void writeSecurityPerformance(StringBuilder out)
    {
        if (securityPerformance == null)
            return;

        var currency = reportingCurrency();

        var table = new MarkdownTable("Security", "Status", "Total gain (" + currency + ")", "Total gain %",
                        "Realized gain (" + currency + ")", "Unrealized gain (" + currency + ")",
                        "Dividends (" + currency + ")", "Fees (" + currency + ")", "Taxes (" + currency + ")",
                        "IRR p.a.");

        var rows = new ArrayList<Map.Entry<String, String[]>>();
        for (LazySecurityPerformanceRecord record : securityPerformance.getRecords())
        {
            var security = record.getSecurity();
            var held = record.getSharesHeld() != null && record.getSharesHeld() != 0;

            // an annualized rate is misleading for short holding periods
            var dates = record.getLineItems().stream().map(i -> i.getDateTime().toLocalDate()).sorted().toList();
            var irr = "";
            if (!dates.isEmpty())
            {
                var holdingEnd = held ? valuationDate : dates.get(dates.size() - 1);
                if (ChronoUnit.DAYS.between(dates.get(0), holdingEnd) >= DAYS_PER_YEAR)
                    irr = MarkdownFormat.percent(record.getIrr());
            }

            rows.add(Map.entry(sortKey(security),
                            new String[] { label(security), held ? "held" : "sold", //
                                            MarkdownFormat.money(record.getDelta()), //
                                            MarkdownFormat.percent(record.getDeltaPercent()), //
                                            MarkdownFormat.money(record
                                                            .getRealizedCapitalGains(COST_METHOD,
                                                                            TaxesAndFees.INCLUDED)
                                                            .getCapitalGains()), //
                                            held ? MarkdownFormat.money(record.getCapitalGainsOnHoldings(COST_METHOD))
                                                            : "", //
                                            MarkdownFormat.money(record.getSumOfDividends()), //
                                            MarkdownFormat.money(record.getFees()), //
                                            MarkdownFormat.money(record.getTaxes()), //
                                            irr }));
        }

        rows.sort(Map.Entry.comparingByKey());
        rows.forEach(r -> table.row(r.getValue()));

        if (table.isEmpty())
            return;

        out.append("Results of each security since the first transaction, including securities that have been sold. ")
                        .append("Total gain is the result after all fees and taxes. ")
                        .append("Realized and unrealized gain already include the fees and taxes of buys and sells. ")
                        .append("Dividends are gross amounts (before taxes and fees). ")
                        .append("Total gain = realized gain + unrealized gain + dividends - fees and taxes that are not part of a buy or sell ")
                        .append("(for example withholding tax on dividends). ")
                        .append("The fees and taxes columns contain all fees and taxes of the security and are shown for information only. ")
                        .append("IRR p.a. is only shown for securities that have been held for at least one year.\n\n");
        table.appendTo(out);
    }

    // ---------------------------------------------------------------------
    // market data
    // ---------------------------------------------------------------------

    private void writeMarketData(StringBuilder out)
    {
        var held = new ArrayList<Security>();
        for (var position : snapshot.getJointPortfolio().getPositions())
        {
            if (position.getShares() != 0 && position.getSecurity() != null)
                held.add(position.getSecurity());
        }

        if (held.isEmpty())
            return;

        out.append("Condensed price history of the securities currently held (prices in the currency of the security). ")
                        .append("Changes are relative to the closest price on or before the reference date; ")
                        .append("52w = last 52 weeks.\n\n");
        marketDataTable(held).appendTo(out);
    }

    private MarkdownTable marketDataTable(List<Security> securities)
    {
        var table = new MarkdownTable("Security", "Currency", "Latest price", "Price date", "1 month", "3 months",
                        "YTD", "1 year", "52w low", "52w high", "Below 52w high");

        var sorted = new ArrayList<>(securities);
        sorted.sort(Comparator.comparing(this::sortKey));

        for (var security : sorted)
        {
            var prices = new ArrayList<>(security.getPricesIncludingLatest());
            prices.removeIf(p -> p.getDate().isAfter(valuationDate) || p.getValue() <= 0);

            if (prices.isEmpty())
            {
                table.row(label(security), security.getCurrencyCode(), MarkdownFormat.NOT_AVAILABLE, "", "", "", "", "",
                                "", "", "");
                continue;
            }

            var latest = prices.get(prices.size() - 1);

            long low = Long.MAX_VALUE;
            long high = Long.MIN_VALUE;
            var windowStart = valuationDate.minusWeeks(52);
            for (var price : prices)
            {
                if (price.getDate().isAfter(windowStart))
                {
                    low = Math.min(low, price.getValue());
                    high = Math.max(high, price.getValue());
                }
            }

            table.row(label(security), security.getCurrencyCode(), MarkdownFormat.quote(latest.getValue()),
                            MarkdownFormat.date(latest.getDate()),
                            change(prices, latest, valuationDate.minusMonths(1)),
                            change(prices, latest, valuationDate.minusMonths(3)),
                            change(prices, latest, LocalDate.of(valuationDate.getYear() - 1, 12, 31)),
                            change(prices, latest, valuationDate.minusYears(1)),
                            low == Long.MAX_VALUE ? "" : MarkdownFormat.quote(low),
                            high == Long.MIN_VALUE ? "" : MarkdownFormat.quote(high),
                            high == Long.MIN_VALUE ? ""
                                            : MarkdownFormat.percent(latest.getValue() / (double) high - 1));
        }

        return table;
    }

    private String change(List<SecurityPrice> prices, SecurityPrice latest, LocalDate reference)
    {
        // no data that goes back far enough
        if (prices.get(0).getDate().isAfter(reference))
            return "";

        SecurityPrice base = null;
        for (var price : prices)
        {
            if (price.getDate().isAfter(reference))
                break;
            base = price;
        }

        return base == null ? "" : MarkdownFormat.percent(latest.getValue() / (double) base.getValue() - 1);
    }

    // ---------------------------------------------------------------------
    // investment plans and watchlists
    // ---------------------------------------------------------------------

    private void writeInvestmentPlans(StringBuilder out)
    {
        if (client.getPlans().isEmpty())
            return;

        var table = new MarkdownTable("Plan", "Type", "Security", "Portfolio", "Account", "Currency", "Amount", "Fees",
                        "Taxes", "Interval", "Start", "Last execution", "Next execution", "Note");

        for (var plan : client.getPlans())
        {
            var currency = plan.getCurrencyCode();
            var interval = plan.getInterval() < InvestmentPlan.WEEKS_THRESHOLD
                            ? "every " + plan.getInterval() + " month(s)"
                            : "every " + (plan.getInterval() - InvestmentPlan.WEEKS_THRESHOLD) + " week(s)";

            table.row(MarkdownFormat.text(plan.getName()), plan.getPlanType() == null ? "" : plan.getPlanType().name(),
                            plan.getSecurity() == null ? "" : label(plan.getSecurity()),
                            plan.getPortfolio() == null ? "" : MarkdownFormat.text(plan.getPortfolio().getName()),
                            plan.getAccount() == null ? "" : MarkdownFormat.text(plan.getAccount().getName()),
                            currency == null ? "" : currency, MarkdownFormat.amount(plan.getAmount()),
                            MarkdownFormat.amount(plan.getFees()), MarkdownFormat.amount(plan.getTaxes()), interval,
                            MarkdownFormat.date(plan.getStart()),
                            plan.getLastDate().map(MarkdownFormat::date).orElse(""),
                            MarkdownFormat.date(plan.getDateOfNextTransactionToBeGenerated()),
                            nullToEmpty(plan.getNote()));
        }

        out.append("Recurring investments (savings plans). The next execution date is the next date for which a transaction is due.\n\n");
        table.appendTo(out);
    }

    private void writeWatchlists(StringBuilder out)
    {
        for (var watchlist : client.getWatchlists())
        {
            if (watchlist.getSecurities().isEmpty())
                continue;

            out.append("### ").append(MarkdownFormat.text(watchlist.getName())).append("\n\n");
            marketDataTable(watchlist.getSecurities()).appendTo(out);
        }
    }

    // ---------------------------------------------------------------------
    // transactions
    // ---------------------------------------------------------------------

    private void writeTransactions(StringBuilder out)
    {
        var all = transactionRows();
        if (all.isEmpty())
            return;

        if (transactionScope == TransactionScope.NONE)
        {
            out.append("The transaction list was omitted from this export (").append(all.size())
                            .append(" transactions exist).\n\n");
            return;
        }

        var cutoff = transactionScope == TransactionScope.LAST_12_MONTHS ? valuationDate.minusYears(1) : null;

        var table = new MarkdownTable("Date", "Type", "Security", "Shares", "Price", "Gross value", "Fees", "Taxes",
                        "Net amount", "Currency", "Portfolio", "Account", "Counterparty", "Note");

        for (var row : all)
        {
            if (cutoff != null && (!row.date().toLocalDate().isAfter(cutoff)
                            || row.date().toLocalDate().isAfter(valuationDate)))
                continue;

            table.row(MarkdownFormat.date(row.date().toLocalDate()), row.type(), row.security(), row.shares(),
                            row.price(), row.gross(), row.fees(), row.taxes(), row.net(), row.currency(),
                            row.portfolio(), row.account(), row.counterparty(), row.note());
        }

        if (cutoff != null)
        {
            out.append("Showing the ").append(table.size()).append(" of ").append(all.size())
                            .append(" transactions dated after ").append(MarkdownFormat.date(cutoff))
                            .append(" and up to ").append(MarkdownFormat.date(valuationDate))
                            .append(" (the full history is not included). ");
        }
        else
        {
            out.append("All ").append(all.size()).append(" transactions. ");
        }
        out.append("Most recent first. All amounts are in the currency of the transaction and unsigned; ")
                        .append("see the glossary for the direction of each type. Each buy, sell and transfer is listed once.\n\n");

        table.appendTo(out);
    }

    private List<TransactionRow> transactionRows()
    {
        if (transactionRows == null)
            transactionRows = collectTransactionRows();
        return transactionRows;
    }

    private List<TransactionRow> collectTransactionRows()
    {
        var rows = new ArrayList<TransactionRow>();

        for (var account : client.getAccounts())
        {
            for (var t : account.getTransactions())
            {
                var row = toRow(account, t);
                if (row != null)
                    rows.add(row);
            }
        }

        for (var portfolio : client.getPortfolios())
        {
            for (var t : portfolio.getTransactions())
            {
                var row = toRow(portfolio, t);
                if (row != null)
                    rows.add(row);
            }
        }

        // stable sort: most recent first
        Collections.sort(rows, Comparator.comparing(TransactionRow::date).reversed());
        return rows;
    }

    private TransactionRow toRow(Account account, AccountTransaction t)
    {
        var crossEntry = t.getCrossEntry();

        switch (t.getType())
        {
            case BUY, SELL:
                // listed once with the portfolio transaction
                if (crossEntry != null)
                    return null;
                break;
            case TRANSFER_IN:
                // listed once with the outgoing side
                if (crossEntry != null)
                    return null;
                break;
            default:
                break;
        }

        var isTransfer = t.getType() == AccountTransaction.Type.TRANSFER_OUT
                        || t.getType() == AccountTransaction.Type.TRANSFER_IN;
        var type = isTransfer ? "TRANSFER" : t.getType().name();

        var counterparty = "";
        if (t.getType() == AccountTransaction.Type.TRANSFER_OUT && crossEntry != null)
        {
            counterparty = ownerName(crossEntry.getCrossOwner(t));

            var credited = crossEntry.getCrossTransaction(t);
            if (credited != null && !t.getCurrencyCode().equals(credited.getCurrencyCode()))
                counterparty += " (credited " + MarkdownFormat.amount(credited.getAmount()) + " "
                                + credited.getCurrencyCode() + ")";
        }

        return row(t, type, "", account.getName(), counterparty);
    }

    private TransactionRow toRow(Portfolio portfolio, PortfolioTransaction t)
    {
        var crossEntry = t.getCrossEntry();

        switch (t.getType())
        {
            case BUY, SELL:
                var accountName = crossEntry instanceof BuySellEntry entry && entry.getAccount() != null
                                ? entry.getAccount().getName()
                                : "";
                return row(t, t.getType().name(), portfolio.getName(), accountName, "");
            case TRANSFER_IN:
                if (crossEntry != null)
                    return null;
                return row(t, "TRANSFER", portfolio.getName(), "", "");
            case TRANSFER_OUT:
                return row(t, "TRANSFER", portfolio.getName(), "",
                                crossEntry != null ? ownerName(crossEntry.getCrossOwner(t)) : "");
            default:
                return row(t, t.getType().name(), portfolio.getName(), "", "");
        }
    }

    private TransactionRow row(Transaction t, String type, String portfolio, String account, String counterparty)
    {
        var security = t.getSecurity();
        var fees = t.getUnitSum(Unit.Type.FEE);
        var taxes = t.getUnitSum(Unit.Type.TAX);

        var isTrade = t instanceof PortfolioTransaction p && (p.getType() == PortfolioTransaction.Type.BUY
                        || p.getType() == PortfolioTransaction.Type.SELL);

        // the gross value only adds information if there is a security or if
        // fees or taxes have been deducted
        var gross = security != null || !fees.isZero() || !taxes.isZero() ? t.getGrossValue() : null;

        return new TransactionRow(t.getDateTime(), type, security == null ? "" : label(security),
                        t.getShares() != 0 ? MarkdownFormat.shares(t.getShares()) : "",
                        isTrade ? MarkdownFormat.pricePerShare(t.getGrossValue().getAmount(), t.getShares()) : "",
                        MarkdownFormat.money(gross), fees.isZero() ? "" : MarkdownFormat.money(fees),
                        taxes.isZero() ? "" : MarkdownFormat.money(taxes), MarkdownFormat.amount(t.getAmount()),
                        t.getCurrencyCode(), MarkdownFormat.text(portfolio), MarkdownFormat.text(account),
                        MarkdownFormat.text(counterparty), nullToEmpty(t.getNote()));
    }

    // ---------------------------------------------------------------------
    // glossary
    // ---------------------------------------------------------------------

    private void writeGlossary(StringBuilder out)
    {
        out.append("## Glossary\n\n");

        out.append("**Metrics**\n\n");
        out.append("- TTWROR (true time-weighted rate of return): performance of the portfolio independent of the timing and size of deposits and removals. ")
                        .append("Use it to judge how well the investments performed. 'p.a.' is the annualized value.\n");
        out.append("- IRR (internal rate of return, money-weighted, annualized): return that takes the timing and size of deposits and removals into account. ")
                        .append("It reflects the investor's actual experience.\n");
        out.append("- Net deposits: deposits and incoming deliveries minus removals and outgoing deliveries in the period.\n");
        out.append("- Gains: realized and unrealized capital gains (price changes) plus currency effects. Earnings: dividends and interest.\n");
        out.append("- Cost: purchase value of the shares currently held, including fees and taxes; unrealized gain = value - cost.\n");
        out.append("- Share / weight: value divided by total assets (securities plus cash).\n");
        out.append("- Target (allocation): the target share configured for a category in the taxonomy (rebalancing), ")
                        .append("calculated like in the application's rebalancing view.\n\n");

        out.append("**Transaction types**\n\n");
        out.append("- BUY, SELL: purchase or sale of a security. Money leaves (BUY) or enters (SELL) the cash account; 'Net amount' includes fees and taxes.\n");
        out.append("- DELIVERY_INBOUND, DELIVERY_OUTBOUND: securities moved into or out of a portfolio without a cash movement (e.g. transfer from another bank).\n");
        out.append("- TRANSFER: movement between two of the investor's own accounts or portfolios, from 'Portfolio'/'Account' to 'Counterparty'. It is not a deposit or removal. ")
                        .append("If the two cash accounts have different currencies, the counterparty shows the amount credited in its currency.\n");
        out.append("- DEPOSIT, REMOVAL: money added to or taken out of a cash account from outside the portfolio.\n");
        out.append("- DIVIDENDS, INTEREST: money received. 'Gross value' is before taxes and fees.\n");
        out.append("- INTEREST_CHARGE, FEES, TAXES: money paid.\n");
        out.append("- FEES_REFUND, TAX_REFUND: money received back.\n");
    }
}
