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
    public void testSnapshots()
    {
        Date requestedTime = Dates.date("2015-01-31");
        TestCurrencyConverter converter = new TestCurrencyConverter(requestedTime);
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, requestedTime);

        AccountSnapshot accountEURsnapshot = lookupAccountSnapshot(snapshot, accountEUR);
        assertThat(accountEURsnapshot.getFunds(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(accountEURsnapshot.getUnconvertedFunds(), is(Money.of(CurrencyUnit.EUR, 1000_00)));

        AccountSnapshot accountUSDsnapshot = lookupAccountSnapshot(snapshot, accountUSD);
        assertThat(accountUSDsnapshot.getFunds(), is(Money.of(CurrencyUnit.EUR, 833_20)));
        assertThat(accountUSDsnapshot.getUnconvertedFunds(), is(Money.of("USD", 1000_00)));

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(client.getTaxonomy("30314ba9-949f-4bf4-944e-6a30802f5190"));

        AssetCategory cash = getAssetCategoryByName(grouping, "Barvermögen");
        assertThat(cash.getValuation(), is(Money.of(CurrencyUnit.EUR, 1833_20)));

        AssetPosition positionEUR = getAssetPositionByName(grouping, accountEUR.getName());
        assertThat(positionEUR.getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));

        AssetPosition positionUSD = getAssetPositionByName(grouping, accountUSD.getName());
        assertThat(positionUSD.getValuation(), is(Money.of(CurrencyUnit.EUR, 833_20)));

        Money equityEURvaluation = Money.of(CurrencyUnit.EUR, 20 * securityEUR.getSecurityPrice(requestedTime)
                        .getValue());
        Money equityUSDvaluation = converter.convert(Money.of("USD", 10 * securityUSD.getSecurityPrice(requestedTime)
                        .getValue()));
        Money equityValuation = Money.of(CurrencyUnit.EUR,
                        equityEURvaluation.getAmount() + equityUSDvaluation.getAmount());

        AssetCategory equity = getAssetCategoryByName(grouping, "Eigenkapital");
        assertThat(equity.getValuation(), is(equityValuation));

        AssetPosition equityEUR = getAssetPositionByName(grouping, securityEUR.getName());
        assertThat(equityEUR.getValuation(), is(equityEURvaluation));

        AssetPosition equityUSD = getAssetPositionByName(grouping, securityUSD.getName());
        assertThat(equityUSD.getValuation(), is(equityUSDvaluation));
        assertThat(equityUSD.getFIFOPurchaseValue().getCurrencyCode(), is(CurrencyUnit.EUR));

        assertThat(grouping.getValuation(), is(Money.of(CurrencyUnit.EUR, 1833_20 + equityValuation.getAmount())));
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
