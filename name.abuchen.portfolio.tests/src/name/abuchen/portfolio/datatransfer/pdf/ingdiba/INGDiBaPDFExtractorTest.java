package name.abuchen.portfolio.datatransfer.pdf.ingdiba;

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
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaExtractor;
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
public class INGDiBaPDFExtractorTest
{
    
    @Test
    public void testWertpapierKauf1() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0002635307"));
        assertThat(security.getWkn(), is("263530"));
        assertThat(security.getName(), is("iSh.STOXX Europe 600 U.ETF DE"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(533.39)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-11-17")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0))));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0002635307"));
        assertThat(security.getWkn(), is("263530"));
        assertThat(security.getName(), is("iSh.STOXX Europe 600 U.ETF DE"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(726.28)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-06-11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(18)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0))));
    }

    @Test
    public void testWertpapierKauf3() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf3.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("XS0138973010"));
        assertThat(security.getWkn(), is("778998"));
        assertThat(security.getName(), is("7,125 % Aareal Bank Capital Fdg Trust"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1027.40)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-11-06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(40)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", 2_50L + 9_90L + 3_00L)));
    }

    @Test
    public void testWertpapierKauf4() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf4.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UF5"));
        assertThat(security.getWkn(), is("A0F5UF"));
        assertThat(security.getName(), is("iShare.NASDAQ-100 UCITS ETF DE"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-12-15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.19591)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 86L)));
    }

    @Test
    public void testWertpapierKauf5() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf5.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0MKQK7"));
        assertThat(security.getWkn(), is("A0MKQK"));
        assertThat(security.getName(), is("ETF-PORTFOLIO GLOBAL Inhaber-Anteile"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-12-15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.67647)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0L)));
    }

    @Test
    public void testWertpapierKauf6() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf6.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("GB0002771383"));
        assertThat(security.getWkn(), is("987665"));
        assertThat(security.getName(), is("Threadn.Inv.Fds-Euro.Sm.Cos Fd"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-12-15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.53245)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0L)));
    }

    @Test
    public void testWertpapierKauf7() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf7.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getWkn(), is("865985"));
        assertThat(security.getName(), is("Apple Inc. Registered Shares o.N."));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(5009.71)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2012-03-20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(12.49))));
    }

    @Test
    public void testWertpapierKauf8() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Kauf8.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("GB0030934490"));
        assertThat(security.getWkn(), is("797739"));
        assertThat(security.getName(), is("M&G Inv.(1)-M&G Global Leaders"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(75.00)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2012-02-01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.41234)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0L)));
    }

    @Test
    public void testWertpapierVerkauf1() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Verkauf1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0006632003"));
        assertThat(security.getWkn(), is("663200"));
        assertThat(security.getName(), is("MorphoSys AG Inhaber-Aktien o.N."));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1887.64)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-05-07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(31)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 9_90L)));
    }

    @Test
    public void testWertpapierVerkauf2() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Verkauf2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0002635281"));
        assertThat(security.getWkn(), is("263528"));
        assertThat(security.getName(), is("iSh.EO ST.Sel.Div.30 U.ETF DE"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(568.41)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2016-12-22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 9_90L)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of("EUR", 19_32)));
    }

    @Test
    public void testWertpapierVerkauf3() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Verkauf3.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getWkn(), is("865985"));
        assertThat(security.getName(), is("Apple Inc. Registered Shares o.N."));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(3421.66)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2013-06-21")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 9_90L)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of("EUR", 0)));
    }

    @Test
    public void testErtragsgutschrift1() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Ertragsgutschrift1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0002635307"));
        assertThat(security.getWkn(), is("263530"));
        assertThat(security.getName(), is("iSh.STOXX Europe 600 U.ETF DE"));

        // check buy sell transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(6.70)));
        assertThat(t.getDate(), is(LocalDate.parse("2015-09-15")));
        assertThat(t.getShares(), is(Values.Share.factorize(18)));
    }

    @Test
    public void testErtragsgutschrift2() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Ertragsgutschrift2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A1PGUT9"));
        assertThat(security.getWkn(), is("A1PGUT"));
        assertThat(security.getName(), is("7,25000% posterXXL AG"));

        // check buy sell transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(72.50)));
        assertThat(t.getDate(), is(LocalDate.parse("2015-12-15")));
        assertThat(t.getShares(), is(Values.Share.factorize(0)));
    }

    @Test
    public void testErtragsgutschrift3() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Ertragsgutschrift3.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0002635281"));
        assertThat(security.getWkn(), is("263528"));
        assertThat(security.getName(), is("iSh.EO ST.Sel.Div.30 U.ETF DE"));

        // check buy sell transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(101.32)));
        assertThat(t.getDate(), is(LocalDate.parse("2016-10-17")));
        assertThat(t.getShares(), is(Values.Share.factorize(1112.0958)));

        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.84))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.72 + 0.8))));
    }

    @Test
    public void testDividendengutschrift1() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Dividendengutschrift1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US5801351017"));
        assertThat(security.getWkn(), is("856958"));
        assertThat(security.getName(), is("McDonald's Corp."));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(44.01)));
        assertThat(t.getDate(), is(LocalDate.parse("2016-11-29")));
        assertThat(t.getShares(), is(Values.Share.factorize(66)));

        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.24))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.91 + 0.32))));
    }

    @Test
    public void testDividendengutschrift2() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Dividendengutschrift2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0744"));
        assertThat(security.getWkn(), is("A0H074"));
        assertThat(security.getName(), is("iSh.DJ Asia Pa.S.D.30 U.ETF DE"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(234.92)));
        assertThat(t.getDate(), is(LocalDate.parse("2016-12-15")));
        assertThat(t.getShares(), is(Values.Share.factorize(694)));

        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(65.1 + 3.58))));
        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(303.60))));
    }
    
    @Test
    public void testGemeinschaftskontoDividendengutschrift1() throws IOException
    {
        INGDiBaExtractor extractor = new INGDiBaExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Arrays.asList(new File("INGDiBa_Gemeinschaftskonto_Dividendengutschrift1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000TLX1005"));
        assertThat(security.getWkn(), is("TLX100"));
        assertThat(security.getName(), is("Talanx AG Namens-Aktien o.N."));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(468.04)));
        assertThat(t.getDate(), is(LocalDate.parse("2016-05-12")));
        assertThat(t.getShares(), is(Values.Share.factorize(500)));

        assertThat(t.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(650.00))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(79.46 + 7.15 +  4.37 + 79.46 + 7.15 +  4.37))));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
