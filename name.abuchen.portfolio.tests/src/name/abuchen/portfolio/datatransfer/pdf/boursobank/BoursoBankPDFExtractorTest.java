package name.abuchen.portfolio.datatransfer.pdf.boursobank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
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

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.BoursoBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class BoursoBankPDFExtractorTest
{
    @Test
    public void testCompteAChat01()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat01.txt"), errors);

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
                        hasName("ISHS VI-ISMWSPE EOA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-29T15:38:46"), hasShares(400.00), //
                        hasSource("AChat01.txt"), //
                        hasNote("à cours limite | Référence : 398406803961"), //
                        hasAmount("EUR", 2008.64), hasGrossValue("EUR", 2008.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteAChat02()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat02.txt"), errors);

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
                        hasIsin("LU1681043599"), hasWkn(null), hasTicker(null), //
                        hasName("AM.MSCI WORLD UCITS ETF EUR C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-19T11:24:01"), hasShares(2.00), //
                        hasSource("AChat02.txt"), //
                        hasNote("à cours limite | Référence : 105902058977"), //
                        hasAmount("EUR", 969.85), hasGrossValue("EUR", 965.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.83))));
    }

    @Test
    public void testCompteAChat03()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat03.txt"), errors);

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
                        hasIsin("FR0000288946"), hasWkn(null), hasTicker(null), //
                        hasName("AXA COURT TERME A CAP.SI.2DEC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-27T00:00"), hasShares(1.02), //
                        hasSource("AChat03.txt"), //
                        hasNote("Référence : 331075868483"), //
                        hasAmount("EUR", 2513.95), hasGrossValue("EUR", 2513.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteAChat04()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat04.txt"), errors);

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
                        hasIsin("FR0000447039"), hasWkn(null), hasTicker(null), //
                        hasName("AXA PEA REGULARITE C FCP 4DEC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-05T00:00"), hasShares(8.00), //
                        hasSource("AChat04.txt"), //
                        hasNote("Référence : 507648049881"), //
                        hasAmount("EUR", 802.22), hasGrossValue("EUR", 802.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteAChat05()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat05.txt"), errors);

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
                        hasIsin("LU1681043599"), hasWkn(null), hasTicker(null), //
                        hasName("AM.MSCI WORLD UCITS ETF EUR C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-08T14:20:31"), hasShares(7.00), //
                        hasSource("AChat05.txt"), //
                        hasNote("au marché | Référence : 337163166408"), //
                        hasAmount("EUR", 3148.33), hasGrossValue("EUR", 3132.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.66))));
    }

    @Test
    public void testCompteAChat06()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat06.txt"), errors);

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
                        hasIsin("US5486611073"), hasWkn(null), hasTicker(null), //
                        hasName("LOWE S"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-31T17:45:50"), hasShares(1.00), //
                        hasSource("AChat06.txt"), //
                        hasNote("non spécifié | Référence : 70Q065050p7509"), //
                        hasAmount("EUR", 195.13), hasGrossValue("EUR", 188.18), //
                        hasForexGrossValue("USD", 200.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.95))));
    }

    @Test
    public void testCompteAChat06WithSecurityInEUR()
    {
        var security = new Security("LOWE S", "EUR");
        security.setIsin("US5486611073");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BoursoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-31T17:45:50"), hasShares(1.00), //
                        hasSource("AChat06.txt"), //
                        hasNote("non spécifié | Référence : 70Q065050p7509"), //
                        hasAmount("EUR", 195.13), hasGrossValue("EUR", 188.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testCompteAChat07()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat07.txt"), errors);

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
                        hasIsin("US67066G1040"), hasWkn(null), hasTicker(null), //
                        hasName("NVIDIA"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-10T18:05:46"), hasShares(18.00), //
                        hasSource("AChat07.txt"), //
                        hasNote("non spécifié | Référence : 09R240610U2204"), //
                        hasAmount("EUR", 2052.28), hasGrossValue("EUR", 2045.33), //
                        hasForexGrossValue("USD", 2195.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.95))));
    }

    @Test
    public void testCompteAchat07WithSecurityInEUR()
    {
        var security = new Security("NVIDIA", "EUR");
        security.setIsin("US67066G1040");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BoursoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AChat07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-10T18:05:46"), hasShares(18.00), //
                        hasSource("AChat07.txt"), //
                        hasNote("non spécifié | Référence : 09R240610U2204"), //
                        hasAmount("EUR", 2052.28), hasGrossValue("EUR", 2045.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testCompteVente01()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vente01.txt"), errors);

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
                        hasIsin("FR0013412020"), hasWkn(null), hasTicker(null), //
                        hasName("AM.PEA MSCI EM.MKTS UC.ETF FCP"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-29T00:00"), hasShares(42.00), //
                        hasSource("Vente01.txt"), //
                        hasNote("à cours limiteCours demandé : 20,5500 EUR | Référence : 493029272303"), //
                        hasAmount("EUR", 858.78), hasGrossValue("EUR", 863.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.32))));
    }

    @Test
    public void testCompteVente02()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vente02.txt"), errors);

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
                        hasIsin("FR0000288946"), hasWkn(null), hasTicker(null), //
                        hasName("AXA COURT TERME A CAP.SI.2DEC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-07T00:00"), hasShares(0.08), //
                        hasSource("Vente02.txt"), //
                        hasNote("Référence : 447335060329"), //
                        hasAmount("EUR", 198.03), hasGrossValue("EUR", 198.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteVente03()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vente03.txt"), errors);

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
                        hasIsin("FR0000447039"), hasWkn(null), hasTicker(null), //
                        hasName("AXA PEA REGULARITE C FCP 4DEC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-30T00:00"), hasShares(19.9890), //
                        hasSource("Vente03.txt"), //
                        hasNote("Référence : 203314830732"), //
                        hasAmount("EUR", 2003.12), hasGrossValue("EUR", 2003.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompteVente04()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vente04.txt"), errors);

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
                        hasIsin("US46625H1005"), hasWkn(null), hasTicker(null), //
                        hasName("JPMORGAN CHASE"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-07T16:22:40"), hasShares(10.00), //
                        hasSource("Vente04.txt"), //
                        hasNote("à cours limite | Référence : 09R241107U2540"), //
                        hasAmount("EUR", 2215.40), hasGrossValue("EUR", 2222.35), //
                        hasForexGrossValue("USD", 2400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.95))));
    }

    @Test
    public void testCompteVenteWithSecurityInEUR()
    {
        var security = new Security("JPMORGAN CHASE", "EUR");
        security.setIsin("US46625H1005");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new BoursoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vente04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-07T16:22:40"), hasShares(10.00), //
                        hasSource("Vente04.txt"), //
                        hasNote("à cours limite | Référence : 09R241107U2540"), //
                        hasAmount("EUR", 2215.40), hasGrossValue("EUR", 2222.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new BoursoBankPDFExtractor(new Client());

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
                        hasIsin("IE00B1FZS350"), hasWkn(null), hasTicker(null), //
                        hasName("ISHS DEV MK PRO US"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-15T00:00"), hasShares(248.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 28.24), hasGrossValue("EUR", 40.33), //
                        hasTaxes("EUR", 12.09), hasFees("EUR", 0.00))));
    }
}
