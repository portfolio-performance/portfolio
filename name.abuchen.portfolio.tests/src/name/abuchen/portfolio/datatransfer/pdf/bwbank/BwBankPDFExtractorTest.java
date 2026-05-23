package name.abuchen.portfolio.datatransfer.pdf.bwbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
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
import name.abuchen.portfolio.datatransfer.pdf.BwBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BwBankPDFExtractorTest
{
    @Test
    public void testKauf01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

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

        assertThat(results, hasItem(security( //
                        hasIsin("DE0007100000"), //
                        hasName("Daimler AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-24T17:26"), hasShares(10.00), //
                        hasSource("Kauf01.txt"), hasNote("Ordernummer 726V907"), //
                        hasAmount("EUR", 353.35), hasGrossValue("EUR", 348.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.30))));
    }

    @Test
    public void testVerkauf01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

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

        assertThat(results, hasItem(security( //
                        hasIsin("DE0007100000"), //
                        hasName("Daimler AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-24T17:26"), hasShares(10.00), //
                        hasSource("Verkauf01.txt"), hasNote("Ordernummer 726V908"), //
                        hasAmount("EUR", 353.35), hasTaxes("EUR", 2.93), hasFees("EUR", 5.30))));
    }

    @Test
    public void testVLSparplan01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VLSparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(security( //
                        hasIsin("LU1508359509"), //
                        hasName("Deka-Industrie 4.0"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-29T00:00"), hasShares(0.3110), //
                        hasSource("VLSparplan01.txt"), hasNote("VL-Fondssparplan Valuta 31.08.2018"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAusschuettung01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ausschuettung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(security( //
                        hasIsin("LU1508359509"), //
                        hasName("Deka-Industrie 4.0 Inhaber-Anteile CF o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2018-09-07T00:00"), hasShares(2.8820), //
                        hasSource("Ausschuettung01.txt"), //
                        hasAmount("EUR", 1.79), hasGrossValue("EUR", 2.19), //
                        hasTaxes("EUR", 0.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

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

        assertThat(results, hasItem(security( //
                        hasIsin("DE0007100000"), //
                        hasName("Daimler AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-13T00:00"), hasShares(10.00), //
                        hasSource("Dividende01.txt"), //
                        hasAmount("EUR", 6.63), hasGrossValue("EUR", 9.00), //
                        hasTaxes("EUR", 2.37), hasFees("EUR", 0.00))));
    }

    @Test
    public void testBestandsuebersicht01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Bestandsuebersicht01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(fee( //
                        hasDate("2020-10-15T00:00"), //
                        hasSource("Bestandsuebersicht01.txt"), hasNote("Depotpreis"), //
                        hasAmount("EUR", 144.94))));

        assertThat(results, hasItem(feeRefund( //
                        hasDate("2020-10-15T00:00"), //
                        hasSource("Bestandsuebersicht01.txt"), hasNote("Provisionsvergütung"), //
                        hasAmount("EUR", 0.52))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new BwBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(fee( //
                        hasDate("2022-01-01T00:00"), //
                        hasSource("Depotauszug01.txt"), hasNote("Depotpreis 2021"), //
                        hasAmount("EUR", 8.93))));
    }
}
