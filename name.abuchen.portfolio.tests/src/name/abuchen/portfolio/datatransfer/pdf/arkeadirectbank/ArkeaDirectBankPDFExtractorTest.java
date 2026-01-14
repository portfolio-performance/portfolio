package name.abuchen.portfolio.datatransfer.pdf.arkeadirectbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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
import name.abuchen.portfolio.datatransfer.pdf.ArkeaDirectBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ArkeaDirectBankPDFExtractorTest
{
    @Test
    public void testCompteAchat01()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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
    public void testCompteAchat05()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRON."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-08T00:00:00"), hasShares(1.00), //
                        hasSource("Achat05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.70), hasGrossValue("EUR", 22.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteAchat06()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRON."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-08T00:00:00"), hasShares(16), //
                        hasSource("Achat06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 363.20), hasGrossValue("EUR", 363.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteAchat07()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRONNEMENT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-22T10:42:24"), hasShares(100.00), //
                        hasSource("Achat07.txt"), //
                        hasNote("Référence 16S7998181666921"), //
                        hasAmount("EUR", 1768.90), hasGrossValue("EUR", 1765.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));
    }

    @Test
    public void testCompteAchat08()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0002XZSHO1"), hasWkn(null), hasTicker(null), //
                        hasName("IS.MSCI WLD SW.PEA UC.ETF EUR"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-10T09:04:13"), hasShares(1450.00), //
                        hasSource("Achat08.txt"), //
                        hasNote("Référence 00F2012613550101"), //
                        hasAmount("EUR", 7994.33), hasGrossValue("EUR", 7966.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 27.88))));
    }

    @Test
    public void testCompteAchat09()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0013412038"), hasWkn(null), hasTicker(null), //
                        hasName("AM.ETF PEA MSCI EUROPE UC.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681043599"), hasWkn(null), hasTicker(null), //
                        hasName("AM.MSCI WORLD UCITS ETF EUR C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-06T09:04:19"), hasShares(40.00), //
                        hasSource("Achat09.txt"), //
                        hasNote("Référence 00F2010258400102"), //
                        hasAmount("EUR", 1327.30), hasGrossValue("EUR", 1322.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.90))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-06T12:49:04"), hasShares(700.00), //
                        hasSource("Achat09.txt"), //
                        hasNote("Référence 00F2010319500103"), //
                        hasAmount("EUR", 23001.45), hasGrossValue("EUR", 22967.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 34.45))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-06T13:03:10"), hasShares(60.00), //
                        hasSource("Achat09.txt"), //
                        hasNote("Référence 00F2010481600104"), //
                        hasAmount("EUR", 1972.60), hasGrossValue("EUR", 1967.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.90))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T09:33:30"), hasShares(45.00), //
                        hasSource("Achat09.txt"), //
                        hasNote("Référence 00F2010542700105"), //
                        hasAmount("EUR", 24923.35), hasGrossValue("EUR", 24960.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 37.44))));
    }

    @Test
    public void testCompteAchat07WithAchatTaxesTreatment07()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Achat07.txt", "AchatTaxesTreatment07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRONNEMENT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-22T10:42:24"), hasShares(100.00), //
                        hasSource("Achat07.txt; AchatTaxesTreatment07.txt"), //
                        hasNote("Référence 16S7998181666921"), //
                        hasAmount("EUR", 1774.2), hasGrossValue("EUR", 1765.00), //
                        hasTaxes("EUR", 5.30), hasFees("EUR", 3.90))));
    }

    @Test
    public void testCompteAchat07WithTaxesTreatment07_SourceFilesReversed()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "AchatTaxesTreatment07.txt", "Achat07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRONNEMENT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-22T10:42:24"), hasShares(100.00), //
                        hasSource("Achat07.txt; AchatTaxesTreatment07.txt"), //
                        hasNote("Référence 16S7998181666921"), //
                        hasAmount("EUR", 1774.20), hasGrossValue("EUR", 1765.00), //
                        hasTaxes("EUR", 5.30), hasFees("EUR", 3.90))));
    }

    @Test
    public void testCompteAchatTaxesTreatment07()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AchatTaxesTreatment07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasWkn(null), hasTicker(null), //
                        hasName("VEOLIA ENVIRONNEMENT"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-04-22T00:00"), hasShares(100.00), //
                        hasSource("AchatTaxesTreatment07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.30), hasGrossValue("EUR", 5.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDeposit01()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-03-02T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 3000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-03-10T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("REGULARISATION"), //
                        hasAmount("EUR", 331.98))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-03-23T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 4000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-04-14T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 6000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-04-23T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 2000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-06-05T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 2000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-07-20T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 2000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-10-05T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("VERSEMENT"), //
                        hasAmount("EUR", 3000.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-10-19T00:00"), hasSource("Deposit01.txt"), //
                        hasNote("REGULARISATION"), //
                        hasAmount("EUR", 4.93))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

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
    public void testDividende02()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010208488"), hasWkn(null), hasTicker(null), //
                        hasName("ENGIE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-28T00:00"), hasShares(100.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 140.00), hasGrossValue("EUR", 140.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2655993207"), hasWkn(null), hasTicker(null), //
                        hasName("AMUND.MSCI WORLD D"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-10T00:00"), hasShares(211.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.31), hasGrossValue("EUR", 44.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTaxesTreatment01()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TaxesTreatment01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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
                        hasSource("TaxesTreatment01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.47), hasGrossValue("EUR", 1.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTaxesTreatment02()
    {
        var extractor = new ArkeaDirectBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TaxesTreatment02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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
                        hasSource("TaxesTreatment02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.17), hasGrossValue("EUR", 15.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-04T00:00"), hasShares(450.00), //
                        hasSource("TaxesTreatment02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.45), hasGrossValue("EUR", 14.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
