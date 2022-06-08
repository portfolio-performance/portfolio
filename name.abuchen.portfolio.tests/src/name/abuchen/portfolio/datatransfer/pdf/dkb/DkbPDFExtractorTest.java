package name.abuchen.portfolio.datatransfer.pdf.dkb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
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
public class DkbPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1HLTD2"));
        assertThat(security.getWkn(), is("A1HLTD"));
        assertThat(security.getName(), is("8,75 % METALCORP GROUP B.V. EO-ANLEIHE 2013(18)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-11-25T11:02:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Limit 97,50 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2030.66))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2023.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.50))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("BMG7945E1057"));
        assertThat(security.getWkn(), is("A0ERZ0"));
        assertThat(security.getName(), is("SEADRILL LTD. REGISTERED SHARES DL 2,-"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-25T09:33:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Limit 1,75 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1760.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1750.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00 + 0.71 + 0.20))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(29.2893)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1410.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1400.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.521)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(130.41))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(128.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.50 + 0.71 + 0.20))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25.6)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1201.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.50))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-07T11:11:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1144.20))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1142.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00 + 0.60))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0000121014"));
        assertThat(security.getWkn(), is("853292"));
        assertThat(security.getName(), is("LVMH MOET HENN. L. VUITTON SE ACTIONS PORT. (C.R.) EO 0,3"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-09-01T08:00:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1918.31))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1902.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000A0U9J2"));
        assertThat(security.getWkn(), is("A1MLSS"));
        assertThat(security.getName(), is("8,5 % SCHOLZ HOLDING INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-10-27T09:05:33")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(60)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Limit 85,00 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4937.19))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5428.36))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(420.24 + 23.11 + 37.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(60)));
        assertThat(transaction.getSource(), is("Verkauf01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(56.57))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(56.57))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005140008"));
        assertThat(security.getWkn(), is("514000"));
        assertThat(security.getName(), is("DEUTSCHE BANK AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-02-10T10:58:02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Limit 15,05 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3010.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A2YN900"));
        assertThat(security.getWkn(), is("A2YN90"));
        assertThat(security.getName(), is("TEAMVIEWER AG INHABER-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-06T14:32:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4123.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4245.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(109.37 + 6.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US00165C1045"));
        assertThat(security.getWkn(), is("A1W90H"));
        assertThat(security.getName(), is("AMC ENTERTAINMENT HOLDINGS INC REG. SHARES CLASS A DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-22T20:56:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Limit 5,44 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(99.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(109.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));
        assertThat(transaction.getSource(), is("Verkauf04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.08))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000LED02V0"));
        assertThat(security.getWkn(), is("LED02V"));
        assertThat(security.getName(), is("OSRAM LICHT AG Z.VERKAUF EING.NAMENS-AKTIEN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("Verkauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(164.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(164.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CA3012831077"));
        assertThat(security.getWkn(), is("A1C30Q"));
        assertThat(security.getName(), is("EXCHANGE INCOME CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-06T14:49:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(75)));
        assertThat(entry.getSource(), is("Verkauf06.txt"));
        assertThat(entry.getNote(), is("Limit 27,80 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2051.02))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2085.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.63 + 1.07))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00 + 0.06 + 1.55 + 1.67))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0010524777"));
        assertThat(security.getWkn(), is("LYX0CB"));
        assertThat(security.getName(), is("LYXOR NEW ENERGY UCITS ETF ACTIONS AU PORT.DIST O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-10-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.982)));
        assertThat(entry.getSource(), is("Verkauf07.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("XS0149161217"));
        assertThat(security.getWkn(), is("858865"));
        assertThat(security.getName(), is("2,309 % RBS CAPITAL TRUST A EO-FLR TR.PREF.SEC.02(12/UND.)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-07-31T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getSource(), is("Verkauf08.txt"));
        assertThat(entry.getNote(), is("Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2974.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.28 + 1.33))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf09.txt"));
        assertThat(entry.getNote(), is("Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1908.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80.01 + 4.40 + 7.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf10.txt"));
        assertThat(entry.getNote(), is("Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1908.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80.01 + 4.40 + 7.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf11.txt"));
        assertThat(entry.getNote(), is("Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende01()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertThat(security.getName(), is("PCC SE INH.-TEILSCHULDV. V.13(13/17)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(144.52))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(181.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.09 + 1.76 + 2.88))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007100000"));
        assertThat(security.getWkn(), is("710000"));
        assertThat(security.getName(), is("DAIMLER AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-04-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(97.50))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(97.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3XNN521"));
        assertThat(security.getWkn(), is("A1JJAG"));
        assertThat(security.getName(), is("DIM.FDS-GLOBAL SMALL COMPANIES REGISTERED SHARES EUR DIS.O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(216)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(45.61))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.18 + 0.61 + 0.89))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende04()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0010570767"));
        assertThat(security.getWkn(), is("870503"));
        assertThat(security.getName(), is("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.72))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(42.65))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.93))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CHF", Values.Amount.factorize(51.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        Security security = new Security("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10", CurrencyUnit.EUR);
        security.setIsin("CH0010570767");
        security.setWkn("870503");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.72))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(42.65))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.93))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende05()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertThat(security.getWkn(), is("A0RPWH"));
        assertThat(security.getName(), is("iShares Core MSCI World UCITS ETF USD REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-06-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.03 + 0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende06()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3VVMM84"));
        assertThat(security.getWkn(), is("A1JX51"));
        assertThat(security.getName(), is("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16.3517)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.45))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.52 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.46))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        Security security = new Security("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON", CurrencyUnit.EUR);
        security.setIsin("IE00B3VVMM84");
        security.setWkn("A1JX51");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16.3517)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.45))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.52 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende07()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertThat(security.getName(), is("PCC SE INH.-TEILSCHULDV. V.13(13/17)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-01-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(173.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(181.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.81 + 0.42))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende08()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005552004"));
        assertThat(security.getWkn(), is("555200"));
        assertThat(security.getName(), is("DEUTSCHE POST AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(92)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(105.80))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(105.80))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende09()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0LGQL5"));
        assertThat(security.getWkn(), is("A0LGQL"));
        assertThat(security.getName(), is("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(53)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.79))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.53))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.85 / 1.1426))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(8.53 * 1.1426))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        Security security = new Security("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N.", CurrencyUnit.EUR);
        security.setIsin("DE000A0LGQL5");
        security.setWkn("A0LGQL");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(53)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.79))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.53))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.85 / 1.1426))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende10()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B6YX5D40"));
        assertThat(security.getWkn(), is("A1JKS0"));
        assertThat(security.getName(), is("SPDR S&P US DIVID.ARISTOCR.ETF REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10.6841)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.09))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.46 / 1.1780))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.48 * 1.1780))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        Security security = new Security("SPDR S&P US DIVID.ARISTOCR.ETF REGISTERED SHARES O.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B6YX5D40");
        security.setWkn("A1JKS0");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10.6841)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.09))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.46 / 1.1780))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende11()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getWkn(), is("865985"));
        assertThat(security.getName(), is("APPLE INC. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(66)));
        assertThat(transaction.getNote(), is("Quartalsdividende"));
        assertThat(transaction.getSource(), is("Dividende11.txt"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.95))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.29))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(37.62))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        Security security = new Security("APPLE INC. REGISTERED SHARES O.N.", CurrencyUnit.EUR);
        security.setIsin("US0378331005");
        security.setWkn("865985");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(66)));
        assertThat(transaction.getNote(), is("Quartalsdividende"));
        assertThat(transaction.getSource(), is("Dividende11.txt"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.95))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.29))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende12()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("JP3414750004"));
        assertThat(security.getWkn(), is("471496"));
        assertThat(security.getName(), is("SEIKO EPSON CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("JPY"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(715)));
        assertThat(transaction.getNote(), is("Schlussdividende"));
        assertThat(transaction.getSource(), is("Dividende12.txt"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(133.58))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.03 + 18.01 + 0.99 + 0.56))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("JPY", Values.Amount.factorize(22165.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        Security security = new Security("SEIKO EPSON CORP. REGISTERED SHARES O.N.", CurrencyUnit.EUR);
        security.setIsin("JP3414750004");
        security.setWkn("471496");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(715)));
        assertThat(transaction.getNote(), is("Schlussdividende"));
        assertThat(transaction.getSource(), is("Dividende12.txt"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(133.58))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.03 + 18.01 + 0.99 + 0.56))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende13()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00B02J6398"));
        assertThat(security.getWkn(), is("A0DJ58"));
        assertThat(security.getName(), is("ADMIRAL GROUP PLC REGISTERED SHARES LS -,001"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-13T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(450)));
        assertThat(transaction.getNote(), is("Zwischendividende"));
        assertThat(transaction.getSource(), is("Dividende13.txt"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(227.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(309.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(77.29 + 4.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(229.50))));
    }

    @Test
    public void testDividende13WithSecurityInEUR()
    {
        Security security = new Security("ADMIRAL GROUP PLC REGISTERED SHARES LS -,001", CurrencyUnit.EUR);
        security.setIsin("GB00B02J6398");
        security.setWkn("A0DJ58");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-13T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(450)));
        assertThat(transaction.getNote(), is("Zwischendividende"));
        assertThat(transaction.getSource(), is("Dividende13.txt"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(227.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(309.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(77.29 + 4.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende14()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertThat(security.getName(), is("PCC SE INH.-TEILSCHULDV. V.13(13/17)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(935)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.24))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.24 + 2.24 + 0.12 + 0.12 + 0.20 + 0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(22.16))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        Security security = new Security("PCC SE INH.-TEILSCHULDV. V.13(13/17)", CurrencyUnit.EUR);
        security.setIsin("DE000A1R1AN5");
        security.setWkn("A1R1AN");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(935)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.24))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.24 + 2.24 + 0.12 + 0.12 + 0.20 + 0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende15()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0010570767"));
        assertThat(security.getWkn(), is("870503"));
        assertThat(security.getName(), is("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende15.txt"));
        assertThat(transaction.getNote(), is("Kapitalrückzahlung"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.12))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.12))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CHF", Values.Amount.factorize(42.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        Security security = new Security("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10", CurrencyUnit.EUR);
        security.setIsin("CH0010570767");
        security.setWkn("870503");

        Client client = new Client();
        client.addSecurity(security);

        DkbPDFExtractor extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende15.txt"));
        assertThat(transaction.getNote(), is("Kapitalrückzahlung"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.12))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.12))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende16()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0Z2ZZ5"));
        assertThat(security.getWkn(), is("A0Z2ZZ"));
        assertThat(security.getName(), is("FREENET AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-05-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
        assertThat(transaction.getSource(), is("Dividende16.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(290.00))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(290.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierAusgang03()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "WertpapierAusgang01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getWkn(), is("US9RGR"));
        assertThat(security.getName(), is("24,75 % UBS AG (LONDON BRANCH) EO-ANL. 14(16) RWE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transfer_out transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-11-30T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
        assertThat(entry.getSource(), is("WertpapierAusgang01.txt"));
        assertThat(entry.getNote(), is("Depotkonto-Nr. 100235452280"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testVorabsteuerpauschale()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertThat(security.getWkn(), is("A2PKXG"));
        assertThat(security.getName(), is("VANGUARD FTSE ALL-WORLD U.ETF REG. SHS USD ACC. ON"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check tax transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(49.1102)));
        assertThat(transaction.getSource(), is("Vorabpauschale01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testFondssparplan01()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondssparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertThat(security.getName(), is("COMSTA.-MSCI EM.MKTS.TRN U.ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.2394)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.1692)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3253)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3496)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-11-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3678)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-12-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3160)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testFondssparplan02()
    {
        // Fonds, only 3 decimal places for amount of shares

        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondssparplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU1737652583"));
        assertThat(security.getWkn(), is("A2H9Q0"));
        assertThat(security.getName(), is("AMUNDI IND.SOL.-A.IN.MSCI E.M."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-02-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.9136)));
        assertThat(entry.getSource(), is("Fondssparplan02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.49))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.8207)));
        assertThat(entry.getSource(), is("Fondssparplan02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.49))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.4425)));
        assertThat(entry.getSource(), is("Fondssparplan02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.49))));
    }

    @Test
    public void testGiroKontoauszug01()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(10));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(10L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-08T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(20)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-28T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(91)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Dauerauftrag"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Dauerauftrag"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(999)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Dauerauftrag"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(43.86)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(50.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-18T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(00.01)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1234.56)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-03T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(105)));
            assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("sonstige Buchung"));
        }

    }

    @Test
    public void testGiroKontoauszug02()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
            assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
            assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Dauerauftrag"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1234.56)));
            assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-29T00:00")));
            assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Steuerausgleich"));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.23)));
            assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Steuerausgleich"));
        }
    }

    @Test
    public void testGiroKontoauszug03()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-11T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(31.95)));
            assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-15T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(69.96)));
            assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-18T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1337.96)));
            assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-23T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(163.80)));
            assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-24T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1337.69)));
            assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Kreditkartenabrechnung"));
        }
    }

    @Test
    public void testGiroKontoauszug04()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-21T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(3450.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Bareinzahlung am GA"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-23T00:00")));
            assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Rechnung Bargeldeinzahlung"));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Rechnung Bargeldeinzahlung"));
        }
    }

    @Test
    public void testGiroKontoauszug05()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(21));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(21L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.24)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.01.2021 bis 31.03.2021"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-05T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(418.5)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-08T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Dauerauftrag"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-08T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(200)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Dauerauftrag"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-10T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(82.88)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-11T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-15T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(20)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-17T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(29.7)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-17T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(42)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-22T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(32.77)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-24T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(28.9)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-24T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(104.21)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kreditkartenabrechnung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-25T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(28)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(19.54)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(30)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(92.35)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(33.92)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(39.66)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(50)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(23.56)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(52.5)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-03T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(105)));
            assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }
    }

    @Test
    public void testGiroKontoauszug06()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-08T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));
            assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(111.11)));
            assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }

    }

    @Test
    public void testGiroKontoauszug07()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));
            assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
            assertThat(transaction.getNote(), is("Eingang Echtzeitüberweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1400)));
            assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
            assertThat(transaction.getNote(), is("Eingang Echtzeitüberweisung"));
        }
    }

    @Test
    public void testGiroKontoauszug08()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-02T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
            assertThat(transaction.getSource(), is("GiroKontoauszug08.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
            assertThat(transaction.getSource(), is("GiroKontoauszug08.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }
    }

    @Test
    public void testGiroKontoauszug09()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
            assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
            assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 03.02.2015 bis 31.03.2015"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-19T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
            assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
            assertThat(transaction.getNote(), is("Überweisung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(34)));
            assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-17T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(500)));
            assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
            assertThat(transaction.getNote(), is("Zahlungseingang"));
        }
    }

    @Test
    public void testGiroKontoauszug10()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.04)));
            assertThat(transaction.getSource(), is("GiroKontoauszug10.txt"));
            assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.07.2015 bis 30.09.2015"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.18)));
            assertThat(transaction.getSource(), is("GiroKontoauszug10.txt"));
            assertThat(transaction.getNote(), is("Zinsen für Dispositionskredit"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
            assertThat(transaction.getSource(), is("GiroKontoauszug10.txt"));
            assertThat(transaction.getNote(), is("Kapitalertragsteuer"));
        }
    }

    @Test
    public void testGiroKontoauszug11()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.22)));
            assertThat(transaction.getSource(), is("GiroKontoauszug11.txt"));
            assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.10.2015 bis 31.12.2015"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.14)));
            assertThat(transaction.getSource(), is("GiroKontoauszug11.txt"));
            assertThat(transaction.getNote(), is("Zinsen für Dispositionskredit"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.06)));
            assertThat(transaction.getSource(), is("GiroKontoauszug11.txt"));
            assertThat(transaction.getNote(), is("Kapitalertragsteuer"));
        }
    }

    @Test
    public void testGiroKontoauszug12()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.19)));
            assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
            assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.04.2015 bis 30.06.2015"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.05)));
            assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
            assertThat(transaction.getNote(), is("Kapitalertragsteuer"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-11T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(94.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
            assertThat(transaction.getNote(), is("Basislastschrift"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-07-03T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.25)));
            assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
            assertThat(transaction.getNote(), is("Lastschrift"));
        }
    }

    @Test
    public void testGiroKontoauszug13()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug13.txt"));
            assertThat(transaction.getNote(), is("Rechnung Rückruf/Nachforschung"));
        }
    }

    @Test
    public void testGiroKontoauszug14()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(6L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.64)));
            assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
            assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.01.2022 bis 31.03.2022"));
        }


        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-21T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(161.65)));
            assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
            assertThat(transaction.getNote(), is("Verfüg. Geldautom. FW"));
        }
        
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-22T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(69)));
            assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung onl"));
        }
        
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-25T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.01)));
            assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
            assertThat(transaction.getNote(), is("Kartenzahlung onl FW"));
        }
        
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-09T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(200.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
            assertThat(transaction.getNote(), is("Geldautomat"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-28T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
            assertThat(transaction.getNote(), is("Geldautomat"));
        }
    }

    @Test
    public void testKreditKontoauszug01()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(7L));

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-09T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Einzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-23T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(57.57)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("WWW.onlineshop.DE,"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-21T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Habenzins auf 28 Tage"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-23T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Abgeltungsteuer"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(62.32)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Laden Ort 220,"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(19.36)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Laden GmbH & Co., Ort"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(2450)));
            assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Auszahlung"));
        }
    }

    @Test
    public void testKreditKontoauszug02()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(8));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(8L));

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-04T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(300.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Einzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-26T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("DEUTSCHE BANK AG, DD-LOEBTAU"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-05T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-09T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-11T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-18T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("DEUTSCHE BANK AG, DD-LOEBTAU"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-18T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.27)));
            assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
            assertThat(transaction.getNote(), is("GITHUB, HTTPSGITHUB.C"));
        }
    }

    @Test
    public void testKreditKontoauszug03()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-21T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(40.72)));
            assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Ausgleich Kreditkarte gem. Abrechnung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-07T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(400.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Einzahlung"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-30T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("VB DRESDEN-BAUTZEN EG, DD"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-09T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("VB DRESDEN-BAUTZEN EG, DD"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-20T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.27)));
            assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
            assertThat(transaction.getNote(), is("GITHUB, HTTPSGITHUB.C"));
        }
    }

    @Test
    public void testKreditKontoauszug04()
    {
        DkbPDFExtractor extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-18T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.80)));
            assertThat(transaction.getSource(), is("KreditKontoauszug04.txt"));
            assertThat(transaction.getNote(), is("GITHUB, HTTPSGITHUB.C"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-20T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.49)));
            assertThat(transaction.getSource(), is("KreditKontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Kartenpreis"));
        }
    }
}
