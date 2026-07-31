package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class PerformanceCalendarTest
{
    private static JsonObject calendar(Client client, String from, String to, String currency)
    {
        return PerformanceCalendarHandler
                        .list(client, new ExchangeRateProviderFactory(client), from, to, currency).getAsJsonObject();
    }

    private static List<String> monthNames(JsonObject result)
    {
        var names = new ArrayList<String>();
        var months = result.get("months").getAsJsonArray();
        for (var ii = 0; ii < months.size(); ii++)
            names.add(months.get(ii).getAsJsonObject().get("month").getAsString());
        return names;
    }

    private static JsonObject month(JsonObject result, String month)
    {
        var months = result.get("months").getAsJsonArray();
        for (var ii = 0; ii < months.size(); ii++)
        {
            var entry = months.get(ii).getAsJsonObject();
            if (month.equals(entry.get("month").getAsString()))
                return entry;
        }
        throw new AssertionError("no entry for " + month);
    }

    /**
     * A consumer showing several period-scoped sections side by side has to be able
     * to tell which period each response actually covers, so every one states it.
     */
    @Test
    public void testReportsThePeriodItCovers()
    {
        var result = calendar(heldPosition(), "2024-02-15", "2024-05-10", null);

        assertThat(result.get("openingDate").getAsString(), is("2024-02-15"));
        assertThat(result.get("closingDate").getAsString(), is("2024-05-10"));
    }

    /**
     * A period is half-open, so one opening on the last day of a month contains no
     * day of that month at all - reporting it would show a month the caller did not
     * ask for. This is the shape a year-to-date range has: it opens on December 31.
     */
    @Test
    public void testAMonthEndOpeningStartsInTheNextMonth()
    {
        var months = monthNames(calendar(heldPosition(), "2023-12-31", "2024-03-31", null));

        assertThat(months, contains("2024-01", "2024-02", "2024-03"));
    }

    /** the period widens to whole months, so a mid-month request still starts at that month */
    @Test
    public void testCoversWholeMonths()
    {
        var result = calendar(heldPosition(), "2024-02-15", "2024-05-10", null);

        assertThat(result.get("currency").getAsString(), is("EUR"));
        assertThat(monthNames(result), contains("2024-02", "2024-03", "2024-04", "2024-05"));
    }

    @Test
    public void testFullYearHasTwelveMonths()
    {
        assertThat(monthNames(calendar(heldPosition(), "2024-01-01", "2024-12-31", null)).size(), is(12));
    }

    /**
     * The monthly returns must chain to the period return. The desktop computes each
     * month off one index, so compounding them has to land on the same figure the
     * performance endpoint reports.
     */
    @Test
    public void testMonthlyReturnsCompoundToThePeriodReturn()
    {
        var client = heldPosition();
        var result = calendar(client, "2024-01-01", "2024-12-31", null);

        var compounded = 1d;
        var months = result.get("months").getAsJsonArray();
        for (var ii = 0; ii < months.size(); ii++)
        {
            var ttwror = months.get(ii).getAsJsonObject().get("ttwror");
            if (!ttwror.isJsonNull())
                compounded *= 1 + ttwror.getAsDouble();
        }

        var period = PerformanceHandler
                        .list(client, new ExchangeRateProviderFactory(client), "2024-01-01", "2024-12-31", null, null)
                        .getAsJsonObject().get("ttwror").getAsDouble();

        assertThat(compounded - 1, closeTo(period, 1e-6));
    }

    /** dividends and interest land in the month they were received */
    @Test
    public void testEarningsAreBucketedByMonth()
    {
        var client = heldPosition(account -> account //
                        .interest("2024-03-10", Values.Amount.factorize(30)) //
                        .interest("2024-07-10", Values.Amount.factorize(12)));

        var result = calendar(client, "2024-01-01", "2024-12-31", null);

        assertThat(month(result, "2024-03").get("earnings").getAsJsonObject().get("interest").getAsDouble(),
                        closeTo(30d, 1e-6));
        assertThat(month(result, "2024-07").get("earnings").getAsJsonObject().get("interest").getAsDouble(),
                        closeTo(12d, 1e-6));
        assertThat(month(result, "2024-04").get("earnings").getAsJsonObject().get("interest").getAsDouble(), is(0d));
    }

    /** fees and taxes are signed contributions, as in the value-change breakdown */
    @Test
    public void testFeesAndTaxesAreNegative()
    {
        var client = heldPosition(account -> account //
                        .fees____("2024-04-01", Values.Amount.factorize(20)) //
                        .tax_____("2024-04-05", Values.Amount.factorize(15)));

        var april = month(calendar(client, "2024-01-01", "2024-12-31", null), "2024-04");

        assertThat(april.get("fees").getAsDouble(), closeTo(-20d, 1e-6));
        assertThat(april.get("taxes").getAsDouble(), closeTo(-15d, 1e-6));
    }

    @Test
    public void testInterestChargeReducesInterest()
    {
        var client = heldPosition(account -> account //
                        .interest("2024-05-01", Values.Amount.factorize(50)) //
                        .interest_charge("2024-05-20", Values.Amount.factorize(20)));

        assertThat(month(calendar(client, "2024-01-01", "2024-12-31", null), "2024-05") //
                        .get("earnings").getAsJsonObject().get("interest").getAsDouble(), closeTo(30d, 1e-6));
    }

    @Test
    public void testDepositsAndRemovalsNetOff()
    {
        var client = heldPosition(account -> account //
                        .deposit_("2024-06-01", Values.Amount.factorize(1000)) //
                        .withdraw("2024-06-15", Values.Amount.factorize(400)));

        assertThat(month(calendar(client, "2024-01-01", "2024-12-31", null), "2024-06") //
                        .get("netTransfers").getAsDouble(), closeTo(600d, 1e-6));
    }

    @Test
    public void testTradesAreCountedInTheirMonth()
    {
        var client = tradedInsidePeriod();
        var result = calendar(client, "2024-01-01", "2024-12-31", null);

        var february = month(result, "2024-02").get("trades").getAsJsonObject();
        assertThat(february.get("buys").getAsInt(), is(1));
        assertThat(february.get("sells").getAsInt(), is(0));
        assertThat(february.get("buyVolume").getAsDouble(), closeTo(500d, 1e-6));

        var august = month(result, "2024-08").get("trades").getAsJsonObject();
        assertThat(august.get("buys").getAsInt(), is(0));
        assertThat(august.get("sells").getAsInt(), is(1));
        assertThat(august.get("sellVolume").getAsDouble(), closeTo(800d, 1e-6));

        var may = month(result, "2024-05").get("trades").getAsJsonObject();
        assertThat(may.get("buys").getAsInt(), is(0));
        assertThat(may.get("buyVolume").getAsDouble(), is(0d));
    }

    /**
     * A month the index cannot cover reports null rather than 0: "no data" and "flat"
     * are different answers. The index is clamped to today, so a request reaching into
     * the future is the case where this happens.
     */
    @Test
    public void testFutureMonthsAreNull()
    {
        var nextYear = LocalDate.now().getYear() + 1;
        var result = calendar(heldPosition(), LocalDate.now().toString(), nextYear + "-12-31", null);
        var months = result.get("months").getAsJsonArray();

        var trailingNulls = 0;
        for (var ii = months.size() - 1; ii >= 0; ii--)
        {
            if (!months.get(ii).getAsJsonObject().get("ttwror").isJsonNull())
                break;
            trailingNulls++;
        }

        // every month after the one containing today has no index to sample
        assertThat(trailingNulls, greaterThan(0));
        assertThat(month(result, nextYear + "-06").get("ttwror").isJsonNull(), is(true));
    }

    /**
     * A month before the file has any holdings is flat, not unknown: the index covers
     * it with a zero return, and reporting null there would overstate the gap.
     */
    @Test
    public void testMonthsBeforeAnyHoldingAreFlat()
    {
        var result = calendar(heldPosition(), "2020-01-01", "2020-06-30", null);
        var months = result.get("months").getAsJsonArray();

        for (var ii = 0; ii < months.size(); ii++)
        {
            var entry = months.get(ii).getAsJsonObject();
            assertThat(entry.get("ttwror").isJsonNull(), is(false));
            assertThat(entry.get("ttwror").getAsDouble(), is(0d));
        }
    }

    /** a month with a real loss reports it as a negative return */
    @Test
    public void testALosingMonthIsNegative()
    {
        var client = new Client();
        var security = new SecurityBuilder() //
                        .addPrice("2023-12-31", Values.Quote.factorize(100)) //
                        .addPrice("2024-03-31", Values.Quote.factorize(70)) //
                        .addTo(client);
        security.setName("FALLING");
        var account = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(2000)).addTo(client);
        new PortfolioBuilder(account) //
                        .buy(security, "2023-12-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        var result = calendar(client, "2024-01-01", "2024-03-31", null);
        var anyNegative = false;
        var months = result.get("months").getAsJsonArray();
        for (var ii = 0; ii < months.size(); ii++)
        {
            var ttwror = months.get(ii).getAsJsonObject().get("ttwror");
            if (!ttwror.isJsonNull() && ttwror.getAsDouble() < 0)
                anyNegative = true;
        }
        assertThat(anyNegative, is(true));
        assertThat(month(result, "2024-03").get("ttwror").getAsDouble(), lessThan(0d));
    }

    @Test
    public void testMissingOpeningDateIs400()
    {
        assertFieldError(() -> calendar(heldPosition(), null, "2024-12-31", null), "openingDate", "required");
    }

    @Test
    public void testInvertedRangeIs400()
    {
        assertFieldError(() -> calendar(heldPosition(), "2024-12-31", "2024-01-01", null), "closingDate",
                        "invalid-range");
    }

    @Test
    public void testUnknownCurrencyIs400()
    {
        assertFieldError(() -> calendar(heldPosition(), "2024-01-01", "2024-12-31", "ZZZ"), "currency",
                        "unknown-currency");
    }

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
        security.setName("HELD");

        var builder = new AccountBuilder().deposit_("2023-06-01", Values.Amount.factorize(5000));
        within.accept(builder);
        var account = builder.addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2023-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        return client;
    }

    private static Client tradedInsidePeriod()
    {
        var client = heldPosition();

        var security = new SecurityBuilder() //
                        .addPrice("2024-02-01", Values.Quote.factorize(50)) //
                        .addPrice("2024-08-01", Values.Quote.factorize(80)) //
                        .addTo(client);
        security.setName("TRADED");

        new PortfolioBuilder(client.getAccounts().get(0)) //
                        .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(500)) //
                        .sell(security, "2024-08-01", Values.Share.factorize(10), Values.Amount.factorize(800)) //
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
