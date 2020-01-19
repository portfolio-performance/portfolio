package name.abuchen.portfolio.datatransfer.pdf.baaderbank;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
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
public class BaaderBankPDFExtractorTest
{

    @Test
    public void testWertpapierKauf1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierKauf1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("IE0032895942"));
        assertThat(security.getWkn(), is("911950"));
        assertThat(security.getName(), is("iShs DL Corp Bond UCITS ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(208.95)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.21))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

    }

    @Test
    public void testWertpapierKauf2()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierKauf2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("DE000A1C22M3"));
        assertThat(security.getWkn(), is("A1C22M"));
        assertThat(security.getName(), is("HSBC S&P 500 UCITS ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1551.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.55))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(70)));
    }

    @Test
    public void testWertpapierKauf3()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierKauf3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("LU0489337690"));
        assertThat(security.getWkn(), is("DBX0F1"));
        assertThat(security.getName(), is("Xtr.FTSE Devel.Europ.R.Estate"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(87.3)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
    }

    @Test
    public void testWertpapierKauf4()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierKauf4.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("LU0677077884"));
        assertThat(security.getWkn(), is("DBX0MB"));
        assertThat(security.getName(), is("Xtr.II USD Emerging Markets Bd"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(28.18)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-12-10T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
    }
    
    @Test
    public void testWertpapierVerkauf1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierVerkauf1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("LU0446734526"));
        assertThat(security.getWkn(), is("A0X97T"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(75.92)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-05-10T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
    }

    @Test
    public void testWertpapierVerkauf2()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierVerkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("IE0032895942"));
        assertThat(security.getWkn(), is("911950"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1161.60)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-19T00:00")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));
    }

    @Test
    public void testSteuerausgleichsrechnung()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankSteuerausgleichsrechnung1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // get transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-22T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(9.01)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    @Test
    public void testMonatlicherKontoauszug1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankMonatlicherKontoauszug1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        // get transaction
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-04T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    @Test
    public void testRechnung1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankRechnung1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        // get transaction
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-02T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(6.48)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    @Test
    public void testFondsausschuettung1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankFondsausschuettung1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("IE00B2NPKV68"));
        assertThat(security.getWkn(), is("A0NECU"));

        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.08))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.36))));
    }

    @Test
    public void testErtragsthesaurierung1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankErtragsthesaurierung1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("DE0005933931"));
        assertThat(security.getWkn(), is("593393"));

        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.24))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(11)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.63))));
    }
}
