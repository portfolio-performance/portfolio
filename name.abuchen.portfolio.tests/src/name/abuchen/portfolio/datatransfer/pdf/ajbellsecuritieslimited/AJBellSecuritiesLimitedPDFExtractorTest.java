package name.abuchen.portfolio.datatransfer.pdf.ajbellsecuritieslimited;

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
import name.abuchen.portfolio.datatransfer.pdf.AJBellSecuritiesLimitedPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AJBellSecuritiesLimitedPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecurityBuy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("BF41Q72"), hasTicker(null), //
                        hasName("LEGAL & GENERAL(UNIT TRUST MNGRS) WORLD CLIM CHNGE EQTY FACTORS IND I ACC"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T13:15"), hasShares(17940.965), //
                        hasSource("SecurityBuy01.txt"), //
                        hasNote("Ref. No. C5L6DQ"), //
                        hasAmount("GBP", 10000.00), hasGrossValue("GBP", 9998.50), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 1.50))));
    }

    @Test
    public void testSecurityBuy02()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecurityBuy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("BF41Q72"), hasTicker(null), //
                        hasName("LEGAL & GENERAL(UNIT TRUST MNGRS) WORLD CLIM CHNGE EQTY FACTORS IND I ACC"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-08T13:15"), hasShares(14008.055), //
                        hasSource("SecurityBuy02.txt"), //
                        hasNote("Ref. No. C46TWX"), //
                        hasAmount("GBP", 8001.50), hasGrossValue("GBP", 8000.00), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 1.50))));
    }

    @Test
    public void testSecurityBuy03()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecurityBuy03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("BDGSVH2"), hasTicker(null), //
                        hasName("XTRACKERS (IE) PLC MSCI WLD INFO TECHNOLOGY UCITS ETF 1C"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T10:22"), hasShares(380), //
                        hasSource("SecurityBuy03.txt"), //
                        hasNote("Ref. No. C5L6BV"), //
                        hasAmount("GBP", 9980.06), hasGrossValue("GBP", 9970.11), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 9.95))));
    }

    @Test
    public void testSecuritySale01()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecuritySale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("B8NZ739"), hasTicker(null), //
                        hasName("UBS (LUX) FUND SOLUTIONS MSCI WORLD SOC RSPON A UCITS USD DIS"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-28T08:10"), hasShares(159), //
                        hasSource("SecuritySale01.txt"), //
                        hasNote("Ref. No. CNG7G1"), //
                        hasAmount("GBP", 15783.20), hasGrossValue("GBP", 15793.15), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 9.95))));
    }

    @Test
    public void testSecuritySale02()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SecuritySale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("B7NLLS3"), hasTicker(null), //
                        hasName("VANGUARD FUNDS PLC S&P 500 UCITS E T F INC NAV GBP"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-08-17T09:01"), hasShares(470), //
                        hasSource("SecuritySale02.txt"), //
                        hasNote("Ref. No. C8M779"), //
                        hasAmount("GBP", 31643.75), hasGrossValue("GBP", 31653.70), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 9.95))));
    }
}
