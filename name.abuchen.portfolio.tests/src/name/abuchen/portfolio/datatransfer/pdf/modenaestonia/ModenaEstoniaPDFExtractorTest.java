package name.abuchen.portfolio.datatransfer.pdf.modenaestonia;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
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
import name.abuchen.portfolio.datatransfer.pdf.ModenaEstoniaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class ModenaEstoniaPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        ModenaEstoniaPDFExtractor extractor = new ModenaEstoniaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-03-13T21:39:01"), hasAmount("EUR", 12.56), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault revenue share"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-14T07:00:00"), hasAmount("EUR", 1.42), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault signup bonus"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-21T21:59:09"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault campaign bonus"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-22T07:00:02"), hasAmount("EUR", 1.48), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault signup bonus"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-03-22T09:00:04"), hasAmount("EUR", 1.23), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault revenue share"))));
    }
}
