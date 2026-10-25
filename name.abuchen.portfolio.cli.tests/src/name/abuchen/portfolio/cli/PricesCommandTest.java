package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;

/** Today is 2026-10-16 (see {@link CliRun#CLOCK}). */
public class PricesCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private CliRun run(Client client, String... options) throws IOException
    {
        return cli.save(client).run("prices", options);
    }

    private static Security instrument(Client client, String name, String currency)
    {
        var security = new SecurityBuilder(currency).addTo(client);
        security.setName(name);
        security.setIsin(null);
        security.setTickerSymbol(null);
        return security;
    }

    @Test
    public void returnsTheInstrumentTheLatestPriceAndTheDecimalHistoryInDateOrder() throws IOException
    {
        var client = new Client();
        var security = new SecurityBuilder("EUR").addPrice("2026-10-14", 12_345_000_000L)
                        .addPrice("2026-10-13", 1L).addTo(client);
        security.setName("ACME");

        var run = run(client, "--instrument", security.getUUID());

        assertThat(run.stdout, run.exitCode, is(0));

        var instrument = run.data().getAsJsonObject("instrument");
        assertThat(instrument.keySet(), is(Set.of("uuid", "name", "currencyCode")));
        assertThat(instrument.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(instrument.get("name").getAsString(), is("ACME"));
        assertThat(instrument.get("currencyCode").getAsString(), is("EUR"));

        // the raw text keeps trailing zeros, and never uses an exponent
        assertThat(run.stdout.contains("\"value\":0.00000001"), is(true));
        assertThat(run.stdout.contains("\"value\":123.45000000"), is(true));

        var items = run.items();
        assertThat(items.size(), is(2));
        assertThat(items.get(0).getAsJsonObject().get("date").getAsString(), is("2026-10-13"));
        assertThat(items.get(1).getAsJsonObject().get("date").getAsString(), is("2026-10-14"));
        assertThat(items.get(1).getAsJsonObject().get("value").getAsBigDecimal().toPlainString(),
                        is("123.45000000"));

        var latest = run.data().getAsJsonObject("latest");
        assertThat(latest.get("date").getAsString(), is("2026-10-14"));
        assertThat(latest.get("value").getAsBigDecimal().toPlainString(), is("123.45000000"));

        assertThat(run.data().get("total").getAsInt(), is(2));
        assertThat(run.data().has("nextOffset"), is(false));
    }

    @Test
    public void latestIsOmittedWhenNoPriceIsDatedOnOrBeforeToday() throws IOException
    {
        var client = new Client();
        var empty = instrument(client, "No prices", "EUR");
        var future = new SecurityBuilder("EUR").addPrice("2026-11-01", 100_000_000L).addTo(client);
        future.setName("Only future");

        var noPrices = run(client, "--instrument", empty.getUUID());
        assertThat(noPrices.stdout, noPrices.exitCode, is(0));
        assertThat(noPrices.data().has("latest"), is(false));
        assertThat(noPrices.items().size(), is(0));
        assertThat(noPrices.data().get("total").getAsInt(), is(0));

        var onlyFuture = run(client, "--instrument", future.getUUID());
        assertThat(onlyFuture.data().has("latest"), is(false));
        assertThat(onlyFuture.items().size(), is(0));

        var withLaterTo = run(client, "--instrument", future.getUUID(), "--to", "2026-12-31");
        assertThat(withLaterTo.data().has("latest"), is(false));
        assertThat(withLaterTo.items().size(), is(1));
    }

    private static Security priced(Client client, String name, String... dates)
    {
        var builder = new SecurityBuilder("EUR");
        for (var date : dates)
            builder.addPrice(date, 100_000_000L);
        var security = builder.addTo(client);
        security.setName(name);
        security.setIsin(null);
        security.setTickerSymbol(null);
        return security;
    }

    private static List<String> dates(CliRun run)
    {
        var dates = new ArrayList<String>();
        run.items().forEach(item -> dates.add(item.getAsJsonObject().get("date").getAsString()));
        return dates;
    }

    @Test
    public void aMissingInstrumentIsAUsageError() throws IOException
    {
        var run = run(new Client());

        assertThat(run.exitCode, is(2));
        assertThat(run.code(), is("option-required"));
        assertThat(run.problem().get("status").getAsInt(), is(400));
        assertThat(run.problem().get("option").getAsString(), is("instrument"));
    }

    @Test
    public void anInstrumentIsFoundByIsinTickerOrNameAndEchoedBack() throws IOException
    {
        var client = new Client();
        var security = new SecurityBuilder("EUR").addPrice("2026-10-01", 100_000_000L).addTo(client);
        security.setName("ACME Corp");
        security.setIsin("US0000000001");
        security.setTickerSymbol("ACME");

        for (var reference : List.of("US0000000001", "acme", "ACME Corp"))
        {
            var run = run(client, "--instrument", reference);

            assertThat(run.stdout, run.exitCode, is(0));
            assertThat(run.data().getAsJsonObject("instrument").get("uuid").getAsString(), is(security.getUUID()));
        }
    }

    @Test
    public void anIndexHasNoCurrencyInTheEchoedInstrument() throws IOException
    {
        var client = new Client();
        var index = priced(client, "DAX", "2026-10-01");
        index.setCurrencyCode(null);

        var run = run(client, "--instrument", index.getUUID());

        assertThat(run.data().getAsJsonObject("instrument").keySet(), is(Set.of("uuid", "name")));
    }

    @Test
    public void anUnknownReferenceIsNotFoundAndNotAUsageError() throws IOException
    {
        var run = run(new Client(), "--instrument", "nothing");

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("reference-not-found"));
        assertThat(run.problem().get("status").getAsInt(), is(404));
        assertThat(run.problem().get("option").getAsString(), is("instrument"));
        assertThat(run.problem().get("value").getAsString(), is("nothing"));
    }

    @Test
    public void anAmbiguousReferenceListsEveryCandidate() throws IOException
    {
        var client = new Client();
        var first = priced(client, "Twin");
        var second = priced(client, "twin");

        var run = run(client, "--instrument", "TWIN");

        assertThat(run.exitCode, is(1));
        assertThat(run.code(), is("reference-ambiguous"));
        assertThat(run.problem().get("status").getAsInt(), is(409));
        var uuids = new ArrayList<String>();
        run.problem().getAsJsonArray("candidates")
                        .forEach(c -> uuids.add(c.getAsJsonObject().get("uuid").getAsString()));
        assertThat(uuids.size(), is(2));
        assertThat(Set.copyOf(uuids), is(Set.of(first.getUUID(), second.getUUID())));
    }

    @Test
    public void aCashAccountUuidIsNotAnInstrument() throws IOException
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);

        var run = run(client, "--instrument", account.getUUID());

        assertThat(run.code(), is("reference-not-found"));
    }

    @Test
    public void malformedDatesAreUsageErrors() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-10-01");

        for (var bad : List.of("2026-13-01", "2026-02-30", "2026-1-1", "20261001", "yesterday", "", "+12026-01-01",
                        "2026-10-01T00:00"))
        {
            for (var option : List.of("--from", "--to"))
            {
                var run = run(client, "--instrument", security.getUUID(), option + "=" + bad);

                assertThat(option + " " + bad, run.exitCode, is(2));
                assertThat(run.code(), is("invalid-option-value"));
                assertThat(run.problem().get("option").getAsString(), is(option.substring(2)));
                assertThat(run.problem().get("value").getAsString(), is(bad));
            }
        }
    }

    @Test
    public void aFromAfterToIsAnInvalidRange() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-10-01");

        var run = run(client, "--instrument", security.getUUID(), "--from", "2026-02-01", "--to", "2026-01-31");

        assertThat(run.exitCode, is(2));
        assertThat(run.code(), is("invalid-date-range"));
        assertThat(run.problem().get("status").getAsInt(), is(400));
        assertThat(run.problem().get("from").getAsString(), is("2026-02-01"));
        assertThat(run.problem().get("to").getAsString(), is("2026-01-31"));
    }

    @Test
    public void aFromAfterTodayWithoutToIsAnInvalidRangeAgainstTheDefaultedTo() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-10-01");

        var run = run(client, "--instrument", security.getUUID(), "--from", "2026-10-17");

        assertThat(run.code(), is("invalid-date-range"));
        assertThat(run.problem().get("to").getAsString(), is("2026-10-16"));
    }

    @Test
    public void theDateRangeIsInclusiveAndLatestDoesNotDependOnIt() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04", "2026-10-14");

        var run = run(client, "--instrument", security.getUUID(), "--from", "2026-01-02", "--to", "2026-01-03");

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(dates(run), is(List.of("2026-01-02", "2026-01-03")));
        assertThat(run.data().get("total").getAsInt(), is(2));
        assertThat(run.data().getAsJsonObject("latest").get("date").getAsString(), is("2026-10-14"));

        var meta = run.meta();
        assertThat(meta.get("from").getAsString(), is("2026-01-02"));
        assertThat(meta.get("to").getAsString(), is("2026-01-03"));
    }

    @Test
    public void toDefaultsToTodayAndFromIsAbsentFromMetaWhenNotGiven() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-10-15", "2026-10-16", "2026-10-17");

        var run = run(client, "--instrument", security.getUUID());

        assertThat(dates(run), is(List.of("2026-10-15", "2026-10-16")));

        var meta = run.meta();
        assertThat(meta.get("to").getAsString(), is("2026-10-16"));
        assertThat(meta.has("from"), is(false));
        assertThat(meta.get("asOf").getAsString(), is("2026-10-16"));
        assertThat(meta.get("limit").getAsInt(), is(200));
        assertThat(meta.get("offset").getAsInt(), is(0));
        assertThat(meta.has("fileVersion"), is(true));
    }

    @Test
    public void theHistoryIsPagedLikeTheLists() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04",
                        "2026-01-05");

        var first = run(client, "--instrument", security.getUUID(), "--limit", "2");
        assertThat(dates(first), is(List.of("2026-01-01", "2026-01-02")));
        assertThat(first.data().get("total").getAsInt(), is(5));
        assertThat(first.data().get("nextOffset").getAsInt(), is(2));

        var last = run(client, "--instrument", security.getUUID(), "--limit", "2", "--offset", "4");
        assertThat(dates(last), is(List.of("2026-01-05")));
        assertThat(last.data().has("nextOffset"), is(false));

        var invalid = run(client, "--instrument", security.getUUID(), "--limit", "0");
        assertThat(invalid.exitCode, is(2));
        assertThat(invalid.code(), is("invalid-option-value"));
    }

    @Test
    public void aFeedsLatestPriceEndsTheHistoryAndIsTheLatest() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-10-13", "2026-10-14");
        security.setLatest(new LatestSecurityPrice(LocalDate.parse("2026-10-16"), 250_000_000L));

        var run = run(client, "--instrument", security.getUUID());

        assertThat(dates(run), is(List.of("2026-10-13", "2026-10-14", "2026-10-16")));
        assertThat(run.data().getAsJsonObject("latest").get("date").getAsString(), is("2026-10-16"));
        assertThat(run.data().getAsJsonObject("latest").get("value").getAsBigDecimal().toPlainString(),
                        is("2.50000000"));
    }

    @Test
    public void aLatestPriceMoreThanSevenDaysOldProducesAStalePriceWarning() throws IOException
    {
        var client = new Client();
        var security = priced(client, "ACME", "2026-09-30");

        var run = run(client, "--instrument", security.getUUID());

        assertThat(run.exitCode, is(0));
        assertThat(run.warnings().size(), is(1));
        var warning = run.warnings().get(0).getAsJsonObject();
        assertThat(warning.get("code").getAsString(), is("stale-price"));
        assertThat(warning.get("detail").getAsString().isBlank(), is(false));
        assertThat(warning.get("instrument").getAsString(), is(security.getUUID()));
        assertThat(warning.get("date").getAsString(), is("2026-09-30"));
        assertThat(warning.get("ageDays").getAsInt(), is(16));
    }

    @Test
    public void aLatestPriceExactlySevenDaysOldIsNotStale() throws IOException
    {
        var client = new Client();
        var seven = priced(client, "Seven", "2026-10-09");
        var eight = priced(client, "Eight", "2026-10-08");

        assertThat(run(client, "--instrument", seven.getUUID()).warnings().size(), is(0));
        assertThat(run(client, "--instrument", eight.getUUID()).warnings().size(), is(1));
    }

    @Test
    public void retiredAndManualFeedInstrumentsAreNeverStale() throws IOException
    {
        var client = new Client();
        var retired = priced(client, "Retired", "2020-01-01");
        retired.setRetired(true);
        var manual = priced(client, "Manual", "2020-01-01");
        manual.setFeed(QuoteFeed.MANUAL);
        var manualLatestFeed = priced(client, "Manual latest feed", "2020-01-01");
        manualLatestFeed.setFeed("YAHOO");
        manualLatestFeed.setLatestFeed(QuoteFeed.MANUAL);
        var latestFeedWins = priced(client, "Latest feed set", "2020-01-01");
        latestFeedWins.setFeed(QuoteFeed.MANUAL);
        latestFeedWins.setLatestFeed("YAHOO");

        for (var security : List.of(retired, manual, manualLatestFeed))
            assertThat(security.getName(), run(client, "--instrument", security.getUUID()).warnings().size(), is(0));

        // the latest feed takes precedence over the historic feed
        assertThat(run(client, "--instrument", latestFeedWins.getUUID()).warnings().size(), is(1));
    }

    @Test
    public void anInstrumentWithoutALatestPriceGetsNoStaleWarning() throws IOException
    {
        var client = new Client();
        var none = priced(client, "None");
        var future = priced(client, "Future", "2026-11-01");

        assertThat(run(client, "--instrument", none.getUUID()).warnings().size(), is(0));
        assertThat(run(client, "--instrument", future.getUUID()).warnings().size(), is(0));
    }
}
