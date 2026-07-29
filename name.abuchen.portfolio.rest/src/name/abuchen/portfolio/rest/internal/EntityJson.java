package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.FileAccessRegistry.FileAccess;
import name.abuchen.portfolio.rest.spi.OpenFile;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
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

        return json;
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

    public static JsonObject toJson(Account account)
    {
        var json = new JsonObject();
        json.addProperty("uuid", account.getUUID()); //$NON-NLS-1$
        json.addProperty("name", account.getName()); //$NON-NLS-1$
        json.addProperty("currencyCode", account.getCurrencyCode()); //$NON-NLS-1$
        if (account.getNote() != null)
            json.addProperty("note", account.getNote()); //$NON-NLS-1$
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
     * uniformly - valued at the snapshot date in the reporting currency.
     */
    public static JsonObject toJson(ClientSnapshot snapshot, String reportingCurrency)
    {
        var total = snapshot.getMonetaryAssets();

        var json = new JsonObject();
        json.addProperty("date", snapshot.getTime().toString()); //$NON-NLS-1$
        json.addProperty("reportingCurrency", reportingCurrency); //$NON-NLS-1$
        json.add("totalAssets", toJson(total)); //$NON-NLS-1$

        var items = new JsonArray();
        snapshot.getAssetPositions() //
                        .sorted(new AssetPosition.ByDescription()) //
                        .forEach(position -> items.add(toJson(position, total)));
        json.add("items", items); //$NON-NLS-1$
        return json;
    }

    private static JsonObject toJson(AssetPosition position, Money totalAssets)
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
        }

        json.add("valuation", toJson(position.getValuation())); //$NON-NLS-1$
        json.add("weight", decimal(totalAssets.isZero() ? 0d : position.getShare())); //$NON-NLS-1$

        // the value before conversion into the reporting currency; only worth
        // stating when the two differ
        var local = position.getPosition().calculateValue();
        if (!local.getCurrencyCode().equals(position.getValuation().getCurrencyCode()))
            json.add("localValuation", toJson(local)); //$NON-NLS-1$

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
     * <p>
     * The cost method is echoed because it moves the numbers: without it a
     * stored or forwarded response cannot be interpreted, as the split it shows
     * depends on a parameter it would not record.
     */
    public static JsonObject performance(LocalDate openingDate, LocalDate closingDate, String reportingCurrency,
                    CostMethod costMethod, double ttwror, double irr, ClientPerformanceSnapshot performance)
    {
        var json = new JsonObject();
        json.addProperty("openingDate", openingDate.toString()); //$NON-NLS-1$
        json.addProperty("closingDate", closingDate.toString()); //$NON-NLS-1$
        json.addProperty("reportingCurrency", reportingCurrency); //$NON-NLS-1$
        json.addProperty("costMethod", CalcParams.wireName(costMethod)); //$NON-NLS-1$
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
     * The six context fields of a per-instrument performance report: the
     * interval, the reporting currency and the three parameters that move the
     * numbers, so that the payload is self-describing.
     */
    public static JsonObject instrumentPerformanceContext(InstrumentPerformanceHandler.Params params)
    {
        var json = new JsonObject();
        json.addProperty("openingDate", params.openingDate().toString()); //$NON-NLS-1$
        json.addProperty("closingDate", params.closingDate().toString()); //$NON-NLS-1$
        json.addProperty("reportingCurrency", params.currency()); //$NON-NLS-1$
        json.addProperty("costMethod", CalcParams.wireName(params.costMethod())); //$NON-NLS-1$
        json.addProperty("taxesAndFees", CalcParams.wireName(params.taxesAndFees())); //$NON-NLS-1$

        var metrics = new JsonArray();
        // in the enum's declaration order, not the order the client asked for:
        // the echo states what was selected, not how it was spelled
        for (var group : MetricGroup.values())
            if (params.metrics().contains(group))
                metrics.add(group.getWireName());
        json.add("metrics", metrics); //$NON-NLS-1$

        return json;
    }

    /**
     * One instrument's performance over the interval, as an object per selected
     * metric group - an unselected group is an absent key, so presence needs no
     * cross-reference to the envelope.
     * <p/>
     * Every money field here is a magnitude, not a signed contribution:
     * {@code fees} is the sum of the fees charged and is emitted positive. Only
     * a field that is a term of a reconciling sum is signed, and this resource
     * publishes no such sum - unlike the aggregate {@code /performance}, whose
     * breakdown must add up.
     */
    public static JsonObject instrumentPerformance(LazySecurityPerformanceRecord record,
                    InstrumentPerformanceHandler.Params params)
    {
        var security = record.getSecurity();

        var json = new JsonObject();
        json.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
        json.addProperty("name", security.getName()); //$NON-NLS-1$
        // the instrument's own currency, which is what explains a non-zero
        // currency component; omitted for the rare instrument without one
        if (security.getCurrencyCode() != null)
            json.addProperty("currencyCode", security.getCurrencyCode()); //$NON-NLS-1$

        var metrics = params.metrics();

        if (metrics.contains(MetricGroup.VALUATION))
        {
            var valuation = new JsonObject();
            valuation.add("shares", decimal(record.getSharesHeld(), Values.Share.precision())); //$NON-NLS-1$
            valuation.add("openingValue", toJson(record.getOpeningValue())); //$NON-NLS-1$
            valuation.add("closingValue", toJson(record.getMarketValue())); //$NON-NLS-1$
            valuation.add("periodCostBasis", //$NON-NLS-1$
                            toJson(record.getCost(params.costMethod(), params.taxesAndFees())));
            json.add("valuation", valuation); //$NON-NLS-1$
        }

        if (metrics.contains(MetricGroup.GAINS))
        {
            var realized = record.getRealizedCapitalGains(params.costMethod(), params.taxesAndFees());
            var unrealized = record.getUnrealizedCapitalGains(params.costMethod(), params.taxesAndFees());

            var gains = new JsonObject();
            gains.add("realizedCapitalGains", toJson(realized.getCapitalGains())); //$NON-NLS-1$
            gains.add("realizedCurrencyComponent", toJson(realized.getForexCaptialGains())); //$NON-NLS-1$
            gains.add("unrealizedCapitalGains", toJson(unrealized.getCapitalGains())); //$NON-NLS-1$
            gains.add("unrealizedCurrencyComponent", toJson(unrealized.getForexCaptialGains())); //$NON-NLS-1$
            json.add("gains", gains); //$NON-NLS-1$
        }

        if (metrics.contains(MetricGroup.INCOME))
        {
            var income = new JsonObject();
            income.add("dividends", toJson(record.getSumOfDividends())); //$NON-NLS-1$
            income.addProperty("dividendCount", record.getDividendEventCount()); //$NON-NLS-1$
            if (record.getLastDividendPayment() != null)
                income.addProperty("lastDividend", record.getLastDividendPayment().toString()); //$NON-NLS-1$
            json.add("income", income); //$NON-NLS-1$
        }

        if (metrics.contains(MetricGroup.EXPENSES))
        {
            var expenses = new JsonObject();
            expenses.add("fees", toJson(record.getFees())); //$NON-NLS-1$
            expenses.add("taxes", toJson(record.getTaxes())); //$NON-NLS-1$
            json.add("expenses", expenses); //$NON-NLS-1$
        }

        if (metrics.contains(MetricGroup.MONEY_WEIGHTED))
        {
            var moneyWeighted = new JsonObject();
            moneyWeighted.add("irr", ratio(record.getIrr())); //$NON-NLS-1$
            json.add("moneyWeighted", moneyWeighted); //$NON-NLS-1$
        }

        // the two groups below share one PerformanceIndex - a full daily series
        // for the instrument - so asking for both costs the same as either

        if (metrics.contains(MetricGroup.TIME_WEIGHTED))
        {
            var timeWeighted = new JsonObject();
            timeWeighted.add("ttwror", ratio(record.getTrueTimeWeightedRateOfReturn())); //$NON-NLS-1$
            timeWeighted.add("ttwrorAnnualized", ratio(record.getTrueTimeWeightedRateOfReturnAnnualized())); //$NON-NLS-1$
            json.add("timeWeighted", timeWeighted); //$NON-NLS-1$
        }

        if (metrics.contains(MetricGroup.RISK))
        {
            var drawdown = record.getDrawdown();
            var volatility = record.getVolatility();

            var risk = new JsonObject();
            risk.add("volatility", ratio(volatility.getStandardDeviation())); //$NON-NLS-1$
            risk.add("semiVolatility", ratio(volatility.getSemiDeviation())); //$NON-NLS-1$
            // a positive fraction, as the model reports it - the charts' sign
            // convention is a presentation choice, not the datum
            risk.add("maxDrawdown", ratio(drawdown.getMaxDrawdown())); //$NON-NLS-1$
            // the longest stretch below a peak, which is not necessarily the
            // stretch containing the deepest drawdown - hence not named
            // maxDrawdownDuration
            risk.addProperty("longestDrawdownDays", drawdown.getMaxDrawdownDuration().getDays()); //$NON-NLS-1$
            json.add("risk", risk); //$NON-NLS-1$
        }

        return json;
    }

    /** a return ratio, or JSON null when the model cannot define it (NaN/infinite) */
    static JsonElement ratio(double value)
    {
        return Double.isFinite(value) ? decimal(value) : JsonNull.INSTANCE;
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

    public static JsonObject toJson(Portfolio portfolio)
    {
        var json = new JsonObject();
        json.addProperty("uuid", portfolio.getUUID()); //$NON-NLS-1$
        json.addProperty("name", portfolio.getName()); //$NON-NLS-1$
        if (portfolio.getNote() != null)
            json.addProperty("note", portfolio.getNote()); //$NON-NLS-1$
        if (portfolio.getReferenceAccount() != null)
            json.addProperty("referenceCashAccount", portfolio.getReferenceAccount().getUUID()); //$NON-NLS-1$
        return json;
    }
}
