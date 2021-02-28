package name.abuchen.portfolio.datatransfer.pdf.traderepublic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TradeRepublicPDFExtractor;
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
public class TradeRepublicPDFExtractorTest
{
    @Test
    public void testKauf01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.05))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-05-13T12:14")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testKauf02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US08862E1091"));
        assertThat(security.getName(), is("Beyond Meat Inc. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.19))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-05-13T13:59")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testKauf03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US88160R1014"));
        assertThat(security.getName(), is("Tesla Inc. Registered Shares DL-,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(193.08))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-06-17T12:27")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testKauf04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0007472060"));
        assertThat(security.getName(), is("Wirecard AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(485.80))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-11-05T15:16")));
        assertThat(tx.getShares(), is(Values.Share.factorize(4)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testKontoauszug()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeRepublicKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item i = iter.next();
        AccountTransaction transaction = (AccountTransaction) i.getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 150_00L)));

        // assert transaction
        transaction = (AccountTransaction) iter.next().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 50_00L)));

        // assert transaction
        transaction = (AccountTransaction) iter.next().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 50_00L)));

        // assert transaction
        transaction = (AccountTransaction) iter.next().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 50_00L)));

        // assert transaction
        transaction = (AccountTransaction) iter.next().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 50_00L)));

    }
    
    @Test
    public void testKontoauszug02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeRepublicKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item i = iter.next();
        AccountTransaction transaction = (AccountTransaction) i.getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 299_96L)));
    }
    
    @Test
    public void testKontoauszug03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeRepublicKontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item i = iter.next();
        AccountTransaction transaction = (AccountTransaction) i.getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 4_20L)));
    }

    @Test
    public void testSteuerabrechnung01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeRepublicSteuerabrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item i = iter.next();
        AccountTransaction transaction = (AccountTransaction) i.getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 4_26L)));
    }

    @Test
    public void testVerkauf01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("AU000000CUV3"));
        assertThat(security.getName(), is("Clinuvel Pharmaceuticals Ltd. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1792.29))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-06-18T17:50")));
        assertThat(tx.getShares(), is(Values.Share.factorize(80)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
        assertThat(tx.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.63 + 1.68))));
    }

    @Test
    public void testVerkauf02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000TR95XU9"));
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG Call 15.12.21 DAX 14000"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3594.00))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-06-10T11:42")));
        assertThat(tx.getShares(), is(Values.Share.factorize(500)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        if (iter.hasNext())
        {
            Item i = iter.next();

            AccountTransaction transaction = (AccountTransaction) i.getSubject();

            // assert transaction
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-12T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(21.63)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
    }

    @Test
    public void testVerkauf03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0007100000"));
        assertThat(security.getName(), is("Daimler AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1199.00))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-07-21T09:30")));
        assertThat(tx.getShares(), is(Values.Share.factorize(30)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        if (iter.hasNext())
        {
            Item i = iter.next();

            AccountTransaction transaction = (AccountTransaction) i.getSubject();

            // assert transaction
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-23T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(139.58)));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
    }

    @Test
    public void testVerkauf04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M62Q58"));
        assertThat(security.getName(), is("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();
    
        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
    
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.292))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-09-12T12:19")));
        assertThat(tx.getShares(), is(Values.Share.factorize(0.0068)));
    
    }

    @Test
    public void testTilgung01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tilgung01.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000TT22GS8"));
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG TurboC O.End Linde"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();
    
        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
    
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.70))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(700)));
    
        // check transaction
        AccountTransaction t = results.stream() //
                        .filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()) //
                        .findAny().get();
        
        assertThat(t.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.19))));
        
    }
    
    @Test
    public void testTilgung02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tilgung02.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE1234567891"));
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG TurboP O.End ShopApEu"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();
    
        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
    
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(250)));     
    }

    @Test
    public void testVorabpauschale01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "TradeRepublicVorabpauschale01.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
    
        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
    
        // assert security
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."));
    
        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();
    
        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(173.3905)));
    }
}
