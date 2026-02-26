package name.abuchen.portfolio.datatransfer.pdf.kbcgroupnv;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.KBCGroupNVPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class KBCGroupNVPDFExtractorTest
{
    @Test
    public void testAankoop01()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aankoop01.txt"), errors);

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
                        hasIsin("NL0013654783"), hasWkn(null), hasTicker(null), //
                        hasName("PROSUS N.V. (AS)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-02T14:50:02"), hasShares(15.00), //
                        hasSource("Aankoop01.txt"), //
                        hasNote("Borderel 275825809"), //
                        hasAmount("EUR", 868.50), hasGrossValue("EUR", 858.00), //
                        hasTaxes("EUR", 3.00), hasFees("EUR", 7.50))));
    }

    @Test
    public void testAankoop02()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aankoop02.txt"), errors);

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
                        hasIsin("BE0974256852"), hasWkn(null), hasTicker(null), //
                        hasName("COLRUYT GROUP NV (BR)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-13T10:33:16"), hasShares(395.00), //
                        hasSource("Aankoop02.txt"), //
                        hasNote("Borderel 019564655"), //
                        hasAmount("EUR", 15249.93), hasGrossValue("EUR", 15136.40), //
                        hasTaxes("EUR", 52.98), hasFees("EUR", 22.71 + 37.84))));
    }

    @Test
    public void testMultiblerAankoopVerkoop01()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "MultiblerAankoopVerkoop01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("ISHAR.III CORE EUR CORP BD UC ETF-D"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("IS CO S&P500 U.ETF USD(ACC-PTG.K"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE0001141836"), hasWkn(null), hasTicker(null), //
                        hasName("GERMANY 21-26 0% 10/04 REGS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("ES0000012I08"), hasWkn(null), hasTicker(null), //
                        hasName("SPAIN 21-28 0% 31/01"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("EU000A3KWCF4"), hasWkn(null), hasTicker(null), //
                        hasName("EUROP UN 21-28 0% 04/10 REGS MTN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("EU000A3KWCF4"), hasWkn(null), hasTicker(null), //
                        hasName("EUROP UN 21-28 0% 04/10 REGS MTN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("EU000A3KRJQ6"), hasWkn(null), hasTicker(null), //
                        hasName("EUROPEAN UNION 21-29 0% 04/07 REGS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-02T10:01:45"), hasShares(1070.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040826294"), //
                        hasAmount("EUR", 125824.15), hasGrossValue("EUR", 127005.21), //
                        hasTaxes("EUR", 839.15 + 151.40), hasFees("EUR", 190.51))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-02T10:37:23"), hasShares(94.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040836544"), //
                        hasAmount("EUR", 50477.27), hasGrossValue("EUR", 50587.98), //
                        hasTaxes("EUR", 60.71), hasFees("EUR", 50.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T11:46:58"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040854432"), //
                        hasAmount("EUR", 38560.00), hasGrossValue("EUR", 38500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 60.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T11:53:00"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040855313"), //
                        hasAmount("EUR", 36732.00), hasGrossValue("EUR", 36672.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 60.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T11:58:19"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040855874"), //
                        hasAmount("EUR", 36171.28), hasGrossValue("EUR", 36068.00), //
                        hasTaxes("EUR", 43.28), hasFees("EUR", 60.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T12:09:44"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040857050"), //
                        hasAmount("EUR", 35622.62), hasGrossValue("EUR", 35520.00), //
                        hasTaxes("EUR", 42.62), hasFees("EUR", 60.00))));
    }

    @Test
    public void testMultiblerAankoopVerkoop02()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "MultiblerAankoopVerkoop02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR", "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IT0003128367"), hasWkn(null), hasTicker(null), //
                        hasName("ENEL SPA (MI)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BE0003810273"), hasWkn(null), hasTicker(null), //
                        hasName("PROXIMUS SA (BR)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("TESLA INC (NY)"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0334081137"), hasWkn(null), hasTicker(null), //
                        hasName("CRISPR THERAPEUTICS AG (NY)"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-05T12:33:57"), hasShares(1000.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016789124"), //
                        hasAmount("EUR", 6727.17), hasGrossValue("EUR", 6778.00), //
                        hasTaxes("EUR", 23.72), hasFees("EUR", 2.11 + 25.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-05T12:33:57"), hasShares(520.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016789126"), //
                        hasAmount("EUR", 3133.31), hasGrossValue("EUR", 3169.40), //
                        hasTaxes("EUR", 11.09), hasFees("EUR", 25.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-05T15:30:00"), hasShares(39.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016841507"), //
                        hasAmount("USD", 10718.20), hasGrossValue("USD", 10638.42), //
                        hasTaxes("USD", 37.23), hasFees("USD", 15.95 + 26.60))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-05T15:30:01"), hasShares(36.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016844337"), //
                        hasAmount("USD", 1538.22), hasGrossValue("USD", 1570.32), //
                        hasTaxes("USD", 5.50), hasFees("USD", 26.55 + 0.05))));
    }

    @Test
    public void testMultiblerAankoopVerkoop02WithSecurityInEUR()
    {
        var security1 = new Security("TESLA INC (NY)", "USD");
        security1.setIsin("US88160R1014");

        var security2 = new Security("CRISPR THERAPEUTICS AG (NY)", "EUR");
        security2.setIsin("CH0334081137");

        var client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);

        var extractor = new KBCGroupNVPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "MultiblerAankoopVerkoop02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR", "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IT0003128367"), hasWkn(null), hasTicker(null), //
                        hasName("ENEL SPA (MI)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BE0003810273"), hasWkn(null), hasTicker(null), //
                        hasName("PROXIMUS SA (BR)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-05T12:33:57"), hasShares(1000.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016789124"), //
                        hasAmount("EUR", 6727.17), hasGrossValue("EUR", 6778.00), //
                        hasTaxes("EUR", 23.72), hasFees("EUR", 2.11 + 25.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-05T12:33:57"), hasShares(520.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016789126"), //
                        hasAmount("EUR", 3133.31), hasGrossValue("EUR", 3169.40), //
                        hasTaxes("EUR", 11.09), hasFees("EUR", 25.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-05T15:30:00"), hasShares(39.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016841507"), //
                        hasAmount("USD", 10718.20), hasGrossValue("USD", 10638.42), //
                        hasTaxes("USD", 37.23), hasFees("USD", 15.95 + 26.60))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-05T15:30:01"), hasShares(36.00), //
                        hasSource("MultiblerAankoopVerkoop02.txt"), //
                        hasNote("Borderel 016844337"), //
                        hasAmount("USD", 1538.22), hasGrossValue("USD", 1570.32), //
                        hasTaxes("USD", 5.50), hasFees("USD", 26.55 + 0.05))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

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
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("ISHAR.III CORE EUR CORP BD UC ETF-D"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-24T00:00"), hasShares(2065.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Borderel 003308592"), //
                        hasAmount("EUR", 2862.79), hasGrossValue("EUR", 4173.16), //
                        hasTaxes("EUR", 1251.95 + 10.14), hasFees("EUR", 48.28))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA32076V1031"), hasWkn(null), hasTicker(null), //
                        hasName("FIRST MAJESTIC SILVER CORP"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-14T00:00"), hasShares(2130.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Borderel 019817049"), //
                        hasAmount("USD", 6.37), hasGrossValue("USD", 12.14), //
                        hasTaxes("USD", 3.04 + 2.73), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("FIRST MAJESTIC SILVER CORP", "EUR");
        security.setIsin("CA32076V1031");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new KBCGroupNVPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-14T00:00"), hasShares(2130.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Borderel 019817049"), //
                        hasAmount("USD", 6.37), hasGrossValue("USD", 12.14), //
                        hasForexGrossValue("EUR", 11.14), //
                        hasTaxes("USD", 3.04 + 2.73), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0126531013"), hasWkn(null), hasTicker(null), //
                        hasName("CP ALBEMARLE CORP (NY) 14.12.23"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-02T00:00"), hasShares(43.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Borderel 000167639"), //
                        hasAmount("USD", 10.23), hasGrossValue("USD", 17.20), //
                        hasTaxes("USD", 2.58 + 4.39), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("CP ALBEMARLE CORP (NY) 14.12.23", "EUR");
        security.setIsin("US0126531013");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new KBCGroupNVPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-02T00:00"), hasShares(43.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Borderel 000167639"), //
                        hasAmount("USD", 10.23), hasGrossValue("USD", 17.20), //
                        hasForexGrossValue("EUR", 15.50), //
                        hasTaxes("USD", 2.58 + 4.39), hasFees("USD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("USD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

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
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("ISHAR.III CORE EUR CORP BD UC"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-29T00:00"), hasShares(912.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Borderel 006631462"), //
                        hasAmount("EUR", 1276.04), hasGrossValue("EUR", 1860.12), //
                        hasTaxes("EUR", 558.04 + 4.52), hasFees("EUR", 21.52))));
    }

    @Test
    public void testRekeninguittreksel01()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-04"), hasAmount("EUR", 32339.70), //
                        hasSource("Rekeninguittreksel01.txt"), hasNote("Overschrijving naar klant"))));
    }

    @Test
    public void testRekeninguittreksel02()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-18"), hasAmount("EUR", 50000.00), //
                        hasSource("Rekeninguittreksel02.txt"), hasNote("Provisionering rekening klant"))));
    }

    @Test
    public void testRekeninguittreksel03()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR", "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES PLC CORE MSC E.M.IM UC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-12T11:55:21"), hasShares(2300.00), //
                        hasSource("Rekeninguittreksel03.txt"), //
                        hasNote("Borderel 017462864"), //
                        hasAmount("USD", 69606.12), hasGrossValue("USD", 69743.43), //
                        hasForexGrossValue("EUR", 64918.22), //
                        hasTaxes("USD", (78.06 / 0.932651)), hasFees("USD", (50.00 / 0.932651)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-12"), hasAmount("EUR", 45000.00), //
                        hasSource("Rekeninguittreksel03.txt"), hasNote("Provisionering rekening klant"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-12"), hasAmount("EUR", 132673.91), //
                        hasSource("Rekeninguittreksel03.txt"), hasNote("Provisionering rekening klant"))));
    }

    @Test
    public void testRekeninguittreksel04()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-05"), hasAmount("EUR", 5.00), //
                        hasSource("Rekeninguittreksel04.txt"), hasNote("Provisionering rekening klant"))));
    }

    @Test
    public void testRekeninguittreksel05()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ENER6Y0"), hasWkn(null), hasTicker(null), //
                        hasName("SIEMENS ENERGY AG NA ON (FR)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-08-05T09:05:43"), hasShares(1670.00), //
                        hasSource("Rekeninguittreksel05.txt"), //
                        hasNote("Borderel 036075923"), //
                        hasAmount("EUR", 37690.98), hasGrossValue("EUR", 37975.80), //
                        hasTaxes("EUR", 132.92), hasFees("EUR", 56.96 + 94.94))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-08-05"), hasAmount("EUR", 179.48), //
                        hasSource("Rekeninguittreksel05.txt"), hasNote("Bewaarloon"))));
    }

    @Test
    public void testRekeninguittreksel06()
    {
        var extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 1275.00), //
                        hasSource("Rekeninguittreksel06.txt"), hasNote("Overschrijving naar klant"))));
    }
}
