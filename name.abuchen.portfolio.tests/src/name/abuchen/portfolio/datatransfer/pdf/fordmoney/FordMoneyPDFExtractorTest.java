package name.abuchen.portfolio.datatransfer.pdf.fordmoney;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.FordMoneyPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class FordMoneyPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new FordMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(deposit( //
                        hasDate("2024-10-04"), //
                        hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null))));

        assertThat(results, hasItem(removal( //
                        hasDate("2024-10-25"), //
                        hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null))));

        assertThat(results, hasItem(interest( //
                        hasDate("2024-10-31"), //
                        hasAmount("EUR", 85.61), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Abschluss"))));

        assertThat(results, hasItem(interest( //
                        hasDate("2024-10-31"), //
                        hasAmount("EUR", 62.73), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Abschluss"))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2024-10-31"), //
                        hasAmount("EUR", 37.09), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Kapitalertragssteuer"))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2024-10-31"), //
                        hasAmount("EUR", 2.03), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Solidaritätszuschlag"))));

    }

}

