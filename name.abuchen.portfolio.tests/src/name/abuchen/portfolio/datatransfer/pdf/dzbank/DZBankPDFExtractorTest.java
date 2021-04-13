package name.abuchen.portfolio.datatransfer.pdf.dzbank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DZBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
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
public class DZBankPDFExtractorTest
{

    @Test
    public void testWertpapierKauf1()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"));
        assertThat(security.getIsin(), is("IE00B3RBWM25"));
        assertThat(security.getWkn(), is("A1JX52"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-13T11:42:59")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1042.34)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(12.35))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(13)));
    }

    @Test
    public void testWertpapierKauf2()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("XING SE NAMENS-AKTIEN O.N."));
        assertThat(security.getIsin(), is("DE000XNG8888"));
        assertThat(security.getWkn(), is("XNG888"));
        
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-06-04T18:09:18")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(690.05)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.05))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
    }
    
    @Test
    public void testWertpapierKauf3()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("DAIMLER AG NAMENS-AKTIEN O.N."));
        assertThat(security.getIsin(), is("DE0007100000"));
        assertThat(security.getWkn(), is("710000"));
        
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-14T15:40:55")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1356.69)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(12.44))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
    }
    
    @Test
    public void testWertpapierKauf4()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf4.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
    
        Optional<Item> item;
    
        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
    
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("NEXTERA ENERGY INC. REGISTERED SHARES DL -,01"));
        assertThat(security.getIsin(), is("US65339F1012"));
        assertThat(security.getWkn(), is("A1CZ4H"));
        
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
    
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
    
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-27T16:18:02")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
    
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(3450.10)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(25.10))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testWertpapierKauf5()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("TECHNOLOGIES INC. REGISTERED SHARES DL-,00001"));
        assertThat(security.getIsin(), is("US90353T1007"));
        assertThat(security.getWkn(), is("A2PHHG"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-01T18:27:57")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(8963.05)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(9.95 + 0.10))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testWertpapierKauf6()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."));
        assertThat(security.getIsin(), is("DE0008404005"));
        assertThat(security.getWkn(), is("840400"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(10531.25)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-17T15:37:21")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX).getAmount(),
                        is(Values.Amount.factorize(0.00)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.15 + 0.10))));
    }

    @Test
    public void testWertpapierKauf7()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Kauf7.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("I.M.III-I.FTSE EM H.D.L.V.UETF REG. SHARES ACC O.N."));
        assertThat(security.getIsin(), is("IE00BYYXBF44"));
        assertThat(security.getWkn(), is("A2AHZU"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(8790.91)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-17T15:09:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(350)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX).getAmount(),
                        is(Values.Amount.factorize(0.00)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26.81 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf1()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Verkauf1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("NEW WORK SE NAMENS-AKTIEN O.N."));
        assertThat(security.getIsin(), is("DE000NWRK013"));
        assertThat(security.getWkn(), is("NWRK01"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-13T15:16:51")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(577.95)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.05))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(29.57))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-13T00:00")));
    }
    
    @Test
    public void testWertpapierVerkauf2()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Verkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("BAYER AG NAMENS-AKTIEN O.N."));
        assertThat(security.getIsin(), is("DE000BAY0017"));
        assertThat(security.getWkn(), is("BAY001"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-09-26T09:04:37")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1904.98)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(37.97))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
    }

    @Test
    public void testWertpapierVerkauf3()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Verkauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getName(), is("NETFLIX INC. REGISTERED SHARES DL -,001"));
        assertThat(security.getIsin(), is("US64110L1061"));
        assertThat(security.getWkn(), is("552484"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();

        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-09T19:48:50")));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(442.29)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(9.95 + 0.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(10.01))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
    }

    @Test
    public void testWertpapierVerkauf4()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankWertpapierabrechnung_Verkauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US29786A1060"));
        assertThat(security.getWkn(), is("A14P98"));
        assertThat(security.getName(), is("ETSY INC. REGISTERED SHARES DL -,001"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(805.45)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-27T15:52:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), 
                        is(Money.of("EUR", Values.Amount.factorize(9.95 + 0.10))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(26.23))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-27T00:00")));
    }
    
    @Test
    public void testAusschuettung1()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankAusschuettung1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("IE00B3RBWM25"));
        assertThat(security.getWkn(), is("A1JX52"));
        assertThat(security.getName(), is("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getShares(), is(Values.Share.factorize(100)));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-12-12T00:00")));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(24.03))));

        assertThat(t.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(26.48))));
        assertThat(t.getUnitSum(Unit.Type.TAX), 
                        is(Money.of("EUR", Values.Amount.factorize(2.17 + 0.11 + 0.17))));
        assertThat(t.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividenden1()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankDividenden1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CA0679011084"));
        assertThat(security.getWkn(), is("870450"));
        assertThat(security.getName(), is("BARRICK GOLD CORP.  SHARES REGISTERED SHARES O.N."));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getShares(), is(Values.Share.factorize(205)));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-02-26T00:00")));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(9.92))));

        assertThat(t.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(15.39))));
        assertThat(t.getUnitSum(Unit.Type.TAX), 
                        is(Money.of("EUR", Values.Amount.factorize(3.85 + 1.54 + 0.08))));
        assertThat(t.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testUmsatzuebersicht01()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DZBankUmsatzuebersicht.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1C81G1"));
        assertThat(security.getName(), is("UniProfiRente UniGlobal Vorsorge"));
        
        // check 1st transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(33.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.159)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.57 + 0.00))));
        
        // check 2nd transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(1).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.196)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.90 + 0.00))));

        // check 3nd transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(2).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.166)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.57 + 0.00))));

        // check 4nd transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(3).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-08-31T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.2)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.90 + 0.00))));
    }

    @Test
    public void testDividenden2()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankDividenden2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0007236101"));
        assertThat(security.getWkn(), is("723610"));
        assertThat(security.getName(), is("SIEMENS AG NAMENS-AKTIEN O.N."));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(160)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(560.00))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(560.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividenden3()
    {
        DZBankPDFExtractor extractor = new DZBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "DZBankDividenden3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("IE00BYYXBF44"));
        assertThat(security.getWkn(), is("A2AHZU"));
        assertThat(security.getName(), is("I.M.III-I.FTSE EM H.D.L.V.UETF REG. SHARES DIS O.N."));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-18T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(350)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(61.58))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(61.58))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
}
