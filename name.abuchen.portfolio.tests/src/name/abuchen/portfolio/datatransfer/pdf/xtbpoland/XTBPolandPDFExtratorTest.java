package name.abuchen.portfolio.datatransfer.pdf.xtbpoland;

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

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.XTBPolandPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class XTBPolandPDFExtratorTest
{
    @Test
    public void testBuy01()
    {
        var extractor = new XTBPolandPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(6));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), //
                        hasWkn(null), //
                        hasTicker("FWIA.DE"), //
                        hasName("Invesco"), //
                        hasCurrencyCode("EUR"))));
        
        
        assertThat(results, hasItem(security( //
                        hasIsin(null), //
                        hasWkn(null), //
                        hasTicker("4GLD.DE"), //
                        hasName("Deutsche Boerse"), //
                        hasCurrencyCode("EUR"))));
         

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-03T11:18:09"), //
                        hasShares(30), //
                        hasSource("Buy01.txt"), //
                        hasNote("Order Number: 2004419557"), //
                        hasAmount("EUR", 6.67500), //
                        hasTaxes("EUR", 0), //
                        hasFees("EUR", 0.1) //
                        
                        )));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-03T11:18:33"), //
                        hasShares(1), //
                        hasSource("Buy01.txt"), //
                        hasNote("Order Number: 2004420096"), //
                        hasAmount("EUR", 97.71500), //
                        hasFees("EUR", 0),
                        hasTaxes("EUR", 0), //
                        hasGrossValue("EUR", 97.71500) //
        )));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-03T11:18:08"), //
                        hasShares(0.2636), //
                        hasSource("Buy01.txt"), //
                        hasNote("Order Number: 2004419557"), //
                        hasAmount("EUR", 6.67600), //
                        hasFees("EUR", 0), //
                        hasTaxes("EUR", 0) //
        )));
                        
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-03T11:18:32"),
                        hasShares(0.0131),
                        hasSource("Buy01.txt"),
                        hasNote("Order Number: 2004420096"), //
                        hasAmount("EUR",97.71500 ),
                        hasFees("EUR",0),
                        hasTaxes("EUR",0)
                        

        )));
    }
}
