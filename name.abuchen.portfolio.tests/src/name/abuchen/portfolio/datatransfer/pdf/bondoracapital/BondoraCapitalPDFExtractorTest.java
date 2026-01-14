package name.abuchen.portfolio.datatransfer.pdf.bondoracapital;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BondoraCapitalPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BondoraCapitalPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(211L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(211));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-15"), hasAmount("EUR", 100.00),
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-01-16"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-08-07"), hasAmount("EUR", 203.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Abheben"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

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

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2020-09-04"), //
                        hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-09-05"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-09-06"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-09-07"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-10-25"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-10-26"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.01), hasGrossValue("EUR", 1.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 300.00),
                        hasSource("Kontoauszug03.txt"), hasNote("Überweisen"))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-11-18"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Go & Grow returns"), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-11-19"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Go & Grow returns"), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-20"), //
                        hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Withdrawal"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-11-20"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Go & Grow returns"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

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

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2020-09-07"), //
                        hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-09-08"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-09-09"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2020-10-05"), //
                        hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2020-10-06"), //
                        hasAmount("EUR", 4.91), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-10-06"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-10-31"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2021-12-25"), //
                        hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2021-12-27"), //
                        hasAmount("EUR", 15.00), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2021-12-27"), //
                        hasAmount("EUR", 980.00), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote("Überweisen"))));
    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(16L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-01-29"), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2022-02-07"), //
                        hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Überweisen"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-02-07"), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug09()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-29"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.86), hasGrossValue("EUR", 0.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2022-12-30"), //
                        hasAmount("EUR", 474.35), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Abheben"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2022-12-30"), //
                        hasAmount("EUR", 4230.27), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Abheben"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-30"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.84), hasGrossValue("EUR", 0.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2022-12-31"), //
                        hasAmount("EUR", 4204.97), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Überweisen"))));
    }

    @Test
    public void testKontoauszug10()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-19"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-20"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-21"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.63), hasGrossValue("EUR", 1.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-22"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug11()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-02"), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-03"), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-04"), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 1.63), hasGrossValue("EUR", 1.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug12()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-11"), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.68), hasGrossValue("EUR", 0.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-12"), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.68), hasGrossValue("EUR", 0.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-13"), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote("Go & Grow Zinsen"), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testKontoauszug13()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

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
        assertThat(results, hasItem(deposit( //
                        hasDate("2023-04-06"), //
                        hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote("Transfer"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-04-10"), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote("Go & Grow returns"), //
                        hasAmount("EUR", 0.85), hasGrossValue("EUR", 0.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2023-04-14"), //
                        hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote("Transfer"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-04-18"), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote("Go & Grow returns"), //
                        hasAmount("EUR", 0.87), hasGrossValue("EUR", 0.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-04-21"), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote("Go & Grow returns"), //
                        hasAmount("EUR", 0.87), hasGrossValue("EUR", 0.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug14()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-11-27"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug14.txt"), hasNote("SEPA-Banküberweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-11-28"), hasAmount("EUR", 0.19), //
                        hasSource("Kontoauszug14.txt"), hasNote("Go & Grow Zinsen"))));
    }

    @Test
    public void testKontoauszug15()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-02"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("SEPA-Banküberweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-01-02"), hasAmount("EUR", 0.49), //
                        hasSource("Kontoauszug15.txt"), hasNote("Go & Grow Zinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-01-01"), hasAmount("EUR", 0.54), //
                        hasSource("Kontoauszug15.txt"), hasNote("Go & Grow Zinsen"))));
    }

    @Test
    public void testKontoauszug16()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-27"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Abheben auf Bankkonto"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-04-28"), hasAmount("EUR", 32.45), //
                        hasSource("Kontoauszug16.txt"), hasNote("Go & Grow Zinsen"))));
    }

    @Test
    public void testKontoauszug17()
    {
        var extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug17.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-02-04"), hasAmount("EUR", 1111.00), //
                        hasSource("Kontoauszug17.txt"), hasNote("SEPA payment"))));
    }
}
