package name.abuchen.portfolio.datatransfer.pdf.fintechgroupbank;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
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
    public void testWertpapierKauf()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        checkSecurity(results, "DE0005194062", "519406", "BAYWA AG VINK.NA. O.N.", CurrencyUnit.EUR);
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        checkSecurity(results, "DE0008402215", "840221", "HANN.RUECK SE NA O.N.", CurrencyUnit.EUR, 1);
        assertSecondTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1));

        assertThirdTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem) //
                        .collect(Collectors.toList()).get(2));

        assertFourthTransaction(results.stream().filter(i -> i instanceof TransactionItem) //
                        .collect(Collectors.toList()).get(0));

    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5893_10L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-01-28T12:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(150)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(39.248))));
    }

    private void assertSecondTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5954_80L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-01-28T12:58")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
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
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-01-28T12:58")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
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
    public void testGutschriftsBelastungsanzeige()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatexGutschriftsBelastungsanzeige.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(20));

        // security
        checkSecurity(results, "DE0008474503", null, "DEKAFONDS CF", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        PortfolioTransaction entry = (PortfolioTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(5.50)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2015-02-16T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(0.0520)));
    }

    @Test
    public void testWertpapierKauf2()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "LU0392495023", "ETF114", "C.S.-MSCI PACIF.T.U.ETF I", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50.30)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-12-03T13:59")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
    }

    @Test
    public void testWertpapierKauf3()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00B2QWCY14", "A0Q1YY", "ISHSIII-S+P SM.CAP600 DLD", CurrencyUnit.EUR);

        PortfolioTransaction transaction = results.stream().filter(i -> i instanceof Extractor.BuySellEntryItem)
                        .map(i -> (BuySellEntry) ((Extractor.BuySellEntryItem) i).getSubject())
                        .map(BuySellEntry::getPortfolioTransaction).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1050)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-15T00:00")));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(19.334524)));
    }

    @Test
    public void testWertpapierKauf4()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "LU0392494992", "ETF113", "C.-MSCI NO.AM.TRN U.ETF I", CurrencyUnit.EUR);

        PortfolioTransaction transaction = results.stream().filter(i -> i instanceof Extractor.BuySellEntryItem)
                        .map(i -> (BuySellEntry) ((Extractor.BuySellEntryItem) i).getSubject())
                        .map(BuySellEntry::getPortfolioTransaction).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(transaction.getAmount(), is(Values.Amount.factorize(800)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-16T00:00")));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(13.268957)));
    }

    @Test
    public void testWertpapierKauf6()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00B2NPKV68", "A0NECU", "ISHSII-JPM DL EM BD DLDIS", CurrencyUnit.EUR);

        PortfolioTransaction transaction = results.stream().filter(i -> i instanceof Extractor.BuySellEntryItem)
                        .map(i -> (BuySellEntry) ((Extractor.BuySellEntryItem) i).getSubject())
                        .map(BuySellEntry::getPortfolioTransaction).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(transaction.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-15T00:00")));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.9))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(9.703363)));
    }

    @Test
    public void testWertpapierKauf7()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf7.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(20));

        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(10));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2017-11-01T14:41"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3008.9)))))));
    }

    @Test
    public void testWertpapierKauf8()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

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
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.16)))))));
    }

    @Test
    public void testWertpapierKauf9Sammelabrechnung()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf9Sammelabrechnung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        checkSecurity(results, "DE000A1MECS1", "A1MECS", "SOURCE PHY.MRKT.ETC00 XAU", CurrencyUnit.EUR);

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(2.72)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-01-09T15:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.025361)));
    }

    @Test
    public void testWertpapierKauf10()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "LU0274211480", "DBX1DA", "DB X-TRACK.DAX ETF(DR)1C", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1000.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7.979324)));
    }

    @Test
    public void testWertpapierKauf11()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00B6YX5D40", "A1JKS0", "SPDR S+P US DIV.ARIST.ETF", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1000.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-01-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.5))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22.973458)));
    }

    @Test
    public void testWertpapierKauf12()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "DE0001234567", "DS5WKN", "DEUT.BANK CALL20 BBB", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1023.90)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-08-13T16:20")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.9))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2000)));
    }

    @Test
    public void testWertpapierKauf13()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "LU0635178014", "ETF127", "COMS.-MSCI EM.M.T.U.ETF I", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(52.50)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.43414)));
    }

    @Test
    public void testWertpapierKauf14()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00BF2B0K52", "A2DTF1", "FRAN.LIB.Q EM EQ.UC.DLA", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1279.55)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-17T17:52")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.61))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
    }

    @Test
    public void testWertpapierKauf15_Fonds2019() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf15_Fonds2019.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00BKM4GZ66", "A111X9", "IS C.MSCI EMIMI U.ETF DLA", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(760.09)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-10T17:30")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.41))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(29)));
    }

    @Test
    public void testWertpapierKauf16_SammelabrechnungSparplan() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatexKauf16SammelabrechnungSparplan.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00B4L5Y983", null, "ISHSIII-CORE MSCI WLD DLA", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-21T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.877841)));
    }

    @Test
    public void testWertpapierKauf17() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKauf17.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "LU0378438732", "ETF001", "COMST.-DAX TR UCITS ETF I", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(250.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-08-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.460378)));
    }

    @Test
    public void testWertpapierKauf18() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDegiroSammelabrechnungKauf18.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check securities
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US17275R1023"));
        assertThat(security.getName(), is("CISCO SYSTEMS    DL-,001"));
        assertThat(security.getWkn(), is("878841"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US88339J1051"));
        assertThat(security.getName(), is("THE TRA.DESK A DL-,000001"));
        assertThat(security.getWkn(), is("A2ARCV"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy sell transactions
        Optional<Item> t = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) t.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-09T17:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(41)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1796.12))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.90 + 2.00))));

        // check 2nd buy sell transactions
        t = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(1).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) t.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-09T17:37")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1737.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.90 + 2.00))));
    }

    @Test
    public void testKontoauszug()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKontoauszug.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1100_00L)));

    }

    @Test
    public void testKontoauszug2()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKontoauszug2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 15000_00L)));
    }

    @Test
    public void testKontoauszug3()
    {
        Client client = new Client();
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(client);

        Security existingSecurity = new Security("Ohne Name AG", CurrencyUnit.EUR);
        existingSecurity.setIsin("ISIN12345678");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKontoauszug3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-11-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 4_56L)));

        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 7_89L)));
    }

    @Test
    public void testKontoauszug4()
    {
        Client client = new Client();
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(client);

        Security existingSecurity = new Security("Ohne Name AG", CurrencyUnit.EUR);
        existingSecurity.setIsin("ISIN12345678");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexKontoauszug4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(250))));

        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.26))));

        item = results.stream().filter(i -> i instanceof TransactionItem).skip(2).findFirst();
        assertThat(item.isPresent(), is(true));
        transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
    }

    @Test
    public void testErtragsgutschrift()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // security
        Security security = checkSecurity(results, "DE0008402215", "840221", "HANN.RUECK SE NA O.N.", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-05-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 795_15L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(360)));
    }

    @Test
    public void testErtragsgutschrift2()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "DE1234567890", "AB1234", "ISH.FOOBAR 12345666 x.EFT",
                        CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-01-15T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(55.55)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(99)));
    }

    @Test
    public void testErtragsgutschrift3()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "DE0006335003", "633500", "KRONES AG O.N.", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(17.13)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.12))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(15)));
    }

    @Test
    public void testErtragsgutschrift4_Fonds2019() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift4_Fonds2019.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "IE00B945VV12", "A1T8FS", "VANG.FTSE DEV.EU.UETF EOD",
                        CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-04-10T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(36.07)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(197)));
    }

    @Test
    public void testErtragsgutschrift5()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift5.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "US5949181045", "870747", "MICROSOFT    DL-,00000625",
                        CurrencyUnit.USD);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.98)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.86 + (1.15 / 1.1137)))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(15)));
    }

    @Test
    public void testErtragsgutschrift6()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift6.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "IE00B8GKDB10", "A1T8FV", "VA.FTSE A.W.H.D.Y.UETFDLD",
                        CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(11.42)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(31.89)));
    }

    @Test
    public void testErtragsgutschrift7()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift7.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "US46284V1017", "A14MS9", "IRON MOUNTAIN (NEW)DL-,01",
                        CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(75.30)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.57))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(197)));
    }

    @Test
    public void testErtragsgutschrift7withSecurity()
    {
        Client client = new Client();
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(client);

        Security existingSecurity = new Security("IRON MOUNTAIN (NEW)DL-,01", CurrencyUnit.USD);
        existingSecurity.setIsin("US46284V1017");
        existingSecurity.setWkn("A14MS9");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexErtragsgutschrift7.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(75.30)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.57))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(197)));
    }

    @Test
    public void testDividendeAusland()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "US8552441094", "884437", "STARBUCKS CORP.", CurrencyUnit.USD);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(14.45)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.78))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(105)));
    }

    @Test
    public void testDividendeAusland2()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "GB00B03MLX29", "A0D94M", "ROYAL DUTCH SHELL A EO-07",
                        CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(60.97)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.76))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(180)));
    }

    @Test
    public void testDividendeAusland3()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "US5949181045", "870747", "MICROSOFT    DL-,00000625",
                        CurrencyUnit.USD);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.98)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.89))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(15)));
    }

    @Test
    public void testDividendeAusland4()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "US5949181045", "870747", "MICROSOFT    DL-,00000625",
                        CurrencyUnit.USD);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(16.73)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.74))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testDividendeAusland4WithSecurity()
    {
        Client client = new Client();
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(client);

        Security existingSecurity = new Security("MICROSOFT    DL-,00000625", CurrencyUnit.EUR);
        existingSecurity.setIsin("US5949181045");
        existingSecurity.setWkn("870747");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(existingSecurity));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(16.73)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.74))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testDividendeAusland5WithSecurityAndAccountUSD()
    {
        Client client = new Client();
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(client);

        Security existingSecurity = new Security("MICROSOFT    DL-,00000625", CurrencyUnit.USD);
        existingSecurity.setIsin("US5949181045");
        existingSecurity.setWkn("870747");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FinTechGroupBankDividendeAusland5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.USD);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(existingSecurity));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(18.99)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.51))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testZinsgutschriftInland()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexZinsgutschriftInland.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Security security = checkSecurity(results, "DE1234567890", "AB1234", "ISH.FOOBAR 12345666 x.EFT",
                        CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-04-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(73.75)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1000)));
    }

    @Test
    public void testWertpapierVerkauf()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "DE000US9RGR9", "US9RGR", "UBS AG LONDON 14/16 RWE", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(16508.16)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-22T16:14")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testWertpapierVerkauf2()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "LU0323578657", "A0M430", "FLOSSB.V.STORCH-MUL.OPP.R", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(10.12)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-12-22T19:51")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
    }

    @Test
    public void testWertpapierVerkauf3()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "DE0009807008", "980700", "GRUNDBESITZ EUROPA RC", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(4840.15)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-07-04T14:23")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(121)));
    }

    @Test
    public void testWertpapierVerkauf4()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00B53HP851", "A0YEDM", "ISHSVII-FTSE 100 LS ACC", CurrencyUnit.EUR);

        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(1));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2018-01-09T15:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95)))))));

        assertThat(tx.get(0).getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0))));
    }

    @Test
    public void testWertpapierVerkauf5()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00BKWQ0D84", "A1191N", "SSGA S.E.E.II-M.EU.CON.S.", CurrencyUnit.EUR);

        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(1));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2019-02-06T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.48)))))));
        assertThat(tx.get(0).getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(5.9))));
    }

    @Test
    public void testWertpapierVerkauf6()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "CA03765K1049", "A12HM0", "APHRIA INC.", CurrencyUnit.EUR);

        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(1));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2019-04-09T16:52"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4416.52)))))));
        assertThat(tx.get(0).getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(8.41))));
    }

    @Test
    public void testWertpapierVerkauf7()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf7.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "IE00B41RYL63", "A1JJTM", "SPDR BL.BA.EO AG.BD U.ETF", CurrencyUnit.EUR);

        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(1));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2019-06-20T09:08"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9529.81)))))));
        assertThat(tx.get(0).getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(8.41))));
    }

    @Test
    public void testWertpapierVerkauf8()
    {
        Client client = new Client();
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(client);
        List<Exception> errors = new ArrayList<>();

        Security existingSecurity = new Security("INTEL CORP.       DL-,001", CurrencyUnit.EUR);
        existingSecurity.setIsin("US4581401001");
        existingSecurity.setWkn("855681");
        client.addSecurity(existingSecurity);

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf8.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        List<PortfolioTransaction> tx = results.stream() //
                        .filter(i -> i instanceof BuySellEntryItem)
                        .map(i -> ((BuySellEntry) i.getSubject()).getPortfolioTransaction())
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(2));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2020-11-30T18:09"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4773.36)))))));
        assertThat(tx.get(0).getUnitSum(Unit.Type.FEE), is(Money.of("USD", Values.Amount.factorize(7.14))));
        assertThat(tx.get(0).getUnitSum(Unit.Type.TAX), is(Money.of("USD", Values.Amount.factorize(0.00))));

        assertThat(tx, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2020-11-30T18:09"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(955.98)))))));
        assertThat(tx.get(1).getUnitSum(Unit.Type.FEE), is(Money.of("USD", Values.Amount.factorize(0.02))));
        assertThat(tx.get(1).getUnitSum(Unit.Type.TAX), is(Money.of("USD", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf9()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf9.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3WJKG14"));
        assertThat(security.getWkn(), is("A142N1"));
        assertThat(security.getName(), is("ISHSV-S+500INF.T.SECT.DLA"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(5681.04)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-19T09:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(425)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), 
                        is(Money.of("EUR", Values.Amount.factorize(9.90 + 2.51))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), 
                        is(Money.of("EUR", Values.Amount.factorize(305.85))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0009848119"));
        assertThat(security.getWkn(), is("984811"));
        assertThat(security.getName(), is("DWS TOP DIVIDENDE LD"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(10746.30)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-10T17:55")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(91)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), 
                        is(Money.of("EUR", Values.Amount.factorize(9.90))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), 
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(18.87))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-10T17:55")));
    }

    @Test
    public void testWertpapierÜbertrag1() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepoteingang1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        checkSecurity(results, "DE000US9RGR9", null, "UBS AG LONDON 14/16 RWE", CurrencyUnit.EUR);

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        PortfolioTransaction entry = (PortfolioTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(7517.50)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2015-11-24T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testWertpapierÜbertrag2() // NOSONAR
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepoteingang2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        checkSecurity(results, "DE000US9RGR9", null, "UBS AG LONDON 14/16 RWE", CurrencyUnit.EUR);

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        PortfolioTransaction entry = (PortfolioTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(7517.50)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2015-11-24T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(250)));
    }

    @Test
    public void testWertpapierAusgang()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepotausgang.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        checkSecurity(results, "DE000CM31SV9", null, "COMMERZBANK INLINE09EO/SF", CurrencyUnit.EUR);

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(2867.88)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2009-12-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(325)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 382_12L)));
    }

    @Test
    public void testWertpapierAusgang2()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexDepotausgang2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        checkSecurity(results, "DE000CK1Q3N7", null, "COMMERZBANK INLINE11EO/SF", CurrencyUnit.EUR);

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(0.20)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2011-07-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
    }

    @Test
    public void testWertpapierBestandsausbuchung()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexBestandsausbuchung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        checkSecurity(results, "DE000CB81KN1", null, "COMMERZBANK PUT10 EOLS", CurrencyUnit.EUR);

        assertFirstTransactionBestandsausbuchung(
                        results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        checkSecurity(results, "DE000CM3C8A3", null, "COMMERZBANK CALL10 EO/DL", CurrencyUnit.EUR, 1);

        assertSecondTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(1));
        checkSecurity(results, "DE000CM3C896", null, "COMMERZBANK CALL10 EO/DL", CurrencyUnit.EUR, 2);
        assertThirdTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(2));

    }

    private void assertFirstTransactionBestandsausbuchung(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-03-16T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2000)));
    }

    private void assertSecondTransactionBestandsausbuchung(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-03-16T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1250)));
    }

    private void assertThirdTransactionBestandsausbuchung(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-03-16T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(750)));
    }

    @Test
    public void testWertpapierBestandsausbuchungNeuesFormat()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexBestandsausbuchung2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        checkSecurity(results, "DE000SG0WRD3", "SG0WRD", "SG EFF. TURBOL ZS", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(111.22)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-09-28T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(83)));
    }

    @Test
    public void testZinsBelastung()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexZinsBelastung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.20)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testWertpapierVerkaufSteuererstattung()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "FlatexVerkaufSteuererstattung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // security
        checkSecurity(results, "DE000SKWM021", "SKWM02", "SKW STAHL-METAL.HLDG.NA", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1253.15)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-09-08T08:32")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(11.85))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(460)));

        // check Steuererstattung
        Item itemTaxReturn = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(0);

        AccountTransaction entryTaxReturn = (AccountTransaction) itemTaxReturn.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(463.04))));
        assertThat(entryTaxReturn.getDateTime(), is(LocalDateTime.parse("2016-09-08T00:00")));
    }

    @Test
    public void testWertpapierKaufVerkaufSteuererstattung()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatexKaufVerkaufSteuererstattung.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        Optional<Item> item;

        // check Käufe
        // security
        checkSecurity(results, "DE000VN4LAU4", "VN4LAU", "VONT.FINL PR CALL17 DAX", CurrencyUnit.EUR);

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1036.40)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T13:15")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1750)));

        // Security 2
        checkSecurity(results, "DE000VN547F8", "VN547F", "VONT.FINL PR PUT17 DAX", CurrencyUnit.EUR, 1);

        Item item2 = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(1);
        entry = (BuySellEntry) item2.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1003.90)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T14:55")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1250)));

        // check Verkäufe
        item2 = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(2);
        entry = (BuySellEntry) item2.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1232.40)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T16:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(3.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1750)));

        item2 = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(3);
        entry = (BuySellEntry) item2.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(844.10)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T16:07")));
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
    public void testSteuertopfoptimierung()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexSteuertopfoptimierung.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.94)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testVorabpauschale01()
    {
        FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatexVorabpauschale01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // security
        Security security = checkSecurity(results, "IE00BKM4GZ66", "A111X9", "ISHS MSCI EM USD-AC", CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.69))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(476)));
    }

    private Security checkSecurity(List<Item> results, String isin, String wkn, String name, String currencyUnit)
    {
        return checkSecurity(results, isin, wkn, name, currencyUnit, 0);
    }

    private Security checkSecurity(List<Item> results, String isin, String wkn, String name, String currencyUnit,
                    int index)
    {
        Security security;
        if (index == 0)
        {
            Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
            assertThat(item.isPresent(), is(true));
            security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        }
        else
        {
            Item item = results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(index);
            security = ((SecurityItem) item).getSecurity();
        }

        assertThat(security.getIsin(), is(isin));
        assertThat(security.getWkn(), is(wkn));
        assertThat(security.getName(), is(name));
        assertThat(security.getCurrencyCode(), is(currencyUnit));
        return security;
    }
}
