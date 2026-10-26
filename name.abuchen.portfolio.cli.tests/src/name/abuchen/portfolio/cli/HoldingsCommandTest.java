package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

/**
 * <code>holdings</code>: what is owned and what it is worth at a date. The
 * expected figures come from core's own client snapshot over the same client.
 * Today is 2026-10-16 (see {@link CliRun#CLOCK}).
 */
public class HoldingsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private Client client;
    private Security alpha;
    private Security beta;
    private Account cash;
    private Portfolio depot;
    private Path cache;

    @Before
    public void setUp()
    {
        cache = folder.getRoot().toPath().resolve("cache").resolve("ecb_exchange_rates.pb");
        client = new Client();

        alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");
        alpha.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"),
                        PortfolioBuilder.quoteOf(120)));

        beta = new SecurityBuilder().addTo(client);
        beta.setName("Beta");
        beta.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"),
                        PortfolioBuilder.quoteOf(55.5)));

        cash = new AccountBuilder().deposit_("2026-01-01", PortfolioBuilder.amountOf(100_000)).addTo(client);
        cash.setName("Cash");

        depot = new PortfolioBuilder(cash).buyPrice(alpha, "2026-01-02", 10, 100).buyPrice(beta, "2026-01-03", 4, 50)
                        .addTo(client);
        depot.setName("Depot");
    }

    private CliRun run(String... options) throws IOException
    {
        return run(Map.of(), options);
    }

    private CliRun run(Map<String, String> env, String... options) throws IOException
    {
        return cli.save(client).run(env, "holdings", options);
    }

    /** With the ECB cache of this test. */
    private CliRun runWithCache(String... options) throws IOException
    {
        return run(Map.of("PP_ECB_RATES", cache.toString()), options);
    }

    private static JsonArray blocks(CliRun run)
    {
        return run.data().getAsJsonArray("holdings");
    }

    private static JsonArray items(JsonObject block)
    {
        return block.getAsJsonArray("items");
    }

    private ClientSnapshot core(String currency, String date)
    {
        return ClientSnapshot.create(client, new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), currency),
                        LocalDate.parse(date));
    }

    private static BigDecimal amount(long stored)
    {
        return BigDecimal.valueOf(stored, Values.Amount.precision());
    }

    @Test
    public void sharesAndValuesMatchCoresClientSnapshot() throws IOException
    {
        var run = run("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var snapshot = core("EUR", "2026-03-01").getJointPortfolio();

        var blocks = blocks(run);
        assertThat(blocks.size(), is(1));
        var items = items(blocks.get(0).getAsJsonObject());
        assertThat(items.size(), is(snapshot.getPositions().size()));

        for (var element : items)
        {
            var item = element.getAsJsonObject();
            var position = snapshot.getPositionsBySecurity().values().stream()
                            .filter(p -> p.getSecurity().getUUID().equals(item.get("instrument").getAsString()))
                            .findFirst().orElseThrow();

            assertThat(item.get("shares").getAsBigDecimal().compareTo(
                            BigDecimal.valueOf(position.getShares(), Values.Share.precision())), is(0));
            assertThat(item.get("amount").getAsBigDecimal().compareTo(amount(position.calculateValue().getAmount())),
                            is(0));
            assertThat(item.get("currencyCode").getAsString(), is("EUR"));
        }

        assertThat(items.size(), is(2));
    }

    private Portfolio secondDepot()
    {
        var cash2 = new AccountBuilder().deposit_("2026-01-01", PortfolioBuilder.amountOf(10_000)).addTo(client);
        cash2.setName("Cash 2");

        var depot2 = new PortfolioBuilder(cash2).buyPrice(alpha, "2026-02-01", 5, 110).addTo(client);
        depot2.setName("Depot 2");
        return depot2;
    }

    private static JsonObject byInstrument(JsonArray items, Security security)
    {
        for (var element : items)
        {
            if (element.getAsJsonObject().get("instrument").getAsString().equals(security.getUUID()))
                return element.getAsJsonObject();
        }
        throw new AssertionError("no position for " + security.getName() + " in " + items);
    }

    private static BigDecimal shares(long stored)
    {
        return BigDecimal.valueOf(stored, Values.Share.precision());
    }

    @Test
    public void positionsAreAggregatedAcrossInvestmentAccountsByDefault() throws IOException
    {
        secondDepot();

        var run = run("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var snapshot = core("EUR", "2026-03-01").getJointPortfolio();
        var items = items(blocks(run).get(0).getAsJsonObject());
        assertThat(items.size(), is(2));
        assertThat(byInstrument(items, alpha).get("shares").getAsBigDecimal().compareTo(shares(15_00000000L)), is(0));
        assertThat(byInstrument(items, alpha).get("amount").getAsBigDecimal().compareTo(
                        amount(snapshot.getPositionsBySecurity().get(alpha).calculateValue().getAmount())), is(0));
        assertThat(blocks(run).get(0).getAsJsonObject().has("investmentAccount"), is(false));
    }

    @Test
    public void investmentAccountFilterRestrictsHoldingsToThatAccount() throws IOException
    {
        var depot2 = secondDepot();

        var run = run("--date", "2026-03-01", "--investment-account", depot2.getUUID());
        assertThat(run.stdout, run.exitCode, is(0));

        var blocks = blocks(run);
        assertThat(blocks.size(), is(1));
        var block = blocks.get(0).getAsJsonObject();
        assertThat(block.getAsJsonObject("investmentAccount").get("uuid").getAsString(), is(depot2.getUUID()));
        assertThat(block.getAsJsonObject("investmentAccount").get("name").getAsString(), is("Depot 2"));

        var snapshot = core("EUR", "2026-03-01").getPortfolios().stream()
                        .filter(p -> p.unwrapPortfolio() == depot2).findFirst().orElseThrow();
        var items = items(block);
        assertThat(items.size(), is(1));
        assertThat(byInstrument(items, alpha).get("shares").getAsBigDecimal().compareTo(shares(5_00000000L)), is(0));
        assertThat(byInstrument(items, alpha).get("amount").getAsBigDecimal().compareTo(
                        amount(snapshot.getPositionsBySecurity().get(alpha).calculateValue().getAmount())), is(0));

        assertThat(run.meta().get("investmentAccount").getAsString(), is(depot2.getUUID()));
    }

    @Test
    public void byAccountGivesOneBlockPerInvestmentAccountOrderedByName() throws IOException
    {
        var depot2 = secondDepot();

        var run = run("--date", "2026-03-01", "--by-account");
        assertThat(run.stdout, run.exitCode, is(0));

        var blocks = blocks(run);
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getAsJsonObject().getAsJsonObject("investmentAccount").get("uuid").getAsString(),
                        is(depot.getUUID()));
        assertThat(blocks.get(1).getAsJsonObject().getAsJsonObject("investmentAccount").get("uuid").getAsString(),
                        is(depot2.getUUID()));
        assertThat(items(blocks.get(0).getAsJsonObject()).size(), is(2));
        assertThat(items(blocks.get(1).getAsJsonObject()).size(), is(1));
        assertThat(byInstrument(items(blocks.get(0).getAsJsonObject()), alpha).get("shares").getAsBigDecimal()
                        .compareTo(shares(10_00000000L)), is(0));
    }

    @Test
    public void byAccountCombinesWithTheInvestmentAccountFilter() throws IOException
    {
        secondDepot();

        var run = run("--date", "2026-03-01", "--by-account", "--investment-account", depot.getUUID());
        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(blocks(run).size(), is(1));
        assertThat(blocks(run).get(0).getAsJsonObject().getAsJsonObject("investmentAccount").get("uuid").getAsString(),
                        is(depot.getUUID()));
    }

    @Test
    public void aFlagTakesNoValue() throws IOException
    {
        var run = run("--date", "2026-03-01", "--by-account=yes");
        assertThat(run.exitCode, is(2));
        assertThat(run.code(), is("invalid-option-value"));
        assertThat(run.problem().get("option").getAsString(), is("by-account"));
    }

    @Test
    public void cashSectionListsCashAccountBalancesAtTheDate() throws IOException
    {
        var cash2 = new AccountBuilder("USD").deposit_("2026-02-01", PortfolioBuilder.amountOf(500))
                        .deposit_("2026-06-01", PortfolioBuilder.amountOf(700)).addTo(client);
        cash2.setName("Aardvark dollars");

        var run = run("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var snapshot = core("EUR", "2026-03-01");
        var items = run.data().getAsJsonObject("cash").getAsJsonArray("items");
        assertThat(items.size(), is(2));

        var first = items.get(0).getAsJsonObject();
        assertThat(first.get("cashAccount").getAsString(), is(cash2.getUUID()));
        assertThat(first.get("name").getAsString(), is("Aardvark dollars"));
        assertThat(first.get("currencyCode").getAsString(), is("USD"));
        assertThat(first.get("amount").getAsBigDecimal().compareTo(new BigDecimal("500.00")), is(0));

        var second = items.get(1).getAsJsonObject();
        assertThat(second.get("cashAccount").getAsString(), is(cash.getUUID()));
        var funds = snapshot.getAccounts().stream().filter(a -> a.getAccount() == cash).findFirst().orElseThrow()
                        .getUnconvertedFunds();
        assertThat(second.get("amount").getAsBigDecimal().compareTo(amount(funds.getAmount())), is(0));
        assertThat(second.get("currencyCode").getAsString(), is("EUR"));
    }

    @Test
    public void rowsCarryAConvertedAmountAndEachBlockAndTheCashSectionATotal() throws IOException
    {
        var run = run("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var snapshot = core("EUR", "2026-03-01");

        for (var element : items(blocks(run).get(0).getAsJsonObject()))
        {
            var converted = element.getAsJsonObject().getAsJsonObject("converted");
            assertThat(converted.get("currencyCode").getAsString(), is("EUR"));
            assertThat(converted.get("amount").getAsBigDecimal().compareTo(
                            element.getAsJsonObject().get("amount").getAsBigDecimal()), is(0));
            assertThat(converted.get("rate").getAsBigDecimal().compareTo(BigDecimal.ONE), is(0));
            assertThat(converted.get("rateDate").getAsString(), is("2026-03-01"));
            assertThat(converted.get("rateSource").getAsString(), is("identity"));
        }

        var total = blocks(run).get(0).getAsJsonObject().getAsJsonObject("total");
        assertThat(total.get("currencyCode").getAsString(), is("EUR"));
        assertThat(total.get("amount").getAsBigDecimal().compareTo(
                        amount(snapshot.getJointPortfolio().getValue().getAmount())), is(0));

        var cashTotal = run.data().getAsJsonObject("cash").getAsJsonObject("total");
        assertThat(cashTotal.get("currencyCode").getAsString(), is("EUR"));
        assertThat(cashTotal.get("amount").getAsBigDecimal().compareTo(
                        amount(snapshot.getAccounts().get(0).getFunds().getAmount())), is(0));

        assertThat(run.meta().get("date").getAsString(), is("2026-03-01"));
        assertThat(run.meta().get("currency").getAsString(), is("EUR"));
    }

    @Test
    public void theDateDefaultsToTodayFromTheClock() throws IOException
    {
        var run = run();
        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("date").getAsString(), is("2026-10-16"));
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
    public void foreignAmountsAreConvertedAtTheRateOfTheDateLikeCore() throws IOException
    {
        var gamma = dollarHolding();
        var usd = EcbCacheFiles.series("EUR", "USD").rate("2026-02-27", "1.0800").rate("2026-03-01", "1.1000")
                        .rate("2026-03-05", "1.2000");
        EcbCacheFiles.write(cache, usd);

        var run = runWithCache("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var core = ClientSnapshot.create(client, CoreRates.converter("EUR", usd), LocalDate.parse("2026-03-01"));
        var date = LocalDate.parse("2026-03-01");

        var block = blocks(run).get(0).getAsJsonObject();
        var position = byInstrument(items(block), gamma);
        var coreValue = core.getJointPortfolio().getPositionsBySecurity().get(gamma).calculateValue();
        assertThat(position.get("currencyCode").getAsString(), is("USD"));
        assertThat(position.get("amount").getAsBigDecimal().compareTo(amount(coreValue.getAmount())), is(0));

        var converted = position.getAsJsonObject("converted");
        assertThat(converted.get("currencyCode").getAsString(), is("EUR"));
        assertThat(converted.get("amount").getAsBigDecimal().compareTo(
                        amount(core.getCurrencyConverter().convert(date, coreValue).getAmount())), is(0));
        assertThat(converted.get("rate").getAsBigDecimal().compareTo(new BigDecimal("0.9090909091")), is(0));
        assertThat(converted.get("rateDate").getAsString(), is("2026-03-01"));
        assertThat(converted.get("rateSource").getAsString(), is("ecb"));

        assertThat(block.getAsJsonObject("total").get("amount").getAsBigDecimal()
                        .compareTo(amount(core.getJointPortfolio().getValue().getAmount())), is(0));

        var cashItems = run.data().getAsJsonObject("cash").getAsJsonArray("items");
        var dollars = cashItems.get(1).getAsJsonObject();
        assertThat(dollars.get("name").getAsString(), is("Dollars"));
        var coreFunds = core.getAccounts().stream().filter(a -> a.getAccount().getName().equals("Dollars"))
                        .findFirst().orElseThrow().getFunds();
        assertThat(dollars.getAsJsonObject("converted").get("amount").getAsBigDecimal()
                        .compareTo(amount(coreFunds.getAmount())), is(0));
        assertThat(run.data().getAsJsonObject("cash").getAsJsonObject("total").get("amount").getAsBigDecimal()
                        .compareTo(amount(core.getAccounts().stream().mapToLong(a -> a.getFunds().getAmount()).sum())),
                        is(0));

        assertThat(run.meta().getAsJsonObject("exchangeRates").get("path").getAsString(), is(cache.toString()));
        assertThat(run.warnings().size(), is(0));
    }

    @Test
    public void currencyReportsTheTotalsInThatCurrencyAndKeepsTheNativeAmounts() throws IOException
    {
        var usd = EcbCacheFiles.series("EUR", "USD").rate("2026-03-01", "1.1000");
        EcbCacheFiles.write(cache, usd);

        var run = runWithCache("--date", "2026-03-01", "--currency", "USD");
        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(run.meta().get("currency").getAsString(), is("USD"));

        var core = ClientSnapshot.create(client, CoreRates.converter("USD", usd), LocalDate.parse("2026-03-01"));
        var block = blocks(run).get(0).getAsJsonObject();
        assertThat(block.getAsJsonObject("total").get("currencyCode").getAsString(), is("USD"));
        assertThat(block.getAsJsonObject("total").get("amount").getAsBigDecimal()
                        .compareTo(amount(core.getJointPortfolio().getValue().getAmount())), is(0));

        var item = items(block).get(0).getAsJsonObject();
        assertThat(item.get("currencyCode").getAsString(), is("EUR"));
        assertThat(item.getAsJsonObject("converted").get("currencyCode").getAsString(), is("USD"));
        assertThat(item.getAsJsonObject("converted").get("rate").getAsBigDecimal().compareTo(new BigDecimal("1.1000")),
                        is(0));
    }

    @Test
    public void aTotalIsLeftOutWhenAFigureHasNoRateAndTheMissingRateIsReportedOnce() throws IOException
    {
        var gamma = dollarHolding();
        // the only rate is dated after the day needed, so there is none
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-03-05", "1.2000"));

        var run = runWithCache("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var block = blocks(run).get(0).getAsJsonObject();
        assertThat(byInstrument(items(block), gamma).has("converted"), is(false));
        assertThat(byInstrument(items(block), alpha).has("converted"), is(true));
        assertThat(block.has("total"), is(false));

        var cashSection = run.data().getAsJsonObject("cash");
        assertThat(cashSection.has("total"), is(false));
        assertThat(cashSection.getAsJsonArray("items").get(0).getAsJsonObject().has("converted"), is(true));

        var missing = run.warnings("exchange-rate-missing");
        assertThat(missing.size(), is(1));
        assertThat(missing.get(0).get("baseCurrency").getAsString(), is("USD"));
        assertThat(missing.get(0).get("termCurrency").getAsString(), is("EUR"));
        // the position and the dollar cash account
        assertThat(missing.get(0).get("count").getAsInt(), is(2));
    }

    @Test
    public void aBlockWithoutTheMissingRateStillHasItsTotal() throws IOException
    {
        dollarHolding();
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-03-05", "1.2000"));

        var run = runWithCache("--date", "2026-03-01", "--by-account", "--investment-account", depot.getUUID());
        assertThat(run.stdout, run.exitCode, is(0));

        assertThat(blocks(run).get(0).getAsJsonObject().has("total"), is(true));
    }

    @Test
    public void nothingToConvertNeedsNoRate() throws IOException
    {
        // sold out, and an empty dollar account: zero amounts convert to zero without a rate
        sellBetaAndBuyGammaLater();
        var empty = new AccountBuilder("USD").addTo(client);
        empty.setName("Empty dollars");
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "GBP").rate("2026-03-01", "0.8500"));

        // no cache at all: it must not even be looked at
        var run = run("--date", "2026-03-01", "--include-zero");
        assertThat(run.stdout, run.exitCode, is(0));

        assertThat(blocks(run).get(0).getAsJsonObject().has("total"), is(true));
        assertThat(run.data().getAsJsonObject("cash").has("total"), is(true));
        assertThat(run.warnings().size(), is(0));
        var emptyAccount = run.data().getAsJsonObject("cash").getAsJsonArray("items").get(1).getAsJsonObject();
        assertThat(emptyAccount.get("name").getAsString(), is("Empty dollars"));
        assertThat(emptyAccount.has("converted"), is(false));
    }

    @Test
    public void aStaleRateIsUsedAndWarnedAboutOncePerPair() throws IOException
    {
        dollarHolding();
        // 28 days before the date
        EcbCacheFiles.write(cache, EcbCacheFiles.series("EUR", "USD").rate("2026-02-01", "1.0800"));

        var run = runWithCache("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var block = blocks(run).get(0).getAsJsonObject();
        assertThat(block.has("total"), is(true));

        var stale = run.warnings("stale-exchange-rate");
        assertThat(stale.size(), is(1));
        assertThat(stale.get(0).get("baseCurrency").getAsString(), is("USD"));
        assertThat(stale.get(0).get("termCurrency").getAsString(), is("EUR"));
        assertThat(stale.get(0).get("count").getAsInt(), is(2));
        assertThat(stale.get(0).get("maxAgeDays").getAsInt(), is(28));
    }

    @Test
    public void anUnavailableCacheWarnsAndLeavesOutWhatNeedsIt() throws IOException
    {
        dollarHolding();

        var run = run("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        assertThat(run.warnings("exchange-rate-cache-unavailable").size(), is(1));
        assertThat(run.warnings("exchange-rate-missing").size(), is(1));
        assertThat(blocks(run).get(0).getAsJsonObject().has("total"), is(false));
        assertThat(run.meta().has("exchangeRates"), is(false));
    }

    @Test
    public void aStalePriceWarnsWithTheInstrumentAndItsAge() throws IOException
    {
        // Alpha's latest price is from 2026-03-01
        var run = run("--date", "2026-03-20");
        assertThat(run.stdout, run.exitCode, is(0));

        var stale = run.warnings("stale-price");
        assertThat(stale.size(), is(2));
        assertThat(stale.get(0).get("instrument").getAsString(), is(alpha.getUUID()));
        assertThat(stale.get(0).get("date").getAsString(), is("2026-03-01"));
        assertThat(stale.get(0).get("ageDays").getAsInt(), is(19));

        var current = run("--date", "2026-03-05");
        assertThat(current.warnings("stale-price").size(), is(0));
    }

    @Test
    public void stalePriceWarningsComeInInstrumentNameOrderAcrossAccounts() throws IOException
    {
        // Another's block comes before Depot's, yet Zulu's warning comes last
        var zulu = new SecurityBuilder().addTo(client);
        zulu.setName("Zulu");
        zulu.addPrice(new SecurityPrice(LocalDate.parse("2026-03-01"), PortfolioBuilder.quoteOf(10)));
        var another = new PortfolioBuilder(cash).buyPrice(zulu, "2026-01-04", 1, 10).addTo(client);
        another.setName("Another");

        var run = run("--date", "2026-03-20", "--by-account");
        assertThat(run.stdout, run.exitCode, is(0));

        var instruments = run.warnings("stale-price").stream().map(w -> w.get("instrument").getAsString()).toList();
        assertThat(instruments, is(List.of(alpha.getUUID(), beta.getUUID(), zulu.getUUID())));
    }

    @Test
    public void anUnknownCurrencyIsAUsageError() throws IOException
    {
        var run = run("--currency", "XXY");
        assertThat(run.exitCode, is(2));
        assertThat(run.code(), is("invalid-option-value"));
        assertThat(run.problem().get("option").getAsString(), is("currency"));
    }

    private void sellBetaAndBuyGammaLater()
    {
        var gamma = new SecurityBuilder().addTo(client);
        gamma.setName("Gamma");

        TransactionFixtures.sell(depot, cash, beta, "2026-02-01", PortfolioBuilder.sharesOf(4),
                        PortfolioBuilder.amountOf(220));
        TransactionFixtures.buy(depot, cash, gamma, "2026-06-01", PortfolioBuilder.sharesOf(1),
                        PortfolioBuilder.amountOf(10));
    }

    @Test
    public void zeroShareHoldingsAreHiddenByDefault() throws IOException
    {
        sellBetaAndBuyGammaLater();

        var run = run("--date", "2026-03-01");
        assertThat(run.stdout, run.exitCode, is(0));

        var items = items(blocks(run).get(0).getAsJsonObject());
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getAsJsonObject().get("instrument").getAsString(), is(alpha.getUUID()));
    }

    @Test
    public void includeZeroShowsSoldOutPositionsButNotOnesNotYetBought() throws IOException
    {
        sellBetaAndBuyGammaLater();

        var run = run("--date", "2026-03-01", "--include-zero");
        assertThat(run.stdout, run.exitCode, is(0));

        var items = items(blocks(run).get(0).getAsJsonObject());
        assertThat(items.size(), is(2));
        var soldOut = byInstrument(items, beta);
        assertThat(soldOut.get("shares").getAsBigDecimal().signum(), is(0));
        assertThat(soldOut.get("amount").getAsBigDecimal().signum(), is(0));
        assertThat(soldOut.get("currencyCode").getAsString(), is("EUR"));
        assertThat(items.get(0).getAsJsonObject().get("name").getAsString(), is("Alpha"));
        assertThat(items.get(1).getAsJsonObject().get("name").getAsString(), is("Beta"));
    }
}
