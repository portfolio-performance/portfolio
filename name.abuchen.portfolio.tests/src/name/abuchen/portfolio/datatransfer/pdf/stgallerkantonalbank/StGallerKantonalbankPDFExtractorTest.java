package name.abuchen.portfolio.datatransfer.pdf.stgallerkantonalbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.StGallerKantonalbankPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class StGallerKantonalbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000TUAG505"), hasWkn("125205291"), hasTicker(null), //
                        hasName("N-Akt TUI AG Aus Konversion"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-24T10:35:48"), hasShares(10000.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Referenznummer 1603933135"), //
                        hasAmount("EUR", 69297.29), hasGrossValue("EUR", 68226.13), //
                        hasTaxes("EUR", 102.34), hasFees("EUR", 955.17 + 13.65))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000TUAG505"), hasWkn("125205291"), hasTicker(null), //
                        hasName("N-Akt TUI AG Aus Konversion"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-02-20T09:00:13"), hasShares(10000.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Referenznummer 1602562989"), //
                        hasAmount("EUR", 65967.79), hasGrossValue("EUR", 67020.00), //
                        hasTaxes("EUR", 100.53), hasFees("EUR", 938.28 + 13.40))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000TUAG505"), hasWkn("125205291"), hasTicker(null), //
                        hasName("N-Akt TUI AG Aus Konversion"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-02-13"), hasExDate("2026-02-11"), //
                        hasShares(10000.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Referenznummer 1746312001"), //
                        hasAmount("EUR", 736.25), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 263.75), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US82575P1075"), hasWkn("52619625"), hasTicker(null), //
                        hasName("N-Akt Sibanye Stillwater Limited Sponsored ADR Repr 4 Shs ADR/ADS"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-04-02"), //
                        hasExDate("2026-03-20"), //
                        hasShares(10000.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Referenznummer 1234123419"), //
                        hasAmount("EUR", 4232.69), //
                        hasGrossValue("EUR", 5290.86), //
                        hasForexGrossValue("USD", 6218.84), //
                        hasTaxes("EUR", 1058.17), //
                        hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        // Valoren-Nr. on its own line, Kurs line only carries the unit price
        // (no second currency/amount pair), and the "Zu Ihren Lasten" prefix
        // sits on the "Total" line instead of on the "Valuta" line.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0038863350"), hasWkn("3886335"), hasTicker(null), //
                        hasName("N-Akt Nestle AG CHF 0.1 nom"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-28T14:04:36"), hasShares(500), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Referenznummer 2327228781"), //
                        hasAmount("CHF", 39728.85), hasGrossValue("CHF", 39695.00), //
                        hasTaxes("CHF", 29.77), hasFees("CHF", 4.08))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        // Short security name: Valoren-Nr. inline on the shares/name line,
        // multi-lot execution table with several rows sharing the same
        // execution timestamp.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1169151003"), hasWkn("116915100"), hasTicker(null), //
                        hasName("N-Akt Georg Fischer AG CHF 0.05 nom"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-07-16T10:04:49"), hasShares(500), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Referenznummer 1234567890"), //
                        hasAmount("CHF", 22780.87), hasGrossValue("CHF", 22760.82), //
                        hasTaxes("CHF", 17.07), hasFees("CHF", 2.98))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        // The nominal-value continuation ("0.01 nom") pushes ISIN off the
        // start of its line: "0.01 nom ISIN CH0024608827".
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0024608827"), hasWkn("2460882"), hasTicker(null), //
                        hasName("N-Akt Partners Group Holding AG CHF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-02T16:57:26"), hasShares(30), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Referenznummer 1111111111"), //
                        hasAmount("CHF", 24633.56), hasGrossValue("CHF", 24612.00), //
                        hasTaxes("CHF", 18.46), hasFees("CHF", 3.10))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        // Same "nom" continuation pattern, this time with "CHF 1 nom" before ISIN.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0210483332"), hasWkn("21048333"), hasTicker(null), //
                        hasName("N-Akt Compagnie Financiere Richemont SA"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-23T15:29:12"), hasShares(250), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Referenznummer 2222222222"), //
                        hasAmount("CHF", 44725.42), hasGrossValue("CHF", 44687.50), //
                        hasTaxes("CHF", 33.52), hasFees("CHF", 4.40))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        // Bugfix: collective order ("Sammelauftrag") with neither a per-lot
        // execution table nor a "Zeitpunkt letzte Ausführung" line on the
        // document - the trade date must fall back to "Wir haben für Sie am
        // ... gekauft" on the first page instead of failing the import.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US11135F1012"), hasWkn("41112361"), hasTicker(null), //
                        hasName("N-Akt Broadcom Inc USD 0.001 nom"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-18"), hasShares(75), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Referenznummer 1798350508"), //
                        hasAmount("USD", 30744.01), hasGrossValue("USD", 30697.52), //
                        hasTaxes("USD", 46.04), hasFees("USD", 0.45))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        // Bugfix: "Abrechnung: Ihr Kauf (Emission)" collective order for a
        // fund, no per-lot execution table but "Zeitpunkt letzte Ausführung"
        // is present and preferred over the day-only fallback. Also covers
        // the "Auftragsausführung Anlagefonds" fee.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0106252546"), hasWkn("1034591"), hasTicker(null), //
                        hasName("Uts Schroder Emerging Markets"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-07-13T18:30"), hasShares(3000), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Referenznummer 1809761290"), //
                        hasAmount("USD", 106847.29), hasGrossValue("USD", 106684.80), //
                        hasTaxes("USD", 160.03), hasFees("USD", 2.46))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        // Valoren-Nr. on its own line followed by a nameContinued line,
        // multi-lot execution table, Kurs line with only the unit price.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKSCBX74"), hasWkn("110951056"), hasTicker(null), //
                        hasName("Ant UBS (Irl) ETF plc - UBS MSCI World Small Cap SR UCITS ETF Accum Shs USD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-05-28T13:48:08"), hasShares(5500), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Referenznummer 0987763798"), //
                        hasAmount("EUR", 59403.62), hasGrossValue("EUR", 59504.78), //
                        hasTaxes("EUR", 89.26), hasFees("EUR", 11.90))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        // Bugfix: collective order ("Sammelauftrag") sale, date falls back to
        // "Zeitpunkt letzte Ausführung". Also covers the "SEC Börsengebühr" fee.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn("645156"), hasTicker(null), //
                        hasName("N-Akt Amazon.com Inc USD 0.01 nom"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-08-05T16:49"), hasShares(220), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Referenznummer 1818805876"), //
                        hasAmount("USD", 60929.95), hasGrossValue("USD", 61024.06), //
                        hasTaxes("USD", 91.53), hasFees("USD", 1.26 + 1.32))));
    }

    @Test
    public void testDividende03()
    {
        // Split "Total ... Zu Ihren Gunsten" / "Valuta ..." layout (no prefix
        // on the Valuta line itself), plus the "Zusätzlicher Steuerrückbehalt"
        // additional US withholding tax.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn("957150"), hasTicker(null), //
                        hasName("N-Akt Nike Inc -B- Namenaktien"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-07-01"), hasExDate("2026-06-01"), //
                        hasShares(750), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Referenznummer 0345051361"), //
                        hasAmount("USD", 215.24), hasGrossValue("USD", 307.50), //
                        hasTaxes("USD", 46.13 + 46.13), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        // Bugfix: the security's WKN/ISIN line has a "Namenaktien" prefix
        // before "Valoren-Nr.:" instead of standing on its own - this must not
        // be mistaken for a separate nameContinued line. Also covers the
        // "Zusätzlicher Steuerrückbehalt" additional US withholding tax on the
        // single-line "Zu Ihren Gunsten Valuta ..." layout.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn("908440"), hasTicker(null), //
                        hasName("N-Akt Apple Inc USD 0.00001 nom"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-08-13"), hasExDate("2026-08-10"), //
                        hasShares(250), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Referenznummer 1820855466"), //
                        hasAmount("USD", 47.24), hasGrossValue("USD", 67.50), //
                        hasTaxes("USD", 10.13 + 10.13), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        // Same "Namenaktien Valoren-Nr.:" prefix as Dividende04, but without
        // any withholding tax at all - the security is EUR-denominated while
        // the distribution itself is paid in USD.
        var extractor = new StGallerKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000S9YS762"), hasWkn("124625792"), hasTicker(null), //
                        hasName("N-Akt Linde PLC EUR 0.001 nom"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-06-18"), hasExDate("2026-06-04"), //
                        hasShares(100), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Referenznummer 1798034814"), //
                        hasAmount("USD", 160.00), hasGrossValue("USD", 160.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
