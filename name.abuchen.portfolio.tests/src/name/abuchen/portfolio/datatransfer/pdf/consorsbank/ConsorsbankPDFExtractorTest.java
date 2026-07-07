package name.abuchen.portfolio.datatransfer.pdf.consorsbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
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
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ConsorsbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-01-15T08:13:35"), hasShares(132.80212), //
                        hasSource("Kauf01.txt"), //
                        hasNote("12345670.001"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 5000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("DE000A0L1NN5"), hasWkn("A0L1NN"), hasTicker(null), //
                        hasName("HELIAD EQ.PARTN.KGAA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-09-21T12:45:38"), hasShares(250.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("92612045.001 | Limitkurs  5,500000 EUR"), //
                        hasAmount("EUR", 1387.85), hasGrossValue("EUR", 1370.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.95 + 3.00 + 5.00 + 4.95 + 1.95))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("DE000A0J3UF6"), hasWkn("A0J3UF"), hasTicker(null), //
                        hasName("EARTH EXPLORAT.FDS UI EOR"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-16T15:24:22"), hasShares(0.95126), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.61))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("IE00B9KNR336"), hasWkn("A1T8GC"), hasTicker(null), //
                        hasName("SPDR S+P P.AS.DIV.ARI.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-15T12:00:56"), hasShares(210.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("123456.001 | Limitkurs  46,200000 EUR"), //
                        hasAmount("EUR", 9745.25), hasGrossValue("EUR", 9702.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.50 + 11.54 + 24.26 + 4.95))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("FR0000120628"), hasWkn("855705"), hasTicker(null), //
                        hasName("AXA S.A. INH.     EO 2,29"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-03T08:02:51"), hasShares(1.01514), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.56), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.37))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("US92243N1037"), hasWkn("A2P1CV"), hasTicker(null), //
                        hasName("VECTO.ACQ.CORP. DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-11T15:52:34"), hasShares(30.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("164920718.001 | Limitkurs 18,000000 USD"), //
                        hasAmount("EUR", 525.92), hasGrossValue("EUR", 500.97), //
                        hasForexGrossValue("USD", 540.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 19.95))));
    }

    @Test
    public void testWertpapierKauf06WithSecurityInEUR()
    {
        var security = new Security("VECTO.ACQ.CORP. DL -,0001", "EUR");
        security.setIsin("US92243N1037");
        security.setWkn("A2P1CV");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-11T15:52:34"), hasShares(30.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("164920718.001 | Limitkurs 18,000000 USD"), //
                        hasAmount("EUR", 525.92), hasGrossValue("EUR", 500.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 19.95))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("851144"), hasTicker(null), //
                        hasName("GENERAL ELECTRIC CO. SHARES DL -,06"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2001-09-18T00:00"), hasShares(50.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("6201999.001"), //
                        hasAmount("EUR", 1928.74), hasGrossValue("EUR", 1917.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.53 + 5.11 + 4.60))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("625952"), hasTicker(null), //
                        hasName("GARTMORE - CONT. EUROP. FUND ACTIONS NOM. A O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-10-17T00:00"), hasShares(15.75243), //
                        hasSource("Kauf08.txt"), //
                        hasNote("2424880.001"), //
                        hasAmount("EUR", 75.00), hasGrossValue("EUR", 73.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.83))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("625952"), hasTicker(null), //
                        hasName("GARTMORE-CONT. EUROP. A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-01-15T00:01:11"), hasShares(11.87891), //
                        hasSource("Kauf09.txt"), //
                        hasNote("24248801.001"), //
                        hasAmount("EUR", 75.00), hasGrossValue("EUR", 73.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.83))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("ISH.STOX.EUROPE 600 U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-07T13:57:45"), hasShares(197.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("183282190.001 | Limitkurs 38,860000 EUR"), //
                        hasAmount("EUR", 7659.37), hasGrossValue("EUR", 7655.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.95))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

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
                        hasIsin("US9168961038"), hasWkn("A0JDRR"), hasTicker(null), //
                        hasName("URANIUM ENERGY DL-,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-07T14:09:07"), hasShares(4974.00), //
                        hasSource("Kauf11.txt"), //
                        hasNote("183283021.001 | Limitkurs 1,100000 EUR"), //
                        hasAmount("EUR", 5441.15), hasGrossValue("EUR", 5414.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.50 + 5.96 + 13.54 + 4.95))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

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
                        hasName("ALLIANZ SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-15T09:30:34"), hasShares(0.12804), //
                        hasSource("Kauf12.txt"), //
                        hasNote("184007672.001"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.37))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

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
                        hasIsin("PO6527623674"), hasWkn("SP110Y"), hasTicker(null), //
                        hasName("Sparplanname"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-06-15T11:07:25"), hasShares(6.43915), //
                        hasSource("Kauf13.txt"), //
                        hasNote("100012345.001"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

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
                        hasIsin("DE000KB8A6S2"), hasWkn("KB8A6S"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL21 EO/DL"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-20T17:09:36"), hasShares(1000.00), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Limitkurs 1,160000 EUR"), //
                        hasAmount("EUR", 1160.00), hasGrossValue("EUR", 1160.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

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
                        hasIsin("US7710491033"), hasWkn("A2QHVS"), hasTicker(null), //
                        hasName("ROBLOX CORP.CL.A DL-,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-29T15:31:01"), hasShares(5.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Limitkurs 70,000000 USD"), //
                        hasAmount("EUR", 317.75), hasGrossValue("EUR", 292.80), //
                        hasForexGrossValue("USD", 343.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 19.95))));
    }

    @Test
    public void testWertpapierKauf15WithSecurityInEUR()
    {
        var security = new Security("ROBLOX CORP.CL.A DL-,0001", "EUR");
        security.setIsin("US7710491033");
        security.setWkn("A2QHVS");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-29T15:31:01"), hasShares(5.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Limitkurs 70,000000 USD"), //
                        hasAmount("EUR", 317.75), hasGrossValue("EUR", 292.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 19.95))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf16.txt"), errors);

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
                        hasIsin("DE000SB8VZT2"), hasWkn("SB8VZT"), hasTicker(null), //
                        hasName("SG EFF. TURBOL BC8"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-06T15:46:08"), hasShares(500.00), //
                        hasSource("Kauf16.txt"), //
                        hasNote("186089202.001 | Limitkurs 1,310000 EUR"), //
                        hasAmount("EUR", 659.95), hasGrossValue("EUR", 655.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.95))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf17.txt"), errors);

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
                        hasIsin("DE000STRA555"), hasWkn("STRA55"), hasTicker(null), //
                        hasName("STRATEC SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-06T09:02:27"), hasShares(30.00), //
                        hasSource("Kauf17.txt"), //
                        hasNote("111111111.001"), //
                        hasAmount("EUR", 3972.29), hasGrossValue("EUR", 3954.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50 + 9.89 + 4.95 + 1.95))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf18.txt"), errors);

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
                        hasIsin("US0231351067"), hasWkn("906866"), hasTicker(null), //
                        hasName("AMAZON.COM INC.  DL-,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-03T09:30:55"), hasShares(0.00830), //
                        hasSource("Kauf18.txt"), //
                        hasNote("123456789.001"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.37))));
    }

    @Test
    public void testWertpapierKauf19_PartialExecution1()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19_partial_execution_1.txt"),
                        errors);

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
                        hasIsin("JP3155350006"), hasWkn("910660"), hasTicker(null), //
                        hasName("UYEMURA + CO., C."), //
                        hasCurrencyCode("JPY"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-01T02:11:52"), hasShares(200.00), //
                        hasSource("Kauf19_partial_execution_1.txt"), //
                        hasNote("238336487.001 | Limitkurs 5.100,000000 JPY"), //
                        hasAmount("EUR", 7352.45), hasGrossValue("EUR", 1020000 / 141.09), //
                        hasForexGrossValue("JPY", 1020000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 18.07 + 49.95 + (7760 / 141.09)))));
    }

    @Test
    public void testWertpapierKauf19_PartialExecution1WithSecurityInEUR()
    {
        var security = new Security("UYEMURA + CO., C.", "EUR");
        security.setIsin("JP3155350006");
        security.setWkn("910660");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19_partial_execution_1.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-01T02:11:52"), hasShares(200.00), //
                        hasSource("Kauf19_partial_execution_1.txt"), //
                        hasNote("238336487.001 | Limitkurs 5.100,000000 JPY"), //
                        hasAmount("EUR", 7352.45), hasGrossValue("EUR", 1020000 / 141.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 18.07 + 49.95 + (7760 / 141.09)))));
    }

    @Test
    public void testWertpapierKauf19_PartialExecution2()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19_partial_execution_2.txt"),
                        errors);

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
                        hasIsin("JP3155350006"), hasWkn("910660"), hasTicker(null), //
                        hasName("UYEMURA + CO., C."), //
                        hasCurrencyCode("JPY"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-01T02:11:52"), hasShares(1500.00), //
                        hasSource("Kauf19_partial_execution_2.txt"), //
                        hasNote("238336487.002 | Limitkurs 5.100,000000 JPY"), //
                        hasAmount("EUR", 54356.26), hasGrossValue("EUR", 54220.71), //
                        hasForexGrossValue("JPY", 7650000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 135.55))));
    }

    @Test
    public void testWertpapierKauf19_PartialExecution2WithSecurityInEUR()
    {
        var security = new Security("UYEMURA + CO., C.", "EUR");
        security.setIsin("JP3155350006");
        security.setWkn("910660");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19_partial_execution_2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-01T02:11:52"), hasShares(1500.00), //
                        hasSource("Kauf19_partial_execution_2.txt"), //
                        hasNote("238336487.002 | Limitkurs 5.100,000000 JPY"), //
                        hasAmount("EUR", 54356.26), hasGrossValue("EUR", 54220.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 135.55))));
    }

    @Test
    public void testWertpapierKauf20()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf20.txt"), errors);

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
                        hasIsin("DE000A2AA402"), hasWkn("A2AA40"), hasTicker(null), //
                        hasName("CLERE AG O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-06T13:43:10"), hasShares(300.00), //
                        hasSource("Kauf20.txt"), //
                        hasNote("148553598.001 | Limitkurs  11,300000 EUR"), //
                        hasAmount("EUR", 3408.64), hasGrossValue("EUR", 3390.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.50 + 2.71 + 8.48 + 4.95))));
    }

    @Test
    public void testWertpapierKauf21()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf21.txt"), errors);

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
                        hasIsin("US00206R1023"), hasWkn("A0HL9Z"), hasTicker(null), //
                        hasName("AT + T INC.          DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-27T20:13:13"), hasShares(37.00), //
                        hasSource("Kauf21.txt"), //
                        hasNote("161127520.001 | Limitkurs  27,700000 EUR"), //
                        hasAmount("EUR", 1026.34), hasGrossValue("EUR", 1016.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 4.95))));
    }

    @Test
    public void testWertpapierKauf22()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf22.txt"), errors);

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
                        hasIsin("DE0005158703"), hasWkn("515870"), hasTicker(null), //
                        hasName("BECHTLE AG O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-13T09:00:28"), hasShares(53.00), //
                        hasSource("Kauf22.txt"), //
                        hasNote("000000000.001"), //
                        hasAmount("EUR", 2131.80), hasGrossValue("EUR", 2120.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.95 + 0.60 + 5.30 + 3.95))));
    }

    @Test
    public void testWertpapierKauf23()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf23.txt"), errors);

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
                        hasIsin("LU0272423673"), hasWkn("A0MKKC"), hasTicker(null), //
                        hasName("T. ROWE PR.-GL.N.R.E.AUSD"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-15T10:51:09"), hasShares(5.28416), //
                        hasSource("Kauf23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 49.50), //
                        hasForexGrossValue("USD", 52.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));
    }

    @Test
    public void testWertpapierKauf23WithSecurityInEUR()
    {
        var security = new Security("T. ROWE PR.-GL.N.R.E.AUSD", "EUR");
        security.setIsin("LU0272423673");
        security.setWkn("A0MKKC");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-15T10:51:09"), hasShares(5.28416), //
                        hasSource("Kauf23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 49.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));
    }

    @Test
    public void testWertpapierKauf24()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf24.txt"), errors);

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
                        hasIsin("ES0105046009"), hasWkn("A12D3A"), hasTicker(null), //
                        hasName("AENA SME S.A. EO 10"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-17T08:10:59"), hasShares(7.00), //
                        hasSource("Kauf24.txt"), //
                        hasNote("5421561.010 | Limitkurs 143,000000 EUR"), //
                        hasAmount("EUR", 1003.95), hasGrossValue("EUR", 1001.00), //
                        hasTaxes("EUR", 2.00), hasFees("EUR", 0.95))));
    }

    @Test
    public void testWertpapierKauf25()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf25.txt"), errors);

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
                        hasIsin("IE0032077012"), hasWkn("801498"), hasTicker(null), //
                        hasName("INVESCOM3 NASDAQ-100 A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-01T09:32:16"), hasShares(0.60087), //
                        hasSource("Kauf25.txt"), //
                        hasNote("123456789.111"), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf26()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf26.txt"), errors);

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
                        hasIsin("XS2343822842"), hasWkn("A2YN0C"), hasTicker(null), //
                        hasName("0,375 % VOLKSWAGEN LEASING 21/26 20.JULI"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-07T13:38:16"), hasShares(70.00), //
                        hasSource("Kauf26.txt"), //
                        hasNote("269730691.001 | Stückzins 356 Tage 25,60 EUR"), //
                        hasAmount("EUR", 6241.78), hasGrossValue("EUR", 6240.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.95))));
    }

    @Test
    public void testWertpapierKauf27()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf27.txt"), errors);

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
                        hasIsin("IE00B6R52259"), hasWkn("A1JMDF"), hasTicker(null), //
                        hasName("ISHSV-MSCI ACWI DL A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T09:30:41"), hasShares(1.16993), //
                        hasSource("Kauf27.txt"), //
                        hasNote("164197312.001"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 49.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.74))));
    }

    @Test
    public void testWertpapierKauf28()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK1PV551"), hasWkn("A1XEY2"), hasTicker(null), //
                        hasName("X(IE)-MSCI WORLD 1D"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2024-06-03T09:30:09"), hasShares(4.09855), //
                                        hasSource("Kauf28.txt"), //
                                        hasNote("298093049.001"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierKauf29()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf29.txt"), errors);

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
                        hasIsin("DE0006231004"), hasWkn("623100"), hasTicker(null), //
                        hasName("INFINEON TECH.AG NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-01-07T08:42:15"), hasShares(200.00), //
                        hasSource("Kauf29.txt"), //
                        hasNote("35641755.001"), //
                        hasAmount("EUR", 1059.45), hasGrossValue("EUR", 1040.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 12.55 + 4.95 + 1.95))));
    }

    @Test
    public void testWertpapierKauf30()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf30.txt"), errors);

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
                        hasIsin("LU0218910023"), hasWkn("A0EQVB"), hasTicker(null), //
                        hasName("VONTOBEL-GL EQ.A-DL"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-16T13:40:01"), hasShares(0.32990), //
                        hasSource("Kauf30.txt"), //
                        hasNote("194783734.001"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasForexGrossValue("USD", 118.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf31()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf31.txt"), errors);

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
                        hasIsin("LU0218910023"), hasWkn("A0EQVB"), hasTicker(null), //
                        hasName("VONTOBEL-GL EQ.A-DL"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-15T14:02:19"), hasShares(0.35347), //
                        hasSource("Kauf31.txt"), //
                        hasNote("439309430.001"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 97.56), //
                        hasForexGrossValue("USD", 107.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.44))));
    }

    @Test
    public void testWertpapierBezug01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Bezug01.txt"), errors);

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
                        hasIsin("DE000A0V9L94"), hasWkn("A0V9L9"), hasTicker(null), //
                        hasName("EYEMAXX R.EST.AG"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-06-06T00:00"), hasShares(66.00), //
                        hasSource("Bezug01.txt"), //
                        hasNote("12345678.001"), //
                        hasAmount("EUR", 399.96), hasGrossValue("EUR", 396.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.96))));
    }

    @Test
    public void testWertpapierverkauf01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("US9286624021"), hasWkn("A0DPR2"), hasTicker(null), //
                        hasName("VOLKSWAGEN AG VZ ADR1/5"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-02-18T12:10:30"), hasShares(140.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("12345678.001 | Limitkurs  42,850000 EUR"), //
                        hasAmount("EUR", 5794.56), hasGrossValue("EUR", 6048.00), //
                        hasTaxes("EUR", 198.08 + 17.82 + 10.89), hasFees("EUR", 2.95 + 3.63 + 15.12 + 4.95))));
    }

    @Test
    public void testWertpapierverkauf02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("DE000PX2LEH3"), hasWkn("PX2LEH"), hasTicker(null), //
                        hasName("BNP PAR.EHG MINIS XAU"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-02T14:51:46"), hasShares(100.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("158026714.001 | Limitkurs One-Cancels-Other 13,880000 EUR"), //
                        hasAmount("EUR", 1386.96), hasGrossValue("EUR", 1388.00), //
                        hasTaxes("EUR", 0.50 + 0.50 + 0.02 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierverkauf03()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("915771"), hasTicker(null), //
                        hasName("CYBERIAN OUTPOST INC. SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2001-11-19T00:00"), hasShares(200.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("7536824.001"), //
                        hasAmount("EUR", 46.78), hasGrossValue("EUR", 56.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.30 + 4.60))));
    }

    @Test
    public void testWertpapierverkauf04()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasIsin(null), hasWkn("974433"), hasTicker(null), //
                        hasName("GARTMORE CSF-CONTIN.EUROPE FD REG.PTG RED.PREF.SHS DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2005-03-24T00:00"), hasShares(52.77908), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("8704534.001"), //
                        hasAmount("EUR", 691.31), hasGrossValue("EUR", 691.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierverkauf05()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("A0MZBE"), hasTicker(null), //
                        hasName("AHOLD, KON. EO-,30"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2008-05-16T16:04:03"), hasShares(334.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("29541397.001 | Limitkurs 9,800000 EUR"), //
                        hasAmount("EUR", 3290.05), hasGrossValue("EUR", 3303.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.26 + 4.95))));
    }

    @Test
    public void testWertpapierverkauf06()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
                        hasIsin("DE000ENER6Y0"), hasWkn("ENER6Y"), hasTicker(null), //
                        hasName("SIEMENS ENERGY AG NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-20T00:00"), hasShares(0.46370), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("178661418.001"), //
                        hasAmount("EUR", 9.96), hasGrossValue("EUR", 9.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierverkauf07()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasIsin("DE000A288904"), hasWkn("A28890"), hasTicker(null), //
                        hasName("COMPUGROUP MED. NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-07T19:18:29"), hasShares(20.00), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("123282999.001 | Limitkurs Trailing Stop proz. 80,376000 EUR"), //
                        hasAmount("EUR", 1590.05), hasGrossValue("EUR", 1600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 4.95))));
    }

    @Test
    public void testWertpapierverkauf08()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

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
                        hasIsin("DE0005785604"), hasWkn("578560"), hasTicker(null), //
                        hasName("FRESENIUS SE+CO.KGAA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-08-05T08:00:04"), hasShares(25.00), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Limitkurs 45,000000 EUR"), //
                        hasAmount("EUR", 1099.34), hasGrossValue("EUR", 1129.00), //
                        hasTaxes("EUR", 24.37 + 1.34), hasFees("EUR", 3.95))));
    }

    @Test
    public void testWertpapierverkauf09()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

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
                        hasIsin("DE000A0LR936"), hasWkn("A0LR93"), hasTicker(null), //
                        hasName("STEICO SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-08-25T09:25:18"), hasShares(23.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Limitkurs 126,600000 EUR"), //
                        hasAmount("EUR", 2797.77), hasGrossValue("EUR", 2911.80), //
                        hasTaxes("EUR", 52.18 + 52.18 + 2.86 + 2.86), hasFees("EUR", 3.95))));
    }

    @Test
    public void testWertpapierverkauf10()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("ISH.STOX.EUROPE 600 U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-08T14:29:55"), hasShares(200.00), //
                        hasSource("Verkauf10.txt"), //
                        hasNote("111111111.001 | Limitkurs 45,490000 EUR"), //
                        hasAmount("EUR", 8786.92), hasGrossValue("EUR", 9098.00), //
                        hasTaxes("EUR", 267.19 + 14.69), hasFees("EUR", 1.50 + 22.75 + 4.95))));
    }

    @Test
    public void testWertpapierverkauf11()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

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
                        hasIsin("DE000A3E5E97"), hasWkn("A3E5E9"), hasTicker(null), //
                        hasName("HOLIDAYCHECK GRP Z.VERK."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-03T00:00"), hasShares(2500.00), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("218450381.001 | Ursprungs-WKN 549532"), //
                        hasAmount("EUR", 6022.23), hasGrossValue("EUR", 6750.00), //
                        hasTaxes("EUR", 641.22 + 35.26 + 51.29), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierverkauf12()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

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
                        hasIsin("DE000WACK012"), hasWkn("WACK01"), hasTicker(null), //
                        hasName("WACKER NEUSON SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-25T19:24:18"), hasShares(240.00), //
                        hasSource("Verkauf12.txt"), //
                        hasNote("111111111.001"), //
                        hasAmount("EUR", 5160.98), hasGrossValue("EUR", 5184.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.50 + 2.61 + 12.96 + 4.95))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf13.txt"), errors);

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
                        hasIsin("DE000A0Z23Q5"), hasWkn("A0Z23Q"), hasTicker(null), //
                        hasName("ADESSO SE INH O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-21T09:06:26"), hasShares(32.00), //
                        hasSource("Verkauf13.txt"), //
                        hasNote("266964025.001 | Limitkurs stop loss 106,600000 EUR"), //
                        hasAmount("EUR", 3357.87), hasGrossValue("EUR", 3372.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.95 + 0.60 + 8.43 + 3.95))));
    }

    @Test
    public void testWertpapierVerkauf14()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf14.txt"), errors);

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
                        hasIsin("DE000SQ6N9E4"), hasWkn("SQ6N9E"), hasTicker(null), //
                        hasName("SG EFF. CALL23 VOW3"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-13T15:22:30"), hasShares(3000.00), //
                        hasSource("Verkauf14.txt"), //
                        hasNote("345678901.001 | Limitkurs 0,001000 EUR"), //
                        hasAmount("EUR", 0.95), hasGrossValue("EUR", 4.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.95))));
    }

    @Test
    public void testWertpapierVerkauf15()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf15.txt"), errors);

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
                        hasIsin(null), hasWkn("600720"), hasTicker(null), //
                        hasName("ESCOM AG I.A."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2007-03-05T00:00"), hasShares(5555.00), //
                        hasSource("Verkauf15.txt"), //
                        hasNote("15681369.001"), //
                        hasAmount("EUR", 211.30), hasGrossValue("EUR", 222.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.95 + 5.00 + 4.95))));
    }

    @Test
    public void testWertpapierEinloesung01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einloesung01.txt"), errors);

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
                        hasIsin("DE000LS846N5"), hasWkn("LS846N"), hasTicker(null), //
                        hasName("Lang & Schwarz AG TurboC O.End Northern 78,25"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-20T00:00"), hasShares(1000.00), //
                        hasSource("Einloesung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin("DE000A12GJD2"), hasWkn("A12GJD"), hasTicker(null), //
                        hasName("L&G-L&G R.Gbl Robot.Autom.UETF Bearer Shares (Dt. Zert.) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-02T00:00"), hasShares(106.00), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.73), hasGrossValue("EUR", 0.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0H0785"), hasWkn("A0H078"), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2020-01-01T00:00"), hasShares(12.47652), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-05-08T00:00"), hasExDate("2015-05-08"), //
                        hasShares(1370.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 326.90), hasGrossValue("EUR", 444.00), //
                        hasTaxes("EUR", 111.00 + 6.10), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-05-08T00:00"), hasExDate("2015-05-08"), //
                        hasShares(370.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1444.00), hasGrossValue("EUR", 1444.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("850866"), hasTicker(null), //
                        hasName("DEERE & CO. Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-11-02T00:00"), hasExDate("2015-09-28"), //
                        hasShares(300.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 121.36), hasGrossValue("EUR", 163.00), //
                        hasForexGrossValue("USD", 180.00), //
                        hasTaxes("EUR", 24.45 + 16.30 + 0.89), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("DEERE & CO. Registered Shares DL 1", "EUR");
        security.setWkn("850866");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

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
                        hasDate("2015-11-02T00:00"), hasExDate("2015-09-28"), //
                        hasShares(300.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 121.36), hasGrossValue("EUR", 163.00), //
                        hasTaxes("EUR", 24.45 + 16.30 + 0.89), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("854242"), hasTicker(null), //
                        hasName("WESTPAC BANKING CORP. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("AUD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-07-02T00:00"), hasExDate("2015-05-13"), //
                        hasShares(1.00020), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.46), hasGrossValue("EUR", 0.62), //
                        hasForexGrossValue("AUD", 0.93), //
                        hasTaxes("EUR", 0.16), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("WESTPAC BANKING CORP. REGISTERED SHARES O.N.", "EUR");
        security.setWkn("854242");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

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
                        hasDate("2015-07-02T00:00"), hasExDate("2015-05-13"), //
                        hasShares(1.00020), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.46), hasGrossValue("EUR", 0.62), //
                        hasTaxes("EUR", 0.16), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("885823"), hasTicker(null), //
                        hasName("GILEAD SCIENCES INC. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-06-29T00:00"), hasExDate("2015-06-12"), //
                        hasShares(0.27072), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.11), //
                        hasForexGrossValue("USD", 0.12), //
                        hasTaxes("EUR", 0.02 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("GILEAD SCIENCES INC. Registered Shares DL -,001", "EUR");
        security.setWkn("885823");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasDate("2015-06-29T00:00"), hasExDate("2015-06-12"), //
                        hasShares(0.27072), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.02 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("200417"), hasTicker(null), //
                        hasName("ALTRIA GROUP INC. Registered Shares DL -,333"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-01-11T00:00"), hasExDate("2015-12-22"), //
                        hasShares(650.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 285.60), hasGrossValue("EUR", 336.00), //
                        hasForexGrossValue("USD", 367.25), //
                        hasTaxes("EUR", 50.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("ALTRIA GROUP INC. Registered Shares DL -,333", "EUR");
        security.setWkn("200417");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

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
                        hasDate("2016-01-11T00:00"), hasExDate("2015-12-22"), //
                        hasShares(650.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 285.60), hasGrossValue("EUR", 336.00), //
                        hasTaxes("EUR", 50.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("891106"), hasTicker(null), //
                        hasName("ROCHE HOLDING AG Inh.-Genuß.(Sp.ADRs) 1/8/SF100"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-04-22T00:00"), hasExDate("2014-03-06"), //
                        hasShares(80.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.51), hasGrossValue("EUR", 64.08), //
                        hasForexGrossValue("USD", 88.72), //
                        hasTaxes("EUR", 22.43 + 6.41 + 0.35), hasFees("EUR", 1.38))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        var security = new Security("ROCHE HOLDING AG Inh.-Genuß.(Sp.ADRs) 1/8/SF100", "EUR");
        security.setWkn("891106");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

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
                        hasDate("2014-04-22T00:00"), hasExDate("2014-03-06"), //
                        hasShares(80.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.51), hasGrossValue("EUR", 64.08), //
                        hasTaxes("EUR", 22.43 + 6.41 + 0.35), hasFees("EUR", 1.38))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("A1409D"), hasTicker(null), //
                        hasName("Welltower Inc. Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-05-20T00:00"), hasExDate("2016-05-06"), //
                        hasShares(50.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.51), hasGrossValue("EUR", 38.25), //
                        hasForexGrossValue("USD", 43.00), //
                        hasTaxes("EUR", 5.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        var security = new Security("Welltower Inc. Registered Shares DL 1", "EUR");
        security.setWkn("A1409D");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasDate("2016-05-20T00:00"), hasExDate("2016-05-06"), //
                        hasShares(50.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.51), hasGrossValue("EUR", 38.25), //
                        hasTaxes("EUR", 5.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasIsin("US6819191064"), hasWkn("871706"), hasTicker(null), //
                        hasName("OMNICOM GROUP INC. Registered Shares DL -,15"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-01-09T00:00"), hasExDate(null), //
                        hasShares(25.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.34), hasGrossValue("EUR", 12.54), //
                        hasForexGrossValue("USD", 15.00), //
                        hasTaxes("EUR", 1.88 + 1.26 + 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        var security = new Security("OMNICOM GROUP INC. Registered Shares DL -,15", "EUR");
        security.setIsin("US6819191064");
        security.setWkn("871706");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasDate("2018-01-09T00:00"), hasExDate(null), //
                        hasShares(25.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.34), hasGrossValue("EUR", 12.54), //
                        hasTaxes("EUR", 1.88 + 1.26 + 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasIsin("LU1033693638"), hasWkn("ETF007"), hasTicker(null), //
                        hasName("ComStage - MDAX UCITS ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-08-23T00:00"), hasExDate(null), //
                        hasShares(43.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.36), hasGrossValue("EUR", 20.06), //
                        hasTaxes("EUR", 3.51 + 0.19), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasIsin("US7960502018"), hasWkn("881823"), hasTicker(null), //
                        hasName("SAMSUNG ELECTRONICS CO. LTD. R.Shs(NV)Pf(GDR144A)/25 SW 100"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-27T00:00"), hasExDate(null), //
                        hasShares(3.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.80), hasGrossValue("EUR", 20.54), //
                        hasForexGrossValue("USD", 22.65), //
                        hasTaxes("EUR", 4.52 + 2.06 + 0.10), hasFees("EUR", 0.06))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        var security = new Security("SAMSUNG ELECTRONICS CO. LTD. R.Shs(NV)Pf(GDR144A)/25 SW 100", "EUR");
        security.setIsin("US7960502018");
        security.setWkn("881823");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasDate("2019-11-27T00:00"), hasExDate(null), //
                        hasShares(3.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.80), hasGrossValue("EUR", 20.54), //
                        hasTaxes("EUR", 4.52 + 2.06 + 0.10), hasFees("EUR", 0.06))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasIsin("IE00B0M62Q58"), hasWkn("A0HGV0"), hasTicker(null), //
                        hasName("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-01-01T00:00"), hasExDate(null), //
                        hasShares(20.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 6.46), hasGrossValue("USD", 9.99), //
                        hasTaxes("USD", (2.23 * 1.12) + (0.12 * 1.12) + (0.80 * 1.12)), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        var security = new Security("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN", "EUR");
        security.setIsin("IE00B0M62Q58");
        security.setWkn("A0HGV0");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-01-01T00:00"), hasExDate(null), //
                        hasShares(20.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 6.46), hasGrossValue("USD", 9.99), //
                        hasTaxes("USD", (2.23 * 1.12) + (0.12 * 1.12) + (0.80 * 1.12)), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

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
                        hasIsin("DE000A2888C9"), hasWkn("A2888C"), hasTicker(null), //
                        hasName("Vonovia SE Dividende Cash"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-28T00:00"), hasExDate(null), //
                        hasShares(125.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 196.25), hasGrossValue("EUR", 196.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasIsin("US5021751020"), hasWkn("884625"), hasTicker(null), //
                        hasName("LTC PROPERTIES INC. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-05-31T00:00"), hasExDate(null), //
                        hasShares(18.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.48), hasGrossValue("EUR", 2.91), //
                        hasForexGrossValue("USD", 3.42), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        var security = new Security("LTC PROPERTIES INC. Registered Shares DL -,01", "EUR");
        security.setIsin("US5021751020");
        security.setWkn("884625");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasDate("2018-05-31T00:00"), hasExDate(null), //
                        hasShares(18.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.48), hasGrossValue("EUR", 2.91), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasIsin(null), hasWkn("865985"), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-02-16T00:00"), hasExDate("2017-02-09"), //
                        hasShares(50.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.90), hasGrossValue("EUR", 26.72), //
                        hasForexGrossValue("USD", 28.50), //
                        hasTaxes("EUR", 4.01 + 2.67 + 0.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        var security = new Security("Apple Inc. Registered Shares o.N.", "EUR");
        security.setWkn("865985");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasDate("2017-02-16T00:00"), hasExDate("2017-02-09"), //
                        hasShares(50.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.90), hasGrossValue("EUR", 26.72), //
                        hasTaxes("EUR", 4.01 + 2.67 + 0.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
                        hasIsin("NL0010273215"), hasWkn("A1J4U4"), hasTicker(null), //
                        hasName("ASML Holding N.V. Aandelen op naam EO -,09"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-12T00:00"), hasExDate(null), //
                        hasShares(8.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.16), hasGrossValue("EUR", 29.60), //
                        hasTaxes("EUR", 4.44), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasIsin(null), hasWkn("A0HGV9"), hasTicker(null), //
                        hasName("iShs-MSCI AC Far E.ex-JP U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-09-30T00:00"), hasExDate("2016-09-15"), //
                        hasShares(3.84865), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.68), hasGrossValue("EUR", 1.68), //
                        hasForexGrossValue("USD", 1.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("iShs-MSCI AC Far E.ex-JP U.ETF Registered Shares o.N.", "EUR");
        security.setWkn("A0HGV9");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasDate("2016-09-30T00:00"), hasExDate("2016-09-15"), //
                        hasShares(3.84865), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.68), hasGrossValue("EUR", 1.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

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
                        hasIsin(null), hasWkn("ETF701"), hasTicker(null), //
                        hasName("ComStage Vermoeg.str.UCITS ETF Inhaber-Anteile I"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-10-13T00:00"), hasExDate("2017-10-02"), //
                        hasShares(27.47377), //
                        hasSource("Dividende18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.29), hasGrossValue("EUR", 1.92), //
                        hasTaxes("EUR", 0.30 + 0.01 + 0.29 + 0.02 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende19()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

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
                        hasIsin("US3765351008"), hasWkn("797937"), hasTicker(null), //
                        hasName("GLADSTONE CAPITAL CORP. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-09-28T00:00"), hasExDate(null), //
                        hasShares(1000.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.78), hasGrossValue("EUR", 60.15), //
                        hasForexGrossValue("USD", 70.00), //
                        hasTaxes("EUR", 9.02 + 6.02 + 0.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende19WithSecurityInEUR()
    {
        var security = new Security("GLADSTONE CAPITAL CORP. Registered Shares o.N.", "EUR");
        security.setIsin("US3765351008");
        security.setWkn("797937");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

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
                        hasDate("2018-09-28T00:00"), hasExDate(null), //
                        hasShares(1000.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.78), hasGrossValue("EUR", 60.15), //
                        hasTaxes("EUR", 9.02 + 6.02 + 0.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

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
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("Allianz SE vink.Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-13T00:00"), hasExDate(null), //
                        hasShares(80.00), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 518.44), hasGrossValue("EUR", 720.00), //
                        hasTaxes("EUR", 176.04 + 9.68 + 15.84), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

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
                        hasIsin("LU0292096186"), hasWkn("DBX1DG"), hasTicker(null), //
                        hasName("Xtr.Stoxx Gbl Sel.Div.100 Swap Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-03-07T00:00"), hasExDate(null), //
                        hasShares(100.4205), //
                        hasSource("Dividende21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.68), hasGrossValue("EUR", 56.02), //
                        hasTaxes("EUR", 9.80 + 0.54), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende22()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

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
                        hasIsin("US49456B1017"), hasWkn("A1H6GK"), hasTicker(null), //
                        hasName("Kinder Morgan Inc. Registered Shares P DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-28T00:00"), hasExDate(null), //
                        hasShares(290.00), //
                        hasSource("Dividende22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 56.33), hasGrossValue("EUR", 56.33), //
                        hasForexGrossValue("USD", 61.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende22WithSecurityInEUR()
    {
        var security = new Security("Kinder Morgan Inc. Registered Shares P DL -,01", "EUR");
        security.setIsin("US49456B1017");
        security.setWkn("A1H6GK");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

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
                        hasDate("2024-02-28T00:00"), hasExDate(null), //
                        hasShares(290.00), //
                        hasSource("Dividende22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 56.33), hasGrossValue("EUR", 56.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

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
                        hasIsin("US3026352068"), hasWkn("A2P6TH"), hasTicker(null), //
                        hasName("FS KKR Capital Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-28T00:00"), hasExDate(null), //
                        hasShares(42.27894), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.44), hasGrossValue("EUR", 1.94), //
                        hasForexGrossValue("USD", 2.11), //
                        hasTaxes("EUR", 0.29 + 0.19 + 0.01 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23WithSecurityInEUR()
    {
        var security = new Security("FS KKR Capital Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US3026352068");
        security.setWkn("A2P6TH");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

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
                        hasDate("2024-02-28T00:00"), hasExDate(null), //
                        hasShares(42.27894), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.44), hasGrossValue("EUR", 1.94), //
                        hasTaxes("EUR", 0.29 + 0.19 + 0.01 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

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
                        hasIsin("US7561581015"), hasWkn("A0YCXM"), hasTicker(null), //
                        hasName("Reaves Utility Income Fund Reg. Shs of Benef. Int. DL-,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-29T00:00"), hasExDate(null), //
                        hasShares(70.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.38), hasGrossValue("EUR", 12.22), //
                        hasForexGrossValue("USD", 13.30), //
                        hasTaxes("EUR", 1.84), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24WithSecurityInEUR()
    {
        var security = new Security("FS KKR Capital Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US7561581015");
        security.setWkn("A0YCXM");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

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
                        hasDate("2024-02-29T00:00"), hasExDate(null), //
                        hasShares(70.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.38), hasGrossValue("EUR", 12.22), //
                        hasTaxes("EUR", 1.84), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende25()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

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
                        hasName("Xtr.(IE) - MSCI World Registered Shares 1D o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-03-07T00:00"), hasExDate(null), //
                        hasShares(80.43549), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 41.99), hasGrossValue("EUR", 41.99), //
                        hasForexGrossValue("USD", 46.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende25WithSecurityInEUR()
    {
        var security = new Security("Xtr.(IE) - MSCI World Registered Shares 1D o.N.", "EUR");
        security.setIsin("IE00BK1PV551");
        security.setWkn("A1XEY2");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

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
                        hasDate("2024-03-07T00:00"), hasExDate(null), //
                        hasShares(80.43549), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 41.99), hasGrossValue("EUR", 41.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende26()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende26.txt"), errors);

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
                        hasIsin("XS2901885041"), hasWkn("A4A52W"), hasTicker(null), //
                        hasName("Leverage Shares PLC ETP 25.09.74 IS Alphabet"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-10T00:00"), hasExDate(null), //
                        hasShares(1000.00), //
                        hasSource("Dividende26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 70.84), hasGrossValue("EUR", 96.21), //
                        hasForexGrossValue("USD", 109.30), //
                        hasTaxes("EUR", 24.05 + 1.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende26WithSecurityInEUR()
    {
        var security = new Security("Leverage Shares PLC ETP 25.09.74 IS Alphabet", "EUR");
        security.setIsin("XS2901885041");
        security.setWkn("A4A52W");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende26.txt"), errors);

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
                        hasDate("2025-04-10T00:00"), hasExDate(null), //
                        hasShares(1000.00), //
                        hasSource("Dividende26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 70.84), hasGrossValue("EUR", 96.21), //
                        hasTaxes("EUR", 24.05 + 1.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende27()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende27.txt"), errors);

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
                        hasIsin("DE000A0F5UF5"), hasWkn("A0F5UF"), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-16T00:00"), hasExDate(null), //
                        hasShares(279.00), //
                        hasSource("Dividende27.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 69.68), hasGrossValue("EUR", 69.68), //
                        hasForexGrossValue("USD", 80.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende27WithSecurityInEUR()
    {
        var security = new Security("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile", "EUR");
        security.setIsin("DE000A0F5UF5");
        security.setWkn("A0F5UF");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende27.txt"), errors);

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
                        hasDate("2025-06-16T00:00"), hasExDate(null), //
                        hasShares(279.00), //
                        hasSource("Dividende27.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 69.68), hasGrossValue("EUR", 69.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende28.txt"), errors);

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
                        hasIsin("US4370761029"), hasWkn("866953"), hasTicker(null), //
                        hasName("HOME DEPOT INC., THE Registered Shares DL -,05"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-18T00:00"), hasExDate(null), //
                        hasShares(5.76152), //
                        hasSource("Dividende28.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 11.47), //
                        hasForexGrossValue("USD", 13.25), //
                        hasTaxes("EUR", 1.72), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28WithSecurityInEUR()
    {
        var security = new Security("HOME DEPOT INC., THE Registered Shares DL -,05", "EUR");
        security.setIsin("US4370761029");
        security.setWkn("866953");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende28.txt"), errors);

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
                        hasDate("2025-06-18T00:00"), hasExDate(null), //
                        hasShares(5.76152), //
                        hasSource("Dividende28.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 11.47), //
                        hasTaxes("EUR", 1.72), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende29.txt"), errors);

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
                        hasIsin("US7475251036"), hasWkn("883121"), hasTicker(null), //
                        hasName("QUALCOMM INC. Registered Shares DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-26T00:00"), hasExDate(null), //
                        hasShares(2.80486), //
                        hasSource("Dividende29.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.81), hasGrossValue("EUR", 2.13), //
                        hasForexGrossValue("USD", 2.50), //
                        hasTaxes("EUR", 0.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29WithSecurityInEUR()
    {
        var security = new Security("QUALCOMM INC. Registered Shares DL -,0001", "EUR");
        security.setIsin("US7475251036");
        security.setWkn("883121");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende29.txt"), errors);

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
                        hasDate("2025-06-26T00:00"), hasExDate(null), //
                        hasShares(2.80486), //
                        hasSource("Dividende29.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.81), hasGrossValue("EUR", 2.13), //
                        hasTaxes("EUR", 0.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende30()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende30.txt"), errors);

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
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("PROCTER & GAMBLE CO., THE Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-11-17T00:00"), hasExDate(null), //
                        hasShares(100.00), //
                        hasSource("Dividende30.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 78.68), hasGrossValue("USD", 105.68), //
                        hasTaxes("USD", 27.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende30WithSecurityInEUR()
    {
        var security = new Security("PROCTER & GAMBLE CO., THE Registered Shares o.N.", "EUR");
        security.setIsin("US7427181091");
        security.setWkn("852062");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ConsorsbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-11-17T00:00"), hasExDate(null), //
                        hasShares(100.00), //
                        hasSource("Dividende30.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 78.68), hasGrossValue("USD", 105.68), //
                        hasForexGrossValue("EUR", 90.99), //
                        hasTaxes("USD", 27.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testStornoDividende01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

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
                        hasIsin("DE0009802306"), hasWkn("980230"), hasTicker(null), //
                        hasName("SEB IMMOINVEST Inhaber-Anteile P"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-01-27T00:00"), hasExDate(null), //
                                        hasShares(230.00), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 145.90), hasGrossValue("EUR", 190.90), //
                                        hasTaxes("EUR", 39.31 + 2.16 + 3.53), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "NachtraeglicheVerlustverrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-07-10T00:00"), hasShares(0.00), //
                        hasSource("NachtraeglicheVerlustverrechnung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 90.61), hasGrossValue("EUR", 90.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "NachtraeglicheVerlustverrechnung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2017-07-10T00:00"), hasShares(0.00), //
                        hasSource("NachtraeglicheVerlustverrechnung02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUebertragEingang01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UebertragEingang01.txt"), errors);

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
                        hasIsin(null), hasWkn("510440"), hasTicker(null), //
                        hasName("ATOSS SOFTWARE AG INHABER-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2022-04-22T00:00"), hasShares(333.00000), //
                                        hasSource("UebertragEingang01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUebertragEingang02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UebertragEingang02.txt"), errors);

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
                        hasIsin(null), hasWkn("ETF018"), hasTicker(null), //
                        hasName("Amu.Idx Sol.Amu.MSCI Wld III Act.Nom. U.ETF USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2023-04-26T00:00"), hasShares(20.37254), //
                                        hasSource("UebertragEingang02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUebertragAusgang01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UebertragAusgang01.txt"), errors);

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
                        hasIsin(null), hasWkn("A110V7"), hasTicker(null), //
                        hasName("Weibo Corp. R.Sh.Cl.A(sp.ADRs)/1 DL-,00025"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2023-12-22T00:00"), hasShares(300.00000), //
                                        hasSource("UebertragAusgang01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUebertragAusgang02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UebertragAusgang02.txt"), errors);

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
                        hasIsin(null), hasWkn("ETF110"), hasTicker(null), //
                        hasName("Lyxor MSCI World UCITS ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2023-04-26T00:00"), hasShares(20.37254), //
                                        hasSource("UebertragAusgang02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testAnschaffung01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Anschaffung01.txt"), errors);

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
                        hasIsin("DE0005104400"), hasWkn("510440"), hasTicker(null), //
                        hasName("ATOSS SOFTWARE AG"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2007-03-22T00:00"), hasShares(333.00000), //
                                        hasSource("Anschaffung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 1261.51), hasGrossValue("EUR", 1261.51), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check transactions
        assertThat(results, hasItem(deposit(hasDate("2012-08-21"), hasAmount("EUR", 6500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2012-08-22"), hasAmount("EUR", 5500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2012-08-22"), hasAmount("EUR", 757.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2012-08-22"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2012-08-22"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2012-08-31"), hasAmount("EUR", 2358.20), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2012-09-03"), hasAmount("EUR", 4900.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("D-Gutschrift"))));

        assertThat(results, hasItem(removal(hasDate("2012-08-21"), hasAmount("EUR", 25308.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(removal(hasDate("2012-08-23"), hasAmount("EUR", 12358.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transactions
        assertThat(results, hasItem(deposit(hasDate("2020-08-10"), hasAmount("EUR", 15000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Euro-Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2020-08-11"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Euro-Überweisung"))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transactions
        assertThat(results, hasItem(removal(hasDate("2021-12-21"), hasAmount("EUR", 6000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Euro-Überweisung"))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-12-31"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.22), hasGrossValue("EUR", 1.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check transactions
        assertThat(results, hasItem(deposit(hasDate("2015-05-20"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Euro-Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2015-05-20"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Euro-Überweisung"))));

        assertThat(results, hasItem(removal(hasDate("2015-05-07"), hasAmount("EUR", 35000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Euro-Überweisung"))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transactions
        assertThat(results, hasItem(deposit(hasDate("2016-12-02"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("D-Gutschrift"))));

        assertThat(results, hasItem(removal(hasDate("2016-12-12"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("DAUERAUFTRAG"))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-03-31"), hasShares(0.00), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 192.58), hasGrossValue("EUR", 261.56), //
                        hasTaxes("EUR", 68.98), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-06"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-14"), hasAmount("EUR", 19850.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-27"), hasAmount("EUR", 9500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-28"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-09-30"), hasShares(0.00), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 62.01), hasGrossValue("EUR", 84.21), //
                        hasTaxes("EUR", 22.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoabschluss01()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoabschluss01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-03-31"), hasShares(0.00), //
                        hasSource("Kontoabschluss01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 192.58), hasGrossValue("EUR", 261.56), //
                        hasTaxes("EUR", 68.98), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoabschluss02()
    {
        var extractor = new ConsorsbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoabschluss02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-09-30"), hasShares(0.00), //
                        hasSource("Kontoabschluss02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 62.01), hasGrossValue("EUR", 84.21), //
                        hasTaxes("EUR", 22.20), hasFees("EUR", 0.00))));
    }
}
