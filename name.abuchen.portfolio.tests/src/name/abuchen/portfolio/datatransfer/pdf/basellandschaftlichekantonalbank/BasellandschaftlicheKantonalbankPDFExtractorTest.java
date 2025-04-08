package name.abuchen.portfolio.datatransfer.pdf.basellandschaftlichekantonalbank;

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
import name.abuchen.portfolio.datatransfer.pdf.BasellandschaftlicheKantonalbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BasellandschaftlicheKantonalbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        BasellandschaftlicheKantonalbankPDFExtractor extractor = new BasellandschaftlicheKantonalbankPDFExtractor(new Client());

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
                        hasIsin("LI1212200714"), hasWkn("121220071"), hasTicker(null), //
                        hasName("radicant SDG Impact Solutions Fund - Global Sustainable Bonds"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-11T00:00"), hasShares(10.467), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer AUF240111-"), //
                        hasAmount("CHF", 105.51), hasGrossValue("CHF", 105.51), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        BasellandschaftlicheKantonalbankPDFExtractor extractor = new BasellandschaftlicheKantonalbankPDFExtractor(new Client());

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
                        hasIsin("CH1139780097"), hasWkn("113978009"), hasTicker(null), //
                        hasName("Zuercher Kantonalbank 2022-ohne festen Verfall auf Aktien"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-17T00:00"), hasShares(8.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer AUF230317-"), //
                        hasAmount("CHF", 90.38), hasGrossValue("CHF", 90.38), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        BasellandschaftlicheKantonalbankPDFExtractor extractor = new BasellandschaftlicheKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LI1212200672"), hasWkn("121220067"), hasTicker(null), //
                        hasName("radicant SDG Impact Solutions Fund - Global Sustainable Equities"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-19T00:00"), hasShares(24.706), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer AUF231219-"), //
                        hasAmount("CHF", 266.08), hasGrossValue("CHF", 266.08), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        BasellandschaftlicheKantonalbankPDFExtractor extractor = new BasellandschaftlicheKantonalbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1139780097"), hasWkn("113978009"), hasTicker(null), //
                        hasName("Zuercher Kantonalbank 2022-ohne festen Verfall auf Aktien"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-04-27T00:00"), hasShares(8.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer AUF230427-"), //
                        hasAmount("CHF", 90.49), hasGrossValue("CHF", 90.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }
}
