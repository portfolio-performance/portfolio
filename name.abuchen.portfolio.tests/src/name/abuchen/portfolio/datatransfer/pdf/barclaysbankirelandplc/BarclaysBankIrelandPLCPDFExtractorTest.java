package name.abuchen.portfolio.datatransfer.pdf.barclaysbankirelandplc;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
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
import name.abuchen.portfolio.datatransfer.pdf.BarclaysBankIrelandPLCPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class BarclaysBankIrelandPLCPDFExtractorTest
{
    @Test
    public void testKreditKontoauszug01()
    {
        BarclaysBankIrelandPLCPDFExtractor extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-11-20"), hasAmount("EUR", 119.96), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("GetYourGuide Tickets Berlin"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-11-20"), hasAmount("EUR", 21.84), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("ALDI ALBUFEIRA ALBUFEIRA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-11-20"), hasAmount("EUR", 8.00), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("GRUPO PESTANA ALCACER DO SA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-01"), hasAmount("EUR", 34.99), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("eBay O*00-00000-00000 Luxembourg"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-06"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("PAYPAL *IONOS SE 00000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-09"), hasAmount("EUR", 8.48), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Globus Baumarkt Ort"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-09"), hasAmount("EUR", 1.29), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Tegut Filiale 0000 LangOrtsnamen"))));
    }

    @Test
    public void testKreditKontoauszug02()
    {
        BarclaysBankIrelandPLCPDFExtractor extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-04"), hasAmount("EUR", 4.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("TooGoodToG xxxxxxxxxxx toogoodtogo.d"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("PAYPAL *IONOS SE 00000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-25"), hasAmount("EUR", 60.78), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Lidl sagt Danke Ort"))));

         // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-25"), hasAmount("EUR", 60.78), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Lidl sagt Danke Ort"))));

        // assert transaction
       assertThat(results, hasItem(deposit(hasDate("2023-12-28"), hasAmount("EUR", 60.78), //
                       hasSource("KreditKontoauszug02.txt"), hasNote("Lidl sagt Danke Ort"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 5.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("PAYPAL *VODAFONE 0000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-08"), hasAmount("EUR", 5.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("PAYPAL *VODAFONE 0000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-06"), hasAmount("EUR", 671.99), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Per Lastschrift dankend erhalten"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-08"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Vorname Nachname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-08"), hasAmount("EUR", 0.50), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Gutschrift Manuelle Lastschrift"))));
    }

    @Test
    public void testKreditKontoauszug03()
    {
        BarclaysBankIrelandPLCPDFExtractor extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 3.54), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote(null), //
                        hasTaxes("EUR", 1.20 + 0.06), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 14.47), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote(null), //
                        hasTaxes("EUR", 4.91 + 0.27), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 16.41), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote(null), //
                        hasTaxes("EUR", 5.57 + 0.30), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 103.38), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote(null), //
                        hasTaxes("EUR", 35.10 + 1.93), hasFees("EUR", 0.00))));
    }
}
