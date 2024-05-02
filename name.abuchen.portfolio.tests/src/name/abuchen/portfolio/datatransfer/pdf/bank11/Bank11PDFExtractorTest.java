package name.abuchen.portfolio.datatransfer.pdf.bank11;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.Bank11PDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class Bank11PDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        Bank11PDFExtractor extractor = new Bank11PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2022-10-19"), hasAmount("EUR", 18500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2022-11-14"), hasAmount("EUR", 4500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2022-11-22"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2022-12-08"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(removal(hasDate("2022-10-31"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2022-11-07"), hasAmount("EUR", 350.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2022-11-15"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2022-11-21"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Umbuchung"))));

        assertThat(results, hasItem(removal(hasDate("2022-11-30"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2022-12-05"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2022-12-16"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2022-12-30"), hasAmount("EUR", 1250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(interest(hasDate("2022-12-30"), hasAmount("EUR", 52.66), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        Bank11PDFExtractor extractor = new Bank11PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(20L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2023-01-05"), hasAmount("EUR", 1300.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2023-01-16"), hasAmount("EUR", 4600.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2023-01-24"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2023-02-15"), hasAmount("EUR", 4200.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2023-03-16"), hasAmount("EUR", 4200.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2023-04-28"), hasAmount("EUR", 17500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsgutschrift"))));

        assertThat(results, hasItem(removal(hasDate("2023-01-02"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-01-09"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-01-17"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-01-25"), hasAmount("EUR", 17500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-01-30"), hasAmount("EUR", 4000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-02-13"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-02-28"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-03-08"), hasAmount("EUR", 800.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-03-09"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-03-30"), hasAmount("EUR", 6900.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(removal(hasDate("2023-05-02"), hasAmount("EUR", 17600.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisungsauftrag"))));

        assertThat(results, hasItem(interest(hasDate("2023-03-31"), hasAmount("EUR", 56.88), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2023-04-27"), hasAmount("EUR", 24.50), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2023-12-29"), hasAmount("EUR", 29.52), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

}
