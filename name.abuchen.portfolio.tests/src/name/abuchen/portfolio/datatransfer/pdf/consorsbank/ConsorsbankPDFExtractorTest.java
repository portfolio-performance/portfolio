package name.abuchen.portfolio.datatransfer.pdf.consorsbank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.NonImportableItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExtractor;
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
import name.abuchen.portfolio.model.Account;

@SuppressWarnings("nls")
public class ConsorsbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMS.-MSCI WORL.T.U.ETF I"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-01-15T08:13:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(132.80212)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0L1NN5"));
        assertThat(security.getWkn(), is("A0L1NN"));
        assertThat(security.getName(), is("HELIAD EQ.PARTN.KGAA"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-09-21T12:45:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1387.85))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1370.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.95 + 3.00 + 5.00 + 4.95 + 1.95))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0J3UF6"));
        assertThat(security.getWkn(), is("A0J3UF"));
        assertThat(security.getName(), is("EARTH EXPLORAT.FDS UI EOR"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-10-16T15:24:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.95126)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.39))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.61))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B9KNR336"));
        assertThat(security.getWkn(), is("A1T8GC"));
        assertThat(security.getName(), is("SPDR S+P P.AS.DIV.ARI.ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-15T12:00:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(210)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9745.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9702.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50 + 11.54 + 24.26 + 4.95))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0000120628"));
        assertThat(security.getWkn(), is("855705"));
        assertThat(security.getName(), is("AXA S.A. INH.     EO 2,29"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-02-03T08:02:51")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.01514)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.56))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US92243N1037"));
        assertThat(security.getWkn(), is("A2P1CV"));
        assertThat(security.getName(), is("VECTO.ACQ.CORP. DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-11T15:52:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(525.92))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.97))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.00 + 19.95))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exists
        assertThat(security.getWkn(), is("851144"));
        assertThat(security.getName(), is("GENERAL ELECTRIC CO. SHARES DL -,06"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2001-09-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1928.74))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1917.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.53 + 5.11 + 4.60))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exists
        assertThat(security.getWkn(), is("625952"));
        assertThat(security.getName(), is("GARTMORE - CONT. EUROP. FUND ACTIONS NOM. A O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2005-10-17T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15.75243)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(75.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(73.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.83))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exists
        assertThat(security.getWkn(), is("625952"));
        assertThat(security.getName(), is("GARTMORE-CONT. EUROP. A"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2008-01-15T00:01:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11.87891)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(75.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(73.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.83))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("DE0002635307"));
        assertThat(security.getWkn(), is("263530"));
        assertThat(security.getName(), is("ISH.STOX.EUROPE 600 U.ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-07T13:57:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(197)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7659.37))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7655.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.95))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("US9168961038"));
        assertThat(security.getWkn(), is("A0JDRR"));
        assertThat(security.getName(), is("URANIUM ENERGY DL-,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-07T14:09:07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4974)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5441.15))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5414.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50 + 5.96 + 13.54 + 4.95))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("DE0008404005"));
        assertThat(security.getWkn(), is("840400"));
        assertThat(security.getName(), is("ALLIANZ SE NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-15T09:30:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.12804)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.63))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankKauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("PO6527623674"));
        assertThat(security.getWkn(), is("SP110Y"));
        assertThat(security.getName(), is("Sparplanname"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-06-15T11:07:25")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.43915)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierBezug01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankBezug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("DE000A0V9L94"));
        assertThat(security.getWkn(), is("A0V9L9"));
        assertThat(security.getName(), is("EYEMAXX R.EST.AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-06-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(66)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(399.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(396.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.96))));
    }

    @Test
    public void testWertpapierverkauf01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("US9286624021"));
        assertThat(security.getWkn(), is("A0DPR2"));
        assertThat(security.getName(), is("VOLKSWAGEN AG VZ ADR1/5"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-02-18T12:10:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(140)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5794.56))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6048.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(198.08 + 17.82 + 10.89))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.95 + 3.63 + 15.12 + 4.95))));
    }

    @Test
    public void testWertpapierverkauf02()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
         assertThat(security.getIsin(), is("DE000PX2LEH3"));
        assertThat(security.getWkn(), is("PX2LEH"));
        assertThat(security.getName(), is("BNP PAR.EHG MINIS XAU"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-03-02T14:51:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1386.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1388.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50 + 0.50 + 0.02 + 0.02))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierverkauf03()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exists
        assertThat(security.getWkn(), is("915771"));
        assertThat(security.getName(), is("CYBERIAN OUTPOST INC. SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2001-11-19T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(56.68))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.30 + 4.60))));
    }

    @Test
    public void testWertpapierverkauf04()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exists
        assertThat(security.getWkn(), is("974433"));
        assertThat(security.getName(), is("GARTMORE CSF-CONTIN.EUROPE FD REG.PTG RED.PREF.SHS DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2005-03-24T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(52.77908)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(691.31))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(691.31))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierverkauf05()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exists
        assertThat(security.getWkn(), is("A0MZBE"));
        assertThat(security.getName(), is("AHOLD, KON. EO-,30"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2008-05-16T16:04:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(334)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3290.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3303.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.26 + 4.95))));
    }

    @Test
    public void testWertpapierverkauf06()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000ENER6Y0"));
        assertThat(security.getWkn(), is("ENER6Y"));
        assertThat(security.getName(), is("SIEMENS ENERGY AG NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.46370)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierverkauf07()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVerkauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A288904"));
        assertThat(security.getWkn(), is("A28890"));
        assertThat(security.getName(), is("COMPUGROUP MED. NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-07T19:18:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1590.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1600.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.00 + 4.95))));
    }

    @Test
    public void testWertpapierEinloesung01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankEinloesung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000LS846N5"));
        assertThat(security.getWkn(), is("LS846N"));
        assertThat(security.getName(), is("Lang & Schwarz AG TurboC O.End Northern 78,25"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testConsorsbankVorabpauschale01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankVorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A12GJD2"));
        assertThat(security.getWkn(), is("A12GJD"));
        assertThat(security.getName(), is("L&G-L&G R.Gbl Robot.Autom.UETF Bearer Shares (Dt. Zert.) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(106)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.73))));
    }

    @Test
    public void testDividende01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMS.-MSCI WORL.T.U.ETF I Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-08T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1370)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(326.90))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(444.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(111.00 + 6.10))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMS.-MSCI WORL.T.U.ETF I Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-05-08T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(370)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1444.00))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1444.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("850866"));
        assertThat(security.getName(), is("DEERE & CO. Registered Shares DL 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(300)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(121.36))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(163.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.45 + 16.30 + 0.89))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende04()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("854242"));
        assertThat(security.getName(), is("WESTPAC BANKING CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("AUD"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-07-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.00020)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.46))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.62))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.16))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende05()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("885823"));
        assertThat(security.getName(), is("GILEAD SCIENCES INC. Registered Shares DL -,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-29T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0.27072)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02 + 0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende06()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("200417"));
        assertThat(security.getName(), is("ALTRIA GROUP INC. Registered Shares DL -,333"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(650)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(285.60))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(336.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.40))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende07()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("891106"));
        assertThat(security.getName(), is("ROCHE HOLDING AG Inh.-Genuß.(Sp.ADRs) 1/8/SF100"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-04-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(80)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.51))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(64.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(22.43 + 6.41 + 0.35))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.38))));
    }

    @Test
    public void testDividende08()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        // assertThat(security.getIsin(), is("")); <-- not exsits
        assertThat(security.getWkn(), is("A1409D"));
        assertThat(security.getName(), is("Welltower Inc. Registered Shares DL 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-05-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.51))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(38.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.74))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende09()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US6819191064"));
        assertThat(security.getWkn(), is("871706"));
        assertThat(security.getName(), is("OMNICOM GROUP INC. Registered Shares DL -,15"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-01-09T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.34))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.54))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.88 + 1.26 + 0.06))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende10()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU1033693638"));
        assertThat(security.getWkn(), is("ETF007"));
        assertThat(security.getName(), is("ComStage - MDAX UCITS ETF Inhaber-Anteile I o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-08-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(43)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.36))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.06))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.51 + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende11()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7960502018"));
        assertThat(security.getWkn(), is("881823"));
        assertThat(security.getName(), is("SAMSUNG ELECTRONICS CO. LTD. R.Shs(NV)Pf(GDR144A)/25 SW 100"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.80))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.54))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.52 + 2.06 + 0.10))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
    }

    @Test
    public void testDividende12()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M62Q58"));
        assertThat(security.getWkn(), is("A0HGV0"));
        assertThat(security.getName(), is("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.46))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize((2.23 * 1.12) + (0.12 * 1.12) + (0.80 * 1.12)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12WithUSDAccountAndExitsSecurityInUSD()
    {
        Security security = new Security("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN", CurrencyUnit.USD);
        security.setIsin("IE00B0M62Q58");
        security.setWkn("A0HGV0");

        Client client = new Client();
        client.addSecurity(security);

        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.46))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize((2.23 * 1.12) + (0.12 * 1.12) + (0.80 * 1.12)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12WithUSDAccountAndExitsSecurityInEUR()
    {
        Security security = new Security("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN", CurrencyUnit.EUR);
        security.setIsin("IE00B0M62Q58");
        security.setWkn("A0HGV0");

        Client client = new Client();
        client.addSecurity(security);

        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.46))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize((2.23 * 1.12) + (0.12 * 1.12) + (0.80 * 1.12)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.USD);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende13()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A2888C9"));
        assertThat(security.getWkn(), is("A2888C"));
        assertThat(security.getName(), is("Vonovia SE Dividende Cash"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(125)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(196.25))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(196.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende14()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5021751020"));
        assertThat(security.getWkn(), is("884625"));
        assertThat(security.getName(), is("LTC PROPERTIES INC. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(18)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.48))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.91))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.43))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende15()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankDividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        //assertThat(security.getIsin(), is("")); <- not exists
        assertThat(security.getWkn(), is("865985"));
        assertThat(security.getName(), is("Apple Inc. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.90))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26.72))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.01 + 2.67 + 0.14))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung01()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankNachtraeglicheVerlustverrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction t = (AccountTransaction) item.get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.61))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2017-07-10T00:00")));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung02()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankNachtraeglicheVerlustverrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction t = (AccountTransaction) item.get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.1))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2017-07-10T00:00")));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung03()
    {
        ConsorsbankPDFExtractor extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ConsorsbankNachtraeglicheVerlustverrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof NonImportableItem).findFirst();
        assertThat(item.isPresent(), is(true));
    }
}
