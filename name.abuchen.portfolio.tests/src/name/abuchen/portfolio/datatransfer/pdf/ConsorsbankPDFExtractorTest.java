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

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ConsorsbankPDFExtractorTest
{
    
    private Security assertSecurity(List<Item> results, boolean mustHaveIsin)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        if (mustHaveIsin)
            assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMS.-MSCI WORL.T.U.ETF I"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = assertSecurity(results, false);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertDividendTransaction(security, item);

        // check tax
        AccountTransaction t = (AccountTransaction) item.get().getSubject();
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 111_00L + 6_10L)));
    }

    private void assertDividendTransaction(Security security, Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2015-05-08")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", 326_90L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1370)));
    }

    @Test
    public void testErtragsgutschrift2() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift2.txt")), errors);

        assertThat(errors, empty());

        // since taxes are zero, no tax transaction must be created
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1444))));
    }

    @Test
    public void testErtragsgutschrift3() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift3.txt")), errors);

        assertThat(errors, empty());

        // since taxes are zero, no tax transaction must be created
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getWkn(), is("850866"));
        assertThat(security.getName(), is("DEERE & CO."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findAny().get().getSubject();
        assertThat(t.getDate(), is(LocalDate.parse("2015-11-02")));
        assertThat(t.getShares(), is(Values.Share.factorize(300)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 121_36)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 180_00)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 16_30 + 89 + 24_45)));
    }

    @Test
    public void testErtragsgutschrift4() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift4.txt")), errors);

        assertThat(errors, empty());

        // since taxes are zero, no tax transaction must be created
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("854242"));
        assertThat(security.getName(), is("WESTPAC BANKING CORP."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDate(), is(LocalDate.parse("2015-07-02")));
        assertThat(t.getShares(), is(Values.Share.factorize(1.0002)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 46)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("AUD", 93)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 16)));
    }

    @Test
    public void testErtragsgutschrift5() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift5.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("885823"));
        assertThat(security.getName(), is("GILEAD SCIENCES INC."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDate(), is(LocalDate.parse("2015-06-29")));
        assertThat(t.getShares(), is(Values.Share.factorize(0.27072)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 8)));
        assertThat(t.getUnit(Unit.Type.GROSS_VALUE).get().getForex(), is(Money.of("USD", 12)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 1 + 2)));
    }
    
    @Test
    public void testErtragsgutschrift8() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift8.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("891106"));
        assertThat(security.getName(), is("ROCHE HOLDING AG"));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDate(), is(LocalDate.parse("2014-04-22")));
        assertThat(t.getShares(), is(Values.Share.factorize(80)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 33_51)));
        assertThat(t.getGrossValue(), is(Money.of("EUR", 64_08)));
        
        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 30_57)));

        checkCurrency(CurrencyUnit.EUR, t);
    }
    
    @Test
    public void testErtragsgutschrift9() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift9.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("A1409D"));
        assertThat(security.getName(), is("Welltower Inc."));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getDate(), is(LocalDate.parse("2016-05-20")));
        assertThat(t.getShares(), is(Values.Share.factorize(50)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 32_51)));
        assertThat(t.getGrossValue(), is(Money.of("EUR", 38_25)));
        
        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 5_74)));

        checkCurrency(CurrencyUnit.EUR, t);
    }
    

    @Test
    public void testErtragsgutschrift8WithExistingSecurity() throws IOException
    {
        Client client = new Client();
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(client)
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        
        Security existingSecurity = new Security("ROCHE HOLDING AG", CurrencyUnit.EUR);
        existingSecurity.setWkn("891106");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankErtragsgutschrift8.txt")), errors);
        
        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividend transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).filter(
                        i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DIVIDENDS)
                        .findFirst().get().getSubject();
        assertThat(t.getSecurity(), is(existingSecurity));
        assertThat(t.getDate(), is(LocalDate.parse("2014-04-22")));
        assertThat(t.getShares(), is(Values.Share.factorize(80)));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", 33_51)));
        assertThat(t.getGrossValue(), is(Money.of("EUR", 64_08)));

        // check tax
        assertThat(t.getUnitSum(Type.TAX), is(Money.of("EUR", 30_57)));
        
        checkCurrency(CurrencyUnit.EUR, t);
    }
    
    @Test
    public void testBezug1() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankBezug1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0V9L94"));
        assertThat(security.getWkn(), is("A0V9L9"));
        assertThat(security.getName(), is("EYEMAXX R.EST.AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
        

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 399_96L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2016-05-10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(66_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 3_96L)));
    }
    
    @Test
    public void testWertpapierVerkauf1() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankVerkauf1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getWkn(), is("A0DPR2"));
        assertThat(security.getName(), is("VOLKSWAGEN AG VZ ADR1/5"));

        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        Item item = results.get(0);
        BuySellEntry entry = (BuySellEntry) item.getSubject();
        PortfolioTransaction t = entry.getPortfolioTransaction();
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5794_56L)));
        assertThat(t.getUnitSum(Type.FEE), is(Money.of(CurrencyUnit.EUR, 26_65L)));
        assertThat(t.getUnitSum(Type.TAX), is(Money.of(CurrencyUnit.EUR, 226_79L)));
        assertThat(t.getDate(), is(LocalDate.parse("2015-02-20")));
        assertThat(t.getShares(), is(Values.Share.factorize(140)));
        assertThat(t.getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(43.2))));
    }
    
    @Test
    public void testWertpapierKauf() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankKauf.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertSecurity(results, true);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5000_00L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-01-15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(132_802120L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankKauf2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> secItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(secItem.isPresent(), is(true));
        Security security = ((SecurityItem) secItem.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A0L1NN5"));
        assertThat(security.getWkn(), is("A0L1NN"));
        assertThat(security.getName(), is("HELIAD EQ.PARTN.KGAA"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1387_85L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-09-21")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 17_85L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(5.48))));
    }
    
    @Test
    public void testWertpapierKaufSparplan() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankKaufSparplan.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> secItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(secItem.isPresent(), is(true));
        Security security = ((SecurityItem) secItem.get()).getSecurity();
        assertThat(security.getIsin(), is("PO6527623674"));
        assertThat(security.getWkn(), is("SP110Y"));
        assertThat(security.getName(), is("Sparplanname"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2016-06-15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.43915)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(15.53))));
    }

    @Test
    public void testWertpapierKaufIfSecurityIsPresent() throws IOException
    {
        Client client = new Client();
        Security s = new Security();
        s.setName("COMS.-MSCI WORL.T.U.ETF I");
        s.setWkn("ETF110");
        client.addSecurity(s);

        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(client)
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankKauf.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        Item item = results.get(0);
        BuySellEntry entry = (BuySellEntry) item.getSubject();
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5000_00L)));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
    
    private void checkCurrency(final String accountCurrency, AccountTransaction transaction)
    {
        Account account = new Account();
        account.setCurrencyCode(accountCurrency);
        Status status = new CheckCurrenciesAction().process(transaction, account);
        assertThat(status.getCode(), is(Code.OK));
    }
}
