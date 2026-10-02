package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class PerformanceTest
{
    /** calls the handler with a factory of its own, as the host would provide it */
    private static JsonObject perf(Client client, String from, String to, String currency, String costMethod)
    {
        return PerformanceHandler.list(client, new ExchangeRateProviderFactory(client), from, to, currency, costMethod)
                        .getAsJsonObject();
    }

    /**
     * A position bought before the interval and held throughout: its value
     * rises 1000 → 1200 over the year with no trades, so the entire change is
     * an unrealized capital gain, TTWROR is 20 %, and the breakdown reconciles.
     */
    @Test
    public void testReturnsAndReconciliation()
    {
        var client = heldPosition();

        var result = perf(client, "2024-01-01", "2024-12-31", null, null);

        assertThat(result.get("openingDate").getAsString(), is("2024-01-01"));
        assertThat(result.get("closingDate").getAsString(), is("2024-12-31"));
        assertThat(result.get("reportingCurrency").getAsString(), is("EUR"));

        assertThat(result.get("ttwror").getAsDouble(), closeTo(0.20, 1e-6));
        assertThat(result.get("irr").getAsDouble(), closeTo(0.20, 0.02));

        var breakdown = result.get("breakdown").getAsJsonObject();
        assertThat(value(breakdown, "openingValue"), is(1000d));
        assertThat(value(breakdown, "unrealizedCapitalGains"), is(200d));
        assertThat(value(breakdown, "realizedCapitalGains"), is(0d));
        assertThat(value(breakdown, "income"), is(0d));
        assertThat(value(breakdown, "fees"), is(0d));
        assertThat(value(breakdown, "taxes"), is(0d));
        assertThat(value(breakdown, "currencyGains"), is(0d));
        assertThat(value(breakdown, "netDeposits"), is(0d));
        assertThat(value(breakdown, "closingValue"), is(1200d));

        assertReconciles(breakdown);
    }

    /**
     * Fees and taxes are stored as positive magnitudes by the model but emitted
     * as signed contributions - negative - so the breakdown adds up.
     */
    @Test
    public void testFeesAndTaxesAreNegative()
    {
        var client = heldPosition(account -> account //
                        .fees____("2024-04-01", Values.Amount.factorize(20)) //
                        .tax_____("2024-05-01", Values.Amount.factorize(15)) //
                        .interest("2024-06-01", Values.Amount.factorize(30)));

        var breakdown = perf(client, "2024-01-01", "2024-12-31", null, null).get("breakdown").getAsJsonObject();

        assertThat(value(breakdown, "fees"), is(-20d));
        assertThat(value(breakdown, "taxes"), is(-15d));
        assertThat(value(breakdown, "income"), is(30d));

        assertReconciles(breakdown);
    }

    /** Deposits less removals over the interval surface as netDeposits. */
    @Test
    public void testNetDepositsCaptureExternalFlow()
    {
        var client = heldPosition(account -> account //
                        .deposit_("2024-02-01", Values.Amount.factorize(500)) //
                        .withdraw("2024-06-01", Values.Amount.factorize(200)));

        var breakdown = perf(client, "2024-01-01", "2024-12-31", null, null).get("breakdown").getAsJsonObject();

        assertThat(value(breakdown, "netDeposits"), is(300d));
        assertReconciles(breakdown);
    }

    /**
     * The valuation window is half-open {@code (openingDate, closingDate]}. A
     * deposit dated exactly on {@code openingDate} is therefore part of the
     * baseline: it lands in {@code openingValue}, is excluded from
     * {@code netDeposits}, and - the cross-engine check - drags the
     * time-weighted return, because both engines must treat it as idle opening
     * cash rather than an in-period flow. Adding 500 idle cash to the
     * 1000 → 1200 position turns 20 % into 200/1500.
     */
    @Test
    public void testDepositOnOpeningDateIsBaseline()
    {
        var client = heldPosition(account -> account.deposit_("2024-01-01", Values.Amount.factorize(500)));

        var result = perf(client, "2024-01-01", "2024-12-31", null, null);
        var breakdown = result.get("breakdown").getAsJsonObject();

        assertThat(value(breakdown, "openingValue"), is(1500d));
        assertThat(value(breakdown, "netDeposits"), is(0d));
        assertThat(value(breakdown, "closingValue"), is(1700d));
        assertThat(result.get("ttwror").getAsDouble(), closeTo(200d / 1500d, 1e-6));
        assertReconciles(breakdown);
    }

    /**
     * The closing date is inclusive. A deposit dated exactly on
     * {@code closingDate} is booked as an in-period flow by the breakdown: it
     * lands in {@code netDeposits} and {@code closingValue} but not
     * {@code openingValue}, and the breakdown reconciles. The cross-engine
     * subtlety worth pinning: the time-weighted return does <em>not</em>
     * neutralise it - a flow on the final day has no sub-period after it to be
     * measured against - so it weighs on TTWROR exactly as opening capital does
     * (200/1500), not the position's bare 20 %. TTWROR and the breakdown
     * therefore legitimately diverge at this boundary; the endpoint reports what
     * {@code ClientIndex} produces, matching the desktop application for the
     * same period.
     */
    @Test
    public void testDepositOnClosingDateIsInPeriodFlow()
    {
        var client = heldPosition(account -> account.deposit_("2024-12-31", Values.Amount.factorize(500)));

        var result = perf(client, "2024-01-01", "2024-12-31", null, null);
        var breakdown = result.get("breakdown").getAsJsonObject();

        assertThat(value(breakdown, "openingValue"), is(1000d));
        assertThat(value(breakdown, "netDeposits"), is(500d));
        assertThat(value(breakdown, "closingValue"), is(1700d));
        assertThat(result.get("ttwror").getAsDouble(), closeTo(200d / 1500d, 1e-6));
        assertReconciles(breakdown);
    }

    /**
     * The valuation dates carry the same meaning as the holdings endpoint's
     * single {@code date}: {@code openingValue} at {@code openingDate} and
     * {@code closingValue} at {@code closingDate} equal the holdings'
     * {@code totalAssets} at those dates. This keeps the performance endpoint
     * consistent with the point-in-time valuation - a client can line the two
     * up. A flow inside the period (here 250 on 2024-03-01) lands in the closing
     * valuation but not the opening, exactly as holdings would report it.
     */
    @Test
    public void testIntervalEndpointsMatchHoldingsValuation()
    {
        var client = heldPosition(account -> account.deposit_("2024-03-01", Values.Amount.factorize(250)));

        var breakdown = perf(client, "2024-01-01", "2024-12-31", null, null).get("breakdown").getAsJsonObject();
        var atFrom = holdingsTotal(client, "2024-01-01");
        var atTo = holdingsTotal(client, "2024-12-31");

        assertThat(value(breakdown, "openingValue"), is(atFrom));
        assertThat(value(breakdown, "closingValue"), is(atTo));
    }

    private static double holdingsTotal(Client client, String date)
    {
        var holdings = HoldingsHandler.list(client, new ExchangeRateProviderFactory(client), date, null)
                        .getAsJsonObject();
        return holdings.get("totalAssets").getAsJsonObject().get("value").getAsDouble();
    }

    @Test
    public void testToDefaultsToToday()
    {
        var result = perf(heldPosition(), "2024-01-01", null, null, null);
        assertThat(result.get("closingDate").getAsString(), is(LocalDate.now().toString()));
    }

    /**
     * The reporting currency is echoed on the envelope - a stored response must
     * not force the reader to infer it from an arbitrary nested money object.
     */
    @Test
    public void testCurrencyOverride()
    {
        var result = perf(heldPosition(), "2024-01-01", "2024-12-31", "USD", null);

        assertThat(result.get("reportingCurrency").getAsString(), is("USD"));
        var breakdown = result.get("breakdown").getAsJsonObject();
        assertThat(breakdown.get("closingValue").getAsJsonObject().get("currency").getAsString(), is("USD"));
    }

    /** The cost method shifts the realized/unrealized split but not their sum. */
    @Test
    public void testCostMethodIsAccepted()
    {
        var result = perf(heldPosition(), "2024-01-01", "2024-12-31", null, "moving-average");
        var breakdown = result.get("breakdown").getAsJsonObject();
        assertThat(value(breakdown, "realizedCapitalGains") + value(breakdown, "unrealizedCapitalGains"), is(200d));
    }

    /**
     * The cost method changes the numbers, so it must appear in the response:
     * without the echo the split cannot be interpreted from the payload alone.
     */
    @Test
    public void testCostMethodIsEchoed()
    {
        assertThat(perf(heldPosition(), "2024-01-01", "2024-12-31", null, "fifo").get("costMethod").getAsString(),
                        is("fifo"));
        assertThat(perf(heldPosition(), "2024-01-01", "2024-12-31", null, "moving-average").get("costMethod")
                        .getAsString(), is("moving-average"));

        // the default is the FIFO the handler applies, not an omitted property
        assertThat(perf(heldPosition(), "2024-01-01", "2024-12-31", null, null).get("costMethod").getAsString(),
                        is("fifo"));
    }

    /**
     * A return the model cannot define (NaN or infinite) is emitted as JSON
     * null - never as a non-finite literal, which Gson rejects and no JSON
     * parser accepts.
     */
    @Test
    public void testNonFiniteReturnsSerializeAsNull()
    {
        var client = new Client();
        var converter = new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), "EUR");
        var interval = Interval.of(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31"));
        var snapshot = new ClientPerformanceSnapshot(client, converter, interval, true);

        var json = EntityJson.performance(interval.getStart(), interval.getEnd(), "EUR", CostMethod.FIFO, Double.NaN,
                        Double.POSITIVE_INFINITY, snapshot);

        assertThat(json.get("ttwror").isJsonNull(), is(true));
        assertThat(json.get("irr").isJsonNull(), is(true));
    }

    @Test
    public void testMissingOpeningDateIs400()
    {
        assertFieldError(() -> perf(heldPosition(), null, "2024-12-31", null, null), "openingDate", "required");
    }

    @Test
    public void testInvalidOpeningDateIs400()
    {
        assertFieldError(() -> perf(heldPosition(), "not-a-date", null, null, null), "openingDate", "invalid-value");
    }

    @Test
    public void testInvertedRangeIs400()
    {
        assertFieldError(() -> perf(heldPosition(), "2024-12-31", "2024-01-01", null, null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testEmptyRangeIs400()
    {
        assertFieldError(() -> perf(heldPosition(), "2024-06-01", "2024-06-01", null, null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        assertFieldError(() -> perf(heldPosition(), "2024-01-01", "2024-12-31", "ZZZ", null), "reportingCurrency",
                        "unknown-currency");
    }

    @Test
    public void testInvalidCostMethodIs400()
    {
        assertFieldError(() -> perf(heldPosition(), "2024-01-01", "2024-12-31", null, "lifo"), "costMethod",
                        "invalid-value");
    }

    /**
     * Every actionable violation is reported at once, so an agent self-corrects
     * in one round-trip: the range constraint does not hide behind an unrelated
     * bad parameter.
     */
    @Test
    public void testRangeAndCurrencyErrorsAreReportedTogether()
    {
        var errors = errorsOf(() -> perf(heldPosition(), "2024-12-31", "2024-01-01", "ZZZ", null));

        assertThat(errors.size(), is(2));
        assertThat(codeOf(errors, "reportingCurrency"), is("unknown-currency"));
        assertThat(codeOf(errors, "closingDate"), is("invalid-range"));
    }

    /**
     * A date that never parsed cannot be judged against the range: reporting
     * both would put two errors on one field, one of them noise - closingDate
     * defaults to today, which would look inverted against a later opening date.
     */
    @Test
    public void testUnparseableClosingDateYieldsNoSpuriousRangeError()
    {
        var errors = errorsOf(() -> perf(heldPosition(), "2026-01-01", "not-a-date", null, null));

        assertThat(errors.size(), is(1));
        assertThat(codeOf(errors, "closingDate"), is("invalid-value"));
    }

    /** Likewise when the opening date is missing altogether. */
    @Test
    public void testMissingOpeningDateYieldsNoRangeError()
    {
        var errors = errorsOf(() -> perf(heldPosition(), null, "2024-12-31", null, null));

        assertThat(errors.size(), is(1));
        assertThat(codeOf(errors, "openingDate"), is("required"));
    }

    private static Client heldPosition()
    {
        return heldPosition(account -> {
            // no transactions within the interval
        });
    }

    /**
     * 10 shares bought at 100 before the interval, valued at 120 at its end.
     * {@code within} adds any further account transactions inside the interval.
     */
    private static Client heldPosition(Consumer<AccountBuilder> within)
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(120)) //
                        .addTo(client);
        security.setName("ACME");

        var builder = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(1000));
        within.accept(builder);
        var account = builder.addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        return client;
    }

    private static double value(JsonObject breakdown, String field)
    {
        return breakdown.get(field).getAsJsonObject().get("value").getAsDouble();
    }

    /** the signed money fields must add up to the final value */
    private static void assertReconciles(JsonObject breakdown)
    {
        var sum = value(breakdown, "openingValue") //
                        + value(breakdown, "unrealizedCapitalGains") //
                        + value(breakdown, "realizedCapitalGains") //
                        + value(breakdown, "income") //
                        + value(breakdown, "fees") //
                        + value(breakdown, "taxes") //
                        + value(breakdown, "currencyGains") //
                        + value(breakdown, "netDeposits");
        assertThat(sum, closeTo(value(breakdown, "closingValue"), 1e-6));
    }

    private static void assertFieldError(Runnable call, String field, String code)
    {
        try
        {
            call.run();
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is(field));
            assertThat(e.getErrors().get(0).code(), is(code));
        }
    }

    /** the 400's field errors, regardless of the order they accumulated in */
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
