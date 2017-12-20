package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hamcrest.number.IsCloseTo;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.GroupByTaxonomy;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;

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

        securityEUR = client.getSecurities().stream().filter(s -> s.getName().equals("BASF")).findFirst().get();
        securityUSD = client.getSecurities().stream().filter(s -> s.getName().equals("Apple")).findFirst().get();
        accountEUR = client.getAccounts().stream().filter(s -> s.getName().equals("Account EUR")).findFirst().get();
        accountUSD = client.getAccounts().stream().filter(s -> s.getName().equals("Account USD")).findFirst().get();
    }

    @Test
    public void testClientSnapshot()
    {
        LocalDate requestedTime = LocalDate.parse("2015-01-16");

        ClientSnapshot snapshot = ClientSnapshot.createEndOfDay(client, converter, requestedTime);

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

        assertThat(grouping.getCategories().map(c -> c.getValuation()).collect(MoneyCollectors.sum(CurrencyUnit.EUR)),
                        is(grouping.getValuation()));
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
                        * securityEUR.getSecurityPrice(grouping.getDate().toLocalDate()).getValue() / Values.Quote.dividerToMoney()));
        Money equityUSDvaluation = Money.of("USD", Math.round(10
                        * securityUSD.getSecurityPrice(grouping.getDate().toLocalDate()).getValue() / Values.Quote.dividerToMoney()))
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
        // purchase value must be sum of both purchases:
        // the one in EUR account and the one in USD account

        // must take the inverse of the exchange used within the transaction
        BigDecimal rate = BigDecimal.ONE.divide(BigDecimal.valueOf(0.8237), 10, BigDecimal.ROUND_HALF_DOWN);
        
        assertThat(equityUSD.getPosition().getFIFOPurchaseValue(),
                        is(Money.of("USD", Math.round(454_60 * rate.doubleValue()) + 571_90)));

        // price per share is the total purchase price minus 20 USD fees and
        // taxes divided by the number of shares
        Money pricePerShare = equityUSD.getPosition().getFIFOPurchaseValue() //
                        .subtract(Money.of("USD", 20_00)).divide(10);
        assertThat(equityUSD.getPosition().getFIFOPurchasePrice(), is(pricePerShare));

        // profit loss w/o rounding differences
        assertThat(equityUSD.getProfitLoss(), is(equityUSD.getValuation().subtract(equityUSD.getFIFOPurchaseValue())));
        assertThat(equityUSD.getPosition().getProfitLoss(), is(equityUSD.getPosition().calculateValue()
                        .subtract(equityUSD.getPosition().getFIFOPurchaseValue())));

    }

    @Test
    public void testClientPerformanceSnapshot()
    {
        ReportingPeriod period = new ReportingPeriod.FromXtoY(LocalDate.parse("2015-01-02"),
                        LocalDate.parse("2015-01-14"));
        ClientPerformanceSnapshot performance = new ClientPerformanceSnapshot(client, converter, period);

        // calculating the totals is tested with #testClientSnapshot
        assertThat(performance.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 4131_99)));
        assertThat(performance.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 4187_94)));

        assertThat(performance.getAbsoluteDelta(),
                        is(performance.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(performance.getValue(CategoryType.TRANSFERS))
                                        .subtract(performance.getValue(CategoryType.INITIAL_VALUE))));

        assertThat(performance.getValue(CategoryType.CAPITAL_GAINS)
                        .add(performance.getValue(CategoryType.CURRENCY_GAINS)),
                        is(performance.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(performance.getValue(CategoryType.INITIAL_VALUE))));

        // compare with result calculated by Excel's XIRR function
        assertThat(performance.getPerformanceIRR(), IsCloseTo.closeTo(0.505460984, 0.00000001));
    }

    @Test
    public void testFIFOPurchasePriceWithForex()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDateTime.of(2015, 8, 9, 0, 0));

        // 1.1. ........ -> 454.60 EUR
        // 1.1. 571.90 $ -> 471.05 EUR (exchange rate: 1.2141)
        // 3.8. 577.60 $ -> 498.45 EUR (exchange rate: 1.1588)

        AssetPosition position = snapshot.getPositionsByVehicle().get(securityUSD);
        assertThat(position.getPosition().getShares(), is(Values.Share.factorize(15)));
        assertThat(position.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 454_60 + 471_05 + 498_45)));

        ReportingPeriod period = new ReportingPeriod.FromXtoY(LocalDate.parse("2014-12-31"),
                        LocalDate.parse("2015-08-10"));
        SecurityPerformanceSnapshot performance = SecurityPerformanceSnapshot.create(client, converter, period);
        SecurityPerformanceRecord record = performance.getRecords().stream().filter(r -> r.getSecurity() == securityUSD)
                        .findAny().get();
        assertThat(record.getSharesHeld(), is(Values.Share.factorize(15)));
        assertThat(record.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 454_60 + 471_05 + 498_45)));
    }

    private AccountSnapshot lookupAccountSnapshot(ClientSnapshot snapshot, Account account)
    {
        for (AccountSnapshot as : snapshot.getAccounts())
            if (account.equals(as.getAccount()))
                return as;

        return null;
    }

    private AssetCategory getAssetCategoryByName(GroupByTaxonomy grouping, String label)
    {
        return grouping.asList().stream().filter(category -> label.equals(category.getClassification().getName()))
                        .findFirst().get();
    }

    private AssetPosition getAssetPositionByName(GroupByTaxonomy grouping, String label)
    {
        return grouping.asList().stream().flatMap(category -> category.getPositions().stream())
                        .filter(position -> label.equals(position.getDescription())).findFirst().get();
    }
}
