package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class InstrumentPerformanceTest
{
    private static final String OPENING = "2024-01-01";
    private static final String CLOSING = "2024-12-31";

    // -- calling the two routes ------------------------------------------

    /** calls the collection with a factory of its own, as the host would provide it */
    private static JsonObject list(Client client, String opening, String closing, String currency, String costMethod,
                    String taxesAndFees, String metrics)
    {
        return InstrumentPerformanceHandler.list(client, new ExchangeRateProviderFactory(client), opening, closing,
                        currency, costMethod, taxesAndFees, metrics).getAsJsonObject();
    }

    private static JsonObject list(Client client)
    {
        return list(client, OPENING, CLOSING, null, null, null, null);
    }

    private static JsonObject item(Client client, String uuid, String currency)
    {
        return InstrumentPerformanceHandler.get(client, new ExchangeRateProviderFactory(client), uuid, OPENING, CLOSING,
                        currency, null, null, null).getAsJsonObject();
    }

    // -- shape and echoes ------------------------------------------------

    /**
     * A position carried through the whole interval: 10 shares valued 1000 at
     * the start and 1200 at the end, no trades. Per M1 the cost basis of a
     * carried-in lot is its opening valuation, not what was once paid for it -
     * which is precisely why openingValue is published next to it.
     */
    @Test
    public void testCollectionShape()
    {
        var client = heldPosition();
        var result = list(client);

        assertThat(result.get("items").getAsJsonArray().size(), is(1));

        var item = onlyItem(result);
        assertThat(item.get("uuid").getAsString(), is(instrumentOf(client).getUUID()));
        assertThat(item.get("name").getAsString(), is("ACME"));
        assertThat(item.get("currencyCode").getAsString(), is("EUR"));

        var valuation = item.get("valuation").getAsJsonObject();
        assertThat(valuation.get("shares").getAsDouble(), is(10d));
        assertThat(money(valuation, "openingValue"), is(1000d));
        assertThat(money(valuation, "closingValue"), is(1200d));
        assertThat(money(valuation, "periodCostBasis"), is(1000d));

        var gains = item.get("gains").getAsJsonObject();
        assertThat(money(gains, "unrealizedCapitalGains"), is(200d));
        assertThat(money(gains, "realizedCapitalGains"), is(0d));
        assertThat(money(gains, "unrealizedCurrencyComponent"), is(0d));
        assertThat(money(gains, "realizedCurrencyComponent"), is(0d));

        // a position held all year with no flows returns its bare 20 %
        assertThat(item.get("timeWeighted").getAsJsonObject().get("ttwror").getAsDouble(), closeTo(0.20, 1e-6));
    }

    /** all six parameters are echoed, so the numbers are self-describing */
    @Test
    public void testParametersAreEchoed()
    {
        var result = list(heldPosition(), OPENING, CLOSING, "USD", "moving-average", "excluded", null);

        assertThat(result.get("openingDate").getAsString(), is(OPENING));
        assertThat(result.get("closingDate").getAsString(), is(CLOSING));
        assertThat(result.get("reportingCurrency").getAsString(), is("USD"));
        assertThat(result.get("costMethod").getAsString(), is("moving-average"));
        assertThat(result.get("taxesAndFees").getAsString(), is("excluded"));

        assertThat(groupNames(result), is(List.of("valuation", "gains", "income", "expenses", "moneyWeighted",
                        "timeWeighted", "risk")));
    }

    /** the echo states the value that was applied, not an omitted property */
    @Test
    public void testDefaultsAreEchoedAsAppliedValues()
    {
        var result = list(heldPosition());

        assertThat(result.get("closingDate").getAsString(), is(CLOSING));
        assertThat(result.get("reportingCurrency").getAsString(), is("EUR"));
        assertThat(result.get("costMethod").getAsString(), is("fifo"));
        assertThat(result.get("taxesAndFees").getAsString(), is("included"));
        assertThat(groupNames(result).size(), is(7));
    }

    /**
     * Fees are a magnitude here, not a signed contribution: this resource
     * publishes no reconciling sum, so the negation the aggregate breakdown
     * needs would be noise.
     */
    @Test
    public void testFeesAndTaxesArePositiveMagnitudes()
    {
        var client = tradedPosition((portfolio, security) -> portfolio.buy(security, "2024-03-01",
                        Values.Share.factorize(5), Values.Amount.factorize(555), Values.Amount.factorize(20),
                        Values.Amount.factorize(15)));

        var expenses = onlyItem(list(client)).get("expenses").getAsJsonObject();
        assertThat(money(expenses, "fees"), is(20d));
        assertThat(money(expenses, "taxes"), is(15d));
    }

    // -- metric groups ---------------------------------------------------

    @Test
    public void testUnselectedGroupsAreAbsentKeys()
    {
        var item = onlyItem(list(heldPosition(), OPENING, CLOSING, null, null, null, "valuation,gains"));

        assertThat(item.has("valuation"), is(true));
        assertThat(item.has("gains"), is(true));
        assertThat(item.has("income"), is(false));
        assertThat(item.has("expenses"), is(false));
        assertThat(item.has("moneyWeighted"), is(false));
        assertThat(item.has("timeWeighted"), is(false));
        assertThat(item.has("risk"), is(false));

        // the identity fields are not a group and are always present
        assertThat(item.has("uuid"), is(true));
        assertThat(item.has("name"), is(true));
    }

    @Test
    public void testSelectedGroupsAreEchoed()
    {
        var result = list(heldPosition(), OPENING, CLOSING, null, null, null, "risk,valuation");
        assertThat(groupNames(result), is(List.of("valuation", "risk")));
    }

    /**
     * The point of {@code ?metrics=}: {@code timeWeighted} and {@code risk} are
     * backed by a {@code PerformanceIndex} - a full daily series per instrument
     * - while every other group is a linear pass over the line items. Building
     * that series values the portfolio on every day of the interval, so a
     * request that stays below one conversion per day cannot have built one.
     */
    @Test
    public void testCheapMetricsNeverBuildAPerformanceIndex()
    {
        var client = heldPosition();
        var days = (int) ChronoUnit.DAYS.between(LocalDate.parse(OPENING), LocalDate.parse(CLOSING));

        var cheap = conversionsFor(client, "valuation,gains,income,expenses,moneyWeighted");
        var full = conversionsFor(client, null);

        assertThat("a daily series needs at least one conversion per day", cheap, is(lessThan(days)));
        assertThat("the full request must build the daily series", full, is(greaterThan(days)));
    }

    /** the conversions the handler routes through the reporting converter */
    private static int conversionsFor(Client client, String metrics)
    {
        var counter = new AtomicInteger();
        var factory = new ExchangeRateProviderFactory(client);
        Function<String, CurrencyConverter> converters = currency -> new CountingConverter(
                        new CurrencyConverterImpl(factory, currency), counter);

        InstrumentPerformanceHandler.list(client, converters, OPENING, CLOSING, null, null, null, metrics);
        return counter.get();
    }

    // -- population and ordering -----------------------------------------

    /**
     * A position sold during the period stays in the report with zero shares:
     * dropping it would silently erase part of the period's performance.
     */
    @Test
    public void testPositionClosedDuringThePeriodIsPresent()
    {
        var client = tradedPosition((portfolio, security) -> portfolio.sell(security, "2024-06-01",
                        Values.Share.factorize(10), Values.Amount.factorize(1300)));

        var item = onlyItem(list(client));
        var valuation = item.get("valuation").getAsJsonObject();

        assertThat(valuation.get("shares").getAsDouble(), is(0d));
        assertThat(money(valuation, "closingValue"), is(0d));
        assertThat(money(item.get("gains").getAsJsonObject(), "realizedCapitalGains"), is(300d));
    }

    /** an instrument never held and never traded simply does not appear */
    @Test
    public void testNeverHeldInstrumentIsAbsent()
    {
        var client = heldPosition();
        untraded(client);

        assertThat(names(list(client)), is(List.of("ACME")));
    }

    /**
     * The snapshot collects its records in a HashMap, so without sorting the
     * order is not even stable between two calls on the same client.
     */
    @Test
    public void testOrderingIsByNameAndDeterministic()
    {
        var client = new Client();
        for (String name : List.of("Zeta", "Alpha", "Mu"))
            heldSecurity(client, name, CurrencyUnit.EUR, (account, security) -> {}, (portfolio, security) -> {});

        assertThat(names(list(client)), is(List.of("Alpha", "Mu", "Zeta")));
        assertThat(names(list(client)), is(List.of("Alpha", "Mu", "Zeta")));
        assertThat(names(list(client)), is(List.of("Alpha", "Mu", "Zeta")));
    }

    // -- interval boundaries ---------------------------------------------

    /**
     * The interval is half-open {@code (openingDate, closingDate]}: a purchase
     * dated exactly on the opening date belongs to the opening balance, not to
     * the period's flows, so it lands in openingValue and the lot is seeded
     * from that valuation rather than from the trade.
     */
    @Test
    public void testBuyOnOpeningDateIsOpeningBalance()
    {
        var client = new Client();
        var security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice(CLOSING, Values.Quote.factorize(120)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(2000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, OPENING, Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var valuation = onlyItem(list(client)).get("valuation").getAsJsonObject();

        assertThat(money(valuation, "openingValue"), is(1000d));
        assertThat(money(valuation, "closingValue"), is(1200d));
        assertThat(money(valuation, "periodCostBasis"), is(1000d));
    }

    /** a sale on the closing date is inside the period and realizes its gain */
    @Test
    public void testSellOnClosingDateIsInPeriod()
    {
        var client = tradedPosition((portfolio, security) -> portfolio.sell(security, CLOSING,
                        Values.Share.factorize(10), Values.Amount.factorize(1200)));

        var item = onlyItem(list(client));
        assertThat(money(item.get("gains").getAsJsonObject(), "realizedCapitalGains"), is(200d));
        assertThat(item.get("valuation").getAsJsonObject().get("shares").getAsDouble(), is(0d));
    }

    /**
     * A dividend on the opening date falls outside the half-open interval; one
     * on the closing date falls inside it.
     */
    @Test
    public void testDividendsAtTheIntervalEdges()
    {
        var onOpening = heldPosition(
                        (account, security) -> account.dividend(OPENING, Values.Amount.factorize(50), security));
        var onClosing = heldPosition(
                        (account, security) -> account.dividend(CLOSING, Values.Amount.factorize(50), security));

        assertThat(money(onlyItem(list(onOpening)).get("income").getAsJsonObject(), "dividends"), is(0d));

        var income = onlyItem(list(onClosing)).get("income").getAsJsonObject();
        assertThat(money(income, "dividends"), is(50d));
        assertThat(income.get("dividendCount").getAsInt(), is(1));
        assertThat(income.get("lastDividend").getAsString(), is(CLOSING));
    }

    /** lastDividend is omitted rather than null when there is no payment */
    @Test
    public void testLastDividendIsOmittedWhenThereIsNone()
    {
        var income = onlyItem(list(heldPosition())).get("income").getAsJsonObject();
        assertThat(income.has("lastDividend"), is(false));
        assertThat(income.get("dividendCount").getAsInt(), is(0));
    }

    // -- the I18 identity ------------------------------------------------

    /**
     * The one arithmetic relation this resource guarantees:
     * {@code closingValue - periodCostBasis = unrealizedCapitalGains}, under
     * both cost bases.
     * <p>
     * The fixture is deliberately a carried-in position <em>plus</em> an
     * in-period purchase carrying a fee. A carried-in position alone reconciles
     * under either setting - both engines seed it from the same opening
     * valuation - and would hide the divergence entirely.
     */
    @Test
    public void testIdentityHoldsUnderBothCostBases()
    {
        // 10 more shares, gross 1000 plus a fee of 18.90
        var client = tradedPosition((portfolio, security) -> portfolio.buy(security, "2024-03-01",
                        Values.Share.factorize(10), Values.Amount.factorize(1018.90), Values.Amount.factorize(18.90),
                        0));

        var included = onlyItem(list(client, OPENING, CLOSING, null, null, "included", null));
        assertThat(money(included.get("valuation").getAsJsonObject(), "periodCostBasis"), is(2018.90d));
        assertThat(money(included.get("gains").getAsJsonObject(), "unrealizedCapitalGains"), is(381.10d));
        assertIdentity(included);

        var excluded = onlyItem(list(client, OPENING, CLOSING, null, null, "excluded", null));
        assertThat(money(excluded.get("valuation").getAsJsonObject(), "periodCostBasis"), is(2000d));
        assertThat(money(excluded.get("gains").getAsJsonObject(), "unrealizedCapitalGains"), is(400d));
        assertIdentity(excluded);
    }

    /**
     * M11: the cost stack and the gains stack apportion a partial disposal
     * independently, each rounding on its own, so the identity is exact only
     * for wholly-relieved lots. This measures how far apart that drives them -
     * the measurement the specification asks for before the identity is
     * published.
     */
    @Test
    public void testIdentityAfterAPartialDisposal()
    {
        var client = new Client();
        var security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(28.891)) //
                        .addPrice(CLOSING, Values.Quote.factorize(33.717)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(10000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20),
                                        Values.Amount.factorize(7.13), 0) //
                        .buy(security, "2024-03-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92),
                                        Values.Amount.factorize(4.27), 0) //
                        .sell(security, "2024-06-01", Values.Share.factorize(15), Values.Amount.factorize(531.50),
                                        Values.Amount.factorize(2.11)) //
                        .addTo(client);

        // both engines, both bases: the moving-average stacks relieve a partial
        // disposal by a different formula than the FIFO ones, so each pairing
        // has to be checked on its own
        for (String method : List.of("fifo", "moving-average"))
            for (String basis : List.of("included", "excluded"))
                assertIdentity(onlyItem(list(client, OPENING, CLOSING, null, method, basis, null)));
    }

    /**
     * The case that used to leave a residue (M12): a foreign security acquired
     * through an account in another currency and reported in the security's own
     * currency, so that the exchange rate recorded on the transaction is what
     * both engines must use. Since the FIFO engine now takes that rate, the
     * identity is exact here too.
     */
    @Test
    public void testIdentityForAForeignCurrencyPosition()
    {
        var result = InstrumentPerformanceHandler
                        .list(forexPosition(), TestCurrencyConverter::new, "2014-12-31", "2015-01-16", "USD", null,
                                        null, null)
                        .getAsJsonObject();

        var item = onlyItem(result);
        var valuation = item.get("valuation").getAsJsonObject();

        assertThat(result.get("reportingCurrency").getAsString(), is("USD"));
        // 100 shares at 100 USD, against the 5000 USD recorded on the trade
        assertThat(money(valuation, "closingValue"), is(10000d));
        assertThat(money(valuation, "periodCostBasis"), is(5000d));
        assertIdentity(item);
    }

    /**
     * The identity, asserted as exact. Measured across roughly a thousand
     * partial-disposal combinations under both cost methods and both cost
     * bases, and across foreign-currency positions with the rate recorded on
     * the transaction - including the case where the gains engine squashes
     * multiple opening valuations. The tolerance is here only to absorb the
     * double arithmetic of this comparison; the amounts themselves agree to the
     * minor unit.
     */
    private static void assertIdentity(JsonObject item)
    {
        var valuation = item.get("valuation").getAsJsonObject();
        var gains = item.get("gains").getAsJsonObject();

        assertThat("closingValue - periodCostBasis = unrealizedCapitalGains",
                        money(valuation, "closingValue") - money(valuation, "periodCostBasis"),
                        closeTo(money(gains, "unrealizedCapitalGains"), 1e-9));
    }

    // -- the item route --------------------------------------------------

    @Test
    public void testItemMergesTheContextWithTheInstrumentsOwnFields()
    {
        var client = heldPosition();
        var result = item(client, instrumentOf(client).getUUID(), null);

        assertThat(result.get("openingDate").getAsString(), is(OPENING));
        assertThat(result.get("reportingCurrency").getAsString(), is("EUR"));
        assertThat(result.get("name").getAsString(), is("ACME"));
        assertThat(result.has("items"), is(false));

        assertThat(money(result.get("valuation").getAsJsonObject(), "closingValue"), is(1200d));
    }

    /**
     * On the item route the report's currency and the instrument's own currency
     * become adjacent top-level fields - the misreading hazard the three-way
     * naming rule exists for. They must be tellable apart, hence
     * reportingCurrency and currencyCode rather than two spellings of one word.
     */
    @Test
    public void testItemKeepsTheThreeCurrencyConceptsApart()
    {
        var client = new Client();
        var security = heldSecurity(client, "Apple Inc.", CurrencyUnit.USD, (account, instrument) -> {},
                        (portfolio, instrument) -> {});

        var result = item(client, security.getUUID(), "EUR");

        assertThat(result.get("reportingCurrency").getAsString(), is("EUR"));
        assertThat(result.get("currencyCode").getAsString(), is("USD"));
        assertThat(result.get("currencyCode").getAsString(), is(not(result.get("reportingCurrency").getAsString())));

        // and every amount states the currency of that amount - the report's
        assertThat(result.get("valuation").getAsJsonObject().get("closingValue").getAsJsonObject().get("currency")
                        .getAsString(), is("EUR"));
    }

    @Test
    public void testUnknownInstrumentIsNotFound()
    {
        var client = heldPosition();

        try
        {
            item(client, "does-not-exist", null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
            assertThat(e.getType(), is("not-found"));
        }
    }

    /**
     * A known instrument with nothing in the interval is a different problem
     * from an unknown id, and calls for different client behaviour: report that
     * nothing was held, rather than fix the id.
     */
    @Test
    public void testKnownButUnheldInstrumentIsNoActivityInPeriod()
    {
        var client = heldPosition();
        var sleeper = untraded(client);

        try
        {
            item(client, sleeper.getUUID(), null);
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
            assertThat(e.getType(), is("no-activity-in-period"));
            assertThat(e.getDetail().contains("Sleeper"), is(true));
        }
    }

    // -- cross-endpoint consistency --------------------------------------

    /**
     * An instrument's closing value must equal its valuation in the holdings
     * report at the same date. Both are a valuation at one date, but they reach
     * it by different paths - ClientSnapshot versus the performance snapshot's
     * own line items - so the agreement is worth pinning.
     */
    @Test
    public void testClosingValueMatchesHoldingsValuation()
    {
        var client = heldPosition();

        var closingValue = money(onlyItem(list(client)).get("valuation").getAsJsonObject(), "closingValue");

        var holdings = HoldingsHandler.list(client, new ExchangeRateProviderFactory(client), CLOSING, null)
                        .getAsJsonObject();
        var holding = holdings.get("items").getAsJsonArray().asList().stream() //
                        .map(element -> element.getAsJsonObject())
                        .filter(element -> "ACME".equals(element.get("name").getAsString())).findFirst()
                        .orElseThrow(() -> new AssertionError("ACME missing from the holdings report"));

        assertThat(holding.get("valuation").getAsJsonObject().get("value").getAsDouble(), is(closingValue));
    }

    // -- parameter validation --------------------------------------------

    @Test
    public void testMissingOpeningDateIs400()
    {
        assertFieldError(() -> list(heldPosition(), null, CLOSING, null, null, null, null), "openingDate", "required");
    }

    @Test
    public void testInvalidOpeningDateIs400()
    {
        assertFieldError(() -> list(heldPosition(), "not-a-date", CLOSING, null, null, null, null), "openingDate",
                        "invalid-value");
    }

    @Test
    public void testInvertedRangeIs400()
    {
        assertFieldError(() -> list(heldPosition(), CLOSING, OPENING, null, null, null, null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testEmptyRangeIs400()
    {
        assertFieldError(() -> list(heldPosition(), OPENING, OPENING, null, null, null, null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        assertFieldError(() -> list(heldPosition(), OPENING, CLOSING, "ZZZ", null, null, null), "currency",
                        "unknown-currency");
    }

    @Test
    public void testInvalidCostMethodIs400()
    {
        assertFieldError(() -> list(heldPosition(), OPENING, CLOSING, null, "lifo", null, null), "costMethod",
                        "invalid-value");
    }

    @Test
    public void testInvalidTaxesAndFeesIs400()
    {
        assertFieldError(() -> list(heldPosition(), OPENING, CLOSING, null, null, "net", null), "taxesAndFees",
                        "invalid-value");
    }

    @Test
    public void testUnknownMetricGroupIs400()
    {
        assertFieldError(() -> list(heldPosition(), OPENING, CLOSING, null, null, null, "valuation,sharpe"), "metrics",
                        "invalid-value");
    }

    /** an empty selection is a mistake, not a request for nothing */
    @Test
    public void testEmptyMetricsIs400()
    {
        assertFieldError(() -> list(heldPosition(), OPENING, CLOSING, null, null, null, ""), "metrics",
                        "invalid-value");
    }

    /** the item route validates the same six parameters as the collection */
    @Test
    public void testItemRouteValidatesTheSameParameters()
    {
        var client = heldPosition();
        var uuid = instrumentOf(client).getUUID();

        assertFieldError(() -> InstrumentPerformanceHandler.get(client, new ExchangeRateProviderFactory(client), uuid,
                        OPENING, CLOSING, null, null, null, "sharpe"), "metrics", "invalid-value");
    }

    /**
     * Every actionable violation is reported in one 400, so an agent
     * self-corrects in a single round-trip - the range constraint included,
     * which depends only on the two dates and must not hide behind an unrelated
     * bad parameter.
     */
    @Test
    public void testAllViolationsAreReportedTogether()
    {
        var errors = errorsOf(() -> list(heldPosition(), CLOSING, OPENING, "ZZZ", null, null, "sharpe"));

        assertThat(errors.size(), is(3));
        assertThat(codeOf(errors, "currency"), is("unknown-currency"));
        assertThat(codeOf(errors, "metrics"), is("invalid-value"));
        assertThat(codeOf(errors, "closingDate"), is("invalid-range"));
    }

    /**
     * A date that never parsed cannot be judged against the range: without the
     * null guard the closing date would default to today and emit a spurious
     * second error on the same field.
     */
    @Test
    public void testUnparseableClosingDateYieldsNoSpuriousRangeError()
    {
        var errors = errorsOf(() -> list(heldPosition(), "2026-01-01", "not-a-date", null, null, null, null));

        assertThat(errors.size(), is(1));
        assertThat(codeOf(errors, "closingDate"), is("invalid-value"));
    }

    /** likewise when the opening date is missing altogether */
    @Test
    public void testMissingOpeningDateYieldsNoRangeError()
    {
        var errors = errorsOf(() -> list(heldPosition(), null, CLOSING, null, null, null, null));

        assertThat(errors.size(), is(1));
        assertThat(codeOf(errors, "openingDate"), is("required"));
    }

    // -- fixtures --------------------------------------------------------

    /** 10 shares bought at 100 before the interval, valued at 120 at its end */
    private static Client heldPosition()
    {
        return heldPosition((account, security) -> {
            // no further account transactions
        });
    }

    private static Client heldPosition(BiConsumer<AccountBuilder, Security> within)
    {
        return heldPosition(within, (portfolio, security) -> {
            // no further trades
        });
    }

    /** the same position, plus trades inside the interval */
    private static Client tradedPosition(BiConsumer<PortfolioBuilder, Security> trades)
    {
        return heldPosition((account, security) -> {
            // no further account transactions
        }, trades);
    }

    private static Client heldPosition(BiConsumer<AccountBuilder, Security> within,
                    BiConsumer<PortfolioBuilder, Security> trades)
    {
        var client = new Client();
        heldSecurity(client, "ACME", CurrencyUnit.EUR, within, trades);
        return client;
    }

    private static Security heldSecurity(Client client, String name, String currency,
                    BiConsumer<AccountBuilder, Security> within, BiConsumer<PortfolioBuilder, Security> trades)
    {
        var security = new SecurityBuilder(currency) //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice(CLOSING, Values.Quote.factorize(120)) //
                        .addTo(client);
        security.setName(name);

        var builder = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(1000));
        within.accept(builder, security);
        var account = builder.addTo(client);

        // every trade must go through the same investment account: FIFO relieves
        // a sale only against lots held by the same owner
        var portfolio = new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000));
        trades.accept(portfolio, security);
        portfolio.addTo(client);

        return security;
    }

    /** an instrument the file knows but that was never held nor traded */
    private static Security untraded(Client client)
    {
        var security = new SecurityBuilder().addPrice("2023-06-01", Values.Quote.factorize(50)).addTo(client);
        security.setName("Sleeper");
        return security;
    }

    /**
     * A USD security acquired through a EUR account, with the exchange rate
     * recorded on the transaction.
     */
    private static Client forexPosition()
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        var security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2014-12-31", Values.Quote.factorize(80)) //
                        .addPrice("2015-01-16", Values.Quote.factorize(100)) //
                        .addTo(client);
        security.setName("Apple Inc.");

        var account = new AccountBuilder(CurrencyUnit.EUR).addTo(client);
        new PortfolioBuilder(account) //
                        .inbound_delivery(security, "2015-01-05", Values.Share.factorize(100),
                                        new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4500)),
                                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(5000)),
                                                        BigDecimal.valueOf(0.90)))
                        .addTo(client);

        return client;
    }

    private static Security instrumentOf(Client client)
    {
        return client.getSecurities().get(0);
    }

    /**
     * Counts every conversion routed through it, handing the counter on to the
     * derived converters so that a nested {@code with(...)} still reports home.
     */
    private static final class CountingConverter implements CurrencyConverter
    {
        private final CurrencyConverter delegate;
        private final AtomicInteger counter;

        CountingConverter(CurrencyConverter delegate, AtomicInteger counter)
        {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public String getTermCurrency()
        {
            counter.incrementAndGet();
            return delegate.getTermCurrency();
        }

        @Override
        public ExchangeRate getRate(LocalDate date, String currencyCode)
        {
            counter.incrementAndGet();
            return delegate.getRate(date, currencyCode);
        }

        @Override
        public CurrencyConverter with(String currencyCode)
        {
            return new CountingConverter(delegate.with(currencyCode), counter);
        }
    }

    // -- assertions ------------------------------------------------------

    private static JsonObject onlyItem(JsonObject result)
    {
        return result.get("items").getAsJsonArray().get(0).getAsJsonObject();
    }

    private static double money(JsonObject group, String field)
    {
        return group.get(field).getAsJsonObject().get("value").getAsDouble();
    }

    private static List<String> groupNames(JsonObject result)
    {
        var names = new ArrayList<String>();
        result.get("metrics").getAsJsonArray().forEach(element -> names.add(element.getAsString()));
        return names;
    }

    private static List<String> names(JsonObject result)
    {
        var names = new ArrayList<String>();
        result.get("items").getAsJsonArray()
                        .forEach(element -> names.add(element.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    private static void assertFieldError(Runnable call, String field, String code)
    {
        assertThat(codeOf(errorsOf(call), field), is(code));
    }

    private static List<ApiException.FieldError> errorsOf(Runnable call)
    {
        try
        {
            call.run();
            throw new AssertionError("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            return e.getErrors();
        }
    }

    private static String codeOf(List<ApiException.FieldError> errors, String field)
    {
        return errors.stream().filter(error -> field.equals(error.field())).map(ApiException.FieldError::code)
                        .findFirst().orElseThrow(() -> new AssertionError("no error on field " + field));
    }
}
