package name.abuchen.portfolio.cli;

import static name.abuchen.portfolio.cli.TaxonomyFixtures.assign;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.child;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.taxonomy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

/**
 * <code>allocation</code>: how the value of the client splits across the
 * classifications of one taxonomy. Today is 2026-10-16 (see {@link CliRun#CLOCK}).
 * <p>
 * The fixture is worth 100,222 on 2026-03-01: Alpha 1,200 (all in Europe), Beta
 * 222 (half in Equity, half in Bonds) and 98,800 of unclassified cash.
 */
public class AllocationCommandTest
{
    private static final String DATE = "2026-03-01";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private Client client;
    private Security alpha;
    private Security beta;
    private Account cash;
    private Portfolio depot;
    private Path cache;

    private Taxonomy classes;
    private Classification equity;
    private Classification europe;
    private Classification america;
    private Classification bonds;
    private Classification empty;

    @Before
    public void setUp()
    {
        cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        client = new Client();

        alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(120)));

        beta = new SecurityBuilder().addTo(client);
        beta.setName("Beta");
        beta.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(55.5)));

        cash = new AccountBuilder().deposit_("2026-01-01", PortfolioBuilder.amountOf(100_000)).addTo(client);
        cash.setName("Cash");

        depot = new PortfolioBuilder(cash).buyPrice(alpha, "2026-01-02", 10, 100).buyPrice(beta, "2026-01-03", 4, 50)
                        .addTo(client);
        depot.setName("Depot");

        classes = taxonomy(client, "Classes");
        equity = child(classes.getRoot(), "Equity");
        europe = child(equity, "Europe");
        america = child(equity, "America");
        bonds = child(classes.getRoot(), "Bonds");
        empty = child(classes.getRoot(), "Empty");

        assign(europe, alpha);
        assign(equity, beta, 50);
        assign(bonds, beta, 50);
    }

    private CliRun run(String... options) throws IOException
    {
        return run(Map.of(), options);
    }

    private CliRun run(Map<String, String> env, String... options) throws IOException
    {
        return cli.save(client).run(env, "allocation", options);
    }

    private CliRun runClasses() throws IOException
    {
        return runClassesAt(DATE);
    }

    private CliRun runClassesAt(String date) throws IOException
    {
        return run("--taxonomy", classes.getId(), "--date", date);
    }

    private static List<JsonObject> rows(CliRun run)
    {
        return StreamSupport.stream(run.items().spliterator(), false)
                        .map(JsonElement::getAsJsonObject).toList();
    }

    private static JsonObject row(CliRun run, String name)
    {
        return rows(run).stream().filter(r -> r.get("name").getAsString().equals(name)).findFirst().orElseThrow();
    }

    private static BigDecimal amount(long stored)
    {
        return BigDecimal.valueOf(stored, Values.Amount.precision());
    }

    private static void assertDecimal(JsonElement actual, String expected)
    {
        assertThat(actual.toString(), actual.getAsBigDecimal().compareTo(new BigDecimal(expected)), is(0));
    }

    @Test
    public void hasARowForEveryClassificationAtEveryDepthInPreOrderAndTheUnassignedRowLast() throws IOException
    {
        var run = runClasses();
        assertThat(run.stdout, run.exitCode, is(0));

        assertThat(rows(run).stream().map(r -> r.get("name").getAsString()).toList(),
                        is(List.of("Equity", "Europe", "America", "Bonds", "Empty", "Unassigned")));

        var unassigned = rows(run).get(5);
        assertThat(unassigned.get("unassigned").getAsBoolean(), is(true));
        assertThat(unassigned.has("uuid"), is(false));
        assertThat(unassigned.has("parent"), is(false));
        assertThat(unassigned.has("targetWeight"), is(false));
        assertThat(unassigned.getAsJsonArray("path").size(), is(0));

        var nested = row(run, "Europe");
        assertThat(nested.get("uuid").getAsString(), is(europe.getId()));
        assertThat(nested.get("parent").getAsString(), is(equity.getId()));
        assertThat(nested.getAsJsonArray("path").get(0).getAsString(), is("Equity"));
        assertThat(nested.has("unassigned"), is(false));

        var top = row(run, "Equity");
        assertThat(top.has("parent"), is(false));
        assertDecimal(top.get("targetWeight"), "1");
    }

    @Test
    public void aRowsValueIsItsWholeSubtreesValueAndEmptyClassificationsAreZero() throws IOException
    {
        var run = runClasses();

        assertDecimal(run.data().get("total"), "100222");
        assertDecimal(row(run, "Equity").get("value"), "1311");
        assertDecimal(row(run, "Europe").get("value"), "1200");
        assertDecimal(row(run, "America").get("value"), "0");
        assertDecimal(row(run, "Bonds").get("value"), "111");
        assertDecimal(row(run, "Empty").get("value"), "0");
        assertDecimal(row(run, "Unassigned").get("value"), "98800");

        assertThat(row(run, "Equity").get("share").getAsDouble(), closeTo(1311.0 / 100222.0, 1e-12));
        assertThat(row(run, "America").get("share").getAsDouble(), is(0.0));
        assertThat(row(run, "Unassigned").get("share").getAsDouble(), closeTo(98800.0 / 100222.0, 1e-12));
    }

    @Test
    public void topLevelAndUnassignedRowsEqualCoresGroupingOfTheClientSnapshot() throws IOException
    {
        var run = runClasses();

        var snapshot = ClientSnapshot.create(client,
                        new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), "EUR"),
                        LocalDate.parse(DATE));
        var grouping = snapshot.groupByTaxonomy(classes);
        assertThat(grouping.asList().size(), is(3));

        for (var category : grouping.asList())
        {
            var classification = category.getClassification();
            var name = Classification.UNASSIGNED_ID.equals(classification.getId()) ? "Unassigned"
                            : classification.getName();
            var row = row(run, name);

            assertThat(name, row.get("value").getAsBigDecimal()
                            .compareTo(amount(category.getValuation().getAmount())), is(0));
            assertThat(name, row.get("share").getAsDouble(), is(category.getShare()));
        }

        // the top level and the unassigned row reconcile to the total
        var sum = rows(run).stream().filter(r -> r.getAsJsonArray("path").size() == 0)
                        .mapToDouble(r -> r.get("share").getAsDouble()).sum();
        assertThat(sum, closeTo(1.0, 1e-9));
    }

    @Test
    public void theUnassignedRowIsPresentEvenWhenEverythingIsClassified() throws IOException
    {
        assign(bonds, cash);

        var run = runClasses();

        var unassigned = rows(run).get(rows(run).size() - 1);
        assertThat(unassigned.get("name").getAsString(), is("Unassigned"));
        assertDecimal(unassigned.get("value"), "0");
        assertDecimal(unassigned.get("share"), "0");
    }

    @Test
    public void aTaxonomyIsResolvedByUuidOrExactNameIgnoringCase() throws IOException
    {
        var byUuid = run("--taxonomy", classes.getId().toUpperCase(), "--date", DATE);
        assertThat(byUuid.stdout, byUuid.exitCode, is(0));

        var byName = run("--taxonomy", "cLASSES", "--date", DATE);
        assertThat(byName.stdout, byName.exitCode, is(0));
        assertThat(rows(byName).size(), is(6));
    }

    @Test
    public void anUnknownTaxonomyIsNotFound() throws IOException
    {
        var run = run("--taxonomy", "Nope", "--date", DATE);

        assertThat(run.stdout, run.exitCode, is(1));
        assertThat(run.code(), is("reference-not-found"));
        assertThat(run.problem().get("option").getAsString(), is("taxonomy"));
    }

    @Test
    public void aNameSeveralTaxonomiesShareIsAmbiguousAndAUuidStillWorks() throws IOException
    {
        var twin = taxonomy(client, "classes");

        var run = run("--taxonomy", "Classes", "--date", DATE);
        assertThat(run.stdout, run.exitCode, is(1));
        assertThat(run.code(), is("reference-ambiguous"));
        assertThat(run.problem().getAsJsonArray("candidates").size(), is(2));

        var byUuid = run("--taxonomy", twin.getId(), "--date", DATE);
        assertThat(byUuid.stdout, byUuid.exitCode, is(0));
        assertThat(rows(byUuid).size(), is(1));
    }

    @Test
    public void theTaxonomyIsRequired() throws IOException
    {
        var run = run("--date", DATE);

        assertThat(run.stdout, run.exitCode, is(2));
    }

    @Test
    public void metaEchoesTheDateTheCurrencyAndTheResolvedTaxonomy() throws IOException
    {
        var run = run("--taxonomy", "Classes");

        assertThat(run.stdout, run.exitCode, is(0));
        // the clock says 2026-10-16
        assertThat(run.meta().get("date").getAsString(), is("2026-10-16"));
        assertThat(run.meta().get("currency").getAsString(), is("EUR"));
        assertThat(run.meta().getAsJsonObject("taxonomy").get("uuid").getAsString(), is(classes.getId()));
        assertThat(run.meta().getAsJsonObject("taxonomy").get("name").getAsString(), is("Classes"));
        assertThat(run.meta().has("exchangeRates"), is(false));
    }

    @Test
    public void currencyConvertsValuesAndTotalLikeCoreAtTheRateOfTheDate() throws IOException
    {
        var usd = EcbCacheFiles.series("EUR", "USD").rate("2026-02-27", "1.0800").rate("2026-03-01", "1.1000")
                        .rate("2026-03-05", "1.2000");
        EcbCacheFiles.write(cache, usd);

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--taxonomy", "Classes", "--date", DATE, "--currency",
                        "USD");
        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("currency").getAsString(), is("USD"));
        assertThat(run.meta().getAsJsonObject("exchangeRates").get("path").getAsString(), is(cache.toString()));

        var core = ClientSnapshot.create(client, CoreRates.converter("USD", usd), LocalDate.parse(DATE));
        assertThat(run.data().get("total").getAsBigDecimal()
                        .compareTo(amount(core.getMonetaryAssets().getAmount())), is(0));

        for (var category : core.groupByTaxonomy(classes).asList())
        {
            var name = Classification.UNASSIGNED_ID.equals(category.getClassification().getId()) ? "Unassigned"
                            : category.getClassification().getName();
            assertThat(name, row(run, name).get("value").getAsBigDecimal()
                            .compareTo(amount(category.getValuation().getAmount())), is(0));
        }
        assertDecimal(row(run, "Europe").get("value"), "1320");
    }

    /** A USD instrument in a USD depot with USD cash, valued in a EUR file. */
    private Security dollarHolding()
    {
        var dollars = new AccountBuilder("USD").deposit_("2026-01-01", PortfolioBuilder.amountOf(10_000))
                        .addTo(client);
        dollars.setName("Dollars");

        var gamma = new SecurityBuilder("USD").addTo(client);
        gamma.setName("Gamma");
        gamma.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(200)));

        var usdDepot = new PortfolioBuilder(dollars).addTo(client);
        usdDepot.setName("USD depot");
        TransactionFixtures.buy(usdDepot, dollars, gamma, "2026-01-05", PortfolioBuilder.sharesOf(5),
                        PortfolioBuilder.amountOf(950));
        return gamma;
    }

    @Test
    public void aMissingExchangeRateLeavesOutEveryValueShareAndTheTotal() throws IOException
    {
        var gamma = dollarHolding();
        assign(equity, gamma);
        // the only rate is dated after the day needed, so there is none
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-03-05", "1.2000"));

        var run = run(Map.of("PP_ECB_RATES", cache.toString()), "--taxonomy", "Classes", "--date", DATE);
        assertThat(run.stdout, run.exitCode, is(0));

        assertThat(run.data().has("total"), is(false));
        assertThat(rows(run).size(), is(6));
        for (var row : rows(run))
        {
            assertThat(row.toString(), row.has("value"), is(false));
            assertThat(row.toString(), row.has("share"), is(false));
        }
        assertThat(row(run, "Europe").get("uuid").getAsString(), is(europe.getId()));
        assertThat(rows(run).get(5).get("unassigned").getAsBoolean(), is(true));

        var missing = run.warnings("exchange-rate-missing");
        assertThat(missing.size(), is(1));
        assertThat(missing.get(0).get("baseCurrency").getAsString(), is("USD"));
        assertThat(missing.get(0).get("termCurrency").getAsString(), is("EUR"));
    }

    @Test
    public void aZeroTotalLeavesOutEveryShareButKeepsTheValues() throws IOException
    {
        client = new Client();
        var plain = taxonomy(client, "Plain");
        child(plain.getRoot(), "Only");

        var run = run("--taxonomy", "Plain");
        assertThat(run.stdout, run.exitCode, is(0));

        assertDecimal(run.data().get("total"), "0");
        assertThat(rows(run).size(), is(2));
        for (var row : rows(run))
        {
            assertDecimal(row.get("value"), "0");
            assertThat(row.toString(), row.has("share"), is(false));
        }
    }

    @Test
    public void aNegativeCashBalanceGivesANegativeShareThatIsNotClamped() throws IOException
    {
        client = new Client();
        var overdrawn = new AccountBuilder().deposit_("2026-01-01", PortfolioBuilder.amountOf(500)).addTo(client);
        overdrawn.setName("Overdrawn");
        var gamma = new SecurityBuilder().addTo(client);
        gamma.setName("Gamma");
        gamma.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(120)));
        new PortfolioBuilder(overdrawn).buyPrice(gamma, "2026-01-02", 10, 100).addTo(client);

        var plain = taxonomy(client, "Plain");
        assign(child(plain.getRoot(), "Only"), gamma);
        classes = plain;

        var run = runClasses();
        assertThat(run.stdout, run.exitCode, is(0));

        assertDecimal(run.data().get("total"), "700");
        assertDecimal(row(run, "Unassigned").get("value"), "-500");
        assertThat(row(run, "Unassigned").get("share").getAsDouble(), closeTo(-500.0 / 700.0, 1e-12));
        assertThat(row(run, "Only").get("share").getAsDouble(), closeTo(1200.0 / 700.0, 1e-12));
    }

    @Test
    public void anOverAssignedInstrumentIsCountedAsCoreCountsItAndWarnedAbout() throws IOException
    {
        // Alpha now adds up to 150%
        assign(equity, alpha, 50);

        var run = runClasses();
        assertThat(run.stdout, run.exitCode, is(0));

        var snapshot = ClientSnapshot.create(client,
                        new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), "EUR"),
                        LocalDate.parse(DATE));
        for (var category : snapshot.groupByTaxonomy(classes).asList())
        {
            if (Classification.UNASSIGNED_ID.equals(category.getClassification().getId()))
                continue;
            assertThat(category.getClassification().getName(), row(run, category.getClassification().getName())
                            .get("value").getAsBigDecimal().compareTo(amount(category.getValuation().getAmount())),
                            is(0));
        }

        var warnings = run.warnings("over-assigned");
        assertThat(warnings.size(), is(1));
        assertThat(warnings.get(0).get("instrument").getAsString(), is(alpha.getUUID()));
        assertDecimal(warnings.get(0).get("totalWeight"), "1.5");
        assertThat(warnings.get(0).get("detail").getAsString().contains("Alpha"), is(true));
    }

    @Test
    public void anOverAssignedCashAccountIsNamedByItsCashAccountUuid() throws IOException
    {
        assign(bonds, cash);
        assign(empty, cash, 20);

        var warnings = runClasses().warnings("over-assigned");

        assertThat(warnings.size(), is(1));
        assertThat(warnings.get(0).get("cashAccount").getAsString(), is(cash.getUUID()));
        assertThat(warnings.get(0).has("instrument"), is(false));
    }

    @Test
    public void anInstrumentThatIsNotOwnedAtTheDateIsNotWarnedAbout() throws IOException
    {
        var gamma = new SecurityBuilder().addTo(client);
        gamma.setName("Gamma");
        assign(bonds, gamma);
        assign(empty, gamma, 50);

        var run = runClasses();

        assertThat(run.warnings("over-assigned").size(), is(0));
    }

    @Test
    public void aStalePriceWarnsOncePerInstrumentJudgedAgainstTheDate() throws IOException
    {
        var run = runClassesAt("2026-03-20");
        assertThat(run.stdout, run.exitCode, is(0));

        var stale = run.warnings("stale-price");
        assertThat(stale.size(), is(2));
        assertThat(stale.stream().map(w -> w.get("instrument").getAsString()).sorted().toList(),
                        is(List.of(alpha.getUUID(), beta.getUUID()).stream().sorted().toList()));
        assertThat(stale.get(0).get("ageDays").getAsInt(), is(19));

        assertThat(runClasses().warnings("stale-price").size(), is(0));
    }

    @Test
    public void stalePriceWarningsComeInInstrumentNameOrder() throws IOException
    {
        // five more instruments, added out of name order; with Alpha and Beta a chance pass is 1 in 5040
        var byName = new TreeMap<String, String>(Map.of("Alpha", alpha.getUUID(), "Beta", beta.getUUID()));
        var more = new PortfolioBuilder(cash);
        for (var name : List.of("Echo", "Charlie", "Golf", "Delta", "Foxtrot"))
        {
            var security = new SecurityBuilder().addTo(client);
            security.setName(name);
            security.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(10)));
            more.buyPrice(security, "2026-01-04", 1, 10);
            byName.put(name, security.getUUID());
        }
        more.addTo(client);

        var run = runClassesAt("2026-03-20");
        assertThat(run.stdout, run.exitCode, is(0));

        var instruments = run.warnings("stale-price").stream().map(w -> w.get("instrument").getAsString()).toList();
        assertThat(instruments, is(List.copyOf(byName.values())));
    }

    @Test
    public void aRetiredInstrumentStillTakesPart() throws IOException
    {
        alpha.setRetired(true);

        var run = runClasses();

        assertDecimal(row(run, "Europe").get("value"), "1200");
    }
}
