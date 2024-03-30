package name.abuchen.portfolio.datatransfer.pdf.stakeshopptyltd;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.StakeshopPtyLtdPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class StakeshopPtyLtdPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        StakeshopPtyLtdPDFExtractor extractor = new StakeshopPtyLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FLT"), //
                        hasName("FLT"), //
                        hasCurrencyCode("AUD"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-27"), hasShares(512), //
                        hasSource("Buy01.txt"), //
                        hasNote("0000001"), //
                        hasAmount("AUD", 10458.04), hasGrossValue("AUD", 10455.04), //
                        hasTaxes("AUD", 0.00), hasFees("AUD", 3.00))));
    }

    @Test
    public void testSecurityBuy01_matchExistingSecurityByTickerWithoutExchange()
    {
        Client client = new Client();

        Security flt = new Security("FLT", "AUD");
        flt.setTickerSymbol("FLT.AX");
        client.addSecurity(flt);

        StakeshopPtyLtdPDFExtractor extractor = new StakeshopPtyLtdPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "AUD");

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-27"), hasShares(512), //
                        hasSource("Buy01.txt"), //
                        hasNote("0000001"), //
                        hasAmount("AUD", 10458.04), hasGrossValue("AUD", 10455.04), //
                        hasTaxes("AUD", 0.00), hasFees("AUD", 3.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        StakeshopPtyLtdPDFExtractor extractor = new StakeshopPtyLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("LLL"), //
                        hasName("LLL"), //
                        hasCurrencyCode("AUD"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-05"), hasShares(1000), //
                        hasSource("Buy02.txt"), //
                        hasNote("0000002"), //
                        hasAmount("AUD", 503.00), hasGrossValue("AUD", 500), //
                        hasTaxes("AUD", 0.00), hasFees("AUD", 3.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        StakeshopPtyLtdPDFExtractor extractor = new StakeshopPtyLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FLT"), //
                        hasName("FLT"), //
                        hasCurrencyCode("AUD"))));

        assertThat(results, hasItem(sale( //
                        hasDate("2022-09-30"), hasShares(512), //
                        hasSource("Sell01.txt"), //
                        hasNote("0000003"), //
                        hasAmount("AUD", 7267.40), hasGrossValue("AUD", 7270.40), //
                        hasTaxes("AUD", 0.00), hasFees("AUD", 3.00))));
    }

    @Test
    public void testSecuritySell02()
    {
        StakeshopPtyLtdPDFExtractor extractor = new StakeshopPtyLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "AUD");

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FMG"), //
                        hasName("FMG"), //
                        hasCurrencyCode("AUD"))));

        assertThat(results, hasItem(sale( //
                        hasDate("2023-03-13"), hasShares(105), //
                        hasSource("Sell02.txt"), //
                        hasNote("0000004"), //
                        hasAmount("AUD", 2278.65), hasGrossValue("AUD", 2281.65), //
                        hasTaxes("AUD", 0.00), hasFees("AUD", 3.00))));
    }
}
