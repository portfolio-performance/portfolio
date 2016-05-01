package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CSVAccountTransactionExtractorTest
{
    @Test
    public void testDividendTransactionPlusSecurityCreation() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "DE0007164600", "SAP.DE", "", "100",
                                        "EUR", "DIVIDENDS", "SAP SE", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is(nullValue()));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDate(), is(LocalDate.parse("2013-01-01")));
        assertThat(t.getShares(), is(Values.Share.factorize(10)));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testDividendTransaction()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "DE0007164600", "SAP.DE", "", "100",
                                        "EUR", "DIVIDENDS", "SAP SE", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDate(), is(LocalDate.parse("2013-01-01")));
        assertThat(t.getShares(), is(Values.Share.factorize(10)));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testDividendTransaction_whenSecurityIsMissing()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "", "", "100", "EUR", "DIVIDENDS", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }

    @Test
    public void testIfMultipleSecuritiesWithSameISINExist()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);
        Security security2 = new Security();
        security2.setIsin("DE0007164600");
        client.addSecurity(security2);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(
                        0, Arrays.<String[]>asList(new String[] { "2013-01-01", "DE0007164600", "SAP.DE", "", "100",
                                        "EUR", "DIVIDENDS", "SAP SE", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }

    @Test
    public void testTypeIsDeterminedByPositiveAmount()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(
                                        new String[] { "2013-01-02", "", "", "", "100", "EUR", "", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = (AccountTransaction) results.get(0).getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDate(), is(LocalDate.parse("2013-01-02")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(nullValue()));
    }

    @Test
    public void testTypeIsDeterminedByNegativeUnaryMinusOperator()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "", "", "-100", "EUR", "", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = (AccountTransaction) results.get(0).getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDate(), is(LocalDate.parse("2013-01-01")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(nullValue()));
    }

    @Test
    public void testTypeIsDeterminedByUnaryMinusOperatorAndSecurity()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "DE0007164600", "", "", "100", "EUR", "", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
    }

    @Test
    public void testThatSecurityIsAddedOnlyOnce()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2013-01-01", "DE0007164600", "", "", "100", "EUR", "", "", "", "Notiz" },
                        new String[] { "2013-01-02", "DE0007164600", "", "", "200", "EUR", "", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
    }

    @Test
    public void testBuyTransaction()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "DE0007164600", "", "", "100", "EUR", "BUY", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        BuySellEntry e = (BuySellEntry) results.get(0).getSubject();
        AccountTransaction t = e.getAccountTransaction();
        assertThat(t.getType(), is(AccountTransaction.Type.BUY));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDate(), is(LocalDate.parse("2013-01-01")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testBuyTransactionFailsWhenSharesAreMissing()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "DE0007164600", "", "", "100", "EUR", "BUY", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testBuyTransactionFailsWhenSecurityIsMissing()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "", "", "100", "EUR", "BUY", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testTransferTransaction()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2013-01-01", "", "", "", "100", "EUR", "TRANSFER_OUT", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());

        AccountTransferEntry entry = (AccountTransferEntry) results.get(0).getSubject();
        AccountTransaction t = entry.getSourceTransaction();
        assertThat(t.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDate(), is(LocalDate.parse("2013-01-01")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(nullValue()));
    }

    @Test
    public void testRequiredFieldDate()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList( //
                                        new String[] { "", "", "", "", "100", "EUR", "", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testRequiredFieldAmount()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList( //
                                        new String[] { "2015-01-01", "", "", "", "", "EUR", "", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }
}
