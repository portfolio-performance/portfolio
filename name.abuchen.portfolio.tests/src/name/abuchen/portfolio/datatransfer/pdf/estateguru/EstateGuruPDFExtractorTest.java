package name.abuchen.portfolio.datatransfer.pdf.estateguru;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.EstateGuruPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class EstateGuruPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-07-25T09:10"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-15T12:41"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-02-02T00:10"), hasAmount("EUR", 51.70), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-06-21T03:00"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 0.42), hasGrossValue("EUR", 0.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-05-02T03:00"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Strafe"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-04-10T00:00"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Entschädigung"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-04-05T00:34"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Vermögensverwaltungsgebühr"), //
                        hasAmount("EUR", 0.61), hasGrossValue("EUR", 0.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-11T03:00"), hasAmount("EUR", 5.41), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-22T03:00"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-09-28T03:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 0.42), hasGrossValue("EUR", 0.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-09-27T03:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-09-06T03:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 1.12), hasGrossValue("EUR", 1.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-08-30T03:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-08-28T03:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 0.46), hasGrossValue("EUR", 0.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-09-05T00:24"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("AUM"), //
                        hasAmount("EUR", 0.24), hasGrossValue("EUR", 0.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-15T03:00"), hasAmount("EUR", 5.67), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-06T03:00"), hasAmount("EUR", 5.82), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-02T12:57"), hasAmount("EUR", 13.54), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-03T09:10"), hasAmount("EUR", 30.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-20T19:23"), hasAmount("EUR", 31.28), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-01-03T09:07"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-20T16:54"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-04T15:16"), hasAmount("EUR", 273.30), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-28T03:00"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 0.46), hasGrossValue("EUR", 0.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-06T03:00"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 0.13), hasGrossValue("EUR", 0.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-11-01T00:00"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Indemnity"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-09-20T19:23"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Secondary Market Profit"), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-10T03:00"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Penalty"), //
                        hasAmount("EUR", 0.36), hasGrossValue("EUR", 0.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-15T03:00"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Interest"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-04T15:16"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Withdraw fee"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-02T12:57"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Sale fee"), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-09-20T19:23"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Sale fee"), //
                        hasAmount("EUR", 0.95), hasGrossValue("EUR", 0.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-10-01T03:00"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-10-01T03:00"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 0.59), hasGrossValue("EUR", 0.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-10-05T00:29"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Vermögensverwaltungsgebühr"), //
                        hasAmount("EUR", 0.24), hasGrossValue("EUR", 0.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(18L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(18));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-15T03:00"), hasAmount("EUR", 5.67), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-02T12:57"), hasAmount("EUR", 13.54), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-03T09:10"), hasAmount("EUR", 30.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-20T19:23"), hasAmount("EUR", 31.28), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-01-03T09:07"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-20T16:54"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-04T15:16"), hasAmount("EUR", 273.30), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-10-03T15:31"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-28T03:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 0.46), hasGrossValue("EUR", 0.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-08T03:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 0.48), hasGrossValue("EUR", 0.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-06T03:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 0.13), hasGrossValue("EUR", 0.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-11-01T00:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Entschädigung"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-09-20T19:23"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Gewinn auf dem Zweitmarkt"), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-11-29T03:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Strafe"), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-15T03:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-04T15:16"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Abhebegebühr"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-02T12:57"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Verkaufsgebühr"), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-09-20T19:23"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Verkaufsgebühr"), //
                        hasAmount("EUR", 0.95), hasGrossValue("EUR", 0.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(859L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(859));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-10-30T03:00"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-23T09:30"), hasAmount("EUR", 64.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-07-04T09:08"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-03-14T03:00"), hasAmount("EUR", 0.38), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-11T19:01"), hasAmount("EUR", 51.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-10T16:21"), hasAmount("EUR", 53.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-25T18:39"), hasAmount("EUR", 64.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-10-30T03:00"), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Zins"), //
                        hasAmount("EUR", 0.63), hasGrossValue("EUR", 0.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-11-28T03:00"), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Entschädigung"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-11-28T03:00"), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Strafe"), //
                        hasAmount("EUR", 0.16), hasGrossValue("EUR", 0.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-12-05T00:30"), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Vermögensverwaltungsgebühr"), //
                        hasAmount("EUR", 1.07), hasGrossValue("EUR", 1.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
