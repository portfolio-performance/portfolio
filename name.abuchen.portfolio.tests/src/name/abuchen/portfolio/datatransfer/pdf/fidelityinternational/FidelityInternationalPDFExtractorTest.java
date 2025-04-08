package name.abuchen.portfolio.datatransfer.pdf.fidelityinternational;

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
import name.abuchen.portfolio.datatransfer.pdf.FidelityInternationalPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class FidelityInternationalPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        FidelityInternationalPDFExtractor extractor = new FidelityInternationalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecurityBuy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("31620M106"), hasTicker("FIS"), //
                        hasName("FIDELITY NATL"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-08T00:00"), hasShares(7.5146), //
                        hasSource("SecurityBuy01.txt"), //
                        hasNote("Ref. No. 23342-0D6SVL"), //
                        hasAmount("USD", 442.83), hasGrossValue("USD", 442.83), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testSecuritySale01()
    {
        FidelityInternationalPDFExtractor extractor = new FidelityInternationalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecuritySale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11135F101"), hasTicker("AAPL"), //
                        hasName("APPLE INC"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-12T00:00"), hasShares(14), //
                        hasSource("SecuritySale01.txt"), //
                        hasNote("Ref. No. 23346-K9T1Q9"), //
                        hasAmount("USD", 2423.41), hasGrossValue("USD", 2423.54), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.13))));
    }

    @Test
    public void testSecuritySale02()
    {
        FidelityInternationalPDFExtractor extractor = new FidelityInternationalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecuritySale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("31620M106"), hasTicker("FIS"), //
                        hasName("FIDELITY NATL"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-07-19T00:00"), hasShares(30), //
                        hasSource("SecuritySale02.txt"), //
                        hasNote("Ref. No. 33107-zz91lf"), //
                        hasAmount("USD", 2887.43), hasGrossValue("USD", 2887.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.07))));
    }

    @Test
    public void testTradeConfirmation01()
    {
        FidelityInternationalPDFExtractor extractor = new FidelityInternationalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeConfirmation01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46428Q109"), hasTicker("SLV"), //
                        hasName("ISHARES SILVER TR ISHARES"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("464287622"), hasTicker("IWB"), //
                        hasName("ISHARES RUSSELL 1000 INDEX FUND"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("464288885"), hasTicker("EFG"), //
                        hasName("ISHARES TR EAFE GRWTH ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46434V621"), hasTicker("DGRO"), //
                        hasName("ISHARES TRUST CORE DIVID GWTH"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46435U440"), hasTicker("BGRN"), //
                        hasName("ISHARES TR GBL GREEN ETF"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-04T00:00"), hasShares(40), //
                        hasSource("TradeConfirmation01.txt"), //
                        hasNote("Ref. No. 20123-1XXXXX | Ord. No. 20123-XXXXX"), //
                        hasAmount("USD", 1011.60), hasGrossValue("USD", 1011.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-04T00:00"), hasShares(24), //
                        hasSource("TradeConfirmation01.txt"), //
                        hasNote("Ref. No. 20123-7RRRRRR | Ord. No. 20123-A2B3C"), //
                        hasAmount("USD", 4994.40), hasGrossValue("USD", 4994.40), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-04T00:00"), hasShares(50), //
                        hasSource("TradeConfirmation01.txt"), //
                        hasNote("Ref. No. 20123-XXXXX | Ord. No. 20123-XXXXX"), //
                        hasAmount("USD", 5082.50), hasGrossValue("USD", 5082.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-04T00:00"), hasShares(114), //
                        hasSource("TradeConfirmation01.txt"), //
                        hasNote("Ref. No. 20123-9JAVAC | Ord. No. 20123-ASDFG"), //
                        hasAmount("USD", 5016.00), hasGrossValue("USD", 5016.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-04T00:00"), hasShares(35), //
                        hasSource("TradeConfirmation01.txt"), //
                        hasNote("Ref. No. 20123-3COCOA | Ord. No. 20123-I789U"), //
                        hasAmount("USD", 1969.45), hasGrossValue("USD", 1969.45), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testTradeConfirmation02()
    {

        FidelityInternationalPDFExtractor extractor = new FidelityInternationalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TradeConfirmation02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("12345A789"), hasTicker("GOOG"), //
                        hasName("ALPHABET INC CAP STK CL C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA1130041058"), hasWkn("113004105"), hasTicker("BAM"), //
                        hasName("BROOKFIELD ASSET MANAGEMENT LTD CLASS"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA67077M1086"), hasWkn("67077M108"), hasTicker("NTR"), //
                        hasName("NUTRIEN LTD COM NPV"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-14T00:00"), hasShares(30), //
                        hasSource("TradeConfirmation02.txt"), //
                        hasNote("Ref. No. 12345-XXX45X | Ord. No. 12345-XX72X"), //
                        hasAmount("USD", 3990.00), hasGrossValue("USD", 3990.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-14T00:00"), hasShares(203), //
                        hasSource("TradeConfirmation02.txt"), //
                        hasNote("Ref. No. 12345-XX78X | Ord. No. 67890-I5XYZ"), //
                        hasAmount("USD", 7935.20), hasGrossValue("USD", 7935.27), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.07))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-14T00:00"), hasShares(0.032), //
                        hasSource("TradeConfirmation02.txt"), //
                        hasNote("Ref. No. 12234-8OPOP | Ord. No. 12345-I8ZZZ"), //
                        hasAmount("USD", 1.25), hasGrossValue("USD", 1.25), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-14T00:00"), hasShares(80), //
                        hasSource("TradeConfirmation02.txt"), //
                        hasNote("Ref. No. 12345-1X2X3X | Ord. No. 12345-A1B2C"), //
                        hasAmount("USD", 4427.59), hasGrossValue("USD", 4427.59), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
