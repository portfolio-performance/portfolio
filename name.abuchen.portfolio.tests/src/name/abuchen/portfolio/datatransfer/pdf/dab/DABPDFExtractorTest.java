package name.abuchen.portfolio.datatransfer.pdf.dab;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
    public void testWertpapierKauf01()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-01-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.91920)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(150.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(150.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-05-04T09:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.42270)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(55.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.95))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-04T09:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.9468)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(325.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(325.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

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
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-07-29T16:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4798.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4786.89))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.97))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

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
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-01T13:31")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0499)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.46))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.46))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

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
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-18T14:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1381.58))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1381.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.58))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getName(), is("ComSta.-MSCI Em.Mkts.TRN U.ETF Inhaber-Anteile I o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-23T12:13")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(125)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(6123.98))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(6123.98))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US72352L1061"));
        assertThat(security.getName(), is("Pinterest Inc. Registered Shares DL-,00001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-05T16:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1146.44))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1117.03))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.41))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE000A2LQLH9"));
        assertThat(security.getName(), is("4,75% Ranft Invest GmbH Inh.-Schv. v.2018(2030)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-06-18T08:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1029.55))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1022.56))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.00 + 2.24 + 0.75))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-18T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.95))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE000TT649A1"));
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG DIZ Siemens 140"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-23T12:58")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2003.55))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2003.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("IE00BLRPRL42"));
        assertThat(security.getName(), is("WisdomTree Multi Ass.Iss.PLC Gas 3x Sh. ETP Secs 12(12/62)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-02T09:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(111.11))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(110.11))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-12-23T10:25")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(43)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1994.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2056.73))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(45.88 + 2.52 + 4.12))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.09))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-08-24T16:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4465.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4476.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.28))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-08-24T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.09))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-08-24T16:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4447.21))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4478.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.24))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-08-24T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(98.08))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-07T10:36")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1900)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5414.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5415.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-07T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.46))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("SE0014855029"));
        assertThat(security.getName(), is("Implantica AG Reg.Sw.Dep.Rcpts (SDRs)/1 o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-09T13:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(220)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2577.52))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2577.52))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US09075V1026"));
        assertThat(security.getName(), is("BioNTech SE Nam.-Akt.(sp.ADRs)1/o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-06-16T10:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17509.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17510.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-16T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(178.59))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("NL0015436031"));
        assertThat(security.getName(), is("CureVac N.V. Namensaktien   o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-31T08:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26252.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26253.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax-refund transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-31T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(551.08))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("NL0015436031"));
        assertThat(security.getName(), is("CureVac N.V. Namensaktien   o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-26T09:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27911.45))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28293.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(335.29 + 18.44 + 26.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("NL0015436031"));
        assertThat(security.getName(), is("CureVac N.V. Namensaktien   o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy sell transactions
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-31T08:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26252.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26253.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax-refund for 1st transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-31T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(551.08))));

        // check 2nd buy sell transactions
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-26T09:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27911.45))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28293.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(335.29 + 18.44 + 26.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testMultipleWertpapierKaufVerkauf01()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "MultipleKaufVerkauf01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check securities
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US01609W1027"));
        assertThat(security.getName(), is("Alibaba Group Holding Ltd. Reg.Shs (sp.ADRs)/8 DL-,000025"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US34959E1091"));
        assertThat(security.getName(), is("Fortinet Inc. Registered Shares DL -,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst();
        assertThat(item.isPresent(), is(true));
        security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US09247X1019"));
        assertThat(security.getName(), is("Blackrock Inc. Reg. Shares Class A DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst();
        assertThat(item.isPresent(), is(true));
        security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US8447411088"));
        assertThat(security.getName(), is("Southwest Airlines Co. Registered Shares DL 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof SecurityItem).skip(4).findFirst();
        assertThat(item.isPresent(), is(true));
        security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US8036071004"));
        assertThat(security.getName(), is("Sarepta Therapeutics Inc. Registered Shares DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy sell transactions
        Optional<Item> t = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) t.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-30T09:59")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(679.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(679.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd buy sell transactions
        t = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(1).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) t.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-27T16:08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(619.92))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(619.92))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 3rd buy sell transactions
        t = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(2).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) t.get().getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-24T19:25")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(585.70))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(585.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 4th buy sell transactions
        t = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(3).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) t.get().getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-24T14:21")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(573.02))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(573.02))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 5th buy sell transactions
        t = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(4).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) t.get().getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-20T20:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(685.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(685.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 6th buy sell transactions
        t = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(5).findFirst();
        assertThat(t.isPresent(), is(true));
        assertThat(t.get().getSubject(), instanceOf(BuySellEntry.class));
        entry = (BuySellEntry) t.get().getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-20T15:32")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(565.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(564.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testDividende01()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0005660104"));
        assertThat(security.getName(), is("EUWAX AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-07-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(326.00))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(326.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US7427181091"));
        assertThat(security.getName(), is("Procter & Gamble Co., The Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(56.91))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(56.91))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Client client = new Client();

        Security security = new Security();
        security.setName("Procter & Gamble Co., The Registered Shares o.N.");
        security.setIsin("US7427181091");
        security.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(security);

        DABPDFExtractor extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(56.91))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(56.91))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("CH0012032048"));
        assertThat(security.getName(), is("Roche Holding AG Inhaber-Genußscheineo.N."));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2006-03-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(80)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(82.92))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(82.92))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende04()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US7033951036"));
        assertThat(security.getName(), is("Patterson Companies Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-07-29T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80.92))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80.92))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende05()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("ZAE000042164"));
        assertThat(security.getName(), is("MTN Group Ltd. Registered Shares RC -,0001"));
        assertThat(security.getCurrencyCode(), is("ZAR"));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1300)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(586.80))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(788.18))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(201.38))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende06()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0006483001"));
        assertThat(security.getName(), is("Linde AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-05-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(198.79))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(270.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(71.21))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende07WithAccountInUSD()
    {
        Client client = new Client();
        DABPDFExtractor extractor = new DABPDFExtractor(client);

        /***
         * Add Security exits in currency EUR
         */
        Security existingSecurity = new Security("Paychex Inc. Registered Shares DL -,01", CurrencyUnit.EUR);
        existingSecurity.setIsin("US7043261079");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.64))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.56))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

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
    public void testDividende08()
    {
        Client client = new Client();
        DABPDFExtractor extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US92826C8394"));
        assertThat(security.getName(), is("VISA Inc. Reg. Shares Class A DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.76))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende08WithSecurityinEUR()
    {
        Client client = new Client();
        DABPDFExtractor extractor = new DABPDFExtractor(client);

        /***
         * Add Security in currency EUR
         */
        Security existingSecurity = new Security("VISA Inc. Reg. Shares Class A DL -,0001", CurrencyUnit.EUR);
        existingSecurity.setIsin("US92826C8394");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.76))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende09()
    {
        Client client = new Client();
        DABPDFExtractor extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("US4404521001"));
        assertThat(security.getWkn(), is("850875"));
        assertThat(security.getName(), is("HORMEL FOODS CORP. Registered Shares DL 0,01465"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-02-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1500)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(209.38))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(281.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(71.87))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende09WithSecurityinEUR()
    {
        Client client = new Client();
        DABPDFExtractor extractor = new DABPDFExtractor(client);

        /***
         * Add Security in currency EUR
         */
        Security existingSecurity = new Security("HORMEL FOODS CORP. Registered Shares DL 0,01465", CurrencyUnit.EUR);
        existingSecurity.setIsin("US4404521001");
        existingSecurity.setWkn("850875");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-02-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1500)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(209.38))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(281.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(71.87))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende10()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("IE00BJ5JP329"));
        assertThat(security.getName(), is("iShs V-MSCI W.C.St.Sec.U.ETF Reg. Shs USD Dis. oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(315)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(20.49))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(25.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize((3.68 * 1.1914) + (0.20 * 1.1914)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende10WithAccountAndSecurityInUSD()
    {
        Security security = new Security("iShs V-MSCI W.C.St.Sec.U.ETF Reg. Shs USD Dis. oN", CurrencyUnit.USD);
        security.setIsin("IE00BJ5JP329");

        Client client = new Client();
        client.addSecurity(security);

        DABPDFExtractor extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(315)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(20.49))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(25.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize((3.68 * 1.1914) + (0.20 * 1.1914)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.USD);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende11()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("IE00B3F81R35"));
        assertThat(security.getName(), is("iShsIII-Core EO Corp.Bd U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-01-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.4256)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.47))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.47))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0480132876"));
        assertThat(security.getName(), is("UBS-ETF - UBS-ETF MSCI Em.Mkts Inhaber-Anteile A o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividend transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(14.3755)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.06))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.81))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.75))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testEinbuchung01()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einbuchung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0006926504"));
        assertThat(security.getName(), is("Solutiance AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check delivery inbound (Einlieferung) transaction
        Optional<Item> item;
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        PortfolioTransaction entry = (PortfolioTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2021-03-11T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.50))));
        assertThat(entry.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50 + 27.00))));
    }

    @Test
    public void testEinbuchung02()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einbuchung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU1291106356"));
        assertThat(security.getName(), is("BNP P.Easy-MSCI Pac.x.Jap.x.CW Nam.-Ant.UCITS ETF CAP o.N"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check delivery inbound (Einlieferung) transaction
        Optional<Item> item;
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        PortfolioTransaction entry = (PortfolioTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2018-12-04T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(1.5884)));

        assertThat(entry.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testKontoumsaetze01()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));

        // check account transactions
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15000.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-07-05T00:00")));

        // check 2nd account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-07-15T00:00")));

        // check 3rd account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(2).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-07-17T00:00")));

        // check 4th account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(3).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1900.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-07-17T00:00")));

        // check 5th account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(4).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-08-13T00:00")));

        // check 6th account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(5).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2019-09-13T00:00")));
    }

    @Test
    public void testKontoumsaetze02()
    {
        DABPDFExtractor extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));

        // check account transactions
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(400.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-01-13T00:00")));

        // check 2nd account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(400))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-02-18T00:00")));

        // check 3rd account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(2).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-03-15T00:00")));

        // check 4rd account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(3).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.FEES));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(53.02))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));

        // check 5rd account transactions
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(4).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.FEES));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.05))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
    }
}
