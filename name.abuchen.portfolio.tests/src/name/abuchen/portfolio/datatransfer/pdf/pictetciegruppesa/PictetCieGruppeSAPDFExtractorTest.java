package name.abuchen.portfolio.datatransfer.pdf.pictetciegruppesa;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
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
import name.abuchen.portfolio.datatransfer.pdf.PictetCieGruppeSAPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class PictetCieGruppeSAPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new PictetCieGruppeSAPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BHZKQ946"), hasWkn(null), hasTicker(null), //
                        hasName("PIMCO GIS-GL.LO.DUR.R/R INS.USD-ACC"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-24T09:51:27"), hasShares(24728.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Transaction no.: 781327840"), //
                        hasAmount("USD", 296598.18), hasGrossValue("USD", 295994.16), //
                        hasTaxes("USD", 443.99), hasFees("USD", 160.03))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new PictetCieGruppeSAPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4ND3602"), hasWkn(null), hasTicker(null), //
                        hasName("GOLD(ISHARES PH.MET.)-ETC-11/PERP S"), //
                        hasCurrencyCode("GBP"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-02T14:41:02"), hasShares(4834.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Transaction no.: 761800293"), //
                        hasAmount("GBP", 137088.54), hasGrossValue("GBP", 136647.03), //
                        hasTaxes("GBP", (204.97 + 1.04)), hasFees("GBP", (136.65 + 95.65 + 3.20)))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new PictetCieGruppeSAPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0419741177"), hasWkn(null), hasTicker(null), //
                        hasName("LYXOR-BL.EQ.WEI.COM EX-AG I ETF"), //
                        hasCurrencyCode("GBP"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-02T14:40:24"), hasShares(1497.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Transaction no.: 761800107"), //
                        hasAmount("GBP", 196855.38), hasGrossValue("GBP", 196402.66), //
                        hasTaxes("GBP", (294.60 + 1.00)), hasFees("GBP", 157.12))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new PictetCieGruppeSAPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("XS2425022303"), hasWkn(null), hasTicker(null), //
                        hasName("6.516% MS (SX5E/SPX) 22/23"), //
                        hasCurrencyCode("GBP"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-23T08:26:01"), hasShares(1800.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Transaction no.: 766962817"), //
                        hasAmount("GBP", 180180.00), hasGrossValue("GBP", 180000.00), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 180.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new PictetCieGruppeSAPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2093580772"), hasWkn(null), hasTicker(null), //
                        hasName("AGIF-CHINA A-SHARES PT GBP-ACC."), //
                        hasCurrencyCode("GBP"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-07-14T19:37:30"), hasShares(74.620), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Transaction no.: 794448726"), //
                        hasAmount("GBP", 116726.92), hasGrossValue("GBP", 116854.17), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 127.25))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new PictetCieGruppeSAPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0629459743"), hasWkn(null), hasTicker(null), //
                        hasName("UBS(LUX)-MSCI WOR.SOC.RES.A USD-INC"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-08-04T00:00"), hasExDate("2022-08-01T00:00"), //
                        hasShares(2420.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Transaction no.: 797075451"), //
                        hasAmount("USD", 2304.81), hasGrossValue("USD", 2304.81), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
