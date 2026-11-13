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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
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

    @Test
    public void testDividendWithoutSecurityConvertedToInterest() throws IOException
    {
        // This test verifies that when loading a file with a dividend
        // transaction that has no security, it is automatically converted
        // to an interest payment transaction

        var client = new Client();

        var account = new Account();
        account.setName("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        // Create a dividend transaction without a security
        // (which should not be possible in the current codebase, but may
        // exist in old files)
        var dividend = new AccountTransaction();
        dividend.setType(AccountTransaction.Type.DIVIDENDS);
        dividend.setDateTime(LocalDateTime.now());
        dividend.setCurrencyCode(CurrencyUnit.EUR);
        dividend.setAmount(Values.Amount.factorize(100));
        dividend.setSecurity(null); // explicitly no security
        account.addTransaction(dividend);

        // Save to binary format
        var protobufWriter = new ProtobufWriter();
        var stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        // Load from binary format
        var in = new ByteArrayInputStream(stream.toByteArray());
        Client reloadedClient = protobufWriter.load(in);

        // Verify the transaction was converted to INTEREST
        assertThat(reloadedClient.getAccounts().size(), is(1));
        assertThat(reloadedClient.getAccounts().get(0).getTransactions().size(), is(1));

        var reloadedTransaction = reloadedClient.getAccounts().get(0).getTransactions().get(0);
        assertThat(reloadedTransaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(reloadedTransaction.getSecurity(), is((Security) null));
        assertThat(reloadedTransaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));
    }

    @Test
    public void testPercentageQuotedSecurityRoundtrip() throws IOException
    {
        Client client = new Client();

        Security bond = new Security("Bond", CurrencyUnit.EUR);
        bond.setUpdatedAt(Instant.now());
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);

        Security share = new Security("Share", CurrencyUnit.EUR);
        share.setUpdatedAt(Instant.now());
        client.addSecurity(share);

        ProtobufWriter protobufWriter = new ProtobufWriter();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        Client newClient = protobufWriter.load(new ByteArrayInputStream(stream.toByteArray()));

        assertThat(newClient.getSecurities().size(), is(2));
        assertThat(newClient.getSecurities().stream().filter(s -> "Bond".equals(s.getName())).findFirst()
                        .orElseThrow().isPercentageQuoted(), is(true));
        assertThat(newClient.getSecurities().stream().filter(s -> "Share".equals(s.getName())).findFirst()
                        .orElseThrow().isPercentageQuoted(), is(false));
    }

    @Test
    public void testAccruedInterestRoundtrip() throws IOException
    {
        Client newClient = saveAndLoad(createClientWithAccruedInterest(), true);
        assertAccruedInterest(newClient);
    }

    @Test
    public void testAccruedInterestRoundtripXML() throws IOException
    {
        Client newClient = saveAndLoad(createClientWithAccruedInterest(), false);
        assertAccruedInterest(newClient);
    }

    private static Client createClientWithAccruedInterest()
    {
        Client client = new Client();

        Account account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(Instant.now());
        client.addAccount(account);

        Portfolio portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        portfolio.setUpdatedAt(Instant.now());
        client.addPortfolio(portfolio);

        Security bond = new Security("Bond", CurrencyUnit.EUR);
        bond.setUpdatedAt(Instant.now());
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);

        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2025-08-19T00:00"));
        entry.setSecurity(bond);
        entry.setShares(Values.Share.factorize(1000));
        entry.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1015.50)));
        entry.getPortfolioTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.ACCRUED_INTEREST,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.50))));
        entry.insert();

        return client;
    }

    private static Client saveAndLoad(Client client, boolean binary) throws IOException
    {
        if (!binary)
            return ClientFactory.duplicate(client); // XML serialization

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new ProtobufWriter().save(client, stream);
        stream.close();
        return new ProtobufWriter().load(new ByteArrayInputStream(stream.toByteArray()));
    }

    private static void assertAccruedInterest(Client client)
    {
        PortfolioTransaction t = client.getPortfolios().get(0).getTransactions().get(0);
        assertThat(t.getAccruedInterest(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.50))));
        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1010.00))));
    }
}
