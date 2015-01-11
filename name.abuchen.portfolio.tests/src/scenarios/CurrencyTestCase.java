package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Date;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.AssetCategory;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.GroupByTaxonomy;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.BeforeClass;
import org.junit.Test;

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
        client = ClientFactory.load(SecurityTestCase.class.getResourceAsStream("currency_sample.xml"),
                        new NullProgressMonitor());

        securityEUR = client.getSecurities().stream().filter(s -> s.getName().equals("BASF")).findFirst().get();
        securityUSD = client.getSecurities().stream().filter(s -> s.getName().equals("Apple")).findFirst().get();
        accountEUR = client.getAccounts().stream().filter(s -> s.getName().equals("Account EUR")).findFirst().get();
        accountUSD = client.getAccounts().stream().filter(s -> s.getName().equals("Account USD")).findFirst().get();
    }

    @Test
    public void testClientSnapshot()
    {
        Date requestedTime = Dates.date("2015-01-31");

        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, requestedTime);

        AccountSnapshot accountEURsnapshot = lookupAccountSnapshot(snapshot, accountEUR);
        assertThat(accountEURsnapshot.getFunds(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(accountEURsnapshot.getUnconvertedFunds(), is(Money.of(CurrencyUnit.EUR, 1000_00)));

        AccountSnapshot accountUSDsnapshot = lookupAccountSnapshot(snapshot, accountUSD);
        assertThat(accountUSDsnapshot.getFunds(), is(Money.of(CurrencyUnit.EUR, 823_66)));
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
        Money cashValuation = Money.of(CurrencyUnit.EUR, 1823_66);
        assertThat(cash.getValuation(), is(cashValuation));

        AssetPosition positionEUR = getAssetPositionByName(grouping, accountEUR.getName());
        assertThat(positionEUR.getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));

        AssetPosition positionUSD = getAssetPositionByName(grouping, accountUSD.getName());
        assertThat(positionUSD.getValuation(), is(Money.of(CurrencyUnit.EUR, 823_66)));

        Money equityEURvaluation = Money.of(CurrencyUnit.EUR, 20 * securityEUR.getSecurityPrice(grouping.getDate())
                        .getValue());
        Money equityUSDvaluation = converter.convert(grouping.getDate(),
                        Money.of("USD", 10 * securityUSD.getSecurityPrice(grouping.getDate()).getValue()));
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

        assertThat(equityUSD.getPosition().getShares(), is(10_00000L));
        // purchase value must be sum of both purchases:
        // the one in EUR account and the one in USD account
        assertThat(equityUSD.getPosition().getFIFOPurchaseValue(),
                        is(Money.of("USD", Math.round(454_60 * 1.2141) + 571_90)));

        // price per share is the total purchase price minus 20 USD fees and
        // taxes divided by the number of shares
        Money pricePerShare = equityUSD.getPosition().getFIFOPurchaseValue() //
                        .substract(Money.of("USD", 20_00)).divide(10);
        assertThat(equityUSD.getPosition().getFIFOPurchasePrice(), is(pricePerShare));

        // profit loss w/o rounding differences
        assertThat(equityUSD.getProfitLoss(), is(equityUSD.getValuation().substract(equityUSD.getFIFOPurchaseValue())));
        assertThat(equityUSD.getPosition().getProfitLoss(),
                        is(equityUSD.getPosition().calculateValue()
                                        .substract(equityUSD.getPosition().getFIFOPurchaseValue())));

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
