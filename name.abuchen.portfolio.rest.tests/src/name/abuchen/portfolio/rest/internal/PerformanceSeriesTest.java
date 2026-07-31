package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class PerformanceSeriesTest
{
    /** calls the handler with a factory of its own, as the host would provide it */
    private static JsonObject series(Client client, String from, String to, String currency)
    {
        return PerformanceHandler.series(client, new ExchangeRateProviderFactory(client), from, to, currency)
                        .getAsJsonObject();
    }

    @Test
    public void testOneEntryPerCalendarDay()
    {
        var result = series(heldPosition(), "2024-01-01", "2024-12-31", null);

        assertThat(result.get("openingDate").getAsString(), is("2024-01-01"));
        assertThat(result.get("closingDate").getAsString(), is("2024-12-31"));
        assertThat(result.get("currency").getAsString(), is("EUR"));

        var days = result.get("days").getAsJsonArray();
        var expected = (int) ChronoUnit.DAYS.between(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31")) + 1;
        assertThat(days.size(), is(expected));

        assertThat(day(days, 0).get("date").getAsString(), is("2024-01-01"));
        assertThat(day(days, days.size() - 1).get("date").getAsString(), is("2024-12-31"));
    }

    /** the index is rebased at the opening date, so the first day is always flat */
    @Test
    public void testFirstDayIsTheBaseline()
    {
        var days = series(heldPosition(), "2024-01-01", "2024-12-31", null).get("days").getAsJsonArray();

        assertThat(day(days, 0).get("ttwror").getAsDouble(), is(0d));
        assertThat(day(days, 0).get("drawdown").getAsDouble(), is(0d));
    }

    /**
     * The same position that returns 20 % over the year in
     * {@code GET .../performance}: the series has to end on the same number.
     */
    @Test
    public void testFinalDayMatchesThePeriodReturn()
    {
        var days = series(heldPosition(), "2024-01-01", "2024-12-31", null).get("days").getAsJsonArray();
        var last = day(days, days.size() - 1);

        assertThat(last.get("ttwror").getAsDouble(), closeTo(0.20, 1e-6));
        assertThat(last.get("totalAssets").getAsDouble(), closeTo(1200d, 1e-6));
    }

    /** invested capital is seeded with the opening value, so the gap is the gain */
    @Test
    public void testInvestedCapitalTracksWhatWasPutIn()
    {
        var days = series(heldPosition(), "2024-01-01", "2024-12-31", null).get("days").getAsJsonArray();
        var last = day(days, days.size() - 1);

        assertThat(last.get("investedCapital").getAsDouble(), closeTo(1000d, 1e-6));
        assertThat(last.get("totalAssets").getAsDouble() - last.get("investedCapital").getAsDouble(),
                        closeTo(200d, 1e-6));
    }

    /** netDeposits is that day's external flow, not a running total */
    @Test
    public void testNetDepositsIsADailyFlow()
    {
        var client = heldPosition(account -> account.deposit_("2024-03-01", Values.Amount.factorize(500)));

        var days = series(client, "2024-01-01", "2024-12-31", null).get("days").getAsJsonArray();

        var onDeposit = dayByDate(days, "2024-03-01");
        assertThat(onDeposit.get("netDeposits").getAsDouble(), closeTo(500d, 1e-6));

        // the day after must be back to zero, which a cumulative series would not be
        assertThat(dayByDate(days, "2024-03-02").get("netDeposits").getAsDouble(), is(0d));
    }

    /**
     * A price that rises, falls, then partly recovers: the drawdown series has to
     * be negative through the dip and the reported maximum has to match its
     * depth, as a negative fraction.
     */
    @Test
    public void testDrawdownIsNegativeAndPeakTroughAreDated()
    {
        var client = dippingPosition();

        var result = series(client, "2024-01-01", "2024-12-31", null);
        var risk = result.get("risk").getAsJsonObject();
        var days = result.get("days").getAsJsonArray();

        var maxDrawdown = risk.get("maxDrawdown").getAsDouble();
        assertThat(maxDrawdown, lessThan(0d));

        var deepest = 0d;
        for (var ii = 0; ii < days.size(); ii++)
            deepest = Math.min(deepest, day(days, ii).get("drawdown").getAsDouble());
        assertThat(maxDrawdown, closeTo(deepest, 1e-9));

        // the peak must precede the trough, and both must sit inside the range
        var peak = LocalDate.parse(risk.get("maxDrawdownPeak").getAsString());
        var trough = LocalDate.parse(risk.get("maxDrawdownTrough").getAsString());
        assertThat(peak.isBefore(trough), is(true));
        assertThat(peak.isBefore(LocalDate.parse("2024-01-01")), is(false));
        assertThat(trough.isAfter(LocalDate.parse("2024-12-31")), is(false));

        assertThat(risk.get("maxDrawdownDurationDays").getAsLong(), greaterThan(0L));
    }

    /** the high water mark is the index's own peak, and distance is its drawdown */
    @Test
    public void testAllTimeHighIsTheIndexPeak()
    {
        var result = series(dippingPosition(), "2024-01-01", "2024-12-31", null);
        var allTimeHigh = result.get("risk").getAsJsonObject().get("allTimeHigh").getAsJsonObject();
        var days = result.get("days").getAsJsonArray();

        var highest = Double.NEGATIVE_INFINITY;
        for (var ii = 0; ii < days.size(); ii++)
            highest = Math.max(highest, day(days, ii).get("ttwror").getAsDouble());

        assertThat(allTimeHigh.get("value").getAsDouble(), closeTo(highest, 1e-9));
        assertThat(allTimeHigh.get("date").getAsString(), is(dayOfHighest(days)));

        // the series ends below its peak, so the distance is a loss
        assertThat(allTimeHigh.get("distance").getAsDouble(), lessThan(0d));
        assertThat(allTimeHigh.get("distance").getAsDouble(),
                        closeTo(day(days, days.size() - 1).get("drawdown").getAsDouble(), 1e-9));
    }

    /** a position that never moves has no drawdown and no volatility */
    @Test
    public void testFlatSeriesHasZeroRisk()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(1000)).addTo(client);

        var risk = series(client, "2024-01-01", "2024-12-31", null).get("risk").getAsJsonObject();

        assertThat(risk.get("maxDrawdown").getAsDouble(), is(0d));
        assertThat(risk.get("volatility").getAsDouble(), is(0d));
        assertThat(risk.get("semiDeviation").getAsDouble(), is(0d));
    }

    @Test
    public void testCurrencyOverrideIsReported()
    {
        var result = series(heldPosition(), "2024-01-01", "2024-12-31", "USD");
        assertThat(result.get("currency").getAsString(), is("USD"));
    }

    @Test
    public void testMissingOpeningDateIs400()
    {
        assertFieldError(() -> series(heldPosition(), null, "2024-12-31", null), "openingDate", "required");
    }

    @Test
    public void testInvalidOpeningDateIs400()
    {
        assertFieldError(() -> series(heldPosition(), "not-a-date", null, null), "openingDate", "invalid-value");
    }

    @Test
    public void testInvertedRangeIs400()
    {
        assertFieldError(() -> series(heldPosition(), "2024-12-31", "2024-01-01", null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testEmptyRangeIs400()
    {
        assertFieldError(() -> series(heldPosition(), "2024-06-01", "2024-06-01", null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        assertFieldError(() -> series(heldPosition(), "2024-01-01", "2024-12-31", "ZZZ"), "currency",
                        "unknown-currency");
    }

    /** one array slot per calendar day upstream, so the span has to be bounded */
    @Test
    public void testExcessiveRangeIs400()
    {
        assertFieldError(() -> series(heldPosition(), "1980-01-01", "2024-12-31", null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testRangeAtTheLimitIsAccepted()
    {
        var days = series(heldPosition(), "2005-01-02", "2025-01-01", null).get("days").getAsJsonArray();
        assertThat(days.size(), greaterThan(0));
    }

    private static JsonObject day(JsonArray days, int index)
    {
        return days.get(index).getAsJsonObject();
    }

    private static JsonObject dayByDate(JsonArray days, String date)
    {
        for (var ii = 0; ii < days.size(); ii++)
        {
            if (date.equals(day(days, ii).get("date").getAsString()))
                return day(days, ii);
        }
        throw new AssertionError("no entry for " + date);
    }

    private static String dayOfHighest(JsonArray days)
    {
        var best = 0;
        for (var ii = 0; ii < days.size(); ii++)
        {
            if (day(days, ii).get("ttwror").getAsDouble() > day(days, best).get("ttwror").getAsDouble())
                best = ii;
        }
        return day(days, best).get("date").getAsString();
    }

    /** 10 shares bought at 100 before the interval, valued at 120 at its end */
    private static Client heldPosition()
    {
        return heldPosition(account -> {
            // no transactions within the interval
        });
    }

    private static Client heldPosition(java.util.function.Consumer<AccountBuilder> within)
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

    /** rises to 130, falls to 90, recovers to 110: one clear drawdown episode */
    private static Client dippingPosition()
    {
        var client = new Client();

        var security = new SecurityBuilder() //
                        .addPrice("2023-06-01", Values.Quote.factorize(100)) //
                        .addPrice("2024-03-01", Values.Quote.factorize(130)) //
                        .addPrice("2024-07-01", Values.Quote.factorize(90)) //
                        .addPrice("2024-12-31", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("ACME");

        var account = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(1000)).addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        return client;
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
}
