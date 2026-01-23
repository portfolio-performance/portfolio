package name.abuchen.portfolio.datatransfer.pdf.suressedirektbank;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SuresseDirektBankPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SuresseDirektBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new SuresseDirektBankPDFExtractor(new Client());

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

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-01-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("01-01-2023 bis 31-01-2023"), //
                        hasAmount("EUR", 8.25), hasGrossValue("EUR", 8.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new SuresseDirektBankPDFExtractor(new Client());

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
        assertThat(results, hasItem(deposit(hasDate("2022-12-19"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Referenz : C2L19XM007000066"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-20"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Referenz : C2L20CW0G00A03NA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-12-20"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Referenz : C2L20XM009000059"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-31"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("01-12-2022 bis 31-12-2022"), //
                        hasAmount("EUR", 2.80), hasGrossValue("EUR", 2.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
