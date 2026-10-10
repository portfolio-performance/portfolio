package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SecurityDividendPersistenceTest
{
    @Test
    public void testProtobufRoundTrip() throws IOException
    {
        Client client = new Client();
        Account account = new Account();
        account.setName("Cash");
        client.addAccount(account);

        Security security = new Security();
        security.setName("Bitcoin");
        security.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("Wallet");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2025, 2, 3, 8, 48), CurrencyUnit.EUR,
                        2_43, security, Values.Share.factorize(0.0243), PortfolioTransaction.Type.DIVIDENDS, 0, 0));

        ProtobufWriter writer = new ProtobufWriter();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.save(client, output);

        Client restored = writer.load(new ByteArrayInputStream(output.toByteArray()));
        PortfolioTransaction transaction = restored.getPortfolios().get(0).getTransactions().get(0);
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DIVIDENDS));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0.0243)));
        assertThat(transaction.getAmount(), is(2_43L));
        assertThat(restored.getAccounts().get(0).getTransactions().size(), is(0));
    }
}
