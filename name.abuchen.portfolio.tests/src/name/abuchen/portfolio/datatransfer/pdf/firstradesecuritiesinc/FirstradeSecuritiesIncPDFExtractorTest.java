package name.abuchen.portfolio.datatransfer.pdf.firstradesecuritiesinc;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.pdf.FirstradeSecuritiesIncPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class FirstradeSecuritiesIncPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        FirstradeSecuritiesIncPDFExtractor extractor = new FirstradeSecuritiesIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("78464A870"), hasTicker("XBI"), //
                        hasName("SPDR SER TR S&P BIOTECH ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-01T00:00"), hasShares(5.00), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 371.65), hasGrossValue("USD", 371.65), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testSecuritySale01()
    {
        FirstradeSecuritiesIncPDFExtractor extractor = new FirstradeSecuritiesIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("88160R101"), hasTicker("TSLA"), //
                        hasName("TESLA INC COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-11-24T00:00"), hasShares(1.00), //
                        hasSource("Sale01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1156.02), hasGrossValue("USD", 1156.03), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.01))));
    }

    @Test
    public void testSecuritySale02()
    {
        FirstradeSecuritiesIncPDFExtractor extractor = new FirstradeSecuritiesIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("8SFXJX7"), hasTicker("PYPL"), //
                        hasName("PAYPAL HOLDINGS INC"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-11-24T00:00"), hasShares(1.00), //
                        hasSource("Sale02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 439.97), hasGrossValue("USD", 440.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.02 + 0.01))));
    }
}
