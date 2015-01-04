package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.MonetaryException;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
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

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(SecurityTestCase.class.getResourceAsStream("currency_sample.xml"),
                        new NullProgressMonitor());
    }

    @Test
    public void testSnapshots()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, new DummyCurrencyConverter(), Dates.date("2015-01-31"));

        AccountSnapshot accountEUR = getAccountSnapshotByName(snapshot, "Account EUR");
        assertThat(accountEUR.getFunds(), is(Money.of("EUR", 1000_00)));
        assertThat(accountEUR.getUnconvertedFunds(), is(Money.of("EUR", 1000_00)));

        AccountSnapshot accountUSD = getAccountSnapshotByName(snapshot, "Account USD");
        assertThat(accountUSD.getFunds(), is(Money.of("EUR", 833_20)));
        assertThat(accountUSD.getUnconvertedFunds(), is(Money.of("USD", 1000_00)));

        GroupByTaxonomy grouping = snapshot.groupByTaxonomy(client.getTaxonomy("30314ba9-949f-4bf4-944e-6a30802f5190"));
        assertThat(grouping.getValuation(), is(Money.of("EUR", 1833_20)));

        AssetPosition positionEUR = getAssetPositionByName(grouping, "Account EUR");
        assertThat(positionEUR.getValuation(), is(Money.of("EUR", 1000_00)));

        AssetPosition positionUSD = getAssetPositionByName(grouping, "Account USD");
        assertThat(positionUSD.getValuation(), is(Money.of("EUR", 833_20)));
    }

    private AccountSnapshot getAccountSnapshotByName(ClientSnapshot snapshot, String label)
    {
        for (AccountSnapshot as : snapshot.getAccounts())
            if (label.equals(as.getAccount().getName()))
                return as;

        return null;
    }

    private AssetPosition getAssetPositionByName(GroupByTaxonomy grouping, String label)
    {
        return grouping.asList().stream().flatMap(category -> category.getPositions().stream())
                        .filter(position -> label.equals(position.getDescription())).findFirst().get();
    }

    private static class DummyCurrencyConverter implements CurrencyConverter
    {
        @Override
        public String getTermCurrency()
        {
            return "EUR";
        }

        @Override
        public Date getTime()
        {
            return Dates.date("2015-01-31");
        }

        @Override
        public Money convert(Money amount)
        {
            if ("EUR".equals(amount.getCurrencyCode()))
                return amount;

            if ("USD".equals(amount.getCurrencyCode()))
                return Money.of("EUR", Math.round((amount.getAmount() * 8332) / Values.ExchangeRate.divider()));

            throw new MonetaryException();
        }
    }
}
