package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

public class ProtobufWriterAdditionalTest
{

    @Test
    public void testSavingFutureDates() throws IOException
    {
        Client client = new Client();
        Security security = new Security();
        security.setName(ProtobufWriterAdditionalTest.class.getName());
        security.setUpdatedAt(Instant.now());
        security.addPrice(new SecurityPrice(LocalDate.now().plusDays(10), Values.Quote.factorize(100)));
        client.addSecurity(security);

        // convert to binary format and back

        ProtobufWriter protobufWriter = new ProtobufWriter();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        ByteArrayInputStream in = new ByteArrayInputStream(stream.toByteArray());
        Client newClient = protobufWriter.load(in);

        String expected = ClientTestUtilities.toString(client);
        String actual = ClientTestUtilities.toString(newClient);

        assertThat(actual, is(expected));
    }

    @SuppressWarnings("nls")
    @Test
    public void testCurrencyMismatchErrorHandling() throws IOException
    {
        // This test verifies that the ProtobufWriter gracefully handles
        // currency mismatch errors during deserialization instead of
        // failing the entire file load operation

        Client client = new Client();

        Account account = new Account();
        account.setName("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("Portfolio");
        client.addPortfolio(portfolio);

        Security security = new Security();
        security.setName("Test Security");
        security.setUpdatedAt(Instant.now());
        client.addSecurity(security);

        // create a valid BuySellEntry
        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setDate(LocalDateTime.now());
        entry.setSecurity(security);
        entry.setShares(1 * Values.Share.factor());
        entry.setAmount(1000 * Values.Amount.factor());
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.insert();

        // now change the account currency to create a mismatch scenario
        account.setCurrencyCode(CurrencyUnit.USD);

        // save client with invalid data
        ProtobufWriter protobufWriter = new ProtobufWriter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        // attempt to load the client
        var in = new ByteArrayInputStream(stream.toByteArray());
        Client reloadedClient = protobufWriter.load(in);

        // Should complete loading without throwing exceptions
        assertThat(reloadedClient.getAccounts().size(), is(1));
        assertThat(reloadedClient.getPortfolios().size(), is(1));
        assertThat(reloadedClient.getSecurities().size(), is(1));

        // check that transaction is skipped
        assertThat(reloadedClient.getPortfolios().get(0).getTransactions().size(), is(0));
        assertThat(reloadedClient.getAccounts().get(0).getTransactions().size(), is(0));
    }
}