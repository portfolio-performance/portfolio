package name.abuchen.portfolio.datatransfer.pdf.dzbankgruppe;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DZBankGruppePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class DZBankGruppePDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
                        hasIsin("ES0177542018"), hasWkn("A1H6AJ"), hasTicker(null), //
                        hasName("INTERNAT. CONS. AIRL. GROUP SA ACCIONES NOM. EO -,10"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-17T09:04:10"), hasShares(2700.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Limit billigst"), //
                        hasAmount("EUR", 5109.01), hasGrossValue("EUR", 5047.65), //
                        hasTaxes("EUR", 10.10), hasFees("EUR", 50.48 + 0.71 + 0.07))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000XNG8888"), hasWkn("XNG888"), hasTicker(null), //
                        hasName("XING SE NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-06-04T18:09:18"), hasShares(2.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Limit 341,00 EUR"), //
                        hasAmount("EUR", 690.05), hasGrossValue("EUR", 680.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95 + 0.10))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007100000"), hasWkn("710000"), hasTicker(null), //
                        hasName("DAIMLER AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-14T15:40:55"), hasShares(50.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Limit billigst"), //
                        hasAmount("EUR", 1356.69), hasGrossValue("EUR", 1344.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95 + 2.49))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US65339F1012"), hasWkn("A1CZ4H"), hasTicker(null), //
                        hasName("NEXTERA ENERGY INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-27T16:18:02"), hasShares(50.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Limit 68,50 EUR"), //
                        hasAmount("EUR", 3450.10), hasGrossValue("EUR", 3425.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 25.00 + 0.10))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US90353T1007"), hasWkn("A2PHHG"), hasTicker(null), //
                        hasName("Aktienname TECHNOLOGIES INC. REGISTERED SHARES DL-,00001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-01T18:27:57"), hasShares(100.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Limit 39,60 EUR"), //
                        hasAmount("EUR", 8963.05), hasGrossValue("EUR", 8953.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95 + 0.10))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-17T15:37:21"), hasShares(50.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Limit 210,00 EUR"), //
                        hasAmount("EUR", 10531.25), hasGrossValue("EUR", 10500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.15 + 0.10))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYYXBF44"), hasWkn("A2AHZU"), hasTicker(null), //
                        hasName("I.M.III-I.FTSE EM H.D.L.V.UETF REG. SHARES ACC O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-17T15:09:38"), hasShares(350.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Limit 25,04 EUR"), //
                        hasAmount("EUR", 8790.91), hasGrossValue("EUR", 8764.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 26.81 + 0.10))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

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
                        hasIsin("IE00BK1PV551"), hasWkn("A1XEY2"), hasTicker(null), //
                        hasName("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1D O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-15T14:18:35"), hasShares(47.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Limit 66,00 EUR"), //
                        hasAmount("EUR", 3112.26), hasGrossValue("EUR", 3078.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.90 + 10.00 + 3.67))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasIsin("LU2575877977"), hasWkn("A3D4YD"), hasTicker(null), //
                        hasName("ALLIANZ EURO CASH ACT.AU PORT. AT EUR ACC. ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-17T00:00"), hasShares(12.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1316.42), hasGrossValue("EUR", 1316.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
                        hasIsin("DE000A0RAD42"), hasWkn("A0RAD4"), hasTicker(null), //
                        hasName("ANTECEDO INDEPENDENT INVEST INHABER-ANTEILE A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-07-08T00:00"), hasShares(61.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10154.67), hasGrossValue("EUR", 10154.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000NWRK013"), hasWkn("NWRK01"), hasTicker(null), //
                        hasName("NEW WORK SE NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-08-13T15:16:51"), hasShares(2.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Stoplimit 291,00 EUR Limit 290,00 EUR"), //
                        hasAmount("EUR", 577.95), hasGrossValue("EUR", 588.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95 + 0.10))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2019-08-13T00:00"), hasShares(2.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Verrechnete Aktienverluste 112,10- EUR"), //
                        hasAmount("EUR", 29.57), hasGrossValue("EUR", 29.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
                        hasIsin("DE000BAY0017"), hasWkn("BAY001"), hasTicker(null), //
                        hasName("BAYER AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-09-26T09:04:37"), hasShares(30.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Stoplimit 65,10 EUR Limit 64,50 EUR"), //
                        hasAmount("EUR", 1904.98), hasGrossValue("EUR", 1953.00), //
                        hasTaxes("EUR", 35.99 + 1.98), hasFees("EUR", 9.95 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US64110L1061"), hasWkn("552484"), hasTicker(null), //
                        hasName("NETFLIX INC. REGISTERED SHARES DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-09T19:48:50"), hasShares(1.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Limit 462,00 EUR"), //
                        hasAmount("EUR", 442.29), hasGrossValue("EUR", 462.35), //
                        hasTaxes("EUR", 9.49 + 0.52), hasFees("EUR", 9.95 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US29786A1060"), hasWkn("A14P98"), hasTicker(null), //
                        hasName("ETSY INC. REGISTERED SHARES DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-27T15:52:15"), hasShares(5.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Limit 161,00 EUR"), //
                        hasAmount("EUR", 805.45), hasGrossValue("EUR", 815.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95 + 0.10))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-01-27T00:00"), hasShares(5.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Verrechnete Aktienverluste 99,50- EUR"), //
                        hasAmount("EUR", 26.23), hasGrossValue("EUR", 26.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin("DE0008474230"), hasWkn("847423"), hasTicker(null), //
                        hasName("DWS EURO FLEXIZINS INHABER-ANTEILE NC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-03-10T00:00"), hasShares(2666.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 200099.53), hasGrossValue("EUR", 200136.62), //
                        hasTaxes("EUR", 32.67 + 1.80 + 2.62), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
                        hasIsin("PLOPTTC00011"), hasWkn("534356"), hasTicker(null), //
                        hasName("CD PROJEKT S.A. INHABER-AKTIEN C ZY 1"), //
                        hasCurrencyCode("PLN"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-10T00:00"), hasExDate("2021-05-31T00:00"), //
                        hasShares(17.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.28), hasGrossValue("EUR", 18.87), //
                        hasForexGrossValue("PLN", 85.00), //
                        hasTaxes("EUR", 3.59 + 1.89 + 0.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("CD PROJEKT S.A. INHABER-AKTIEN C ZY 1", "EUR");
        security.setIsin("PLOPTTC00011");
        security.setWkn("534356");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DZBankGruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-10T00:00"), hasExDate("2021-05-31T00:00"), //
                        hasShares(17.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.28), hasGrossValue("EUR", 18.87), //
                        hasTaxes("EUR", 3.59 + 1.89 + 0.11), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
                        hasIsin("CA0679011084"), hasWkn("870450"), hasTicker(null), //
                        hasName("BARRICK GOLD CORP.  SHARES REGISTERED SHARES O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-17T00:00"), hasExDate("2021-02-26T00:00"), //
                        hasShares(205.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 9.92), hasGrossValue("EUR", 15.39), //
                        hasForexGrossValue("USD", 18.45), //
                        hasTaxes("EUR", 2.31 + 1.54 + 0.08 + (1.85 / 1.1989)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("BARRICK GOLD CORP.  SHARES REGISTERED SHARES O.N.", "EUR");
        security.setIsin("CA0679011084");
        security.setWkn("870450");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DZBankGruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-17T00:00"), hasExDate("2021-02-26T00:00"), //
                        hasShares(205.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 9.92), hasGrossValue("EUR", 15.39), //
                        hasTaxes("EUR", 2.31 + 1.54 + 0.08 + (1.85 / 1.1989)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007236101"), hasWkn("723610"), hasTicker(null), //
                        hasName("SIEMENS AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-02-08T00:00"), hasExDate("2021-02-04T00:00"), //
                        hasShares(160.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 560.00), hasGrossValue("EUR", 560.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYYXBF44"), hasWkn("A2AHZU"), hasTicker(null), //
                        hasName("I.M.III-I.FTSE EM H.D.L.V.UETF REG. SHARES DIS O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-24T00:00"), hasExDate("2021-03-18T00:00"), //
                        hasShares(350.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.58), hasGrossValue("EUR", 61.58), //
                        hasForexGrossValue("USD", 73.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("I.M.III-I.FTSE EM H.D.L.V.UETF REG. SHARES DIS O.N.", "EUR");
        security.setIsin("IE00BYYXBF44");
        security.setWkn("A2AHZU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DZBankGruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-24T00:00"), hasExDate("2021-03-18T00:00"), //
                        hasShares(350.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.58), hasGrossValue("EUR", 61.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0Z2ZZ5"), hasWkn("A0Z2ZZ"), hasTicker(null), //
                        hasName("FREENET AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-23T00:00"), hasExDate("2021-06-21T00:00"), //
                        hasShares(250.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 412.50), hasGrossValue("EUR", 412.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasIsin("IE00BK1PV551"), hasWkn("A1XEY2"), hasTicker(null), //
                        hasName("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1D O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-21T00:00"), hasExDate("2022-06-08T00:00"), //
                        hasShares(102.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 24.60), hasGrossValue("EUR", 30.54), //
                        hasForexGrossValue("USD", 32.33), //
                        hasTaxes("EUR", 2.62 + 0.14 + 0.21 + 2.62 + 0.14 + 0.21), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1D O.N.", "EUR");
        security.setIsin("IE00BK1PV551");
        security.setWkn("A1XEY2");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DZBankGruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-21T00:00"), hasExDate("2022-06-08T00:00"), //
                        hasShares(102.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 24.60), hasGrossValue("EUR", 30.54), //
                        hasTaxes("EUR", 2.62 + 0.14 + 0.21 + 2.62 + 0.14 + 0.21), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasIsin("JP3420600003"), hasWkn("850022"), hasTicker(null), //
                        hasName("SEKISUI HOUSE LTD. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("JPY"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-30T00:00"), hasExDate("2022-07-28T00:00"), //
                        hasShares(1400.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Zwischendividende"), //
                        hasAmount("EUR", 378.24), hasGrossValue("EUR", 514.76), //
                        hasForexGrossValue("JPY", 72800.00), //
                        hasTaxes("EUR", 78.84 + 25.18 + 25.18 + 1.39 + 1.39 + 2.27 + 2.27), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        var security = new Security("SEKISUI HOUSE LTD. REGISTERED SHARES O.N.", "JPY");
        security.setIsin("JP3420600003");
        security.setWkn("850022");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DZBankGruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-30T00:00"), hasExDate("2022-07-28T00:00"), //
                        hasShares(1400.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Zwischendividende"), //
                        hasAmount("EUR", 378.24), hasGrossValue("EUR", 514.76), //
                        hasTaxes("EUR", 78.84 + 25.18 + 25.18 + 1.39 + 1.39 + 2.27 + 2.27), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasIsin("DE000A3MQQJ0"), hasWkn("A3MQQJ"), hasTicker(null), //
                        hasName("RECONCEPT GREEN ENERGY ASSET B INH.-SCHULDV. 2022(2024/2027)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-28T00:00"), hasExDate(null), //
                        hasShares(100.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 153.02), hasGrossValue("EUR", 212.50), //
                        hasTaxes("EUR", 25.98 + 25.98 + 1.43 + 1.43 + 2.33 + 2.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

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
                        hasIsin("IE000RHYOR04"), hasWkn("A3DJQJ"), hasTicker(null), //
                        hasName("ISHSIV-EO ULTRASHORT BD U.ETF REG.SHS EUR ACC. ON"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(1000.00), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungsnr. 99999999999"), //
                        hasAmount("EUR", 8.26), hasGrossValue("EUR", 8.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

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
                        hasIsin("LU0570870567"), hasWkn("A1JJHG"), hasTicker(null), //
                        hasName("CT (LUX) GLOBAL SMALLER COS NAMENS-ANTEILE AE O.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(100.00), //
                        hasSource("Vorabpauschale02.txt"), //
                        hasNote("Abrechnungsnr. 99999999999"), //
                        hasAmount("EUR", 13.48), hasGrossValue("EUR", 13.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

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
                        hasIsin("IE000RHYOR04"), hasWkn("A3DJQJ"), hasTicker(null), //
                        hasName("ISHSIV-EO ULTRASHORT BD U.ETF REG.SHS EUR ACC. ON"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(2935.00), //
                        hasSource("Vorabpauschale03.txt"), //
                        hasNote("Abrechnungsnr. 99999999999"), //
                        hasAmount("EUR", 24.28), hasGrossValue("EUR", 24.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht01.txt"), errors);

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
                        hasIsin("DE000A0RPAN3"), hasWkn(null), hasTicker(null), //
                        hasName("PrivatFonds: Kontrolliert pro"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-07-17T00:00"), hasShares(14.286), //
                        hasSource("Umsatzuebersicht01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2125.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht02()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(22L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0011269588"), hasWkn(null), hasTicker(null), //
                        hasName("Carmignac Patrimoine FCP D EUR"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0599946976"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Concept SICAV - Kaldemorge"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0KFTH1"), hasWkn(null), hasTicker(null), //
                        hasName("First Private Wealth B"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0720541993"), hasWkn(null), hasTicker(null), //
                        hasName("LOYS FCP-Global L/S P"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0RPAN3"), hasWkn(null), hasTicker(null), //
                        hasName("PrivatFonds: Kontrolliert pro"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0323578657"), hasWkn(null), hasTicker(null), //
                        hasName("Flossbach von Storch SICAV - Multip"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0395794307"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan Funds - Global Income Fund"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(18.730), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2083.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 41.67))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(18.658), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2083.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 41.67))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(19.010), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2083.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 41.67))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(15.025), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2068.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 56.87))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(14.869), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2068.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 56.87))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(14.950), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2068.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 56.87))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(28.739), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2093.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.40))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(28.395), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2093.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.40))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(28.461), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2093.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.40))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(29.894), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(29.766), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(30.199), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(14.420), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2125.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(14.086), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2125.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(14.251), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2125.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(8.853), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(8.708), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(8.729), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-09-15T00:00"), hasShares(14.585), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T00:00"), hasShares(14.364), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-15T00:00"), hasShares(14.671), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-24T00:00"), hasShares(0.665), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote("Wiederanlage"), //
                        hasAmount("EUR", 94.78), hasGrossValue("EUR", 94.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-11-24T00:00"), hasExDate(null), //
                        hasShares(0.665), //
                        hasSource("Umsatzuebersicht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 94.78), hasGrossValue("EUR", 94.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht03()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009805515"), hasWkn(null), hasTicker(null), //
                        hasName("UniImmo: Europa"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-10T00:00"), hasShares(6.730), //
                        hasSource("Umsatzuebersicht03.txt"), //
                        hasNote("Wiederanlage"), //
                        hasAmount("EUR", 362.80), hasGrossValue("EUR", 362.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-10T00:00"), hasExDate(null), //
                        hasShares(6.730), //
                        hasSource("Umsatzuebersicht03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 362.80), hasGrossValue("EUR", 362.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht04()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0720541993"), hasWkn(null), hasTicker(null), //
                        hasName("LOYS FCP-Global L/S P"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0323578657"), hasWkn(null), hasTicker(null), //
                        hasName("Flossbach von Storch SICAV - Multip"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-15T00:00"), hasShares(29.570), //
                        hasSource("Umsatzuebersicht04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-15T00:00"), hasShares(8.658), //
                        hasSource("Umsatzuebersicht04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2125.00), hasGrossValue("EUR", 2073.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 51.83))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-15T00:00"), hasShares(0.387), //
                        hasSource("Umsatzuebersicht04.txt"), //
                        hasNote("Wiederanlage"), //
                        hasAmount("EUR", 92.69), hasGrossValue("EUR", 92.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-12-15T00:00"), hasExDate(null), //
                        hasShares(0.387), //
                        hasSource("Umsatzuebersicht04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 92.69), hasGrossValue("EUR", 92.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht05()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0RPAN3"), hasWkn(null), hasTicker(null), //
                        hasName("PrivatFonds: Kontrolliert pro"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-22T00:00"), hasShares(0.092), //
                        hasSource("Umsatzuebersicht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.45), hasGrossValue("EUR", 14.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-24T00:00"), hasShares(0.00), //
                        hasSource("Umsatzuebersicht05.txt"), //
                        hasNote("Depotgebühr"), //
                        hasAmount("EUR", 32.55), hasGrossValue("EUR", 32.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-12-24T00:00"), hasShares(0.00), //
                        hasSource("Umsatzuebersicht05.txt"), //
                        hasNote("Abrechnung Nr. 45"), //
                        hasAmount("EUR", 16.83), hasGrossValue("EUR", 16.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-12-24T00:00"), hasShares(0.00), //
                        hasSource("Umsatzuebersicht05.txt"), //
                        hasNote("Abrechnung Nr. 45"), //
                        hasAmount("EUR", 1.27), hasGrossValue("EUR", 1.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht06()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0KFTH1"), hasWkn(null), hasTicker(null), //
                        hasName("First Private Wealth B"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0395794307"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan Funds - Global Income Fund"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-19T00:00"), hasShares(285.637), //
                        hasSource("Umsatzuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 18103.67), hasGrossValue("EUR", 18103.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-24T00:00"), hasShares(1.591), //
                        hasSource("Umsatzuebersicht06.txt"), //
                        hasNote("Wiederanlage"), //
                        hasAmount("EUR", 207.69), hasGrossValue("EUR", 207.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-24T00:00"), hasExDate(null), //
                        hasShares(1.591), //
                        hasSource("Umsatzuebersicht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 207.69), hasGrossValue("EUR", 207.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht07()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht07.txt"), errors);

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
                        hasIsin("DE000A1W2CK8"), hasWkn(null), hasTicker(null), //
                        hasName("GLS Bank Aktienfonds"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02T00:00"), hasShares(0.346), //
                        hasSource("Umsatzuebersicht07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 30.00), hasGrossValue("EUR", 30.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht08()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht08.txt"), errors);

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
                        hasIsin("LU0260869739"), hasWkn(null), hasTicker(null), //
                        hasName("Franklin U.S. Opportunities Fund A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02T00:00"), hasShares(28.405), //
                        hasSource("Umsatzuebersicht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 700.00), hasGrossValue("EUR", 671.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 28.54))));
    }

    @Test
    public void testUmsatzuebersicht09()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008491051"), hasWkn(null), hasTicker(null), //
                        hasName("UniGlobal"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008491044"), hasWkn(null), hasTicker(null), //
                        hasName("UniRak"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-04-11T00:00"), hasShares(0.137), //
                        hasSource("Umsatzuebersicht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 23.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.19))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-04-11T00:00"), hasShares(0.225), //
                        hasSource("Umsatzuebersicht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.73))));
    }

    @Test
    public void testUmsatzuebersicht10()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0089558679"), hasWkn(null), hasTicker(null), //
                        hasName("UniDynamicFonds: Global A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0186860408"), hasWkn(null), hasTicker(null), //
                        hasName("UniDividendenAss"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0115904467"), hasWkn(null), hasTicker(null), //
                        hasName("UniEM Global"), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2014-02-10T00:00"), hasShares(12.152), //
                        hasSource("Umsatzuebersicht10.txt"), //
                        hasNote("Umtausch aus Unterdepot 1345674218"), //
                        hasAmount("EUR", 458.99), hasGrossValue("EUR", 458.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2014-02-10T00:00"), hasShares(5.148), //
                        hasSource("Umsatzuebersicht10.txt"), //
                        hasNote("Umtausch aus Unterdepot 1345674218"), //
                        hasAmount("EUR", 271.20), hasGrossValue("EUR", 271.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2014-02-10T00:00"), hasShares(4.136), //
                        hasSource("Umsatzuebersicht10.txt"), //
                        hasNote("Umtausch zugunsten Unterdepot 1345674217"), //
                        hasAmount("EUR", 271.20), hasGrossValue("EUR", 271.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2014-02-10T00:00"), hasShares(7.00), //
                        hasSource("Umsatzuebersicht10.txt"), //
                        hasNote("Umtausch zugunsten Unterdepot 1345674205"), //
                        hasAmount("EUR", 458.99), hasGrossValue("EUR", 458.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht11()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008491051"), hasWkn(null), hasTicker(null), //
                        hasName("UniGlobal"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-12-20T00:00"), hasShares(0.036), //
                        hasSource("Umsatzuebersicht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.00), hasGrossValue("EUR", 9.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2019-11-21T00:00"), hasShares(0.009), //
                        hasSource("Umsatzuebersicht11.txt"), //
                        hasNote("Ausgleichsbuchung Steuer"), //
                        hasAmount("EUR", 2.09), hasGrossValue("EUR", 2.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-12-24T00:00"), hasShares(0.00), //
                        hasSource("Umsatzuebersicht11.txt"), //
                        hasNote("Depotgebühr"), //
                        hasAmount("EUR", 9.00), hasGrossValue("EUR", 9.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht12()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(9L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(18));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A2H9A01"), hasWkn(null), hasTicker(null), //
                        hasName("UniMultiAsset: Exklusiv"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A2H9A19"), hasWkn(null), hasTicker(null), //
                        hasName("UniMultiAsset: Chance I"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BMDQ4622"), hasWkn(null), hasTicker(null), //
                        hasName("LMGF-L.M.Mart.Cu.Gl.L.T.Uncon. Reg."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4WXJJ64"), hasWkn(null), hasTicker(null), //
                        hasName("iShares III- Core EO Govt Bond UCIT"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0113258742"), hasWkn(null), hasTicker(null), //
                        hasName("SISF EURO Corporate Bond C Acc EUR"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0368266499"), hasWkn(null), hasTicker(null), //
                        hasName("BGF Euro Corporate Bond Fund D2 EUR"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0480132876"), hasWkn(null), hasTicker(null), //
                        hasName("UBS(L)F.S-MSCI EM.MKTS UC ETF A USD"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0659579733"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers MSCI World Swap"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1663883681"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Inv.-Euro-Gov Bonds"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T00:00"), hasShares(0.915), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 55.39), hasGrossValue("EUR", 55.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T00:00"), hasShares(0.926), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 51.30), hasGrossValue("EUR", 51.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T00:00"), hasShares(0.022), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 2.04), hasGrossValue("EUR", 2.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T15:48"), hasShares(0.095), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 12.20), hasGrossValue("EUR", 12.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T00:00"), hasShares(0.166), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 4.24), hasGrossValue("EUR", 4.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T00:00"), hasShares(0.227), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 4.05), hasGrossValue("EUR", 4.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T15:44"), hasShares(0.034), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 3.46), hasGrossValue("EUR", 3.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T15:46"), hasShares(0.289), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 9.09), hasGrossValue("EUR", 9.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-01T00:00"), hasShares(0.086), //
                        hasSource("Umsatzuebersicht12.txt"), //
                        hasNote("Anlage"), //
                        hasAmount("EUR", 8.23), hasGrossValue("EUR", 8.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht13()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A1W2CK8"), hasWkn(null), hasTicker(null), //
                        hasName("GLS Bank Aktienfonds Inhaber-Anteil"), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-06T00:00"), hasShares(2.560), //
                        hasSource("Umsatzuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 198.99), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 1.01), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-27T00:00"), hasShares(2.541), //
                        hasSource("Umsatzuebersicht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht14()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht14.txt"), errors);

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
                        hasIsin("DE000A1C81G1"), hasWkn(null), hasTicker(null), //
                        hasName("UniGlobal Vorsorge"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-07T00:00"), hasShares(0.085), //
                        hasSource("Umsatzuebersicht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 23.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.19))));
    }

    @Test
    public void testUmsatzuebersicht15()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht15.txt"), errors);

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
                        hasIsin("DE000A1C81G1"), hasWkn(null), hasTicker(null), //
                        hasName("UniGlobal Vorsorge"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-16T00:00"), hasShares(0.087), //
                        hasSource("Umsatzuebersicht15.txt"), //
                        hasNote("Kauf aus Zulage"), //
                        hasAmount("EUR", 26.79), hasGrossValue("EUR", 25.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.28))));
    }

    @Test
    public void testUmsatzuebersicht16()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(22L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008491051"), hasWkn(null), hasTicker(null), //
                        hasName("UniGlobal"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-20T00:00"), hasShares(0.457), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-31T00:00"), hasShares(0.075), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-21T00:00"), hasShares(0.472), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-28T00:00"), hasShares(0.077), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-21T00:00"), hasShares(0.455), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-31T00:00"), hasShares(0.074), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-20T00:00"), hasShares(0.457), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-29T00:00"), hasShares(0.078), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-16T00:00"), hasShares(0.522), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote("Kauf aus Zulage"), //
                        hasAmount("EUR", 175.00), hasGrossValue("EUR", 170.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.68))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-20T00:00"), hasShares(0.499), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-31T00:00"), hasShares(0.079), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-20T00:00"), hasShares(0.528), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-30T00:00"), hasShares(0.085), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-20T00:00"), hasShares(0.489), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-29T00:00"), hasShares(0.077), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-08-22T00:00"), hasShares(0.460), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-08-31T00:00"), hasShares(0.079), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-20T00:00"), hasShares(0.492), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-30T00:00"), hasShares(0.085), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-20T00:00"), hasShares(0.509), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 162.00), hasGrossValue("EUR", 157.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.34))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-28T00:00"), hasShares(0.080), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 25.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.71))));

        // check reinvestment transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-10T00:00"), hasShares(1.942), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote("Wiederanlage"), //
                        hasAmount("EUR", 630.18), hasGrossValue("EUR", 630.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-10T00:00"), hasExDate(null), //
                        hasShares(1.942), //
                        hasSource("Umsatzuebersicht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 630.18), hasGrossValue("EUR", 630.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht17()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008491044"), hasWkn(null), hasTicker(null), //
                        hasName("UniRak"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A2QFXS3"), hasWkn(null), hasTicker(null), //
                        hasName("UniZukunft Klima -net- A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0101442050"), hasWkn(null), hasTicker(null), //
                        hasName("UniSector: BasicIndustries A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0262776809"), hasWkn(null), hasTicker(null), //
                        hasName("UniOpti4"), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-09T00:00"), hasShares(341.919), //
                        hasSource("Umsatzuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50000.00), hasGrossValue("EUR", 52176.82), //
                        hasTaxes("EUR", 2176.82), hasFees("EUR", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-09T00:00"), hasShares(602.304), //
                        hasSource("Umsatzuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 30217.59), hasGrossValue("EUR", 30217.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-09T00:00"), hasShares(104.034), //
                        hasSource("Umsatzuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 20000.00), hasGrossValue("EUR", 20524.93), //
                        hasTaxes("EUR", 524.93), hasFees("EUR", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-10-09T00:00"), hasShares(200.999), //
                        hasSource("Umsatzuebersicht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 20000.00), hasGrossValue("EUR", 20075.81), //
                        hasTaxes("EUR", 75.81), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-10-10T00:00"), hasShares(602.304), //
                        hasSecurity(hasIsin("DE000A2QFXS3")), //
                        hasSource("Umsatzuebersicht17.txt"), //
                        hasNote("Abrechnung Nr. 447"), //
                        hasAmount("EUR", 126.14), hasGrossValue("EUR", 126.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmschichtung01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umschichtung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0732152185"), hasWkn(null), hasTicker(null), //
                        hasName("UniVorsorge 3 ASP"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0186860408"), hasWkn(null), hasTicker(null), //
                        hasName("UniDividendenAss A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0732152268"), hasWkn(null), hasTicker(null), //
                        hasName("UniVorsorge 4 ASP"), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2024-02-19T00:00"), hasShares(19.468), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 893.19), hasGrossValue("EUR", 893.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2024-02-19T00:00"), hasShares(6.973), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 435.32), hasGrossValue("EUR", 435.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2024-02-19T00:00"), hasShares(27.291), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 1328.53), hasGrossValue("EUR", 1328.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2024-03-08T00:00"), hasShares(2.145), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 98.84), hasGrossValue("EUR", 98.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2024-03-08T00:00"), hasShares(7.649), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 486.17), hasGrossValue("EUR", 486.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2024-03-08T00:00"), hasShares(11.893), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 585.02), hasGrossValue("EUR", 585.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2024-04-30T00:00"), hasShares(12.637), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 578.52), hasGrossValue("EUR", 578.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2024-04-30T00:00"), hasShares(8.222), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 537.39), hasGrossValue("EUR", 537.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2024-04-30T00:00"), hasShares(22.993), //
                        hasSource("Umschichtung01.txt"), //
                        hasNote("Umschichtung durch Produktkonzept"), //
                        hasAmount("EUR", 1115.85), hasGrossValue("EUR", 1115.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAktiensplit01()
    {
        var extractor = new DZBankGruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aktiensplit01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn("906866"), hasTicker(null), //
                        hasName("AMAZON.COM INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2022-06-06T00:00"), hasShares(5.00), //
                                        hasSource("Aktiensplit01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
