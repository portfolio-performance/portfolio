package name.abuchen.portfolio.datatransfer.pdf.jtdirektbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.JTDirektbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class JTDirektbankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        JTDirektbankPDFExtractor extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);


        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-03-31"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));
    }

    @Test
    public void testKontoauszug02()
    {
        JTDirektbankPDFExtractor extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-01"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-06"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-15"), hasAmount("EUR", 4500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-19"), hasAmount("EUR", 26150.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-29"), hasAmount("EUR", 5400.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-06-30"), hasAmount("EUR", 37.50), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        JTDirektbankPDFExtractor extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-18"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Dauerauftragsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-29"), hasAmount("EUR", 32.16), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug04()
    {
        JTDirektbankPDFExtractor extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        interestCharge(hasDate("2023-05-03"), hasAmount("EUR", 13.44), //
                        hasSource("Kontoauszug04.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-04"), hasAmount("EUR", 15200.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-05-04"), hasAmount("EUR", 11.69), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-16"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-19"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-31"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-05-31"), hasAmount("EUR", 54.32), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug05()
    {
        JTDirektbankPDFExtractor extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-02"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Spar/Fest/Termingeld"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-15"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-15"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-16"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-17"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-23"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-24"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-29"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Spar/Fest/Termingeld"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-01-31"), hasAmount("EUR", 139.23), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug06()
    {
        JTDirektbankPDFExtractor extractor = new JTDirektbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-07"), hasAmount("EUR", 1650.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-08"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Umbuchung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-20"), hasAmount("EUR", 2650.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-28"), hasAmount("EUR", 1750.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-15"), hasAmount("EUR", 3650.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-21"), hasAmount("EUR", 2700.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-26"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-03-28"), hasAmount("EUR", 12.67), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));
    }
}
