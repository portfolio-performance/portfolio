package name.abuchen.portfolio.datatransfer.pdf.renaultbankdirekt;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RenaultBankDirektPDFExtractor;
import name.abuchen.portfolio.math.NegativeValue;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class RenaultBankDirektPDFExtractorTest
{
    private NegativeValue negativeValue = new NegativeValue();

    @Test
    public void testKontoauszug01()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        extractor.setNegativeValue(negativeValue);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 4480.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungs-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 351.50), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungs-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungs-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-11-21"), hasAmount("EUR", 210.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-11-29"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-11-29"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Zinsen/Kontoführung"), //
                        hasAmount("EUR", 2.44), hasGrossValue("EUR", 2.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
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

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-02-17"), hasAmount("EUR", 210.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Dauerauftrag-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-02-28"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Zinsen/Kontoführung"), //
                        hasAmount("EUR", 3.41), hasGrossValue("EUR", 3.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
                        hasDate("2020-06-30"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Zinsen/Kontoführung"), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
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

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-17"), hasAmount("EUR", 150), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-22"), hasAmount("EUR", 7547.85), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-02-26"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("01.02.2021 bis 28.02.2021"), //
                        hasAmount("EUR", 0.52), hasGrossValue("EUR", 0.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
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
        assertThat(results, hasItem(deposit(hasDate("2021-04-19"), hasAmount("EUR", 10), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-19"), hasAmount("EUR", 10), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-04-30"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("01.04.2021 bis 30.04.2021"), //
                        hasAmount("EUR", 0.73), hasGrossValue("EUR", 0.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
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
        assertThat(results, hasItem(deposit(hasDate("2021-12-04"), hasAmount("EUR", 3200), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-12-22"), hasAmount("EUR", 5000), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-12-31"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Bonuszinsen"), //
                        hasAmount("EUR", 1.23), hasGrossValue("EUR", 1.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-12-31"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.84), hasGrossValue("EUR", 2.46), //
                        hasTaxes("EUR", 0.62), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-03"), hasAmount("EUR", 2300.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-04"), hasAmount("EUR", 2200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-13"), hasAmount("EUR", 616.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-16"), hasAmount("EUR", 7000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungs-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-17"), hasAmount("EUR", 5800.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-20"), hasAmount("EUR", 5400.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungs-Gutschrift"))));
    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2022-06-21"), hasAmount("EUR", 5000), //
                        hasSource("Kontoauszug08.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-06-10"), hasAmount("EUR", 1000), //
                        hasSource("Kontoauszug08.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-06-29"), hasAmount("EUR", 2500), //
                        hasSource("Kontoauszug08.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-06-30"), hasAmount("EUR", 2000), //
                        hasSource("Kontoauszug08.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-06-30"), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote("01.06.2022 bis 30.06.2022"), //
                        hasAmount("EUR", 0.60), hasGrossValue("EUR", 0.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug09()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-08-04"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-08-07"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-08-10"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-08-13"), hasAmount("EUR", 616.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Dauerauftrag-Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-08-31"), hasAmount("EUR", 1500), //
                        hasSource("Kontoauszug09.txt"), hasNote("Internet-Euro-Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-08-31"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Zinsen/Kontoführung"), //
                        hasAmount("EUR", 5.82), hasGrossValue("EUR", 5.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug10()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-08-23"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug10.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-08-05"), hasAmount("EUR", 600.00), //
                        hasSource("Kontoauszug10.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-08-01"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug10.txt"), hasNote(null))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        interestCharge( //
                                        hasDate("2022-08-05"), //
                                        hasSource("Kontoauszug10.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 3.59), hasGrossValue("EUR", 3.59), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-08-05"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("01.07.2022 bis 31.07.2022"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-12"), hasAmount("EUR", 4250.00), //
                        hasSource("Kontoauszug10.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-08-23"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug10.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-08-30"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug10.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-08-31"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("01.08.2022 bis 31.08.2022"), //
                        hasAmount("EUR", 3.82), hasGrossValue("EUR", 3.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug11()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

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
                        hasDate("2025-11-28"), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug12()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-08"), hasAmount("EUR", 2362.66), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-08"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-09"), hasAmount("EUR", 1300.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2026-01-09"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-13"), hasAmount("EUR", 2531.91), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-13"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-14"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2026-01-19"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2026-01-20"), hasAmount("EUR", 3700.00), //
                        hasSource("Kontoauszug12.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-30"), //
                        hasSource("Kontoauszug12.txt"), //
                        hasAmount("EUR", 2.20), hasGrossValue("EUR", 2.93), //
                        hasTaxes("EUR", 0.73), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug13()
    {
        var extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

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
                        hasDate("2021-02-05"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug13.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-02-26"), //
                        hasSource("Kontoauszug13.txt"), //
                        hasAmount("EUR", 4.00), hasGrossValue("EUR", 4.72), //
                        hasTaxes("EUR", 0.72), hasFees("EUR", 0.00))));
    }
}
