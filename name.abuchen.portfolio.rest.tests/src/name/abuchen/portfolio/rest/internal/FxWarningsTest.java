package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

/**
 * What {@link FxWarnings} itself decides. The route tests assert that a report
 * carries {@code fxWarnings}; this asserts which pairs land in it, because a
 * silently converted pair is the one thing a report must never present as
 * converted.
 * <p/>
 * The currencies are chosen on purpose: USD, GBP and CHF all have built-in ECB
 * series, so they cannot exercise the fallback. ARS and BBD are in the currency
 * bundle but reachable by no provider, which is the state a real file ends up in
 * when a user adds an account in a currency nothing quotes.
 */
public class FxWarningsTest
{
    private static ExchangeRateProviderFactory factory(Client client)
    {
        return new ExchangeRateProviderFactory(client);
    }

    @Test
    public void testPairWithoutAnySeriesIsReportedAsNoSeries()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        assertThat(FxWarnings.list(client, factory(client), CurrencyUnit.EUR),
                        contains(new FxWarnings.Warning("ARS", "EUR", "no-series")));
    }

    /** an exchange-rate security without prices: the series exists, answers no date */
    @Test
    public void testSeriesWithoutRatesIsReportedAsNoRate()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(rates);

        assertThat(FxWarnings.list(client, factory(client), CurrencyUnit.EUR),
                        contains(new FxWarnings.Warning("ARS", "EUR", "no-rate")));
    }

    /** a pair with rates converts, so it is never reported */
    @Test
    public void testPairWithRatesIsSilent()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var rates = new Security("ARS/EUR", "ARS");
        rates.setTargetCurrencyCode(CurrencyUnit.EUR);
        rates.addPrice(new SecurityPrice(LocalDate.of(2026, 1, 2), Values.Quote.factorize(965)));
        client.addSecurity(rates);

        assertThat(FxWarnings.list(client, factory(client), CurrencyUnit.EUR), is(empty()));
    }

    /**
     * An inverse or a chained series must not be mistaken for the placeholder:
     * USD and GBP reach EUR through the built-in ECB series, in one step for
     * USD and in two for GBP, and neither is a fallback.
     */
    @Test
    public void testInverseAndChainedSeriesAreSilent()
    {
        var client = new Client();
        new AccountBuilder("USD").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);
        new AccountBuilder("GBP").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        assertThat(FxWarnings.list(client, factory(client), CurrencyUnit.EUR), is(empty()));
        assertThat(FxWarnings.list(client, factory(client), "USD"), is(empty()));
    }

    /** the reporting currency is never a fallback for itself */
    @Test
    public void testReportingCurrencyIsNeverReported()
    {
        var client = new Client();
        new AccountBuilder("EUR").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        assertThat(FxWarnings.list(client, factory(client), CurrencyUnit.EUR), is(empty()));
    }

    /**
     * A report that covers part of the file names only the currencies it
     * converted. The file's currency list would add an account this report never
     * valued, and the reader would hunt for amounts that are not in the response.
     */
    @Test
    public void testScopedListNamesOnlyTheCurrenciesOfTheReport()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        assertThat(FxWarnings.list(List.of("EUR"), factory(client), CurrencyUnit.EUR), is(empty()));
        assertThat(FxWarnings.list(List.of("ARS"), factory(client), CurrencyUnit.EUR),
                        contains(new FxWarnings.Warning("ARS", "EUR", "no-series")));
    }

    /** a scoped list skips the reporting currency, a currency named twice and one it does not know */
    @Test
    public void testScopedListSkipsTheReportingCurrencyAndDuplicates()
    {
        var client = new Client();

        assertThat(FxWarnings.list(Arrays.asList("EUR", "EUR", null), factory(client), CurrencyUnit.EUR), is(empty()));
        assertThat(FxWarnings.list(List.of("ARS", "ARS"), factory(client), CurrencyUnit.EUR).size(), is(1));
    }

    /** a currency the application does not know is skipped, not reported */
    @Test
    public void testCurrencyOutsideTheBundleIsSkipped()
    {
        var client = new Client();
        var account = new AccountBuilder("EUR").addTo(client);
        account.setCurrencyCode("XXX");

        assertThat(FxWarnings.list(client, factory(client), CurrencyUnit.EUR), is(empty()));
    }

    /** the order is the file's currency order, so a report is reproducible */
    @Test
    public void testWarningsFollowTheFilesCurrencyOrder()
    {
        var client = new Client();
        new AccountBuilder("BBD").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var warnings = FxWarnings.list(client, factory(client), CurrencyUnit.EUR);
        assertThat(warnings.size(), is(2));
        assertThat(warnings.get(0).currency(), is("ARS"));
        assertThat(warnings.get(1).currency(), is("BBD"));
    }

    /**
     * A report that echoes no reporting currency has nothing to describe. The
     * field is still added, because its absence would read as "this report did
     * not look" rather than "nothing fell back".
     */
    @Test
    public void testAttachAddsAnEmptyArrayWithoutAReportingCurrency()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var report = new JsonObject();
        FxWarnings.attach(report, client, factory(client));

        assertThat(report.get("fxWarnings").getAsJsonArray().size(), is(0));
    }

    /** the currency the payload echoes is the one the warning describes */
    @Test
    public void testAttachNamesTheCurrencyThePayloadEchoes()
    {
        var client = new Client();
        new AccountBuilder("ARS").deposit_("2026-01-01", Values.Amount.factorize(100)).addTo(client);

        var report = new JsonObject();
        report.addProperty("reportingCurrency", "ARS");
        FxWarnings.attach(report, client, factory(client));

        var warnings = report.get("fxWarnings").getAsJsonArray();
        assertThat(warnings.size(), is(1));

        var warning = warnings.get(0).getAsJsonObject();
        assertThat(warning.get("currency").getAsString(), is("EUR"));
        assertThat(warning.get("reportingCurrency").getAsString(), is("ARS"));
        assertThat(warning.get("reason").getAsString(), is("no-series"));
    }
}
