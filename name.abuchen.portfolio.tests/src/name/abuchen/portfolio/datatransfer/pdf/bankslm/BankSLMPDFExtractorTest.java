package name.abuchen.portfolio.datatransfer.pdf.bankslm;

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
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class BankSLMPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("472672"), hasTicker(null), //
                        hasName("Nokia Corp Inhaber-Aktien"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-09-03T00:00"), hasShares(17000.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 92658.45), hasGrossValue("CHF", 92031.25), //
                        hasForexGrossValue("EUR", 74120.00), //
                        hasTaxes("CHF", 138.05), hasFees("CHF", 489.15))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInEUR()
    {
        var security = new Security("Nokia Corp Inhaber-Aktien", "EUR");
        security.setWkn("472672");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BankSLMPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-09-03T00:00"), hasShares(17000.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 92658.45), hasGrossValue("CHF", 92031.25), //
                        hasTaxes("CHF", 138.05), hasFees("CHF", 489.15))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("135186"), hasTicker(null), //
                        hasName("Bank SLM AG Namen-Aktien nom CHF 100.00"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-06-21T00:00"), hasShares(1.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1481.10), hasGrossValue("CHF", 1480.00), //
                        hasTaxes("CHF", 1.10), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
                        hasIsin(null), hasWkn("24476758"), hasTicker(null), //
                        hasName("UBS Group AG Namen-Aktien nom CHF 0.10"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-02-10T00:00"), hasShares(3000.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 43412.10), hasGrossValue("CHF", 43200.00), //
                        hasTaxes("CHF", 32.40), hasFees("CHF", 3.50 + 176.20))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("2489948"), hasTicker(null), //
                        hasName("UBS AG Namen-Aktien nom CHF 0.10"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-08-22T00:00"), hasShares(7798.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 142359.40), hasGrossValue("CHF", 142859.35), //
                        hasTaxes("CHF", 107.15), hasFees("CHF", 3.50 + 389.30))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("472672"), hasTicker(null), //
                        hasName("Nokia Corp Inhaber-Aktien"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-01-24T00:00"), hasShares(11500.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 43180.00), hasGrossValue("CHF", 43509.55), //
                        hasForexGrossValue("EUR", 35516.56), //
                        hasTaxes("CHF", 65.25), hasFees("CHF", 264.30))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInEUR()
    {
        var security = new Security("Nokia Corp Inhaber-Aktien", "EUR");
        security.setWkn("472672");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BankSLMPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-01-24T00:00"), hasShares(11500.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 43180.00), hasGrossValue("CHF", 43509.55), //
                        hasTaxes("CHF", 65.25), hasFees("CHF", 264.30))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0001351862"), hasWkn("135186"), hasTicker(null), //
                        hasName("Bank SLM AG Namen-Aktien nom CHF 100.00"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-05-02T00:00"), hasExDate("2016-05-02T00:00"), //
                        hasShares(1.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 18.20), hasGrossValue("CHF", 28.00), //
                        hasTaxes("CHF", 9.80), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new BankSLMPDFExtractor(new Client());

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
                        hasIsin("FI0009000681"), hasWkn("472672"), hasTicker(null), //
                        hasName("Nokia Corp Inhaber-Aktien"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-07-05T00:00"), hasExDate("2016-06-17T00:00"), //
                        hasShares(17000.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3094.00), hasGrossValue("EUR", 4420.00), //
                        hasTaxes("EUR", 884.00 + 442.00), hasFees("EUR", 0.00))));
    }
}
