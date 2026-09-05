package name.abuchen.portfolio.datatransfer.pdf.gbm;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.GBMPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class GBMPDFExtractorTest
{
    @Test
    public void testEstadoDeCuenta01()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("GBMF2BF"), //
                        hasName("GBMF2 BF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FUNO11"), //
                        hasName("FUNO 11"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("TERRA13"), //
                        hasName("TERRA 13"), //
                        hasCurrencyCode("MXN"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote("Folio: 52898941"), //
                        hasAmount("MXN", 74.32), hasGrossValue("MXN", 74.32), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-05-10T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote("Folio: 1671315"), //
                        hasAmount("MXN", 33.33), hasGrossValue("MXN", 47.61), //
                        hasTaxes("MXN", 14.28), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-05-10T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote("Folio: 1689408"), //
                        hasAmount("MXN", 24.63), hasGrossValue("MXN", 24.63), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-10T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote("Folio: 53698248"), //
                        hasAmount("MXN", 74.33), hasGrossValue("MXN", 74.33), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-05-13T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote("Folio: 1737618"), //
                        hasAmount("MXN", 68.75), hasGrossValue("MXN", 98.22), //
                        hasTaxes("MXN", 29.47), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-13T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta01.txt"), //
                        hasNote("Folio: 54290716"), //
                        hasAmount("MXN", 74.34), hasGrossValue("MXN", 74.34), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta02()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("GBMF2BF"), //
                        hasName("GBMF2 BF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VWO"), //
                        hasName("VWO"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEA"), //
                        hasName("VEA"), //
                        hasCurrencyCode("MXN"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-24T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta02.txt"), //
                        hasNote("Folio: 30895065"), //
                        hasAmount("MXN", 17.79), hasGrossValue("MXN", 19.77), //
                        hasTaxes("MXN", 1.98), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-24T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta02.txt"), //
                        hasNote("Folio: 30944223"), //
                        hasAmount("MXN", 78.79), hasGrossValue("MXN", 87.54), //
                        hasTaxes("MXN", 8.75), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-24T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta02.txt"), //
                        hasNote("Folio: 61475171"), //
                        hasAmount("MXN", 74.45), hasGrossValue("MXN", 74.45), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta03()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("GBMF2BF"), //
                        hasName("GBMF2 BF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VOO"), //
                        hasName("VOO"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FIBRAPL14"), //
                        hasName("FIBRAPL 14"), //
                        hasCurrencyCode("MXN"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta03.txt"), //
                        hasNote("Folio: 31968894"), //
                        hasAmount("MXN", 67.04), hasGrossValue("MXN", 74.49), //
                        hasTaxes("MXN", 7.45), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-02T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta03.txt"), //
                        hasNote("Folio: 63309965"), //
                        hasAmount("MXN", 74.46), hasGrossValue("MXN", 74.46), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-30T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta03.txt"), //
                        hasNote("Folio: 1951276"), //
                        hasAmount("MXN", 81.47), hasGrossValue("MXN", 116.39), //
                        hasTaxes("MXN", 34.92), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-30T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta03.txt"), //
                        hasNote("Folio: 69843400"), //
                        hasAmount("MXN", 74.54), hasGrossValue("MXN", 74.54), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta04()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("GBMF2BF"), //
                        hasName("GBMF2 BF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("TERRA13"), //
                        hasName("TERRA 13"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FUNO11"), //
                        hasName("FUNO 11"), //
                        hasCurrencyCode("MXN"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-05T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta04.txt"), //
                        hasNote("Folio: 2019312"), //
                        hasAmount("MXN", 52.45), hasGrossValue("MXN", 74.93), //
                        hasTaxes("MXN", 22.48), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-05T00:00"), hasShares(2.00), //
                        hasSource("EstadoDeCuenta04.txt"), //
                        hasNote("Folio: 71302078"), //
                        hasAmount("MXN", 74.56), hasGrossValue("MXN", 74.56), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-09T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta04.txt"), //
                        hasNote("Folio: 2082726"), //
                        hasAmount("MXN", 33.66), hasGrossValue("MXN", 48.08), //
                        hasTaxes("MXN", 14.42), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-09T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta04.txt"), //
                        hasNote("Folio: 2105960"), //
                        hasAmount("MXN", 24.77), hasGrossValue("MXN", 24.77), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-09T00:00"), hasShares(1.00), //
                        hasSource("EstadoDeCuenta04.txt"), //
                        hasNote("Folio: 71993813"), //
                        hasAmount("MXN", 37.28), hasGrossValue("MXN", 37.28), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta05()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(39L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(45));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VOO"), //
                        hasName("VOO"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FIBRAPL14"), //
                        hasName("FIBRAPL 14"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LD"), //
                        hasName("LD"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BI"), //
                        hasName("BI"), //
                        hasCurrencyCode("MXN"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-10-04T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 43745715"), //
                        hasAmount("MXN", 67.53), hasGrossValue("MXN", 75.03), //
                        hasTaxes("MXN", 7.50), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-04T00:00"), hasShares(1.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 90565309 | LD 260806"), //
                        hasAmount("MXN", 99.41), hasGrossValue("MXN", 99.41), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-05T00:00"), hasShares(1.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 90565310 | LD 260806"), //
                        hasAmount("MXN", 99.41), hasGrossValue("MXN", 99.41), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-05T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 91202734 | BI 211104"), //
                        hasAmount("MXN", 159.36), hasGrossValue("MXN", 159.36), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-06T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 91202735 | BI 211104"), //
                        hasAmount("MXN", 159.36), hasGrossValue("MXN", 159.36), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-06T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 91873329 | BI 211104"), //
                        hasAmount("MXN", 159.38), hasGrossValue("MXN", 159.38), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-07T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 91873330 | BI 211104"), //
                        hasAmount("MXN", 159.38), hasGrossValue("MXN", 159.38), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-07T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 92483164 | BI 211021"), //
                        hasAmount("MXN", 159.70), hasGrossValue("MXN", 159.70), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-08T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 92483165 | BI 211021"), //
                        hasAmount("MXN", 159.70), hasGrossValue("MXN", 159.70), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-08T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 93153109 | BI 211104"), //
                        hasAmount("MXN", 159.41), hasGrossValue("MXN", 159.41), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-11T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 93153110 | BI 211104"), //
                        hasAmount("MXN", 159.41), hasGrossValue("MXN", 159.42), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-11T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 93845019 | BI 211118"), //
                        hasAmount("MXN", 159.17), hasGrossValue("MXN", 159.17), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-12T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 93845020 | BI 211118"), //
                        hasAmount("MXN", 159.17), hasGrossValue("MXN", 159.17), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-12T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 94493105 | BI 211021"), //
                        hasAmount("MXN", 159.81), hasGrossValue("MXN", 159.81), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-13T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 94493106 | BI 211021"), //
                        hasAmount("MXN", 159.81), hasGrossValue("MXN", 159.81), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-13T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 95167241 | BI 211021"), //
                        hasAmount("MXN", 159.83), hasGrossValue("MXN", 159.83), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-14T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 95167242 | BI 211021"), //
                        hasAmount("MXN", 159.83), hasGrossValue("MXN", 159.83), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-14T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 95819374 | BI 211216"), //
                        hasAmount("MXN", 158.48), hasGrossValue("MXN", 158.48), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-15T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 95819375 | BI 211216"), //
                        hasAmount("MXN", 158.48), hasGrossValue("MXN", 158.48), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-15T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 96461059 | BI 220113"), //
                        hasAmount("MXN", 157.93), hasGrossValue("MXN", 157.93), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-18T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 96461060 | BI 220113"), //
                        hasAmount("MXN", 157.93), hasGrossValue("MXN", 157.94), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-18T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 97244055 | BI 211104"), //
                        hasAmount("MXN", 159.64), hasGrossValue("MXN", 159.64), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-19T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 97244056 | BI 211104"), //
                        hasAmount("MXN", 159.64), hasGrossValue("MXN", 159.64), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-19T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 97922882 | BI 211104"), //
                        hasAmount("MXN", 159.66), hasGrossValue("MXN", 159.66), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-20T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 97922883 | BI 211104"), //
                        hasAmount("MXN", 159.66), hasGrossValue("MXN", 159.66), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-20T00:00"), hasShares(1.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 98577403 | LD 250220"), //
                        hasAmount("MXN", 99.60), hasGrossValue("MXN", 99.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-21T00:00"), hasShares(1.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 98577404 | LD 250220"), //
                        hasAmount("MXN", 99.60), hasGrossValue("MXN", 99.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-21T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 99294446 | BI 211118"), //
                        hasAmount("MXN", 159.37), hasGrossValue("MXN", 159.37), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-22T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 99294447 | BI 211118"), //
                        hasAmount("MXN", 159.37), hasGrossValue("MXN", 159.37), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-22T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 99977916 | BI 211118"), //
                        hasAmount("MXN", 159.38), hasGrossValue("MXN", 159.38), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-25T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 99977917 | BI 211118"), //
                        hasAmount("MXN", 159.38), hasGrossValue("MXN", 159.39), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-25T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 100769386 | BI 211104"), //
                        hasAmount("MXN", 159.78), hasGrossValue("MXN", 159.78), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-26T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 100769387 | BI 211104"), //
                        hasAmount("MXN", 159.78), hasGrossValue("MXN", 159.78), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-26T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 101449913 | BI 211104"), //
                        hasAmount("MXN", 159.79), hasGrossValue("MXN", 159.79), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-27T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 101449914 | BI 211104"), //
                        hasAmount("MXN", 159.79), hasGrossValue("MXN", 159.79), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-27T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 102109415 | BI 211118"), //
                        hasAmount("MXN", 159.52), hasGrossValue("MXN", 159.52), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-10-29T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 2336614"), //
                        hasAmount("MXN", 82.90), hasGrossValue("MXN", 118.43), //
                        hasTaxes("MXN", 35.53), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-28T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 102109416 | BI 211118"), //
                        hasAmount("MXN", 159.52), hasGrossValue("MXN", 159.52), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-28T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 102863706 | BI 211104"), //
                        hasAmount("MXN", 159.85), hasGrossValue("MXN", 159.85), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-29T00:00"), hasShares(16.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 102863707 | BI 211104"), //
                        hasAmount("MXN", 159.85), hasGrossValue("MXN", 159.85), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-29T00:00"), hasShares(24.00), //
                        hasSource("EstadoDeCuenta05.txt"), //
                        hasNote("Folio: 103612880 | BI 211104"), //
                        hasAmount("MXN", 239.80), hasGrossValue("MXN", 239.80), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta06()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(38L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(42));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BI"), //
                        hasName("BI"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LF"), //
                        hasName("LF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FIBRAPL14"), //
                        hasName("FIBRAPL 14"), //
                        hasCurrencyCode("MXN"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-01T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 216267162 | BI 220630"), //
                        hasAmount("MXN", 962.96), hasGrossValue("MXN", 962.96), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 217586377 | BI 220714"), //
                        hasAmount("MXN", 960.24), hasGrossValue("MXN", 960.24), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-04T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 217586378 | BI 220714"), //
                        hasAmount("MXN", 960.25), hasGrossValue("MXN", 960.26), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 218838476 | BI 220714"), //
                        hasAmount("MXN", 960.75), hasGrossValue("MXN", 960.75), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-05T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 218838477 | BI 220714"), //
                        hasAmount("MXN", 960.75), hasGrossValue("MXN", 960.75), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-05T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 220321857 | BI 220714"), //
                        hasAmount("MXN", 960.89), hasGrossValue("MXN", 960.89), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-06T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 220321858 | BI 220714"), //
                        hasAmount("MXN", 960.89), hasGrossValue("MXN", 960.89), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-06T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 221420211 | BI 220630"), //
                        hasAmount("MXN", 963.52), hasGrossValue("MXN", 963.52), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-07T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 221420212 | BI 220630"), //
                        hasAmount("MXN", 963.52), hasGrossValue("MXN", 963.52), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-07T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 222503899 | BI 220714"), //
                        hasAmount("MXN", 961.20), hasGrossValue("MXN", 961.20), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-08T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 222503900 | BI 220714"), //
                        hasAmount("MXN", 961.20), hasGrossValue("MXN", 961.20), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-08T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 224157425 | BI 220630"), //
                        hasAmount("MXN", 963.76), hasGrossValue("MXN", 963.76), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-11T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 224157426 | BI 220630"), //
                        hasAmount("MXN", 963.76), hasGrossValue("MXN", 963.77), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-11T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 225609330 | BI 220630"), //
                        hasAmount("MXN", 964.08), hasGrossValue("MXN", 964.08), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-12T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 225609331 | BI 220630"), //
                        hasAmount("MXN", 964.08), hasGrossValue("MXN", 964.08), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-12T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 226902269 | BI 220630"), //
                        hasAmount("MXN", 964.17), hasGrossValue("MXN", 964.17), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-13T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 226902270 | BI 220630"), //
                        hasAmount("MXN", 964.17), hasGrossValue("MXN", 964.17), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-13T00:00"), hasShares(9.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 228191322 | LF 231130"), //
                        hasAmount("MXN", 900.72), hasGrossValue("MXN", 900.72), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-18T00:00"), hasShares(9.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 228191323 | LF 231130"), //
                        hasAmount("MXN", 900.73), hasGrossValue("MXN", 900.74), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-18T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 229763173 | BI 220630"), //
                        hasAmount("MXN", 966.28), hasGrossValue("MXN", 966.28), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-19T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 229763174 | BI 220630"), //
                        hasAmount("MXN", 966.28), hasGrossValue("MXN", 966.28), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-19T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 231014376 | BI 220630"), //
                        hasAmount("MXN", 966.44), hasGrossValue("MXN", 966.44), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-20T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 231014377 | BI 220630"), //
                        hasAmount("MXN", 966.44), hasGrossValue("MXN", 966.44), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-20T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 232338158 | BI 220630"), //
                        hasAmount("MXN", 966.49), hasGrossValue("MXN", 966.49), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-21T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 232338159 | BI 220630"), //
                        hasAmount("MXN", 966.49), hasGrossValue("MXN", 966.49), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-21T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 233672242 | BI 220630"), //
                        hasAmount("MXN", 966.60), hasGrossValue("MXN", 966.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-22T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 233672243 | BI 220630"), //
                        hasAmount("MXN", 966.60), hasGrossValue("MXN", 966.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-22T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 234626652 | BI 220714"), //
                        hasAmount("MXN", 963.67), hasGrossValue("MXN", 963.67), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-25T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 234626653 | BI 220714"), //
                        hasAmount("MXN", 963.67), hasGrossValue("MXN", 963.68), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-25T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 236416169 | BI 220630"), //
                        hasAmount("MXN", 966.96), hasGrossValue("MXN", 966.96), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-26T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 236416170 | BI 220630"), //
                        hasAmount("MXN", 966.96), hasGrossValue("MXN", 966.96), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-26T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 237723471 | BI 220714"), //
                        hasAmount("MXN", 964.33), hasGrossValue("MXN", 964.33), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-27T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 237723472 | BI 220714"), //
                        hasAmount("MXN", 964.33), hasGrossValue("MXN", 964.33), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-27T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 239018619 | BI 220630"), //
                        hasAmount("MXN", 967.22), hasGrossValue("MXN", 967.22), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-29T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 3978837"), //
                        hasAmount("MXN", 90.25), hasGrossValue("MXN", 128.93), //
                        hasTaxes("MXN", 38.68), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-28T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 239018620 | BI 220630"), //
                        hasAmount("MXN", 967.22), hasGrossValue("MXN", 967.22), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-28T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 240225812 | BI 220630"), //
                        hasAmount("MXN", 967.36), hasGrossValue("MXN", 967.36), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-29T00:00"), hasShares(98.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 240225813 | BI 220630"), //
                        hasAmount("MXN", 967.37), hasGrossValue("MXN", 967.37), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-29T00:00"), hasShares(107.00), //
                        hasSource("EstadoDeCuenta06.txt"), //
                        hasNote("Folio: 241220673 | BI 220630"), //
                        hasAmount("MXN", 1056.36), hasGrossValue("MXN", 1056.36), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta07()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(44L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(50));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BI"), //
                        hasName("BI"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LF"), //
                        hasName("LF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VWO"), //
                        hasName("VWO"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEA"), //
                        hasName("VEA"), //
                        hasCurrencyCode("MXN"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-01T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 271651468 | BI 220804"), //
                        hasAmount("MXN", 1193.31), hasGrossValue("MXN", 1193.31), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-01T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 273456781 | BI 220804"), //
                        hasAmount("MXN", 1193.49), hasGrossValue("MXN", 1193.49), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-02T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 273456782 | BI 220804"), //
                        hasAmount("MXN", 1193.49), hasGrossValue("MXN", 1193.49), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-02T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 274502835 | BI 220804"), //
                        hasAmount("MXN", 1193.66), hasGrossValue("MXN", 1193.66), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-03T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 274502836 | BI 220804"), //
                        hasAmount("MXN", 1193.67), hasGrossValue("MXN", 1193.67), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-03T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 275914502 | BI 220804"), //
                        hasAmount("MXN", 1193.84), hasGrossValue("MXN", 1193.84), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-06T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 275914503 | BI 220804"), //
                        hasAmount("MXN", 1193.84), hasGrossValue("MXN", 1193.85), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-06T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 277413820 | BI 220804"), //
                        hasAmount("MXN", 1195.11), hasGrossValue("MXN", 1195.11), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-07T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 277413821 | BI 220804"), //
                        hasAmount("MXN", 1195.11), hasGrossValue("MXN", 1195.11), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-07T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 278550398 | BI 220804"), //
                        hasAmount("MXN", 1195.25), hasGrossValue("MXN", 1195.25), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-08T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 278550399 | BI 220804"), //
                        hasAmount("MXN", 1195.25), hasGrossValue("MXN", 1195.25), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-08T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 280146915 | BI 220804"), //
                        hasAmount("MXN", 1195.39), hasGrossValue("MXN", 1195.39), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-09T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 280146916 | BI 220804"), //
                        hasAmount("MXN", 1195.39), hasGrossValue("MXN", 1195.39), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-09T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 281554973 | BI 220804"), //
                        hasAmount("MXN", 1195.53), hasGrossValue("MXN", 1195.53), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-10T00:00"), hasShares(121.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 281554974 | BI 220804"), //
                        hasAmount("MXN", 1195.53), hasGrossValue("MXN", 1195.53), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-10T00:00"), hasShares(11.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 282867661 | LF 240321"), //
                        hasAmount("MXN", 1101.70), hasGrossValue("MXN", 1101.70), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-13T00:00"), hasShares(11.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 282867662 | LF 240321"), //
                        hasAmount("MXN", 1101.70), hasGrossValue("MXN", 1101.71), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 284508177 | BI 220707"), //
                        hasAmount("MXN", 1193.99), hasGrossValue("MXN", 1193.99), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-14T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 284508178 | BI 220707"), //
                        hasAmount("MXN", 1193.99), hasGrossValue("MXN", 1193.99), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-14T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 285890927 | BI 220707"), //
                        hasAmount("MXN", 1194.13), hasGrossValue("MXN", 1194.13), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-15T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 285890928 | BI 220707"), //
                        hasAmount("MXN", 1194.13), hasGrossValue("MXN", 1194.13), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-15T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 287203876 | BI 220707"), //
                        hasAmount("MXN", 1194.27), hasGrossValue("MXN", 1194.27), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-16T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 287203877 | BI 220707"), //
                        hasAmount("MXN", 1194.27), hasGrossValue("MXN", 1194.27), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-16T00:00"), hasShares(11.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 288592815 | LF 240321"), //
                        hasAmount("MXN", 1096.80), hasGrossValue("MXN", 1096.80), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-17T00:00"), hasShares(11.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 288592816 | LF 240321"), //
                        hasAmount("MXN", 1096.80), hasGrossValue("MXN", 1096.80), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-17T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 290040065 | BI 220804"), //
                        hasAmount("MXN", 1187.18), hasGrossValue("MXN", 1187.18), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-20T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 290040066 | BI 220804"), //
                        hasAmount("MXN", 1187.18), hasGrossValue("MXN", 1187.19), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-20T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 291509977 | BI 220714"), //
                        hasAmount("MXN", 1193.87), hasGrossValue("MXN", 1193.87), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-21T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 291509978 | BI 220714"), //
                        hasAmount("MXN", 1193.87), hasGrossValue("MXN", 1193.87), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-21T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 292936418 | BI 220707"), //
                        hasAmount("MXN", 1195.88), hasGrossValue("MXN", 1195.88), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-22T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 292936419 | BI 220707"), //
                        hasAmount("MXN", 1195.88), hasGrossValue("MXN", 1195.88), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-22T00:00"), hasShares(122.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 294297142 | BI 221006"), //
                        hasAmount("MXN", 1190.88), hasGrossValue("MXN", 1190.88), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-23T00:00"), hasShares(122.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 294297143 | BI 221006"), //
                        hasAmount("MXN", 1190.88), hasGrossValue("MXN", 1190.88), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-23T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 295442393 | BI 220714"), //
                        hasAmount("MXN", 1194.60), hasGrossValue("MXN", 1194.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-24T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 295442394 | BI 220714"), //
                        hasAmount("MXN", 1194.60), hasGrossValue("MXN", 1194.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-24T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 297038762 | BI 220714"), //
                        hasAmount("MXN", 1194.74), hasGrossValue("MXN", 1194.74), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-27T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 93960162"), //
                        hasAmount("MXN", 19.10), hasGrossValue("MXN", 21.22), //
                        hasTaxes("MXN", 2.12), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-27T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 94044647"), //
                        hasAmount("MXN", 99.25), hasGrossValue("MXN", 110.28), //
                        hasTaxes("MXN", 11.03), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-27T00:00"), hasShares(120.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 297038763 | BI 220714"), //
                        hasAmount("MXN", 1194.74), hasGrossValue("MXN", 1194.75), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-27T00:00"), hasShares(131.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 298412438 | BI 220714"), //
                        hasAmount("MXN", 1305.25), hasGrossValue("MXN", 1305.25), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-28T00:00"), hasShares(131.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 298412439 | BI 220714"), //
                        hasAmount("MXN", 1305.25), hasGrossValue("MXN", 1305.25), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-28T00:00"), hasShares(131.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 299883038 | BI 220714"), //
                        hasAmount("MXN", 1305.53), hasGrossValue("MXN", 1305.53), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-29T00:00"), hasShares(131.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 299883039 | BI 220714"), //
                        hasAmount("MXN", 1305.53), hasGrossValue("MXN", 1305.53), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-29T00:00"), hasShares(134.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 301274500 | BI 221006"), //
                        hasAmount("MXN", 1309.63), hasGrossValue("MXN", 1309.63), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-30T00:00"), hasShares(134.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 301274501 | BI 221006"), //
                        hasAmount("MXN", 1309.63), hasGrossValue("MXN", 1309.63), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-30T00:00"), hasShares(13.00), //
                        hasSource("EstadoDeCuenta07.txt"), //
                        hasNote("Folio: 302677408 | LF 231130"), //
                        hasAmount("MXN", 1301.18), hasGrossValue("MXN", 1301.18), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta08()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(42L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(49));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LF"), //
                        hasName("LF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VWO"), //
                        hasName("VWO"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEA"), //
                        hasName("VEA"), //
                        hasCurrencyCode("MXN"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-01T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 829472974 | LF 251023"), //
                        hasAmount("MXN", 3188.65), hasGrossValue("MXN", 3188.66), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-01T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 831576504 | LF 251023"), //
                        hasAmount("MXN", 3189.80), hasGrossValue("MXN", 3189.80), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-04T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 831576505 | LF 251023"), //
                        hasAmount("MXN", 3189.80), hasGrossValue("MXN", 3189.83), //
                        hasTaxes("MXN", 0.03), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-04T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 834027642 | LF 251023"), //
                        hasAmount("MXN", 3192.94), hasGrossValue("MXN", 3192.94), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-05T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 834027643 | LF 251023"), //
                        hasAmount("MXN", 3192.94), hasGrossValue("MXN", 3192.95), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-05T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 836114487 | LF 251023"), //
                        hasAmount("MXN", 3193.95), hasGrossValue("MXN", 3193.95), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-05T00:00"), hasAmount("MXN", 1129.94), //
                        hasSource("EstadoDeCuenta08.txt"), hasNote("Folio: 209452300"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-06T00:00"), hasShares(32.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 836114488 | LF 251023"), //
                        hasAmount("MXN", 3193.95), hasGrossValue("MXN", 3193.96), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-06T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 838184535 | LF 251023"), //
                        hasAmount("MXN", 4293.31), hasGrossValue("MXN", 4293.31), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-07T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 838184536 | LF 251023"), //
                        hasAmount("MXN", 4293.31), hasGrossValue("MXN", 4293.32), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-07T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 840233201 | LF 251023"), //
                        hasAmount("MXN", 4294.75), hasGrossValue("MXN", 4294.75), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-08T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 840233202 | LF 251023"), //
                        hasAmount("MXN", 4294.75), hasGrossValue("MXN", 4294.76), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-08T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 842311039 | LF 251023"), //
                        hasAmount("MXN", 4295.99), hasGrossValue("MXN", 4295.99), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-11T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 842311040 | LF 251023"), //
                        hasAmount("MXN", 4295.99), hasGrossValue("MXN", 4296.03), //
                        hasTaxes("MXN", 0.04), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-11T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 844687655 | LF 251002"), //
                        hasAmount("MXN", 4291.65), hasGrossValue("MXN", 4291.65), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-12T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 844687656 | LF 251002"), //
                        hasAmount("MXN", 4291.65), hasGrossValue("MXN", 4291.66), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-12T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 846762023 | LF 251002"), //
                        hasAmount("MXN", 4293.24), hasGrossValue("MXN", 4293.24), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-13T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 846762024 | LF 251002"), //
                        hasAmount("MXN", 4293.24), hasGrossValue("MXN", 4293.25), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-13T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 848778050 | LF 251002"), //
                        hasAmount("MXN", 4294.37), hasGrossValue("MXN", 4294.37), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-14T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 848778051 | LF 251002"), //
                        hasAmount("MXN", 4294.37), hasGrossValue("MXN", 4294.38), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-14T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 850803175 | LF 251002"), //
                        hasAmount("MXN", 4295.01), hasGrossValue("MXN", 4295.01), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-15T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 850803176 | LF 251002"), //
                        hasAmount("MXN", 4295.01), hasGrossValue("MXN", 4295.02), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-15T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 852937527 | LF 251002"), //
                        hasAmount("MXN", 4297.18), hasGrossValue("MXN", 4297.18), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-18T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 852937528 | LF 251002"), //
                        hasAmount("MXN", 4297.18), hasGrossValue("MXN", 4297.22), //
                        hasTaxes("MXN", 0.04), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-18T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 855326671 | LF 251204"), //
                        hasAmount("MXN", 4290.14), hasGrossValue("MXN", 4290.14), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-19T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 855326672 | LF 251204"), //
                        hasAmount("MXN", 4290.14), hasGrossValue("MXN", 4290.15), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-19T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 857410980 | LF 261001"), //
                        hasAmount("MXN", 4294.31), hasGrossValue("MXN", 4294.31), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-20T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 857410981 | LF 261001"), //
                        hasAmount("MXN", 4294.31), hasGrossValue("MXN", 4294.32), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-20T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 859755568 | LF 261001"), //
                        hasAmount("MXN", 4295.70), hasGrossValue("MXN", 4295.70), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-21T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 859755569 | LF 261001"), //
                        hasAmount("MXN", 4295.70), hasGrossValue("MXN", 4295.72), //
                        hasTaxes("MXN", 0.02), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-21T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 861802980 | LF 261001"), //
                        hasAmount("MXN", 4297.07), hasGrossValue("MXN", 4297.07), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-22T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 215400125"), //
                        hasAmount("MXN", 17.77), hasGrossValue("MXN", 19.75), //
                        hasTaxes("MXN", 1.98), hasFees("MXN", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-22T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 215478147"), //
                        hasAmount("MXN", 50.24), hasGrossValue("MXN", 55.82), //
                        hasTaxes("MXN", 5.58), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-22T00:00"), hasShares(43.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 861802981 | LF 261001"), //
                        hasAmount("MXN", 4297.06), hasGrossValue("MXN", 4297.08), //
                        hasTaxes("MXN", 0.02), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-22T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 863857115 | LF 261001"), //
                        hasAmount("MXN", 4398.40), hasGrossValue("MXN", 4398.40), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-25T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 863857116 | LF 261001"), //
                        hasAmount("MXN", 4398.38), hasGrossValue("MXN", 4398.43), //
                        hasTaxes("MXN", 0.05), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-25T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 866296196 | LF 251204"), //
                        hasAmount("MXN", 4399.60), hasGrossValue("MXN", 4399.60), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-26T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 866296197 | LF 251204"), //
                        hasAmount("MXN", 4399.59), hasGrossValue("MXN", 4399.61), //
                        hasTaxes("MXN", 0.02), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-26T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 868177685 | LF 250724"), //
                        hasAmount("MXN", 4394.77), hasGrossValue("MXN", 4394.77), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-27T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 868177686 | LF 250724"), //
                        hasAmount("MXN", 4394.77), hasGrossValue("MXN", 4394.79), //
                        hasTaxes("MXN", 0.02), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-27T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 870340731 | LF 250724"), //
                        hasAmount("MXN", 4396.16), hasGrossValue("MXN", 4396.16), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-28T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 870340732 | LF 250724"), //
                        hasAmount("MXN", 4396.16), hasGrossValue("MXN", 4396.18), //
                        hasTaxes("MXN", 0.02), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-28T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 872448886 | LF 260604"), //
                        hasAmount("MXN", 4380.48), hasGrossValue("MXN", 4380.48), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-09-29T00:00"), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 218952286"), //
                        hasAmount("MXN", 0.05), hasGrossValue("MXN", 0.05), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-29T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 872448887 | LF 260604"), //
                        hasAmount("MXN", 4380.48), hasGrossValue("MXN", 4380.49), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-29T00:00"), hasShares(44.00), //
                        hasSource("EstadoDeCuenta08.txt"), //
                        hasNote("Folio: 874470007 | LF 260604"), //
                        hasAmount("MXN", 4381.92), hasGrossValue("MXN", 4381.92), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }

    @Test
    public void testEstadoDeCuenta09()
    {
        var extractor = new GBMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(40L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(46));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FIBRAPL14"), //
                        hasName("FIBRAPL 14"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FUNO11"), //
                        hasName("FUNO 11"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LF"), //
                        hasName("LF"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LD"), //
                        hasName("LD"), //
                        hasCurrencyCode("MXN"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-09T00:00"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("EstadoDeCuenta09.txt"), //
                        hasNote("Folio: 10804616"), //
                        hasAmount("MXN", 93.29), hasGrossValue("MXN", 133.27), //
                        hasTaxes("MXN", 39.98), hasFees("MXN", 0.00))));
    }
}
