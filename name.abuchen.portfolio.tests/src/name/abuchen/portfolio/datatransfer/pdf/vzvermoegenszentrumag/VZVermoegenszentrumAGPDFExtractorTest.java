package name.abuchen.portfolio.datatransfer.pdf.vzvermoegenszentrumag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import name.abuchen.portfolio.datatransfer.pdf.VZVermoegenszentrumAGPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class VZVermoegenszentrumAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        VZVermoegenszentrumAGPDFExtractor extractor = new VZVermoegenszentrumAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0126639464"), hasWkn("12663946"), hasTicker(null), //
                        hasName("Calida Holding AG Namen-Aktie"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-31T10:19:24"), hasShares(20.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer AUF221133-43219876"), //
                        hasAmount("CHF", 1027.30), hasGrossValue("CHF", 986.00), //
                        hasTaxes("CHF", 0.75), hasFees("CHF", 1.55 + 39.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        VZVermoegenszentrumAGPDFExtractor extractor = new VZVermoegenszentrumAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1144400782"), hasWkn("26377052"), hasTicker(null), //
                        hasName("- Credit Suisse (Lux) Inflation Linked CHF Bond Fd Anteile -UB- CS Investment Funds 14 FCP"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-08T12:00:00"), hasShares(120.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer AUF223377-44378629"), //
                        hasAmount("CHF", 12274.55), hasGrossValue("CHF", 12116.40), //
                        hasTaxes("CHF", 18.15), hasFees("CHF", 140.00))));
    }

    @Test
    public void testDividende01()
    {
        VZVermoegenszentrumAGPDFExtractor extractor = new VZVermoegenszentrumAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0111762537"), hasWkn("11176253"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SMIM (R) Anteile -(CHF) A-dis-"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(38.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Referenz CA20237768/20888"), //
                        hasAmount("CHF", 147.95), hasGrossValue("CHF", 227.60), //
                        hasTaxes("CHF", 79.65), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        VZVermoegenszentrumAGPDFExtractor extractor = new VZVermoegenszentrumAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0126639464"), hasWkn("12663946"), hasTicker(null), //
                        hasName("Calida Holding AG Namen-Aktie"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-27T00:00"), hasShares(20.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Referenz CA20236723/50692"), //
                        hasAmount("CHF", 7.80), hasGrossValue("CHF", 12.00), //
                        hasTaxes("CHF", 4.20), hasFees("CHF", 0.00))));
    }
}
