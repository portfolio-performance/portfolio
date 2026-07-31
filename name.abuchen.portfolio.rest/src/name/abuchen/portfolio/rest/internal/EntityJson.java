package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.math.AllTimeHigh;
import name.abuchen.portfolio.math.Risk;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.FileAccessRegistry.FileAccess;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;

/**
 * Maps the model entities to the wire format. The API vocabulary is
 * deliberately more general than the model: a {@link Security} is an
 * <em>instrument</em>, an {@link Account} a <em>cash account</em> and a
 * {@link Portfolio} an <em>investment account</em> - the latter can hold any
 * kind of instrument, not just securities held at a bank.
 */
public final class EntityJson
{
    private EntityJson()
    {
    }

    public static JsonObject envelope(JsonArray items)
    {
        var json = new JsonObject();
        json.add("items", items); //$NON-NLS-1$
        return json;
    }

    public static <T> JsonObject envelope(Collection<T> entities, Function<T, JsonObject> mapper)
    {
        var items = new JsonArray();
        for (T entity : entities)
            items.add(mapper.apply(entity));
        return envelope(items);
    }

    public static JsonObject toJson(Client client, Security security)
    {
        var json = new JsonObject();
        json.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
        json.addProperty("name", security.getName()); //$NON-NLS-1$
        json.addProperty("currencyCode", security.getCurrencyCode()); //$NON-NLS-1$
        addSecurityDetails(json, client, security);
        return json;
    }

    /**
     * The identifying and descriptive fields of a security that are not
     * already implied by its role in the enclosing object: ISIN/WKN/ticker,
     * note and custom attributes. Shared by the instruments endpoint and by
     * the holdings endpoint's per-security enrichment.
     */
    private static void addSecurityDetails(JsonObject json, Client client, Security security)
    {
        if (security.getIsin() != null)
            json.addProperty("isin", security.getIsin()); //$NON-NLS-1$
        if (security.getWkn() != null)
            json.addProperty("wkn", security.getWkn()); //$NON-NLS-1$
        if (security.getTickerSymbol() != null)
            json.addProperty("tickerSymbol", security.getTickerSymbol()); //$NON-NLS-1$
        if (security.getNote() != null)
            json.addProperty("note", security.getNote()); //$NON-NLS-1$

        var attributes = attributesJson(client, security);
        if (attributes.size() > 0)
            json.add("attributes", attributes); //$NON-NLS-1$
    }

    /**
     * The security's custom attributes, only those that are set and of a
     * supported scalar type, keyed by attribute id. Iterating the type
     * definitions (rather than the stored map) gives a stable order and skips
     * orphaned or unsupported entries.
     */
    private static JsonObject attributesJson(Client client, Security security)
    {
        var result = new JsonObject();
        var stored = security.getAttributes().getMap();

        client.getSettings().getAttributeTypes() //
                        .filter(type -> type.supports(Security.class)) //
                        .forEach(type -> {
                            var value = stored.get(type.getId());
                            if (value == null)
                                return;

                            var fieldType = AttributeFieldType.of(type);
                            if (fieldType == null || !AttributeCodec.isSupported(fieldType))
                                return;

                            // A single attribute whose stored value is inconsistent with its
                            // declared type (e.g. a field type edited after values were stored, or
                            // a non-finite double from a corrupted file) must not fail the
                            // serialization of the whole instruments collection. Skip the offender.
                            try
                            {
                                result.add(type.getId(), AttributeCodec.encode(fieldType, value));
                            }
                            catch (RuntimeException e)
                            {
                                // skip this attribute
                            }
                        });

        return result;
    }

    public static JsonObject toJson(Account account, Money balance)
    {
        var json = new JsonObject();
        json.addProperty("uuid", account.getUUID()); //$NON-NLS-1$
        json.addProperty("name", account.getName()); //$NON-NLS-1$
        json.addProperty("currencyCode", account.getCurrencyCode()); //$NON-NLS-1$
        if (account.getNote() != null)
            json.addProperty("note", account.getNote()); //$NON-NLS-1$
        json.add("balance", toJson(balance)); //$NON-NLS-1$
        return json;
    }

    public static JsonObject toJson(FileAccess access, OpenFile file)
    {
        var json = new JsonObject();
        json.addProperty("id", access.uuid()); //$NON-NLS-1$
        if (access.alias() != null)
            json.addProperty("alias", access.alias()); //$NON-NLS-1$
        json.addProperty("label", file.getLabel()); //$NON-NLS-1$
        json.addProperty("path", file.getPath()); //$NON-NLS-1$
        return json;
    }

    /**
     * The statement of assets: every holding - securities and cash accounts
     * uniformly - valued at the snapshot date in the reporting currency, each
     * instrument additionally enriched with the period-dependent metrics
     * carried in {@code context} (cost basis, returns, dividends, ...).
     */
    public static JsonObject toJson(HoldingsContext context, ClientSnapshot snapshot)
    {
        var total = snapshot.getMonetaryAssets();

        var json = new JsonObject();
        json.addProperty("date", snapshot.getTime().toString()); //$NON-NLS-1$
        json.add("totalAssets", toJson(total)); //$NON-NLS-1$

        var items = new JsonArray();
        snapshot.getAssetPositions() //
                        .sorted(new AssetPosition.ByDescription()) //
                        .forEach(position -> items.add(toJson(context, position, total)));
        json.add("items", items); //$NON-NLS-1$
        return json;
    }

    private static JsonObject toJson(HoldingsContext context, AssetPosition position, Money totalAssets)
    {
        var security = position.getSecurity();
        var vehicle = position.getInvestmentVehicle();

        var json = new JsonObject();
        json.addProperty("type", security != null ? "instrument" : "cash-account"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        json.addProperty("uuid", vehicle.getUUID()); //$NON-NLS-1$
        json.addProperty("name", vehicle.getName()); //$NON-NLS-1$

        if (security != null)
        {
            json.add("shares", decimal(position.getPosition().getShares(), Values.Share.precision())); //$NON-NLS-1$

            // the price the valuation is based on; its date may be well before
            // the snapshot date (stale quotes, fallback to the price of the
            // last transaction) - that staleness must be visible on the wire
            var securityPrice = position.getPosition().getPrice();
            var price = new JsonObject();
            price.add("value", decimal(securityPrice.getValue(), Values.Quote.precision())); //$NON-NLS-1$
            if (security.getCurrencyCode() != null)
                price.addProperty("currency", security.getCurrencyCode()); //$NON-NLS-1$
            if (securityPrice.getDate() != null)
                price.addProperty("date", securityPrice.getDate().toString()); //$NON-NLS-1$
            json.add("price", price); //$NON-NLS-1$

            addSecurityDetails(json, context.client(), security);
            var record = context.record(security);
            record.ifPresent(r -> {
                addCostBasis(json, context.costMethod(), r);
                addProfitLoss(json, context.costMethod(), r);
                addReturns(json, r);
            });
            addDividends(json, context.costMethod(), record, security);
            addQuoteInReportingCurrency(json, context, security, securityPrice);
            context.localRecord(security).ifPresent(r -> addLocalCostBasis(json, context.costMethod(), r));
            addTechnical(json, context, security);
        }

        addClassifications(json, context.taxonomies(), vehicle);
        addExchangeRate(json, context, vehicle);

        json.add("valuation", toJson(position.getValuation())); //$NON-NLS-1$
        json.add("weight", decimal(totalAssets.isZero() ? 0d : position.getShare())); //$NON-NLS-1$

        // the value before conversion into the reporting currency; only worth
        // stating when the two differ
        var local = position.getPosition().calculateValue();
        if (!local.getCurrencyCode().equals(position.getValuation().getCurrencyCode()))
            json.add("localValuation", toJson(local)); //$NON-NLS-1$

        return json;
    }

    /**
     * Purchase price (per share) and purchase value (total), both gross and
     * net of fees/taxes, at the given cost method - mirrors the desktop's
     * "purchase price"/"purchase value" columns
     * ({@code StatementOfAssetsViewer#addPurchaseCostColumns}).
     */
    private static void addCostBasis(JsonObject json, CostMethod costMethod, LazySecurityPerformanceRecord record)
    {
        var purchasePrice = new JsonObject();
        purchasePrice.add("gross", //$NON-NLS-1$
                        toJsonQuote(record.getCostPerSharesHeld(costMethod, TaxesAndFees.INCLUDED)));
        purchasePrice.add("net", //$NON-NLS-1$
                        toJsonQuote(record.getCostPerSharesHeld(costMethod, TaxesAndFees.NOT_INCLUDED)));
        json.add("purchasePrice", purchasePrice); //$NON-NLS-1$

        var purchaseValue = new JsonObject();
        purchaseValue.add("gross", toJson(record.getCost(costMethod, TaxesAndFees.INCLUDED))); //$NON-NLS-1$
        purchaseValue.add("net", toJson(record.getCost(costMethod, TaxesAndFees.NOT_INCLUDED))); //$NON-NLS-1$
        json.add("purchaseValue", purchaseValue); //$NON-NLS-1$
    }

    /**
     * The unrealized profit/loss on the shares still held at the given cost
     * method, plus {@code delta}/{@code deltaPercent} - a cost-method
     * independent "absolute performance" that additionally accounts for sells
     * and dividends over the period (mirrors the desktop's "capital gains" and
     * "absolute performance" columns).
     */
    private static void addProfitLoss(JsonObject json, CostMethod costMethod, LazySecurityPerformanceRecord record)
    {
        var profitLoss = new JsonObject();
        profitLoss.add("value", toJson(record.getCapitalGainsOnHoldings(costMethod))); //$NON-NLS-1$
        profitLoss.add("percent", ratio(record.getCapitalGainsOnHoldingsPercent(costMethod))); //$NON-NLS-1$
        json.add("profitLoss", profitLoss); //$NON-NLS-1$

        json.add("delta", toJson(record.getDelta())); //$NON-NLS-1$
        json.add("deltaPercent", ratio(record.getDeltaPercent())); //$NON-NLS-1$
    }

    /**
     * The time-weighted (TTWROR, and its annualized form) and money-weighted
     * (IRR) return over the period, as fractions - independent of the cost
     * method (mirrors the desktop's "ttwror"/"ttwror_pa"/"irr" columns).
     * <p>
     * TTWROR is computed through {@code PerformanceIndex.forInvestment}, which
     * filters the client down to a single security via
     * {@code ClientSecurityFilter} - a filter that assumes every portfolio
     * transaction for that security has a linked cash account and throws an
     * NPE for one that does not (e.g. a security acquired only through an
     * inbound delivery, with no buy/sell ever booked). One security's
     * unsupported shape must not fail the whole holdings response, so this is
     * caught and reported as "cannot be computed" - same as the model's own
     * NaN/infinite convention for a return it cannot define.
     */
    private static void addReturns(JsonObject json, LazySecurityPerformanceRecord record)
    {
        json.add("ttwror", safeRatio(record::getTrueTimeWeightedRateOfReturn)); //$NON-NLS-1$
        json.add("ttwrorAnnualized", safeRatio(record::getTrueTimeWeightedRateOfReturnAnnualized)); //$NON-NLS-1$
        json.add("irr", ratio(record.getIrr())); //$NON-NLS-1$
    }

    private static JsonElement safeRatio(Supplier<Double> supplier)
    {
        try
        {
            return ratio(supplier.get());
        }
        catch (RuntimeException e)
        {
            return JsonNull.INSTANCE;
        }
    }

    /**
     * Dividend metrics: the sum received and the total rate of return over the
     * period (both from {@code record}, hence omitted together with it when
     * the security has no line item in the period), plus the next upcoming
     * ex-date/payment-date/amount - independent of the record, always relative
     * to today regardless of the valuation date, mirroring
     * {@code DividendPaymentColumns}. The whole {@code dividends} object is
     * omitted when none of these apply.
     */
    private static void addDividends(JsonObject json, CostMethod costMethod,
                    Optional<LazySecurityPerformanceRecord> record, Security security)
    {
        var dividends = new JsonObject();

        record.ifPresent(r -> {
            dividends.add("sum", toJson(r.getSumOfDividends())); //$NON-NLS-1$
            dividends.add("totalRateOfReturn", ratio(r.getTotalRateOfReturnDiv(costMethod))); //$NON-NLS-1$
        });

        var now = LocalDate.now();
        var events = security.getEvents().stream() //
                        .filter(DividendEvent.class::isInstance) //
                        .map(DividendEvent.class::cast) //
                        .toList();

        events.stream() //
                        .map(DividendEvent::getDate) //
                        .filter(d -> !now.isAfter(d)) //
                        .min(Comparator.naturalOrder()) //
                        .ifPresent(d -> dividends.addProperty("nextExDate", d.toString())); //$NON-NLS-1$

        events.stream() //
                        .filter(e -> !now.isAfter(e.getPaymentDate())) //
                        .min(Comparator.comparing(DividendEvent::getPaymentDate)) //
                        .ifPresent(e -> {
                            dividends.addProperty("nextPaymentDate", e.getPaymentDate().toString()); //$NON-NLS-1$
                            dividends.add("nextPaymentAmount", toJson(e.getAmount())); //$NON-NLS-1$
                        });

        if (dividends.size() > 0)
            json.add("dividends", dividends); //$NON-NLS-1$
    }

    /**
     * The vehicle's classification in every taxonomy the client defines, keyed
     * by taxonomy id - an instrument or a cash account can each be classified
     * (e.g. an asset-allocation taxonomy commonly classifies cash accounts
     * too), possibly split across several categories with a partial weight.
     * Mirrors {@code TaxonomyColumn}, one dynamically generated column per
     * taxonomy. The root node is never part of {@code path} - it is the
     * taxonomy itself, not a category. Omitted entirely when the client
     * defines no taxonomies, or the vehicle is unclassified in all of them.
     */
    private static void addClassifications(JsonObject json, List<Taxonomy> taxonomies, InvestmentVehicle vehicle)
    {
        var result = new JsonObject();

        for (Taxonomy taxonomy : taxonomies)
        {
            var assignments = new JsonArray();

            for (Classification classification : taxonomy.getClassifications(vehicle))
            {
                classification.getAssignments().stream() //
                                .filter(a -> vehicle.equals(a.getInvestmentVehicle())) //
                                .findFirst() //
                                .ifPresent(assignment -> {
                                    var path = new JsonArray();
                                    classification.getPathToRoot().stream().skip(1)
                                                    .forEach(c -> path.add(c.getName()));

                                    var entry = new JsonObject();
                                    entry.add("path", path); //$NON-NLS-1$
                                    entry.add("weight", //$NON-NLS-1$
                                                    decimal((double) assignment.getWeight()
                                                                    / Classification.ONE_HUNDRED_PERCENT));
                                    assignments.add(entry);
                                });
            }

            if (assignments.size() > 0)
                result.add(taxonomy.getId(), assignments);
        }

        if (result.size() > 0)
            json.add("classifications", result); //$NON-NLS-1$
    }

    /**
     * The exchange rate used to convert the holding's own currency into the
     * reporting currency, on {@code closingDate} - applies to an instrument or
     * a cash account alike. Omitted when the two currencies are the same (rate
     * 1, not worth stating) or the vehicle has no currency at all.
     */
    private static void addExchangeRate(JsonObject json, HoldingsContext context, InvestmentVehicle vehicle)
    {
        var currencyCode = vehicle.getCurrencyCode();
        if (currencyCode == null || currencyCode.equals(context.converter().getTermCurrency()))
            return;

        var rate = context.converter().getRate(context.closingDate(), currencyCode);
        json.add("exchangeRate", decimal(rate.getValue())); //$NON-NLS-1$
    }

    /**
     * The valuation price converted into the reporting currency - distinct
     * from {@code price}, which stays in the security's own currency. Omitted
     * when the two currencies are the same (identical to {@code price}) or the
     * security has no currency at all.
     */
    private static void addQuoteInReportingCurrency(JsonObject json, HoldingsContext context, Security security,
                    SecurityPrice securityPrice)
    {
        var securityCurrency = security.getCurrencyCode();
        if (securityCurrency == null || securityCurrency.equals(context.converter().getTermCurrency()))
            return;

        var converted = context.converter().convert(securityPrice.getDate(),
                        Quote.of(securityCurrency, securityPrice.getValue()));
        json.add("quoteReportingCurrency", toJsonQuote(converted)); //$NON-NLS-1$
    }

    /**
     * Purchase price/value and profit/loss expressed in the security's own
     * currency instead of the reporting currency - mirrors the desktop's
     * "...BaseCurrency" columns, which always use FIFO/gross regardless of the
     * chosen cost method's other, reporting-currency figures. Only ever called
     * when the security's own currency differs from the reporting currency
     * (see {@link HoldingsContext#localRecord}).
     */
    private static void addLocalCostBasis(JsonObject json, CostMethod costMethod, LazySecurityPerformanceRecord record)
    {
        json.add("purchasePriceLocal", //$NON-NLS-1$
                        toJsonQuote(record.getCostPerSharesHeld(costMethod, TaxesAndFees.INCLUDED)));
        json.add("purchaseValueLocal", toJson(record.getCost(costMethod, TaxesAndFees.INCLUDED))); //$NON-NLS-1$
        json.add("profitLossLocal", toJson(record.getCapitalGainsOnHoldings(costMethod))); //$NON-NLS-1$
    }

    private static final int SMA_DAYS = 200;

    /**
     * Distance from the 200-day simple moving average, and the distance from
     * (plus the low/high range of) the all-time high over the reporting
     * period - mirrors {@code DistanceFromMovingAverageColumn},
     * {@code DistanceFromAllTimeHighColumn} and {@code QuoteRangeColumn}. Both
     * are omitted independently when there is not enough price history.
     */
    private static void addTechnical(JsonObject json, HoldingsContext context, Security security)
    {
        var smaDistance = distanceFromMovingAverage(security, context.closingDate(), SMA_DAYS);
        if (smaDistance != null)
            json.add("distanceFromMovingAverage200", ratio(smaDistance.doubleValue())); //$NON-NLS-1$

        var allTimeHigh = new AllTimeHigh(security, context.interval());
        if (allTimeHigh.getValue() == null)
            return;

        json.add("distanceFromAllTimeHigh", ratio(allTimeHigh.getDistance())); //$NON-NLS-1$

        var quoteRange = new JsonObject();
        quoteRange.add("low", toJsonQuote(allTimeHigh.getLow(), security.getCurrencyCode())); //$NON-NLS-1$
        quoteRange.addProperty("lowDate", allTimeHigh.getLowDate().toString()); //$NON-NLS-1$
        quoteRange.add("high", toJsonQuote(allTimeHigh.getHigh(), security.getCurrencyCode())); //$NON-NLS-1$
        quoteRange.addProperty("highDate", allTimeHigh.getHighDate().toString()); //$NON-NLS-1$
        json.add("quoteRange", quoteRange); //$NON-NLS-1$
    }

    /**
     * The relative distance of the latest price on or before {@code date} from
     * the simple moving average over the preceding {@code days} prices, or
     * {@code null} when there is not that much price history yet - mirrors
     * {@code SimpleMovingAverage.calculateSma}, reimplemented here rather than
     * depended on since it lives in the UI plugin (which this module - reused
     * headlessly - does not, and must not, depend on).
     */
    private static Double distanceFromMovingAverage(Security security, LocalDate date, int days)
    {
        var prices = security.getLatestNPricesOfDate(date, days);
        if (prices.isEmpty() || prices.size() < days)
            return null;

        var sum = prices.stream().mapToLong(SecurityPrice::getValue).sum();
        var sma = sum / Values.Quote.divider() / prices.size();
        var last = prices.get(prices.size() - 1).getValue();
        return last / Values.Quote.divider() / sma - 1;
    }

    /** a per-share quote, e.g. a cost basis per share - {value, currency}, like {@link #toJson(Money)} */
    private static JsonObject toJsonQuote(Quote quote)
    {
        return toJsonQuote(quote.getAmount(), quote.getCurrencyCode());
    }

    /** a per-share quote value in the given (possibly null, for a currency-less instrument) currency */
    private static JsonObject toJsonQuote(long value, String currencyCode)
    {
        var json = new JsonObject();
        json.add("value", decimal(value, Values.Quote.precision())); //$NON-NLS-1$
        if (currencyCode != null)
            json.addProperty("currency", currencyCode); //$NON-NLS-1$
        return json;
    }

    public static JsonObject toJson(Money money)
    {
        var json = new JsonObject();
        json.add("value", decimal(money.getAmount(), Values.Money.precision())); //$NON-NLS-1$
        json.addProperty("currency", money.getCurrencyCode()); //$NON-NLS-1$
        return json;
    }

    /**
     * The performance between an opening and a closing valuation date: the
     * time-weighted (TTWROR) and money-weighted (IRR) return as fractions, plus
     * the value-change breakdown that reconciles the opening to the closing
     * value:
     * <p>
     * {@code openingValue + unrealizedCapitalGains + realizedCapitalGains +
     * income + fees + taxes + currencyGains + netDeposits = closingValue}.
     * <p>
     * The model stores fees and taxes as positive magnitudes; they are emitted
     * as signed contributions (negative) so the identity holds by plain
     * addition. {@code netDeposits} is the external flow (deposits less
     * removals) that the model excludes from the performance-only delta.
     */
    public static JsonObject performance(LocalDate openingDate, LocalDate closingDate, String currency, double ttwror,
                    double irr, ClientPerformanceSnapshot performance)
    {
        var json = new JsonObject();
        json.addProperty("openingDate", openingDate.toString()); //$NON-NLS-1$
        json.addProperty("closingDate", closingDate.toString()); //$NON-NLS-1$
        json.addProperty("currency", currency); //$NON-NLS-1$
        json.add("ttwror", ratio(ttwror)); //$NON-NLS-1$
        json.add("irr", ratio(irr)); //$NON-NLS-1$

        var breakdown = new JsonObject();
        breakdown.add("openingValue", toJson(performance.getValue(CategoryType.INITIAL_VALUE))); //$NON-NLS-1$
        breakdown.add("unrealizedCapitalGains", toJson(performance.getValue(CategoryType.CAPITAL_GAINS))); //$NON-NLS-1$
        breakdown.add("realizedCapitalGains", toJson(performance.getValue(CategoryType.REALIZED_CAPITAL_GAINS))); //$NON-NLS-1$
        breakdown.add("income", toJson(performance.getValue(CategoryType.EARNINGS))); //$NON-NLS-1$
        breakdown.add("fees", toJson(negate(performance.getValue(CategoryType.FEES)))); //$NON-NLS-1$
        breakdown.add("taxes", toJson(negate(performance.getValue(CategoryType.TAXES)))); //$NON-NLS-1$
        breakdown.add("currencyGains", toJson(performance.getValue(CategoryType.CURRENCY_GAINS))); //$NON-NLS-1$
        breakdown.add("netDeposits", toJson(performance.getValue(CategoryType.TRANSFERS))); //$NON-NLS-1$
        breakdown.add("closingValue", toJson(performance.getValue(CategoryType.FINAL_VALUE))); //$NON-NLS-1$
        json.add("breakdown", breakdown); //$NON-NLS-1$

        return json;
    }

    /**
     * The daily index for a period: one entry per calendar day, plus the risk
     * metrics derived from it. Amounts are plain numbers in the response's
     * {@code currency} rather than objects - the currency is fixed for the whole
     * series, and a year is ~365 entries.
     */
    public static JsonObject performanceSeries(LocalDate openingDate, LocalDate closingDate, String currency,
                    PerformanceIndex index)
    {
        var json = new JsonObject();
        json.addProperty("openingDate", openingDate.toString());
        json.addProperty("closingDate", closingDate.toString());
        json.addProperty("currency", currency);

        var dates = index.getDates();
        var accumulated = index.getAccumulatedPercentage();
        var totals = index.getTotals();
        var investedCapital = index.calculateInvestedCapital();
        var transferals = index.getTransferals();
        var drawdown = index.getDrawdown();
        var drawdownSerie = drawdown.getMaxDrawdownSerie();

        json.add("risk", risk(index, dates, accumulated, drawdown, drawdownSerie));

        // upstream clamps the series to today, so it can be shorter than the
        // requested range - the arrays, not the range, decide how many days there are
        var length = dates.length;
        var days = new JsonArray();
        for (var ii = 0; ii < length; ii++)
        {
            var day = new JsonObject();
            day.addProperty("date", dates[ii].toString());
            day.add("ttwror", ratio(at(accumulated, ii)));
            day.add("totalAssets", decimal(at(totals, ii), Values.Money.precision()));
            day.add("investedCapital", decimal(at(investedCapital, ii), Values.Money.precision()));
            day.add("netDeposits", decimal(at(transferals, ii), Values.Money.precision()));
            day.add("drawdown", ratio(at(drawdownSerie, ii)));
            days.add(day);
        }
        json.add("days", days);

        return json;
    }

    private static JsonObject risk(PerformanceIndex index, LocalDate[] dates, double[] accumulated,
                    Risk.Drawdown drawdown, double[] drawdownSerie)
    {
        var json = new JsonObject();

        // reported as a loss, matching the sign of the per-day drawdown series
        json.add("maxDrawdown", ratio(-drawdown.getMaxDrawdown()));

        var maxDrawdownInterval = drawdown.getIntervalOfMaxDrawdown();
        json.addProperty("maxDrawdownPeak", maxDrawdownInterval.getStart().toString());
        json.addProperty("maxDrawdownTrough", maxDrawdownInterval.getEnd().toString());

        var duration = drawdown.getMaxDrawdownDuration();
        json.addProperty("maxDrawdownDurationDays", duration.getDays());
        json.addProperty("maxDrawdownDurationStart", duration.getStart().toString());
        json.addProperty("maxDrawdownDurationEnd", duration.getEnd().toString());

        var volatility = index.getVolatility();
        json.add("volatility", ratio(volatility.getStandardDeviation()));
        json.add("semiDeviation", ratio(volatility.getSemiDeviation()));

        // the index's own high water mark, not a security's: the value is an
        // accumulated return, and the distance is the drawdown from that peak
        var peak = 0;
        for (var ii = 0; ii < accumulated.length; ii++)
            if (accumulated[ii] > accumulated[peak])
                peak = ii;

        var allTimeHigh = new JsonObject();
        allTimeHigh.add("value", ratio(at(accumulated, peak)));
        if (peak < dates.length)
            allTimeHigh.addProperty("date", dates[peak].toString());
        allTimeHigh.add("distance", ratio(at(drawdownSerie, drawdownSerie.length - 1)));
        json.add("allTimeHigh", allTimeHigh);

        return json;
    }

    /**
     * Upstream returns several parallel series that are normally the same length
     * as the dates array but are computed independently; reading past the end of
     * a shorter one would be a crash in production for an edge-case client.
     */
    private static double at(double[] values, int index)
    {
        return values != null && index >= 0 && index < values.length ? values[index] : 0d;
    }

    private static long at(long[] values, int index)
    {
        return values != null && index >= 0 && index < values.length ? values[index] : 0L;
    }

    /** a return ratio, or JSON null when the model cannot define it (NaN/infinite) */
    private static JsonElement ratio(double value)
    {
        return Double.isFinite(value) ? decimal(value) : JsonNull.INSTANCE;
    }

    /** a return ratio the model may not be able to define at all (null), not only non-finite */
    private static JsonElement ratio(Double value)
    {
        return value == null ? JsonNull.INSTANCE : ratio(value.doubleValue());
    }

    private static Money negate(Money money)
    {
        return Money.of(money.getCurrencyCode(), -money.getAmount());
    }

    /** a fixed-point long, e.g. an amount of money or a number of shares */
    static JsonElement decimal(long value, int precision)
    {
        return decimal(BigDecimal.valueOf(value, precision));
    }

    /** a computed ratio, e.g. the weight of a holding */
    static JsonElement decimal(double value)
    {
        return decimal(BigDecimal.valueOf(value));
    }

    /**
     * Renders a number as a plain decimal literal. Gson writes a Number by its
     * toString, and BigDecimal#toString switches to scientific notation both
     * for round numbers (1.1E+3) and for small ones (5E-7) - valid JSON, but
     * hostile to consumers, and the specification promises never to emit it.
     * Only toPlainString avoids both, so the token is built from it and parsed
     * back into a number.
     */
    private static JsonElement decimal(BigDecimal value)
    {
        // stripTrailingZeros is documented to misbehave for zero
        var plain = value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString(); //$NON-NLS-1$
        return JsonParser.parseString(plain);
    }

    public static JsonObject toJson(Portfolio portfolio, Money value)
    {
        var json = new JsonObject();
        json.addProperty("uuid", portfolio.getUUID()); //$NON-NLS-1$
        json.addProperty("name", portfolio.getName()); //$NON-NLS-1$
        if (portfolio.getNote() != null)
            json.addProperty("note", portfolio.getNote()); //$NON-NLS-1$
        if (portfolio.getReferenceAccount() != null)
            json.addProperty("referenceCashAccount", portfolio.getReferenceAccount().getUUID()); //$NON-NLS-1$
        json.add("value", toJson(value)); //$NON-NLS-1$
        return json;
    }

    /**
     * One entry of the "all transactions" list: an {@link AccountTransaction}
     * or {@link PortfolioTransaction}, plus the account or investment account
     * that owns it. {@code value} is the signed net cash flow (negative for a
     * debit/liquidation), matching the sign convention {@code CSVExporter}
     * already uses; {@code grossValue}, {@code fees} and {@code taxes} are
     * plain (unsigned) magnitudes in the transaction's own currency.
     */
    public static JsonObject toJson(TransactionPair<?> pair)
    {
        var transaction = pair.getTransaction();

        var json = new JsonObject();
        json.addProperty("uuid", transaction.getUUID());
        json.addProperty("date", transaction.getDateTime().toString());
        json.addProperty("type", wireType(transaction));
        json.add("value", toJson(Money.of(transaction.getCurrencyCode(), signedAmount(transaction))));
        json.add("grossValue", toJson(transaction.getGrossValue()));
        json.add("fees", toJson(transaction.getUnitSum(Transaction.Unit.Type.FEE)));
        json.add("taxes", toJson(transaction.getUnitSum(Transaction.Unit.Type.TAX)));

        var security = transaction.getSecurity();
        if (security != null)
        {
            json.add("shares", decimal(transaction.getShares(), Values.Share.precision()));

            var securityJson = new JsonObject();
            securityJson.addProperty("uuid", security.getUUID());
            securityJson.addProperty("name", security.getName());
            json.add("security", securityJson);
        }

        json.add("owner", toJsonOwner(pair));

        if (transaction.getNote() != null)
            json.addProperty("note", transaction.getNote());

        return json;
    }

    private static JsonObject toJsonOwner(TransactionPair<?> pair)
    {
        var json = new JsonObject();
        if (pair.isAccountTransaction())
        {
            var account = (Account) pair.getOwner();
            json.addProperty("uuid", account.getUUID());
            json.addProperty("name", account.getName());
            json.addProperty("type", "cash-account");
        }
        else
        {
            var portfolio = (Portfolio) pair.getOwner();
            json.addProperty("uuid", portfolio.getUUID());
            json.addProperty("name", portfolio.getName());
            json.addProperty("type", "investment-account");
        }
        return json;
    }

    /**
     * A stable, machine-readable transaction type, deliberately independent of
     * {@code Type#toString()} (locale-dependent, see {@code AttributeCodec}'s
     * class comment for the same concern with attribute types).
     */
    private static String wireType(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction t)
        {
            return switch (t.getType())
            {
                case DEPOSIT -> "deposit";
                case REMOVAL -> "removal";
                case INTEREST -> "interest";
                case INTEREST_CHARGE -> "interest-charge";
                case DIVIDENDS -> "dividends";
                case FEES -> "fees";
                case FEES_REFUND -> "fees-refund";
                case TAXES -> "taxes";
                case TAX_REFUND -> "tax-refund";
                case BUY -> "buy";
                case SELL -> "sell";
                case TRANSFER_IN -> "transfer-in";
                case TRANSFER_OUT -> "transfer-out";
            };
        }
        else if (transaction instanceof PortfolioTransaction t)
        {
            return switch (t.getType())
            {
                case BUY -> "buy";
                case SELL -> "sell";
                case TRANSFER_IN -> "transfer-in";
                case TRANSFER_OUT -> "transfer-out";
                case DELIVERY_INBOUND -> "delivery-inbound";
                case DELIVERY_OUTBOUND -> "delivery-outbound";
            };
        }
        throw new IllegalArgumentException("unsupported transaction type: " + transaction.getClass());
    }

    /** the net cash flow, signed negative for a debit (account) or liquidation (portfolio) transaction */
    private static long signedAmount(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction t)
            return t.getType().isDebit() ? -t.getAmount() : t.getAmount();
        else if (transaction instanceof PortfolioTransaction t)
            return t.getType().isLiquidation() ? -t.getAmount() : t.getAmount();
        throw new IllegalArgumentException("unsupported transaction type: " + transaction.getClass());
    }
}
