package name.abuchen.portfolio.datatransfer.pdf.fintechgroupbank;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class FinTechGroupBankPDFExtractorTest
{

    @Test
    public void testWertpapierKauf() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertFirstSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        assertSecondSecurity(results.stream().filter(i -> i instanceof SecurityItem) //
                        .collect(Collectors.toList()).get(1));
        assertSecondTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1));

        assertThirdTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem) //
                        .collect(Collectors.toList()).get(2));

        assertFourthTransaction(results.stream().filter(i -> i instanceof TransactionItem) //
                        .collect(Collectors.toList()).get(0));

    }

    private Security assertFirstSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005194062"));
        assertThat(security.getWkn(), is("519406"));
        assertThat(security.getName(), is("BAYWA AG VINK.NA. O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5893_10L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-01-28T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(150_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(39.248))));
    }

    private Security assertSecondSecurity(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE0008402215"));
        assertThat(security.getWkn(), is("840221"));
        assertThat(security.getName(), is("HANN.RUECK SE NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertSecondTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5954_80L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-01-28T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(100_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(59.489))));
    }

    private void assertThirdTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5943_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-01-28T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(100_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        // keine Steuer, sondern Steuererstattung!
        // assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
        // is(Money.of(CurrencyUnit.EUR, 100_00L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(59.489))));
    }

    private void assertFourthTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(AccountTransaction.class));

        // check Steuererstattung
        AccountTransaction entryTaxReturn = (AccountTransaction) item.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(entryTaxReturn.getDateTime(), is(LocalDateTime.parse("2014-01-28T00:00")));
    }

    @Test
    public void testGutschriftsBelastungsanzeige() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatexGutschriftsBelastungsanzeige.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(20));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0008474503"));
        assertThat(security.getName(), is("DEKAFONDS CF"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(5.50)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2015-02-16T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(0.0520)));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0392495023"));
        assertThat(security.getWkn(), is("ETF114"));
        assertThat(security.getName(), is("C.S.-MSCI PACIF.T.U.ETF I"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50.30)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-12-03T13:59")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
    }

    @Test
    public void testWertpapierKauf3() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B2QWCY14"));
        assertThat(security.getWkn(), is("A0Q1YY"));
        assertThat(security.getName(), is("ISHSIII-S+P SM.CAP600 DLD"));

        PortfolioTransaction transaction = results.stream().filter(i -> i instanceof Extractor.BuySellEntryItem)
                        //
                        .map(i -> (BuySellEntry) ((Extractor.BuySellEntryItem) i).getSubject())
                        .map(b -> b.getPortfolioTransaction()).findAny().get();

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1050)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-15T00:00")));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(19.334524)));
    }

    @Test
    public void testWertpapierKauf4() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0392494992"));
        assertThat(security.getWkn(), is("ETF113"));
        assertThat(security.getName(), is("C.-MSCI NO.AM.TRN U.ETF I"));

        PortfolioTransaction transaction = results.stream().filter(i -> i instanceof Extractor.BuySellEntryItem)
                        //
                        .map(i -> (BuySellEntry) ((Extractor.BuySellEntryItem) i).getSubject())
                        .map(b -> b.getPortfolioTransaction()).findAny().get();

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(transaction.getAmount(), is(Values.Amount.factorize(800)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-16T00:00")));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(13.268957)));
    }

    @Test
    public void testWertpapierKauf6() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B2NPKV68"));
        assertThat(security.getWkn(), is("A0NECU"));
        assertThat(security.getName(), is("ISHSII-JPM DL EM BD DLDIS"));

        PortfolioTransaction transaction = results.stream().filter(i -> i instanceof Extractor.BuySellEntryItem)
                        //
                        .map(i -> (BuySellEntry) ((Extractor.BuySellEntryItem) i).getSubject())
                        .map(b -> b.getPortfolioTransaction()).findAny().get();

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-15T00:00")));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.9))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(9.703363)));
    }
    
    @Test
    public void testWertpapierKauf7() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf7.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(20));
        
        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());
        
        assertThat(tx.size(), is(10));
        
        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2017-11-01T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3008.9)))))));
    }

    @Test
    public void testWertpapierKauf8() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf8.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        
        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());
        
        assertThat(tx.size(), is(1));
        
        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2018-01-09T15:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.16)))))));
    }

    @Test
    public void testWertpapierKauf9Sammelabrechnung() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf9Sammelabrechnung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1MECS1"));
        assertThat(security.getWkn(), is("A1MECS"));
        assertThat(security.getName(), is("SOURCE PHY.MRKT.ETC00 XAU"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(2.72)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-01-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.025361)));
    }
    
    @Test
    public void testWertpapierKauf10() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0274211480"));
        assertThat(security.getWkn(), is("DBX1DA"));
        assertThat(security.getName(), is("DB X-TRACK.DAX ETF(DR)1C"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1000.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7.979324)));
    }

    @Test
    public void testWertpapierKauf11() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B6YX5D40"));
        assertThat(security.getWkn(), is("A1JKS0"));
        assertThat(security.getName(), is("SPDR S+P US DIV.ARIST.ETF"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1000.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.5))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22.973458)));
    }

    @Test
    public void testWertpapierKauf12() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0001234567"));
        assertThat(security.getWkn(), is("DS5WKN"));
        assertThat(security.getName(), is("DEUT.BANK CALL20 BBB"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1023.90)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-08-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.9))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2000)));
    }
    
    @Test
    public void testWertpapierKauf13() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertThat(security.getName(), is("COMS.-MSCI EM.M.T.U.ETF I"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(52.50)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.43414)));
    }

    @Test
    public void testWertpapierKauf14() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00BF2B0K52"));
        assertThat(security.getWkn(), is("A2DTF1"));
        assertThat(security.getName(), is("FRAN.LIB.Q EM EQ.UC.DLA"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1279.55)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-17T17:52")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.61))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
    }
    
    @Test
    public void testWertpapierKauf15_Fonds2019() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf15_Fonds2019.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertThat(security.getWkn(), is("A111X9"));
        assertThat(security.getName(), is("IS C.MSCI EMIMI U.ETF DLA"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(760.09)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-10T17:30")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.41))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(29)));
    }

    @Test
    public void testKontoauszug() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKontoauszug.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1100_00L)));

    }

    @Test
    public void testKontoauszug2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKontoauszug2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 15000_00L)));
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0008402215"));
        assertThat(security.getWkn(), is("840221"));
        assertThat(security.getName(), is("HANN.RUECK SE NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-05-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 795_15L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(360)));
    }

    @Test
    public void testErtragsgutschrift2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE1234567890"));
        assertThat(security.getWkn(), is("AB1234"));
        assertThat(security.getName(), is("ISH.FOOBAR 12345666 x.EFT"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-01-15T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(55.55)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(99)));
    }

    @Test
    public void testErtragsgutschrift3() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0006335003"));
        assertThat(security.getWkn(), is("633500"));
        assertThat(security.getName(), is("KRONES AG O.N."));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(17.13)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.12))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(15)));
    }
    
    @Test
    public void testErtragsgutschrift4_Fonds2019() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift4_Fonds2019.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("IE00B945VV12"));
        assertThat(security.getWkn(), is("A1T8FS"));
        assertThat(security.getName(), is("VANG.FTSE DEV.EU.UETF EOD"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-04-10T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(36.07)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(197)));
    }

    @Test
    public void testDividendeAusland() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US8552441094"));
        assertThat(security.getWkn(), is("884437"));
        assertThat(security.getName(), is("STARBUCKS CORP."));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(14.45)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.78))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(105)));
    }
    
    @Test
    public void testDividendeAusland2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("GB00B03MLX29"));
        assertThat(security.getWkn(), is("A0D94M"));
        assertThat(security.getName(), is("ROYAL DUTCH SHELL A EO-07"));
        
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(60.97)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.76))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(180)));
    }

    @Test
    public void testZinsgutschriftInland() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexZinsgutschriftInland.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE1234567890"));
        assertThat(security.getWkn(), is("AB1234"));
        assertThat(security.getName(), is("ISH.FOOBAR 12345666 x.EFT"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-04-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(73.75)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1000)));
    }

    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getWkn(), is("US9RGR"));
        assertThat(security.getName(), is("UBS AG LONDON 14/16 RWE"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(16508.16)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testWertpapierVerkauf2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0323578657"));
        assertThat(security.getWkn(), is("A0M430"));
        assertThat(security.getName(), is("FLOSSB.V.STORCH-MUL.OPP.R"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(10.12)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-12-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
    }

    @Test
    public void testWertpapierVerkauf3() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0009807008"));
        assertThat(security.getWkn(), is("980700"));
        assertThat(security.getName(), is("GRUNDBESITZ EUROPA RC"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(4840.15)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-07-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(121)));
    }
    
    @Test
    public void testWertpapierVerkauf4() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        
        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());
        
        assertThat(tx.size(), is(1));
        
        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2018-01-09T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95)))))));
    }
    
    @Test
    public void testWertpapierVerkauf5() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        
        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());
        
        assertThat(tx.size(), is(1));
        
        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2019-02-06T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.48)))))));
    }
    
    @Test
    public void testWertpapierVerkauf6() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        
        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());
        
        assertThat(tx.size(), is(1));
        
        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2019-04-09T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4416.52)))))));
    }
    
    @Test
    public void testWertpapierÜbertrag1() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepoteingang1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getName(), is("UBS AG LONDON 14/16 RWE"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(7517.50)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2015-11-24T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testWertpapierÜbertrag2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepoteingang2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getName(), is("UBS AG LONDON 14/16 RWE"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(7517.50)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2015-11-24T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testWertpapierAusgang() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepotausgang.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CM31SV9"));
        assertThat(security.getName(), is("COMMERZBANK INLINE09EO/SF"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(2867.88)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2009-12-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(325)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 382_12L)));
    }

    @Test
    public void testWertpapierAusgang2() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepotausgang2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CK1Q3N7"));
        assertThat(security.getName(), is("COMMERZBANK INLINE11EO/SF"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(0.20)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2011-07-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
    }

    @Test
    public void testWertpapierBestandsausbuchung() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexBestandsausbuchung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertFirstSecurityBestandsausbuchung(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransactionBestandsausbuchung(
                        results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        assertSecondSecurityBestandsausbuchung(results.stream().filter(i -> i instanceof SecurityItem) //
                        .collect(Collectors.toList()).get(1));
        assertSecondTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(1));
        assertThirdSecurityBestandsausbuchung(results.stream().filter(i -> i instanceof SecurityItem) //
                        .collect(Collectors.toList()).get(2));
        assertThirdTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(2));

    }

    private Security assertFirstSecurityBestandsausbuchung(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CB81KN1"));
        assertThat(security.getName(), is("COMMERZBANK PUT10 EOLS"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertFirstTransactionBestandsausbuchung(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-03-16T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(2000_000000L));
    }

    private Security assertSecondSecurityBestandsausbuchung(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CM3C8A3"));
        assertThat(security.getName(), is("COMMERZBANK CALL10 EO/DL"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertSecondTransactionBestandsausbuchung(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-03-16T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(1250_000000L));
    }

    private Security assertThirdSecurityBestandsausbuchung(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CM3C896"));
        assertThat(security.getName(), is("COMMERZBANK CALL10 EO/DL"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertThirdTransactionBestandsausbuchung(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-03-16T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(750_000000L));
    }

    @Test
    public void testWertpapierBestandsausbuchungNeuesFormat() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexBestandsausbuchung2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000SG0WRD3"));
        assertThat(security.getWkn(), is("SG0WRD"));
        assertThat(security.getName(), is("SG EFF. TURBOL ZS"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(111.22)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-09-28T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(83)));
    }

    @Test
    public void testZinsBelastung() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexZinsBelastung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.20)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testWertpapierVerkaufSteuererstattung() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkaufSteuererstattung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000SKWM021"));
        assertThat(security.getWkn(), is("SKWM02"));
        assertThat(security.getName(), is("SKW STAHL-METAL.HLDG.NA"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1253.15)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-09-08T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(11.85))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(460)));

        // check Steuererstattung
        Item itemTaxReturn = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(0);
        // Optional<Item> itemTaxReturn = results.stream().filter(i -> i
        // instanceof TransactionItem).findFirst();

        AccountTransaction entryTaxReturn = (AccountTransaction) itemTaxReturn.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(463.04))));
        assertThat(entryTaxReturn.getDateTime(), is(LocalDateTime.parse("2016-09-08T00:00")));
    }

    @Test
    public void testWertpapierKaufVerkaufSteuererstattung() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatexKaufVerkaufSteuererstattung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        Optional<Item> item;

        // check Käufe
        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000VN4LAU4"));
        assertThat(security.getWkn(), is("VN4LAU"));
        assertThat(security.getName(), is("VONT.FINL PR CALL17 DAX"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1036.40)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1750)));

        Item item2;

        item2 = results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1);
        security = ((SecurityItem) item2).getSecurity();
        assertThat(security.getIsin(), is("DE000VN547F8"));
        assertThat(security.getWkn(), is("VN547F"));
        assertThat(security.getName(), is("VONT.FINL PR PUT17 DAX"));

        item2 = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(1);
        assertThat(item2.getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) item2.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1003.90)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1250)));

        // check Verkäufe
        item2 = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(2);
        assertThat(item2.getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) item2.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1232.40)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1750)));

        item2 = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(3);
        assertThat(item2.getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) item2.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(844.10)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1250)));

        // check Steuererstattung
        Item itemTaxReturn = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(0);

        AccountTransaction entryTaxReturn = (AccountTransaction) itemTaxReturn.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.72))));
        assertThat(entryTaxReturn.getDateTime(), is(LocalDateTime.parse("2017-01-02T00:00")));
    }

    @Test
    public void testSteuertopfoptimierung() throws IOException
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexSteuertopfoptimierung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.94)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }
}
