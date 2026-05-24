package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import org.hamcrest.number.IsCloseTo;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.GroupByTaxonomy;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CurrencyTestCase
{
    private static TestCurrencyConverter converter = new TestCurrencyConverter();

    private static Client client;
    private static Security securityEUR;
    private static Security securityUSD;
    private static Account accountEUR;
    private static Account accountUSD;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(SecurityTestCase.class.getResourceAsStream("currency_sample.xml"));

        securityEUR = client.getSecurities().stream().filter(s -> s.getName().equals("BASF")).findFirst()
                        .orElseThrow(IllegalArgumentException::new);
        securityUSD = client.getSecurities().stream().filter(s -> s.getName().equals("Apple")).findFirst()
                        .orElseThrow(IllegalArgumentException::new);
        accountEUR = client.getAccounts().stream().filter(s -> s.getName().equals("Account EUR")).findFirst()
                        .orElseThrow(IllegalArgumentException::new);
        accountUSD = client.getAccounts().stream().filter(s -> s.getName().equals("Account USD")).findFirst()
                        .orElseThrow(IllegalArgumentException::new);
    }

    @Test
    public void testClientSnapshot()
    {
        LocalDate requestedTime = LocalDate.parse("2015-01-16");

        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, requestedTime);

        AccountSnapshot accountEURsnapshot = lookupAccountSnapshot(snapshot, accountEUR);
        assertThat(accountEURsnapshot.getFunds(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(accountEURsnapshot.getUnconvertedFunds(), is(Money.of(CurrencyUnit.EUR, 1000_00)));

        AccountSnapshot accountUSDsnapshot = lookupAccountSnapshot(snapshot, accountUSD);
        assertThat(accountUSDsnapshot.getFunds(), is(Money.of(CurrencyUnit.EUR, Math.round(1000_00 * (1 / 1.1588)))));
        assertThat(accountUSDsnapshot.getUnconvertedFunds(), is(Money.of("USD", 1000_00)));

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(client.getTaxonomy("30314ba9-949f-4bf4-944e-6a30802f5190"));

        testTotals(snapshot, grouping);
        testAssetCategories(grouping);
        testUSDAssetPosition(grouping);
    }

    private void testTotals(ClientSnapshot snapshot, GroupByTaxonomy grouping)
    {
        assertThat(snapshot.getMonetaryAssets(), is(grouping.getValuation()));

        assertThat(grouping.getCategories().map(AssetCategory::getValuation)
                        .collect(MoneyCollectors.sum(CurrencyUnit.EUR)), is(grouping.getValuation()));
    }

    private void testAssetCategories(GroupByTaxonomy grouping)
    {
        AssetCategory cash = getAssetCategoryByName(grouping, "Barvermögen");
        Money cashValuation = Money.of(CurrencyUnit.EUR, 1000_00 + Math.round(1000_00 * (1 / 1.1588)));
        assertThat(cash.getValuation(), is(cashValuation));

        AssetPosition positionEUR = getAssetPositionByName(grouping, accountEUR.getName());
        assertThat(positionEUR.getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));

        AssetPosition positionUSD = getAssetPositionByName(grouping, accountUSD.getName());
        assertThat(positionUSD.getValuation(), is(Money.of(CurrencyUnit.EUR, Math.round(1000_00 * (1 / 1.1588)))));

        Money equityEURvaluation = Money.of(CurrencyUnit.EUR, Math.round(20
                        * securityEUR.getSecurityPrice(grouping.getDate()).getValue() / Values.Quote.dividerToMoney()));
        Money equityUSDvaluation = Money.of("USD", Math.round(10
                        * securityUSD.getSecurityPrice(grouping.getDate()).getValue() / Values.Quote.dividerToMoney()))
                        .with(converter.at(grouping.getDate()));
        Money equityValuation = Money.of(CurrencyUnit.EUR,
                        equityEURvaluation.getAmount() + equityUSDvaluation.getAmount());

        AssetCategory equity = getAssetCategoryByName(grouping, "Eigenkapital");
        assertThat(equity.getValuation(), is(equityValuation));

        AssetPosition equityEUR = getAssetPositionByName(grouping, securityEUR.getName());
        assertThat(equityEUR.getValuation(), is(equityEURvaluation));

        AssetPosition equityUSD = getAssetPositionByName(grouping, securityUSD.getName());
        assertThat(equityUSD.getValuation(), is(equityUSDvaluation));

        assertThat(grouping.getValuation(), is(cashValuation.add(equityValuation)));
    }

    private void testUSDAssetPosition(GroupByTaxonomy grouping)
    {
        AssetPosition equityUSD = getAssetPositionByName(grouping, securityUSD.getName());

        assertThat(equityUSD.getPosition().getShares(), is(Values.Share.factorize(10)));

        Interval interval = Interval.of(LocalDate.MIN, grouping.getDate());
        LazySecurityPerformanceRecord recordUSD = LazySecurityPerformanceSnapshot
                        .create(client, converter.with(CurrencyUnit.USD), interval).getRecord(securityUSD)
                        .orElseThrow(IllegalArgumentException::new);

        LazySecurityPerformanceRecord recordEUR = LazySecurityPerformanceSnapshot
                        .create(client, converter.with(CurrencyUnit.EUR), interval).getRecord(securityUSD)
                        .orElseThrow(IllegalArgumentException::new);

        // purchase value must be sum of both purchases:
        // the one in EUR account and the one in USD account

        // must take the inverse of the exchange used within the transaction
        BigDecimal rate = BigDecimal.ONE.divide(BigDecimal.valueOf(0.8237), 10, RoundingMode.HALF_DOWN);

        assertThat(recordUSD.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.USD, Math.round(454_60 * rate.doubleValue()) + 571_90)));

        // price per share is the total purchase price minus 20 USD fees and
        // taxes divided by the number of shares
        Money pricePerShare = recordUSD.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED) //
                        .subtract(Money.of(CurrencyUnit.USD, 20_00)).divide(10);
        assertThat(recordUSD.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED).toMoney(),
                        is(pricePerShare));

        // profit loss w/o rounding differences

        assertThat(equityUSD.getValuation(), is(recordEUR.getMarketValue()));

        assertThat(recordEUR.getCapitalGainsOnHoldings(CostMethod.FIFO),
                        is(equityUSD.getValuation()
                                        .subtract(recordEUR.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED))));

        assertThat(recordUSD.getCapitalGainsOnHoldings(CostMethod.FIFO),
                        is(equityUSD.getPosition().calculateValue()
                                        .subtract(recordUSD.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED))));

    }

    @Test
    public void testClientPerformanceSnapshot()
    {
        ReportingPeriod period = new ReportingPeriod.FromXtoY(LocalDate.parse("2015-01-02"),
                        LocalDate.parse("2015-01-14"));
        ClientPerformanceSnapshot performance = new ClientPerformanceSnapshot(client, converter,
                        period.toInterval(LocalDate.now()));

        // calculating the totals is tested with #testClientSnapshot
        assertThat(performance.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 4131_99)));
        assertThat(performance.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 4187_94)));

        assertThat(performance.getAbsoluteDelta(),
                        is(performance.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(performance.getValue(CategoryType.TRANSFERS))
                                        .subtract(performance.getValue(CategoryType.INITIAL_VALUE))));

        assertThat(performance.getValue(CategoryType.CAPITAL_GAINS)
                        .add(performance.getValue(CategoryType.CURRENCY_GAINS)
                                        .add(performance.getValue(CategoryType.REALIZED_CAPITAL_GAINS))),
                        is(performance.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(performance.getValue(CategoryType.INITIAL_VALUE))));

        // compare with result calculated by Excel's XIRR function
        assertThat(performance.getPerformanceIRR(), IsCloseTo.closeTo(0.505460984, 0.00000001));

        performance.getCategories().stream().flatMap(c -> c.getPositions().stream()).forEach(p -> {
            Money value = p.getValue();
            Optional<Trail> valueTrail = p.explain(ClientPerformanceSnapshot.Position.TRAIL_VALUE);
            valueTrail.ifPresent(t -> assertThat(t.getRecord().getValue(), is(value)));

            Money gain = p.getForexGain();
            Optional<Trail> gainTrail = p.explain(ClientPerformanceSnapshot.Position.TRAIL_FOREX_GAIN);
            gainTrail.ifPresent(t -> assertThat(t.getRecord().getValue(), is(gain)));
        });
    }

    @Test
    public void testFIFOPurchasePriceWithForex()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.parse("2015-08-09"));

        // 1.1. ........ -> 454.60 EUR
        // 1.1. 571.90 $ -> 471.05 EUR (exchange rate: 1.2141)
        // 3.8. 577.60 $ -> 498.45 EUR (exchange rate: 1.1588)

        AssetPosition position = snapshot.getPositionsByVehicle().get(securityUSD);
        assertThat(position.getPosition().getShares(), is(Values.Share.factorize(15)));

        Interval inteval = Interval.of(LocalDate.MIN, snapshot.getTime());
        LazySecurityPerformanceRecord recordEUR = LazySecurityPerformanceSnapshot
                        .create(client, converter.with(CurrencyUnit.EUR), inteval).getRecord(securityUSD)
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(recordEUR.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, 454_60L + 471_05 + 498_45)));

        Interval period = Interval.of(LocalDate.parse("2014-12-31"), LocalDate.parse("2015-08-10"));
        LazySecurityPerformanceSnapshot performance = LazySecurityPerformanceSnapshot.create(client, converter, period);

        LazySecurityPerformanceRecord record = performance.getRecords().stream()
                        .filter(r -> r.getSecurity() == securityUSD).findAny()
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(record.getSharesHeld(), is(Values.Share.factorize(15)));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, 454_60L + 471_05 + 498_45)));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        assertThat(record.getMarketValue(), is(Money.of("EUR", 149534L)));
        assertThat(record.getQuote(), is(Quote.of("USD", 11552000000L)));
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of("EUR", 142410L)));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of("EUR", 9384200000L)));
        assertThat(record.getFees(), is(Money.of("EUR", 824L)));
        assertThat(record.getTaxes(), is(Money.of("EUR", 824L)));
        assertThat(record.getDelta(), is(Money.of("EUR", 7124L)));
        assertThat(record.getDeltaPercent(), IsCloseTo.closeTo(0.050024576925777685, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO), is(Money.of("EUR", 7124L)));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE), is(Money.of("EUR", 7124L)));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO),
                        IsCloseTo.closeTo(0.050024576925777664, 0.0001));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        IsCloseTo.closeTo(0.050024576925777664, 0.0001));
        assertThat(record.getIrr(), IsCloseTo.closeTo(0.1446282845545095, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), IsCloseTo.closeTo(0.10417377427377805, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturnAnnualized(),
                        IsCloseTo.closeTo(0.17695466576686947, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdown(), IsCloseTo.closeTo(0.12421268652674962, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdownDuration().getDays(), is(168L));
        assertThat(record.getVolatility().getStandardDeviation(), IsCloseTo.closeTo(0.17746878011093473, 0.0001));
        assertThat(record.getVolatility().getSemiDeviation(), IsCloseTo.closeTo(0.12318433923170118, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 0L)));
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.NONE));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 0L)));
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 0L)));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 8771L)));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getForexCaptialGains(),
                        is(Money.of("EUR", 4339L)));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 8771L)));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getForexCaptialGains(),
                        is(Money.of("EUR", 4335L)));
    }

    private AccountSnapshot lookupAccountSnapshot(ClientSnapshot snapshot, Account account)
    {
        for (AccountSnapshot as : snapshot.getAccounts())
            if (account.equals(as.getAccount()))
                return as;

        throw new IllegalArgumentException("account " + account + " not found");
    }

    private AssetCategory getAssetCategoryByName(GroupByTaxonomy grouping, String label)
    {
        return grouping.asList().stream().filter(category -> label.equals(category.getClassification().getName()))
                        .findFirst().orElseThrow(IllegalArgumentException::new);
    }

    private AssetPosition getAssetPositionByName(GroupByTaxonomy grouping, String label)
    {
        return grouping.asList().stream().flatMap(category -> category.getPositions().stream())
                        .filter(position -> label.equals(position.getDescription())).findFirst()
                        .orElseThrow(IllegalArgumentException::new);
    }
}
