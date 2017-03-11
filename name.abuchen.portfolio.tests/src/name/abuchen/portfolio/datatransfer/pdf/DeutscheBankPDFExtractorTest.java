package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DeutscheBankPDFExtractorTest
{

    @Test
    public void testSanityCheckForBankName() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return "some text";
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), instanceOf(UnsupportedOperationException.class));
    }

    private Security assertSecurity(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertThat(security.getWkn(), is("BASF11"));
        assertThat(security.getName(), is("BASF SE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankErtragsgutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = assertSecurity(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2014-12-15")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 14_95L)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 4_52)));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, 19_47)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(123)));
    }

    @Test
    public void testErtragsgutschriftWhenSecurityExists() throws IOException
    {
        Client client = new Client();
        Security security = new Security("BASF", "DE000BASF111", null, null);
        client.addSecurity(security);

        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(client)
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankErtragsgutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
    }
    
    @Test
    public void testDividendengutschriftWhenSecurityExists() throws IOException
    {
        Client client = new Client();
        Security security = new Security("CISCO", "US17275R1023", null, null);
        client.addSecurity(security);

        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(client)
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankDividendengutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2014-12-15")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 64_88L)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 8_71 + 47 + 13_07)));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, 87_13)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(380)));
    }

    @Test
    public void testErtragsgutschrift2() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("DeutscheBankErtragsgutschrift2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getName(), is("ISHS-MSCI N. AMERIC.UCITS ETF BE.SH.(DT.ZT.)"));
        assertThat(security.getIsin(), is("DE000A0J2060"));
        assertThat(security.getWkn(), is("A0J206"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));

        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2015-03-24")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 16_17L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(123)));

        Optional<Unit> grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE);
        assertThat(grossValue.isPresent(), is(true));
        assertThat(grossValue.get().getAmount(), is(Money.of("EUR", 16_17L)));
        assertThat(grossValue.get().getForex(), is(Money.of("USD", 17_38L)));
        assertThat(grossValue.get().getExchangeRate().doubleValue(), IsCloseTo.closeTo(0.930578, 0.000001));
    }

    @Test
    public void testWertpapierKauf() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankKauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(675.50))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-04-08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(19)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 10_50L)));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankKauf2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3524.98))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-04-08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(36)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 11_38L)));
    }

    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankVerkauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2074.71))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-04-08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(122.94 + 6.76))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.90 + 0.60 + 2))));
    }

    @Test
    public void testWertpapierVerkauf2() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankVerkauf2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(453.66))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-01-30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.90 + 0.60 + 2))));
    }
    
    @Test
    public void testWertpapierVerkauf3() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("DeutscheBankVerkauf3.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4753.16))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2017-02-20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.94 + 3.50 + 5.40))));
    }


    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
