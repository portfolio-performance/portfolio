package name.abuchen.portfolio.datatransfer.pdf.audibank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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
import name.abuchen.portfolio.datatransfer.pdf.AudiBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class AudiBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        AudiBankPDFExtractor extractor = new AudiBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2021-12-25"), hasAmount("EUR", 0.83), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2021-12-25"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug01.txt"), hasNote("Solidaritätszuschlag"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2021-12-25"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kirchensteuer"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2021-12-25"), hasAmount("EUR", 0.20), //
                        hasSource("Kontoauszug01.txt"), hasNote("Abgeltungsteuer"))));
    }

    @Test
    public void testKontoauszug02()
    {
        AudiBankPDFExtractor extractor = new AudiBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-22"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-22"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-23"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-08-25"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-08-25"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug02.txt"), hasNote("Solidaritätszuschlag"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2023-08-25"), hasAmount("EUR", 0.00), //
                                        hasSource("Kontoauszug02.txt"), hasNote("Kirchensteuer")))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-08-25"), hasAmount("EUR", 0.25), //
                        hasSource("Kontoauszug02.txt"), hasNote("Abgeltungsteuer"))));

    }

    @Test
    public void testKontoauszug03()
    {
        AudiBankPDFExtractor extractor = new AudiBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-11"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-18"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-03-25"), hasAmount("EUR", 1.13), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2022-03-25"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Solidaritätszuschlag"))));

        // check cancellation transaction
        assertThat(results, hasItem(taxes(hasDate("2022-03-25"), hasAmount("EUR", 0.02), //
                        hasSource("Kontoauszug03.txt"), hasNote("Kirchensteuer"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2022-03-25"), hasAmount("EUR", 0.27), //
                        hasSource("Kontoauszug03.txt"), hasNote("Abgeltungsteuer"))));

    }
}
