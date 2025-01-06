package name.abuchen.portfolio.datatransfer.pdf.cetesdirecto;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
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
import name.abuchen.portfolio.datatransfer.pdf.CetesDirectoPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class CetesDirectoPDFExtractorTest
{

    @Test
    public void testEdoCta01()
    {
        CetesDirectoPDFExtractor extractor = new CetesDirectoPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Purchase01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES"), //
                        hasCurrencyCode("MXN"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-06"), hasShares(6080), //
                        hasSource("Purchase01.txt"), //
                        hasNote("ID:SVD147529623 Series:220203 Term:2 Rate:5.51"), //
                        hasAmount("MXN", 60540.38), //
                        hasGrossValue("MXN", 60540.38), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-06"), hasShares(6055), //
                        hasSource("Purchase01.txt"), //
                        hasNote("ID:SVD147779466 Series:220106 Term:0"), //
                        hasAmount("MXN", 60550.00 - 3.7), //
                        hasGrossValue("MXN", 60550.00), //
                        hasTaxes("MXN", 3.70), hasFees("MXN", 0.00))));
    }

}
