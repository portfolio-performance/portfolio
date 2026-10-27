package name.abuchen.portfolio.datatransfer.pdf.jtdirektbank;

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
import name.abuchen.portfolio.datatransfer.pdf.JTDirektbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class JTDirektbankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-03-31"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-01"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-06"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-15"), hasAmount("EUR", 4500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-19"), hasAmount("EUR", 26150.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-29"), hasAmount("EUR", 5400.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-06-30"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

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

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-18"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Dauerauftragsgutschrift"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-29"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.16), hasGrossValue("EUR", 32.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        interestCharge( //
                                        hasDate("2023-05-03"), //
                                        hasSource("Kontoauszug04.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 13.44), hasGrossValue("EUR", 13.44), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-04"), hasAmount("EUR", 15200.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsgutschrift"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-05-04"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.69), hasGrossValue("EUR", 11.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-16"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsgutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-19"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-31"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsauftrag"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-05-31"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 54.32), hasGrossValue("EUR", 54.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-02"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Spar/Fest/Termingeld"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-15"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-15"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-16"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-17"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-23"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-24"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-29"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Spar/Fest/Termingeld"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-01-31"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 139.23), hasGrossValue("EUR", 139.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-07"), hasAmount("EUR", 1650.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-08"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Umbuchung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-20"), hasAmount("EUR", 2650.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-28"), hasAmount("EUR", 1750.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-15"), hasAmount("EUR", 3650.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-21"), hasAmount("EUR", 2700.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-26"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsgutschrift"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-03-28"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.67), hasGrossValue("EUR", 12.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAccountStatement_sk01()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement_sk01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("AccountStatement_sk01.txt"), //
                        hasNote("3,50 % p.a."), //
                        hasAmount("EUR", 3.03), hasGrossValue("EUR", 3.74), //
                        hasTaxes("EUR", 0.71), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAccountStatement_sk02()
    {
        var extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement_sk02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-05-14"), //
                        hasSource("AccountStatement_sk02.txt"), //
                        hasNote("3,80 % p.a."), //
                        hasAmount("EUR", 153.48), hasGrossValue("EUR", 189.48), //
                        hasTaxes("EUR", 36.00), hasFees("EUR", 0.00))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-14"), hasAmount("EUR", 10153.48), //
                        hasSource("AccountStatement_sk02.txt"), hasNote("Ukončenie vkladu"))));
    }
}
