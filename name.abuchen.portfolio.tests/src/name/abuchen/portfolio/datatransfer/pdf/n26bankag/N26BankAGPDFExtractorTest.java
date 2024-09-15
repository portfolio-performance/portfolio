package name.abuchen.portfolio.datatransfer.pdf.n26bankag;

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
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.N26BankAGkPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class N26BankAGPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        N26BankAGkPDFExtractor extractor = new N26BankAGkPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-06-01T00:00"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 185.66), hasGrossValue("EUR", 252.16), //
                        hasTaxes("EUR", 63.04 + 3.46), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-19"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        N26BankAGkPDFExtractor extractor = new N26BankAGkPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01T00:00"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 196.67), hasGrossValue("EUR", 267.12), //
                        hasTaxes("EUR", 66.78 + 3.67), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-02"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-02"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        N26BankAGkPDFExtractor extractor = new N26BankAGkPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-01T00:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.42), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.14), hasFees("EUR", 0.00))));
    }
}
