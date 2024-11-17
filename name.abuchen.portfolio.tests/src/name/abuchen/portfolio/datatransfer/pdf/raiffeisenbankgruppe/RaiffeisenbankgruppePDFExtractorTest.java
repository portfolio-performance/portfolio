package name.abuchen.portfolio.datatransfer.pdf.raiffeisenbankgruppe;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RaiffeisenBankgruppePDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class RaiffeisenbankgruppePDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BAY0017"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Bayer AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-03T13:45:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 11441163"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(107.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(106.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7475251036"));
        assertThat(security.getWkn(), is("883121"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("QUALCOMM INC. REGISTERED SHARES DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-09T09:58:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 338500/63.00 | Limit billigst"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14399.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14368.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.74 + 2.50 + 0.10))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B6YX5C33"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SPDR S&P 500 UCITS ETF Registered Shares USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-12T16:42:58")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 12345678"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2510.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2478.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.00 + 25.00 + 3.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CA1363851017"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Canadian Natural Resources Ltd Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("CAD"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-19T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 12345678"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1121.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1041.48))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((73.00 / 1.406) + 25.00 + 3.00))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(1464.32))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        Security security = new Security("Canadian Natural Resources Ltd Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("CA1363851017");
        security.setWkn("884437");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-19T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 12345678"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1121.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1041.48))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((73.00 / 1.406) + 25.00 + 3.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-03-24T12:20:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 44083824"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18521.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18472.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.47 + 30.87))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-03-06T10:46:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 41601727"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9681.58))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9652.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.65 + 19.93))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0102295455"), hasWkn("10229545"), hasTicker(null), //
                        hasName("Raiffeisen Futura - Pension Invest Balanced -V-"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-21T00:00"), hasShares(2.549), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Ref.-Nr.: 13819098307"), //
                        hasAmount("CHF", 399.95), hasGrossValue("CHF", 396.78), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.17))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B14X4S71"), hasWkn("A0J202"), hasTicker(null), //
                        hasName("ISHS DL TREAS.BD 1-3YR U.ETF REGISTERED SHARES USD (DIST)ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-15T09:33:55"), hasShares(0.859), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Auftragsnummer 464088/93.00"), //
                        hasAmount("EUR", 101.50), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0478205379"), hasWkn("DBX0EY"), hasTicker(null), //
                        hasName("XTRACKERS II EUR CORPORATE BD INHABER-ANTEILE 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-05T15:38:26"), hasShares(35.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Auftragsnummer 464088/93.00 | Limit billigst"), //
                        hasAmount("EUR", 5007.32), hasGrossValue("EUR", 5002.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.95))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0037430946"), hasWkn("3743094"), hasTicker(null), //
                        hasName("Swisscanto (CH) Real Estate Fund Responsible IFCA"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(27.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Ref.-Nr.: 17815533192"), //
                        hasAmount("CHF", 5081.10), hasGrossValue("CHF", 4995.00), //
                        hasTaxes("CHF", 3.75), hasFees("CHF", 2.35 + 80.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000837307"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Zumtobel Group AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 21112411"));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-07T11:17:43")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4500)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36115.76))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36923.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(696.65))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(110.77))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US09075V1026"));
        assertThat(security.getWkn(), is("A2PSR2"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BIONTECH SE NAM.-AKT.(SP.ADRS)1/O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 183260/06.00 | Limit bestens"));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-06T11:39:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(110)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19966.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20009.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.02 + 2.50 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5949181045"));
        assertThat(security.getWkn(), is("870747"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("MICROSOFT CORP. REGISTERED SHARES DL-,00000625"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 554374/09.00 | Limit bestens"));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-04-01T11:23:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4187.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4197.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.49 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0009848119"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DWS Top Dividende Inhaber-Anteile LD"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Auftrags-Nr.: 47199493"));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-03-27T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BAY0017"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Bayer AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(90)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(110.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(47.48 + 22.50))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7475251036"));
        assertThat(security.getWkn(), is("883121"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("QUALCOMM INC. REGISTERED SHARES DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 85127406360 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.88))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.86))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.98))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(68.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Security security = new Security("QUALCOMM INC. REGISTERED SHARES DL -,0001", CurrencyUnit.EUR);
        security.setIsin("US7475251036");
        security.setWkn("883121");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 85127406360 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.88))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.86))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.98))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende03()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US17275R1023"));
        assertThat(security.getWkn(), is("878841"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("CISCO SYSTEMS INC.  SHARES REGISTERED SHARES DL-,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 88888888888 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.84))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.68))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.65 + 3.69 + 0.20 + 0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(38.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("CISCO SYSTEMS INC.  SHARES REGISTERED SHARES DL-,001", CurrencyUnit.EUR);
        security.setIsin("US17275R1023");
        security.setWkn("878841");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 88888888888 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.84))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.68))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.65 + 3.69 + 0.20 + 0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende04()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US3703341046"));
        assertThat(security.getWkn(), is("853862"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("GENERAL MILLS INC. REGISTERED SHARES DL -,10"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(400)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 80642931040 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(127.60))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(217.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(65.10 + 21.22 + 1.17 + 1.91))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(216.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        Security security = new Security("GENERAL MILLS INC. REGISTERED SHARES DL -,10", CurrencyUnit.EUR);
        security.setIsin("US3703341046");
        security.setWkn("853862");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(400)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 80642931040 | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(127.60))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(217.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(65.10 + 21.22 + 1.17 + 1.91))));
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
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0D8Q49"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(221)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(97.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(109.96))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00 + 0.29 + (0.64 / 1.0856)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.45))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(119.37))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", CurrencyUnit.EUR);
        security.setIsin("DE000A0D8Q49");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(221)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(97.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(109.96))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00 + 0.29 + (0.64 / 1.0856)))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.45))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende06()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007664039"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VOLKSWAGEN AG VORZUGSAKTIEN O.ST. O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-09T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(70)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(813.78))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1334.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(351.90 + 166.78 + 0.29))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.45))));
    }

    @Test
    public void testDividende07()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getWkn(), is("555750"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DEUTSCHE TELEKOM AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(111)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 66666666666"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(77.70))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(77.70))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende08()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0486851024"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers MSCI Europe Value Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-07-07T00:00"), hasShares(800), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 156.32), hasGrossValue("EUR", 156.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-15T00:00"), hasShares(221), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 82.99), hasGrossValue("EUR", 97.81), //
                        hasForexGrossValue("USD", 106.58), //
                        hasTaxes("EUR", (0.86 / 1.0897) + 9.29 + 0.29), hasFees("EUR", 1.45 + 3.00))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        Security security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", CurrencyUnit.EUR);
        security.setIsin("DE000A0D8Q49");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-15T00:00"), hasShares(221), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 82.99), hasGrossValue("EUR", 97.81), //
                        hasTaxes("EUR", (0.86 / 1.0897) + 9.29 + 0.29), hasFees("EUR", 1.45 + 3.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A1TW21"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.-EMERGINGMARKETS-AKTIEN RZ(A) MITEIGENTUMSANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-16T00:00"), hasShares(7.01), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.59), hasGrossValue("EUR", 8.34), //
                        hasTaxes("EUR", 2.15 + 0.60), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006599905"), hasWkn("659990"), hasTicker(null), //
                        hasName("MERCK KG A.A. INHABER-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-05-02T00:00"), hasShares(25.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 76560429680"), //
                        hasAmount("EUR", 40.50), hasGrossValue("EUR", 55.00), //
                        hasTaxes("EUR", 13.75 + 0.75), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7475251036"), hasWkn("883121"), hasTicker(null), //
                        hasName("QUALCOMM INC. REGISTERED SHARES DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-25T00:00"), hasShares(29.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungsnr. 76560429680 | Quartalsdividende"), //
                        hasAmount("EUR", 16.16), hasGrossValue("EUR", 21.71), //
                        hasForexGrossValue("USD", 23.20), //
                        hasTaxes("EUR", 3.26 + 2.17 + 0.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        Security security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", CurrencyUnit.EUR);
        security.setIsin("US7475251036");
        security.setWkn("883121");

        Client client = new Client();
        client.addSecurity(security);

        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-25T00:00"), hasShares(29.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungsnr. 76560429680 | Quartalsdividende"), //
                        hasAmount("EUR", 16.16), hasGrossValue("EUR", 21.71), //
                        hasTaxes("EUR", 3.26 + 2.17 + 0.12), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende13()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0130595124"), hasWkn("13059512"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SPI (R) Mid"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-11T00:00"), hasShares(254.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Ref.-Nr.: 17518731738"), //
                        hasAmount("CHF", 392.94), hasGrossValue("CHF", 604.52), //
                        hasTaxes("CHF", 211.58), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(17));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(17L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(42.13))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.01))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(79.58))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("EURO-Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(119.06))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("EURO-Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.34))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("EURO-Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("EURO-Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(85.65))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(70.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(34.62))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1097.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einnahmen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00 + 1.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Entgelte vom 01.12.2020 - 31.12.2020"));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.00 + 1.00)));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.95))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Abschluss vom 01.10.2020 bis 31.12.2020"));
    }

    @Test
    public void testKontoauszug02()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(31));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(31L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.94))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.20))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.70))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.16))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(80.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.58))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.60))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.01))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.68))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.99))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.08))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Auszahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200.00))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-30T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lohn/Gehalt"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.95))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Abschluss vom 30.07.2021 bis 31.08.2021"));
    }

    @Test
    public void testKontoauszug03()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(136.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(133.29))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(139.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(35.19))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));
    }

    @Test
    public void testKontoauszug04()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.00))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung SEPA"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(700.00))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung SEPA"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36.99))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1870.70))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lohn/Gehalt"));
    }

    @Test
    public void testKontoauszug05()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-20"), hasAmount("EUR", 26350.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-29"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-30"), hasAmount("EUR", 1700.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug06()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-01"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-06"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-11"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-13"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-14"), hasAmount("EUR", 5400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-29"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 534.59), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-12-31"), hasAmount("EUR", 0.46), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-12-31"), hasAmount("EUR", 0.76), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-12-31"), hasAmount("EUR", 8.46), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug07()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(41L));
        assertThat(results.size(), is(41));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-02"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-09"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-12"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-12"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-12"), hasAmount("EUR", 1111.11), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lohn/Gehalt"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-15"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));
        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-16"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Retouren"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-23"), hasAmount("EUR", 10975.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-24"), hasAmount("EUR", 15.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 228.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 1167.18), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 11001.58), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));
        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 12049.12), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-09"), hasAmount("EUR", 141.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 4.99), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 19.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Übertrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 5550.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-15"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-16"), hasAmount("EUR", 3.53), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-19"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-24"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-24"), hasAmount("EUR", 10074.36), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-26"), hasAmount("EUR", 63.27), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-30"), hasAmount("EUR", 752.88), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 77.91), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 189.76), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 650.51), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 1516.78), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 25000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

    }

    @Test
    public void testKontoauszug08()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-24"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Ihr MastercardCashback"))));

        assertThat(results, hasItem(deposit(hasDate("2024-04-24"), hasAmount("EUR", 5.52), //
                        hasSource("Kontoauszug08.txt"), hasNote("Umsatz vom 23.04.2024 MC Hauptkarte"))));

        assertThat(results, hasItem(deposit(hasDate("2024-04-26"), hasAmount("EUR", 0.55), //
                        hasSource("Kontoauszug08.txt"), hasNote("Ihr MastercardCashback"))));

        assertThat(results, hasItem(removal(hasDate("2024-04-22"), hasAmount("EUR", 63.39), //
                        hasSource("Kontoauszug08.txt"), hasNote("Umsatz vom 21.04.2024 MC Hauptkarte"))));

        assertThat(results, hasItem(removal(hasDate("2024-04-29"), hasAmount("EUR", 36.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Umsatz vom 26.04.2024 MC Hauptkarte"))));

        assertThat(results, hasItem(interestCharge(hasDate("2024-04-30"), hasAmount("EUR", 0.11), //
                        hasSource("Kontoauszug08.txt"), hasNote(null))));
    }

    @Test
    public void testDepotauszug01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-08"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-17"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Gutschrift"))));
    }

    @Test
    public void testEinbuchung01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("LU0392496344"));
        assertNull(security1.getWkn());
        assertNull(security1.getTickerSymbol());
        assertThat(security1.getName(), is("Lyxor MSCI Europe SmallCap ETF Inh.-An. I o.N."));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("LU2572257470"));
        assertNull(security2.getWkn());
        assertNull(security2.getTickerSymbol());
        assertThat(security2.getName(), is("AIS-MSCI Eu.SC ESG CL.NZ AMCTB Act.Nom. U.ETF EUR Dis. oN"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check delivery outbound (Auslieferung) transaction
        PortfolioTransaction entry = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2023-03-10T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(433)));
        assertThat(entry.getSource(), is("Umtausch01.txt"));
        assertThat(entry.getNote(), is("Abrechnungsnummer: 46711492"));

        assertThat(entry.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check delivery inbound (Einlieferung) transaction
        entry = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2023-03-10T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(433)));
        assertThat(entry.getSource(), is("Umtausch01.txt"));
        assertThat(entry.getNote(), is("Abrechnungsnummer: 45664992 | Verhältnis: 1 : 1"));

        assertThat(entry.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 1st cancellation (Amount 0,00) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((PortfolioTransaction) cancellation.getSubject()).getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionTypeNotSupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2023-03-10T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(433)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("Umtausch01.txt"));
        assertThat(((Transaction) cancellation.getSubject()).getNote(), is("Abrechnungsnummer: 46711492"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd cancellation (Amount 0,00) transaction
        cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((PortfolioTransaction) cancellation.getSubject()).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionTypeNotSupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2023-03-10T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(433)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("Umtausch01.txt"));
        assertThat(((Transaction) cancellation.getSubject()).getNote(), is("Abrechnungsnummer: 45664992 | Verhältnis: 1 : 1"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testFreierErhalt01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FreierErhalt01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55T58"), hasWkn(null), hasTicker(null), //
                        hasName("Vang.FTSE Develop.World U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        inboundDelivery( //
                                        hasDate("2022-03-18"), hasShares(550), //
                                        hasSource("FreierErhalt01.txt"), //
                                        hasNote("Änderung/Stornierung"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFreierErhalt02()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FreierErhalt02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0038863350"), hasWkn(null), hasTicker(null), //
                        hasName("Nestlé S.A. Namens-Aktien SF -,10"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2022-07-08"), hasShares(200), //
                                        hasSource("FreierErhalt02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testGebuehrenbelastung011()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehrenbelastung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-09-30T00:00"), //
                        hasSource("Gebuehrenbelastung01.txt"), //
                        hasNote("Ref.-Nr.: 17495598455 | 01.07.2024 bis 30.09.2024"), //
                        hasAmount("CHF", 100.04), hasGrossValue("CHF", 100.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }
}
