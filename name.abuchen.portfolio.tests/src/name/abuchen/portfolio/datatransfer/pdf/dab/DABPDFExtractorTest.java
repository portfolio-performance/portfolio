package name.abuchen.portfolio.datatransfer.pdf.dab;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExtractor;
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
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DABPDFExtractorTest
{

    private Security getSecurity(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        return ((SecurityItem) item.get()).getSecurity();
    }

    @Test
    public void testWertpapierKauf() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABKauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0360863863"));
        assertThat(security.getName(), is("ARERO - Der Weltfonds Inhaber-Anteile o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 150_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-01-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.91920)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABKauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0274208692"));
        assertThat(security.getName(), is("db x-tr.MSCI World Index ETF Inhaber-Anteile 1C o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 60_00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-05-04T09:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.42270)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 4_95L)));
    }

    @Test
    public void testWertpapierKauf3() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABKauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getName(), is("ComSta.-MSCI Em.Mkts.TRN U.ETF Inhaber-Anteile I o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 325_00)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-04T09:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.9468)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKauf4() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABKauf4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US8270481091"));
        assertThat(security.getName(), is("Silgan Holdings Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4798.86))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-07-29T16:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKauf5() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABKauf5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("IE00B3F81R35"));
        assertThat(security.getName(), is("iShsIII-Core EO Corp.Bd U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.46))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-01T13:31")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0499)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKauf6() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABKauf6.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0005810055"));
        assertThat(security.getName(), is("Deutsche Börse AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
    
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1381.58))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-18T14:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.58))));
    }

    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABVerkauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0392495700"));
        assertThat(security.getName(), is("ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1994_12)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-12-23T10:25")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(43)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 10_09)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, 45_88 + 2_52 + 4_12)));
    }

    @Test
    public void testWertpapierVerkauf2() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABVerkauf2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US8270481091"));
        assertThat(security.getName(), is("Silgan Holdings Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4465.12))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-08-24T16:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 90_09)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-08-24T00:00")));
    }

    @Test
    public void testWertpapierVerkauf3() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABVerkauf3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US8270481091"));
        assertThat(security.getName(), is("Silgan Holdings Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4447.21))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-08-24T16:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 98_08)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-08-24T00:00")));
    }

    @Test
    public void testWertpapierVerkauf4() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABVerkauf4.txt"), errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE000A0B65S3"));
        assertThat(security.getName(), is("PAION AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    
        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();
    
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
    
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5414.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-07T10:36")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1900)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    
        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
    
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 16_46)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-07T00:00")));
    }

    @Test
    public void testDividend() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividend.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0005660104"));
        assertThat(security.getName(), is("EUWAX AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 326_00)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-07-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testDividendForeignCurrency() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividendForeignCurrency.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US7427181091"));
        assertThat(security.getName(), is("Procter & Gamble Co., The Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, 56_91)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testDividendInForeignCurrencyButSecurityListedInEuro() throws IOException
    {
        Client client = new Client();

        Security security = new Security();
        security.setName("Procter & Gamble Co., The Registered Shares o.N.");
        security.setIsin("US7427181091");
        security.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(security);

        DABPDFExtractor extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividendForeignCurrency.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, 56_91)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testDividend3() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividend3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("CH0012032048"));
        assertThat(security.getName(), is("Roche Holding AG Inhaber-Genußscheineo.N."));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(82.92))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2006-03-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(80)));
    }

    @Test
    public void testDividend4() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividend4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US7033951036"));
        assertThat(security.getName(), is("Patterson Companies Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80.92))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-07-29T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
    }

    @Test
    public void testDividend5() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividend5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("ZAE000042164"));
        assertThat(security.getName(), is("MTN Group Ltd. Registered Shares RC -,0001"));
        assertThat(security.getCurrencyCode(), is("ZAR"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(586.80))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1300)));
    }

    @Test
    public void testDividend6() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividend6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0006483001"));
        assertThat(security.getName(), is("Linde AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(198.79))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-05-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
    }
    
    @Test
    public void testDividend7withUSDAccount() throws IOException
    {
        Client client = new Client();
        DABPDFExtractor extractor = new DABPDFExtractor(client);

        Security existingSecurity = new Security("Paychex Inc. Registered Shares DL -,01",
                        CurrencyUnit.EUR);
        existingSecurity.setIsin("US7043261079");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABDividend7.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.64))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.56))));
        
        Unit grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.20))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.25))));

        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.20))));
        
        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.USD);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

    }
    
    @Test
    public void testProceeds1() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABProceeds1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("IE00B3F81R35"));
        assertThat(security.getName(), is("iShsIII-Core EO Corp.Bd U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.47))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-01-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.4256)));
    }    

    @Test
    public void testProceeds2() throws IOException
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DABProceeds2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0480132876"));
        assertThat(security.getName(), is("UBS-ETF - UBS-ETF MSCI Em.Mkts Inhaber-Anteile A o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.06))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(14.3755)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.13))));
    }
}
