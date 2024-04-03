package name.abuchen.portfolio.datatransfer.pdf.computershare;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.pdf.ComputersharePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ComputershareExtractorTest
{

    @Test
    public void testDepotAuszugWithPurchase01()
    {
        final ComputersharePDFExtractor extractor = new ComputersharePDFExtractor(new Client());

        final List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(10));

        // check security
        assertThat(results, hasItem(security( //
                        hasWkn("023135106"), hasTicker("AMZN"), //
                        hasCurrencyCode("USD"))));

        // check one buy transaction - without fees

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-28"), hasShares(7.5), //
                        hasSource("Depotauszug01.txt"), //
                        hasAmount("USD", 750.00), //
                        hasFees("USD", 0.00))));

        // check one buy transaction - with fees

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-31"), hasShares(7.5), //
                        hasSource("Depotauszug01.txt"), //
                        hasAmount("USD", 756.00), //
                        hasFees("USD", 6.00))));
    }

}
