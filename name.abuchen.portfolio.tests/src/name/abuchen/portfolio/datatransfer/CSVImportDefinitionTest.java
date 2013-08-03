package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.CSVImportDefinition;
import name.abuchen.portfolio.datatransfer.CSVImportDefinition.AccountTransactionDef;
import name.abuchen.portfolio.datatransfer.CSVImportDefinition.PortfolioTransactionDef;
import name.abuchen.portfolio.datatransfer.CSVImportDefinition.SecurityDef;
import name.abuchen.portfolio.datatransfer.CSVImportDefinition.SecurityPriceDef;
import name.abuchen.portfolio.datatransfer.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.CSVImporter.Field;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("nls")
public class CSVImportDefinitionTest
{
    @Test
    public void testThatSecurityIsFoundByISIN_whenImportingAccountTransactions() throws ParseException
    {
        Client client = buildClient();
        Account account = client.getAccounts().get(0);

        AccountTransactionDef def = new AccountTransactionDef();

        def.build(client, account, //
                        new String[] { "2013-01-01", "DE0007164600", "", "", "100", "" }, //
                        buildField2Column(def));

        AccountTransaction t = account.getTransactions().get(account.getTransactions().size() - 1);
        assertThat(t.getAmount(), is(100L * Values.Amount.factor()));
        assertThat(t.getDate(), is(Dates.date(2013, Calendar.JANUARY, 1)));
        assertThat(t.getSecurity(), is(client.getSecurities().get(0)));
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
    }

    @Test
    public void testThatSecurityIsFoundByTicker_whenImportingAccountTransactions() throws ParseException
    {
        Client client = buildClient();
        Account account = client.getAccounts().get(0);

        AccountTransactionDef def = new AccountTransactionDef();

        def.build(client, account, //
                        new String[] { "2013-01-01", "", "SAP.DE", "", "200", "DIVIDENDS" }, //
                        buildField2Column(def));

        AccountTransaction t = account.getTransactions().get(account.getTransactions().size() - 1);
        assertThat(t.getAmount(), is(200L * Values.Amount.factor()));
        assertThat(t.getDate(), is(Dates.date(2013, Calendar.JANUARY, 1)));
        assertThat(t.getSecurity(), is(client.getSecurities().get(0)));
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
    }

    @Test
    public void testThatSecurityIsFoundByWKN_whenImportingAccountTransactions() throws ParseException
    {
        Client client = buildClient();
        Account account = client.getAccounts().get(0);

        AccountTransactionDef def = new AccountTransactionDef();

        def.build(client, account, //
                        new String[] { "2013-01-01", "", "", "716460", "300", "FEES" }, //
                        buildField2Column(def));

        AccountTransaction t = account.getTransactions().get(account.getTransactions().size() - 1);
        assertThat(t.getAmount(), is(300L * Values.Amount.factor()));
        assertThat(t.getDate(), is(Dates.date(2013, Calendar.JANUARY, 1)));
        assertThat(t.getSecurity(), is(client.getSecurities().get(0)));
        assertThat(t.getType(), is(AccountTransaction.Type.FEES));
    }

    @Test
    public void testThatSecurityIsCreatedIfNotFound_whenImportingAccountTransactions() throws ParseException
    {
        Client client = buildClient();
        Account account = client.getAccounts().get(0);

        AccountTransactionDef def = new AccountTransactionDef();

        def.build(client, account, //
                        new String[] { "2013-01-01", "DE000BASF111", "BAS.DE", "BASF11", "100", "" }, //
                        buildField2Column(def));

        AccountTransaction t = account.getTransactions().get(account.getTransactions().size() - 1);
        assertThat(t.getAmount(), is(100L * Values.Amount.factor()));
        assertThat(t.getDate(), is(Dates.date(2013, Calendar.JANUARY, 1)));
        assertThat(t.getSecurity(), isIn(client.getSecurities()));
        assertThat(t.getSecurity().getIsin(), is("DE000BASF111"));
        assertThat(t.getSecurity().getTickerSymbol(), is("BAS.DE"));
        assertThat(t.getSecurity().getWkn(), is("BASF11"));
    }

    @Test
    public void testThatPortfolioTransactionIsImported() throws ParseException
    {
        Client client = buildClient();
        Portfolio portfolio = client.getPortfolios().get(0);
        Security security = client.getSecurities().get(0);

        PortfolioTransactionDef def = new PortfolioTransactionDef();

        def.build(client, portfolio, //
                        new String[] { "2013-01-01", security.getIsin(), "", "", "1000,00", "10,00", "1,234", "BUY" }, //
                        buildField2Column(def));

        PortfolioTransaction t = portfolio.getTransactions().get(portfolio.getTransactions().size() - 1);
        assertThat(t.getSecurity(), is(security));
        assertThat(t.getShares(), is((long) (1.234 * Values.Share.factor())));
        assertThat(t.getFees(), is(10L * Values.Amount.factor()));
        assertThat(t.getAmount(), is(1000L * Values.Amount.factor()));
        assertThat(t.getType(), is(PortfolioTransaction.Type.BUY));
    }

    @Test
    public void testThatSecurityPriceIsImported() throws ParseException
    {
        Client client = buildClient();
        Security security = client.getSecurities().get(0);

        SecurityPriceDef def = new SecurityPriceDef();

        def.build(client, security, //
                        new String[] { "2013-01-01", "123,45" }, //
                        buildField2Column(def));

        SecurityPrice price = security.getSecurityPrice(Dates.date(2013, Calendar.JANUARY, 1));
        assertThat(price.getValue(), is(12345L));
    }

    @Test
    public void testThatSecurityIsImported() throws ParseException
    {
        Client client = buildClient();

        SecurityDef def = new SecurityDef();

        def.build(client, Messages.CSVDefSecurityMasterData, //
                        new String[] { "DE0008404005", "ALV.DE", "840400", "Allianz", "" }, //
                        buildField2Column(def));

        Security s = client.getSecurities().get(client.getSecurities().size() - 1);
        assertThat(s.getName(), is("Allianz"));
        assertThat(s.getIsin(), is("DE0008404005"));
        assertThat(s.getTickerSymbol(), is("ALV.DE"));
        assertThat(s.getWkn(), is("840400"));
    }

    @Test
    public void testThatSecurityIsNotImportedIfItAlreadyExists()
    {
        try
        {
            Client client = buildClient();

            SecurityDef def = new SecurityDef();

            def.build(client, Messages.CSVDefSecurityMasterData, //
                            new String[] { "DE0007164600", "", "", "SAP AG", "" }, //
                            buildField2Column(def));

            Assert.fail("Expected an exception about missing security identifier");
        }
        catch (ParseException e)
        {
            int p = Messages.CSVImportSecurityExists.indexOf('{');
            assertThat(e.getMessage(), startsWith(Messages.CSVImportSecurityExists.substring(0, p)));
        }
    }

    @Test
    public void testThatExceptionIsThrownIfOneOfRequiredFieldsIsMissing_whenImportingSecurities()
    {
        try
        {
            Client client = buildClient();

            SecurityDef def = new SecurityDef();

            def.build(client, Messages.CSVDefSecurityMasterData, //
                            new String[] { "something" }, //
                            new HashMap<String, Column>());

            Assert.fail("Expected an exception about missing security identifier");
        }
        catch (ParseException e)
        {
            int p = Messages.CSVImportMissingOneOfManyFields.indexOf('{');
            assertThat(e.getMessage(), startsWith(Messages.CSVImportMissingOneOfManyFields.substring(0, p)));
        }
    }

    private Client buildClient()
    {
        Client client = new Client();

        Account account = new Account();
        account.setName("test");
        client.addAccount(account);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("test");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        Security security = new Security();
        security.setName("SAP AG");
        security.setIsin("DE0007164600");
        security.setWkn("716460");
        security.setTickerSymbol("SAP.DE");
        client.addSecurity(security);

        return client;
    }

    private Map<String, Column> buildField2Column(CSVImportDefinition def)
    {
        Map<String, Column> field2column = new HashMap<String, Column>();

        int index = 0;
        for (Field f : def.getFields())
        {
            Column column = new Column(index++, f.getName());
            column.setField(f);

            if (f instanceof DateField)
                column.setFormat(DateField.FORMATS[0]);
            else if (f instanceof AmountField)
                column.setFormat(AmountField.FORMATS[0]);

            field2column.put(f.getName(), column);
        }
        return field2column;
    }

}
