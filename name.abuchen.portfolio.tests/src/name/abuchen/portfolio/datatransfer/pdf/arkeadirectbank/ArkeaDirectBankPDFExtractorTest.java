package name.abuchen.portfolio.datatransfer.pdf.arkeadirectbank;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;

import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.ArkeaDirectBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ArkeaDirectBankPDFExtractorTest
{
    @Test
    public void testCompteAchat01()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000133308"), hasWkn(null), hasTicker(null), //
                        hasName("ORANGE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-26T16:27:35"), hasShares(46.00), //
                        hasSource("Achat01.txt"), //
                        hasNote("Référence 94R6134018440990"), //
                        hasAmount("EUR", 491.67), hasGrossValue("EUR", 489.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.95))));
    }

    @Test
    public void testCompteAchat02()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000045072"), hasWkn(null), hasTicker(null), //
                        hasName("CREDIT AGRICOLE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000133308"), hasWkn(null), hasTicker(null), //
                        hasName("ORANGE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-04T09:14:18"), hasShares(460.00), //
                        hasSource("Achat02.txt"), //
                        hasNote("Référence 94R6134018440989"), //
                        hasAmount("EUR", 5068.28), hasGrossValue("EUR", 5058.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.12))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-04T09:15:36"), hasShares(450.00), //
                        hasSource("Achat02.txt"), //
                        hasNote("Référence 94R6134018440989"), //
                        hasAmount("EUR", 4827.34), hasGrossValue("EUR", 4817.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.64))));
    }

    @Test
    public void testCompteAchat03()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2655993207"), hasWkn(null), hasTicker(null), //
                        hasName("AMUNDI MSCI WORLD UC.ETF EUR D"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-28T09:39:48"), hasShares(7.00), //
                        hasSource("Achat03.txt"), //
                        hasNote("Référence 50Z3117582492059"), //
                        hasAmount("EUR", 212.42), hasGrossValue("EUR", 211.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.74))));
    }

    @Test
    public void testCompteAchat04()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0013412285"), hasWkn(null), hasTicker(null), //
                        hasName("AM.PEA SP500 ESG UCIT ETF EUR"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-03T09:29:05"), hasShares(12.00), //
                        hasSource("Achat04.txt"), //
                        hasNote("Référence 00F1913647290101"), //
                        hasAmount("EUR", 482.94), hasGrossValue("EUR", 482.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-03T09:29:38"), hasShares(12.00), //
                        hasSource("Achat04.txt"), //
                        hasNote("Référence 00F1913647700101"), //
                        hasAmount("EUR", 482.95), hasGrossValue("EUR", 482.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-03T09:31:22"), hasShares(10.00), //
                        hasSource("Achat04.txt"), //
                        hasNote("Référence 00F1913649810101"), //
                        hasAmount("EUR", 402.61), hasGrossValue("EUR", 402.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000133308"), hasWkn(null), hasTicker(null), //
                        hasName("ORANGE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-04T00:00"), hasShares(450.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 135.00), hasGrossValue("EUR", 135.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuern01()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuern01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000133308"), hasWkn(null), hasTicker(null), //
                        hasName("ORANGE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-03-26T00:00"), hasShares(46.00), //
                        hasSource("Steuern01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.47), hasGrossValue("EUR", 1.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuern02()
    {
        ArkeaDirectBankPDFExtractor extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuern02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000045072"), hasWkn(null), hasTicker(null), //
                        hasName("CREDIT AGRICOLE"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000133308"), hasWkn(null), hasTicker(null), //
                        hasName("ORANGE"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-04T00:00"), hasShares(460.00), //
                        hasSource("Steuern02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.17), hasGrossValue("EUR", 15.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-04T00:00"), hasShares(450.00), //
                        hasSource("Steuern02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.45), hasGrossValue("EUR", 14.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
