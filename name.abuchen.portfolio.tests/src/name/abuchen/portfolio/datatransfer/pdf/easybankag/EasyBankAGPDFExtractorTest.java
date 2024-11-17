package name.abuchen.portfolio.datatransfer.pdf.easybankag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
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
import name.abuchen.portfolio.datatransfer.pdf.EasyBankAGPDFExtractor;
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
public class EasyBankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UK5"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h . S T . E u . 6 00 Bas.Res.U.ETF DE Inhaber-Anlageaktien"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.94)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 00000000-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(468.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(464.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.28 + 3.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYZK4776"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h a r e s  I V - Healthc.Innovation Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(73.84)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(475.72))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(472.72))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYWQWR46"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("V a n E c k  V i d  eSports UC. ETF Reg. Shares A USD Acc. o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15.75)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(476.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(471.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.30 + 3.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I I I - C o r e MSCI World U.ETF Registered Shs USD (Acc) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.855)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(476.31))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(473.31))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BGL86Z12"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h a r e s  I V - Electr.Veh.+Dr.Tech Registered Shares USD Acc.o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(78.80)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(479.35))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(476.35))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BZ0PKT83"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I V - E d g e MSCI Wo.Mult.U.ETF Registered Shares USD (Acc)o.N"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(60.75)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(479.98))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(475.67))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.31 + 3.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0010527275"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("L . M S C I  W a t . ESG F.(DR)UCITS ETF Actions au Port.Dist o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9.67)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(483.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(480.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B9CQXS71"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15.373)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(489.27))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(484.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.33 + 3.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0032523478"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s - E O  C o r p Bd Lar.Cap U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("Kauf09.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(491.08))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(486.74))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.34 + 3.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYZK4883"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I V - D i g i talisation U.ETF Registered Shares USD (Acc) oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(110.20)));
        assertThat(entry.getSource(), is("Kauf10.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(704.42))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(701.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1XNHC34"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(90.68)));
        assertThat(entry.getSource(), is("Kauf11.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(960.67))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(955.04))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.63 + 3.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BM67HK77"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X t r . ( I E ) - M S CI Wrld Health Care Registered Shares 1C USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.622)));
        assertThat(entry.getSource(), is("Kauf12.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(243.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(239.44))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.66 + 3.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BM67HT60"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X t r . ( I E ) - M S CI Wo.Inform.Techn. Registered Shares 1C USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-06-13T09:04:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.04)));
        assertThat(entry.getSource(), is("Kauf13.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(464.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(460.59))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.27 + 3.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005557508"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("D E U T S C H E  T E LEKOM AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-07-25T09:00:07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80.00)));
        assertThat(entry.getSource(), is("Kauf14.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36978407-25.7.2022"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1482.52))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1446.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.86 + 25.31 + 7.95))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000815402"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UBM Development AG STAMMAKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-05T09:04:16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100.00)));
        assertThat(entry.getSource(), is("Kauf15.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 25095540 | Limit: 42,500000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4210.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.47 + 0.24 + 6.30))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf16.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0006204407"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("KUKA AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-12-27T09:11:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(27)));
        assertThat(entry.getSource(), is("Kauf16.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 25484466 | Limit: 73,400000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1997.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1981.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00 + 14.00))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf17.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US1912161007"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COCA-COLA CO., THE Registered Shares DL -,25"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-10-27T12:21:47")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(13)));
        assertThat(entry.getSource(), is("Kauf17.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 25456794 | Limit: 59,150000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(780.59))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(768.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.69 + 2.00 + 8.95))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf18.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BM67HK77"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X t r . ( I E ) - M S CI Wrld Health Care Registered Shares 1C USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7.617)));
        assertThat(entry.getSource(), is("Kauf18.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(361.14))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(357.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.98 + 3.00))));
    }

    @Test
    public void testWertpapierKauf19()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BZ0PKT83"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I V - E d g e MSCI Wo.Mult.U.ETF Registered Shares USD (Acc)o.N"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(86)));
        assertThat(entry.getSource(), is("Kauf19.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(726.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(721.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.98 + 3.00))));
    }

    @Test
    public void testWertpapierKauf20()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf20.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0032523478"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s - E O  C o r p Bd Lar.Cap U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6.05)));
        assertThat(entry.getSource(), is("Kauf20.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(727.09))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(722.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.99 + 3.00))));
    }

    @Test
    public void testWertpapierKauf21()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf21.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B9CQXS71"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(23.954)));
        assertThat(entry.getSource(), is("Kauf21.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(729.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(724.61))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.99 + 3.00))));
    }

    @Test
    public void testWertpapierKauf22()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf22.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I I I - C o r e MSCI World U.ETF Registered Shs USD (Acc) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.065)));
        assertThat(entry.getSource(), is("Kauf22.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(739.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(734.93))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.02 + 3.00))));
    }

    @Test
    public void testWertpapierKauf23()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf23.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYZK4776"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h a r e s  I V - Healthc.Innovation Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(99.98)));
        assertThat(entry.getSource(), is("Kauf23.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(743.18))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(738.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.03 + 3.00))));
    }

    @Test
    public void testWertpapierKauf24()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf24.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UK5"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h . S T . E u . 6 00 Bas.Res.U.ETF DE Inhaber-Anlageaktien"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11.98)));
        assertThat(entry.getSource(), is("Kauf24.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(746.48))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(741.44))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.04 + 3.00))));
    }

    @Test
    public void testWertpapierKauf25()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf25.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0010527275"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("L . M S C I  W a t . ESG F.(DR)UCITS ETF Actions au Port.Dist o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(13.76)));
        assertThat(entry.getSource(), is("Kauf25.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(748.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(745.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
    }

    @Test
    public void testWertpapierKauf26()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf26.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BGL86Z12"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h a r e s  I V - Electr.Veh.+Dr.Tech Registered Shares USD Acc.o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(118)));
        assertThat(entry.getSource(), is("Kauf26.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(751.64))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(746.59))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.05 + 3.00))));
    }

    @Test
    public void testWertpapierKauf27()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf27.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYWQWR46"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("V a n E c k  V i d  eSports UC. ETF Reg. Shares A USD Acc. o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(28.89)));
        assertThat(entry.getSource(), is("Kauf27.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(764.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(759.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.09 + 3.00))));
    }

    @Test
    public void testWertpapierKauf28()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf28.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYZK4883"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I V - D i g i talisation U.ETF Registered Shares USD (Acc) oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(173)));
        assertThat(entry.getSource(), is("Kauf28.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1148.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1142.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.14 + 3.00))));
    }

    @Test
    public void testWertpapierKauf29()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf29.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1XNHC34"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(128.1618)));
        assertThat(entry.getSource(), is("Kauf29.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1494.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1487.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.09 + 3.00))));
    }

    @Test
    public void testWertpapierKauf30()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf30.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BM67HT60"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X t r . ( I E ) - M S CI Wo.Inform.Techn. Registered Shares 1C USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-11-11T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15.67)));
        assertThat(entry.getSource(), is("Kauf30.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36338031-3.5.2017"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(764.22))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(759.13))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.09 + 3.00))));
    }

    @Test
    public void testWertpapierKauf31()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf31.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000APOST4"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("O E S T E R R E I C H ISCHE POST AG AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-29T13:01:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(750)));
        assertThat(entry.getSource(), is("Kauf31.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 36075607-29.12.2022"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21706.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21686.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.95))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000743059"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("OMV AG AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-04T13:12:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 25866072 | Limit: 51,520000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2482.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2584.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(93.23))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.67 + 0.24 + 5.95))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0006204407"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("KUKA AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-10-27T11:15:57")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(27)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 25058391 | Limit: 83,100000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2155.68))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2243.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(72.02))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00 + 14.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US0231351067"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("AMAZON.COM INC. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-22T09:04:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 25521770 | Stoplimit: 2.725,000000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21414.65))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21648.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(178.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.19 + 2.00 + 51.96))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A1R0RZ5"), hasWkn(null), hasTicker(null), //
                        hasName("Ekosem-Agrar AG Inh.-Schv. v.2012(2020/2027)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-11T11:38:21"), hasShares(20), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftrags-Nr.: 23456789 | Limit: 20,200000"), //
                        hasAmount("EUR", 394.69), hasGrossValue("EUR", 404.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.85 + 0.71 + 6.75))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-07-15T00:00"), hasShares(20.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.55), hasGrossValue("EUR", 42.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A2E4A94"), hasWkn(null), hasTicker(null), //
                        hasName("BDT Media Automation GmbH Nachr.Anl.v.2017(2019/2024)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-14T00:00"), hasShares(20.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1982.15), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", 14.85 + 0.50), hasFees("EUR", 2.50))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-07-15T00:00"), hasShares(20.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.85), hasGrossValue("EUR", 14.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0008404005"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("A l l i a n z  S E vink.Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-09T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(115)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(910.69))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1242.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(327.58 + 0.62))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.11))));
    }

    @Test
    public void testDividende02()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005552004"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("D E U T S C H E  P O ST AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1250)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1649.80))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2250.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(593.44 + 1.13))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.63))));
    }

    @Test
    public void testDividende03()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B9CQXS71"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1204.86)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(315.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((336.16 / 1.06145)))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(336.16))));
    }

    @Test
    public void testDividende03WithSecurityinEUR()
    {
        Security security = new Security("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B9CQXS71");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1204.86)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(315.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((336.16 / 1.06145)))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende04()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7427181091"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("P R O C T E R  &  G AMBLE CO., THE Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120.62))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((182.66 / 1.05005)))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((54.80 / 1.05005) + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(182.66))));
    }

    @Test
    public void testDividende04WithSecurityinEUR()
    {
        Security security = new Security("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("US7427181091");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120.62))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((182.66 / 1.05005)))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((54.80 / 1.05005) + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende05()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007164600"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("S A P  S E Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(179.24))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(245.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(64.62 + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));
    }

    @Test
    public void testDividende06()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1XNHC34"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3568.5778)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(101.96))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((110.63 / 1.07305)))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(110.63))));
    }

    @Test
    public void testDividende06WithSecurityinEUR()
    {
        Security security = new Security("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B1XNHC34");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3568.5778)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(101.96))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((110.63 / 1.07305)))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende07()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000APOST4"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("O E S T E R R E I C H ISCHE POST AG AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(137.75))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(190.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(52.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende08()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0008404005"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("A l l i a n z  S E vink.Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-09T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(115)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(910.69))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1242.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(327.58 + 0.62))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.11))));
    }

    @Test
    public void testDividende09()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005552004"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("D E U T S C H E  P O ST AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1250)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1649.80))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2250.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(593.44 + 1.13))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.63))));
    }

    @Test
    public void testDividende10()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B9CQXS71"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1204.86)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(315.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(316.70))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(336.16))));
    }

    @Test
    public void testDividende10WithSecurityinEUR()
    {
        Security security = new Security("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B9CQXS71");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1204.86)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(315.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(316.70))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende11()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007164600"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("S A P  S E Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende11.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(179.24))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(245.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(64.62 + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));
    }

    @Test
    public void testDividende12()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1XNHC34"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3568.5778)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(101.96))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(103.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(110.63))));
    }

    @Test
    public void testDividende12WithSecurityinEUR()
    {
        Security security = new Security("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B1XNHC34");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3568.5778)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(101.96))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(103.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende13()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000821103"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UNIQA Insurance Group AG Stamm-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1200)));
        assertThat(transaction.getSource(), is("Dividende13.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(478.50))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(660.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(181.50))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende14()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005937007"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("MAN SE Inhaber-Stammaktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(60)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.03))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.60))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.83 + 1.74))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende15()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US46625H1005"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("JPMorgan Chase & Co. Registered Shares DL 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getSource(), is("Dividende15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.00 * 0.99975))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((2.00 + 2.40) * 0.99975))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(16.00))));
    }

    @Test
    public void testDividende15WithSecurityinEUR()
    {
        Security security = new Security("JPMorgan Chase & Co. Registered Shares DL 1", CurrencyUnit.EUR);
        security.setIsin("US46625H1005");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getSource(), is("Dividende15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.00 * 0.99975))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((2.00 + 2.40) * 0.99975))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende16()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UK5"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h . S T . E u . 6 00 Bas.Res.U.ETF DE Inhaber-Anlageaktien"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(966.48)));
        assertThat(transaction.getSource(), is("Dividende16.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1043.22))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1046.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.52))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.62))));
    }

    @Test
    public void testDividende17()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UH1"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h . S T . G l . S el.Div.100 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-19T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(750)));
        assertThat(transaction.getSource(), is("Dividende17.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(232.49))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(233.63))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));
    }

    @Test
    public void testDividende18()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0032523478"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("i S h s - E O  C o r p Bd Lar.Cap U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(374.95)));
        assertThat(transaction.getSource(), is("Dividende18.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(118.07))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));
    }

    @Test
    public void testDividende19()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A14J587"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("t h y s s e n k r u p p AG Medium Term Notes v.15(25)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));
        assertThat(transaction.getSource(), is("Dividende19.txt"));
        assertThat(transaction.getNote(), is("2,5 % von 25.2.2022 bis 24.2.2023"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(48.86))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95))));
    }

    @Test
    public void testDividende20()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9CQXS71"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR S&P Glob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-13T00:00"), hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.09), hasGrossValue("EUR", 8.59), //
                        hasForexGrossValue("USD", 9.18), //
                        hasTaxes("EUR", 2.55), hasFees("EUR", 0.95))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-11-13T00:00"), hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.36), hasGrossValue("EUR", 2.36), //
                        hasForexGrossValue("USD", 2.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20WithSecurityInEUR()
    {
        Security security = new Security("SPDR S&P Glob.Div.Aristocr.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B9CQXS71");

        Client client = new Client();
        client.addSecurity(security);

        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-13T00:00"), hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.09), hasGrossValue("EUR", 8.59), //
                        hasTaxes("EUR", 2.55), hasFees("EUR", 0.95), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-11-13T00:00"), hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.36), hasGrossValue("EUR", 2.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug01()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("REF: 38000220627-5336530-0000561"));
    }

    @Test
    public void testDepotauszug02()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Depotauszug02.txt"));
        assertThat(transaction.getNote(), is("REF: 38000221017-1648514-0002320"));
    }

    @Test
    public void testDepotauszug03()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000.00))));
        assertThat(transaction.getSource(), is("Depotauszug03.txt"));
        assertThat(transaction.getNote(), is("REF: 38000221128-0294080-0002067"));
    }

    @Test
    public void testDepotauszug04()
    {
        EasyBankAGPDFExtractor extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.50))));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Kontoführungsgebühr"));
    }
}
