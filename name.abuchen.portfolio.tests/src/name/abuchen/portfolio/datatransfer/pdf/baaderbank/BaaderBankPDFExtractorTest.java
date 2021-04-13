package name.abuchen.portfolio.datatransfer.pdf.baaderbank;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
        assertThat(security.getName(), is("iShs DL Corp Bond UCITS ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(208.95)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-20T15:31")));
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
        assertThat(security.getName(), is("HSBC S&P 500 UCITS ETF Bearer Shares (Dt. Zert.) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1551.00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-20T14:59")));
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
        assertThat(security.getName(), is("Xtr.FTSE Devel.Europ.R.Estate Inhaber-Anteile 1C o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(87.3)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-22T16:14")));
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
        assertThat(security.getName(), is("Xtr.II USD Emerging Markets Bd Inhaber-Anteile 2D USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(28.18)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-12-10T12:58")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
    }

    @Test
    public void testWertpapierKauf5()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierKauf5.txt"),
                        errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        Optional<Item> item;
    
        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
    
        // assert security
        assertThat(security.getIsin(), is("IE00B3XXRP09"));
        assertThat(security.getWkn(), is("A1JX53"));
        assertThat(security.getName(), is("Vanguard S&P 500 UCITS ETF Registered Shares USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
    
        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1030.25)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-12-21T12:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(24)));
    }

    @Test
    public void testWertpapierKauf6()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierKauf6.txt"),
                        errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        Optional<Item> item;
    
        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
    
        // assert security
        assertThat(security.getIsin(), is("ES0173093024"));
        assertThat(security.getWkn(), is("A2ANA3"));
        assertThat(security.getName(), is("Red Electrica Corporacion S.A. Acciones Port. EO -,50"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
    
        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(985.12)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-24T14:49")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(70)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(1.97))));
    }

    @Test
    public void testGratisbrokerWertpapierKauf01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("JP3436100006"));
        assertThat(security.getWkn(), is("891624"));
        assertThat(security.getName(), is("SoftBank Group Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(519.03)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-12T18:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(13)));
    }

    @Test
    public void testGratisbrokerWertpapierKauf02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("US5021751020"));
        assertThat(security.getWkn(), is("884625"));
        assertThat(security.getName(), is("LTC Properties Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(513.6)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-29T15:49")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(12)));
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
        assertThat(security.getName(), is("UBS-ETF-UBS-ETF MSCI Pa.(ExJ.) Inhaber-Anteile A o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(75.92)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-05-10T14:10")));
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
        assertThat(security.getName(), is("iShs DL Corp Bond UCITS ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1161.60)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-19T11:13")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));
    }

    @Test
    public void testWertpapierVerkauf3()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankWertpapierVerkauf3.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
    
        Optional<Item> item;
    
        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
    
        // assert security
        assertThat(security.getIsin(), is("LU0446734526"));
        assertThat(security.getWkn(), is("A0X97T"));
        assertThat(security.getName(), is("UBS-ETF-UBS-ETF MSCI PXJ U.ETF Inhaber-Anteile (USD) A-dis oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
    
        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(226.68)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-12-21T13:06")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6)));
    }

    @Test
    public void testGratisbrokerWertpapierVerkauf01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item;

        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getIsin(), is("DE000A0TGJ55"));
        assertThat(security.getWkn(), is("A0TGJ5"));
        assertThat(security.getName(), is("VARTA AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(809)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-13T14:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
    }

    @Test
    public void testGratisbrokerWertpapierVerkauf02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerVerkauf02.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        Optional<Item> item;
    
        // get security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
    
        // assert security
        assertThat(security.getIsin(), is("US30212P3038"));
        assertThat(security.getWkn(), is("A1JRLJ"));
        assertThat(security.getName(), is("Expedia Group Inc. Registered Shares DL-,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // get transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
    
        // assert transaction
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1454.56)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-30T16:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(127.73 + 11.49 + 7.02))));
    }

    @Test
    public void testGratisbrokerAusschüttung01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerAusschuettung01.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        AccountTransaction t = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()).findAny()
                        .orElseThrow(IllegalArgumentException::new);
        
        assertThat(t.getSecurity().getName(), is("Deutsche Konsum REIT-AG"));
        assertThat(t.getSecurity().getIsin(), is("DE000A14KRD3"));
        assertThat(t.getSecurity().getWkn(), is("A14KRD"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.EUR));
        
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-03-16T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(35)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.00))));
        assertThat(t.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(t.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung1()
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
    public void testSteuerausgleichsrechnung2()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankSteuerausgleichsrechnung2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // get transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(29.06)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    @Test
    public void testMonatlicherKontoauszug1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankMonatlicherKontoauszug1.txt"), errors);

        assertThat(results.size(), is(27));
        assertThat(errors, empty());
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
    public void testScalableTageskontoauszug1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankScalableTageskontoauszug01.txt"), errors);
    
        assertThat(results.size(), is(1));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
    
        // get transaction
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();
    
        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20.00)));
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
        assertThat(security.getName(), is("iShsII-J.P.M.$ EM Bond U.ETF Registered Shares o.N."));
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
        assertThat(security.getName(), is("iShares Core DAX UCITS ETF DE Inhaber-Anteile"));
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

    @Test
    public void testDividende01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getName(), is("Mastercard Inc."));
        assertThat(security.getIsin(), is("US57636Q1040"));
        assertThat(security.getWkn(), is("A0F602"));

        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.84))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27))));
    }

    @Test
    public void testDividende02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getName(), is("Main Street Capital Corp."));
        assertThat(security.getIsin(), is("US56035L1044"));
        assertThat(security.getWkn(), is("A0X8Y3"));

        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.23))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.13))));
    }

    @Test
    public void testDividende03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankGratisbrokerDividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();

        // assert security
        assertThat(security.getName(), is("Deutsche Telekom AG"));
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getWkn(), is("555750"));

        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.20))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(77)));
    }

    @Test
    public void testScalableCapitalPeriodenauszugKauf1()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankScalablePeriodenauszug01.txt"), errors);

        assertThat(results.size(), is(5));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Item item;
        Iterator<Extractor.Item> iter;

        // get securities
        iter = results.stream().filter(i -> i instanceof SecurityItem).iterator();
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B5BMR087"));
            assertThat(security.getName(), is("ISHSVII-CORE S+P500 DLACC"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B52MJY50"));
            assertThat(security.getName(), is("ISHSVII-C.MSCI P.XJPDLACC"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        // get transactions
        iter = results.stream().filter(i -> i instanceof BuySellEntryItem).iterator();
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(418.00)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-04-13T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(238.20)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-04-13T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        }

        // check deposit (Lastschrift) transaction
        Optional<Item> it = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        // get transaction
        AccountTransaction transaction = (AccountTransaction) it.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10000.00)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));

    }

    @Test
    public void testScalableCapitalPeriodenauszugKauf2()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankScalablePeriodenauszug02.txt"), errors);

        assertThat(results.size(), is(13));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Item item;
        Iterator<Extractor.Item> iter;

        // get securities
        iter = results.stream().filter(i -> i instanceof SecurityItem).iterator();
        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B1FZS798"));
            assertThat(security.getName(), is("ISHSII-DLT.BD7-10YR DLDIS"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("LU0908500753"));
            assertThat(security.getName(), is("LY.I.-L.CO.ST.EO 600(DR)A"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B4WXJJ64"));
            assertThat(security.getName(), is("ISHSIII-C.EO GOV. B.EODIS"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }

        if (iter.hasNext())
        {
            item = iter.next();
            Security security = item.getSecurity();

            // assert security
            assertThat(security.getIsin(), is("IE00B3F81R35"));
            assertThat(security.getName(), is("ISHSIII-C.EO CORP.B.EODIS"));
            assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        }
       
        // get transactions
        iter = results.stream().filter(i -> i instanceof BuySellEntryItem).iterator();
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(642.40)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-25T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(304.92)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-25T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(488.72)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-25T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        }
        if (iter.hasNext())
        {
            item = iter.next();
            BuySellEntry entry = (BuySellEntry) item.getSubject();

            // assert transaction
            assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
            assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
            assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(515.61)));
            assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-25T00:00")));
            assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        }
        

        // check transaction items - deposit (Lastschrift), fees, dividends, removal transactions
        Optional<Item> it = results.stream().filter(i -> i instanceof TransactionItem).findFirst();

        // get transaction
        AccountTransaction transaction = (AccountTransaction) it.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-08-22T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2000.0)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));

    }

    @Test
    public void testVorabpauschale01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<>();
    
        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "BaaderBankScalableVorabpauschale01.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
    
        // get security
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
    
        // assert security
        assertThat(security.getName(), is("SPDR S+P US Con.Sta.Sel.S.UETF"));
        assertThat(security.getIsin(), is("IE00BWBXM385"));
        assertThat(security.getWkn(), is("A14QBZ"));
    
        // get transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();
    
        // assert transaction
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(112)));
    }
}
