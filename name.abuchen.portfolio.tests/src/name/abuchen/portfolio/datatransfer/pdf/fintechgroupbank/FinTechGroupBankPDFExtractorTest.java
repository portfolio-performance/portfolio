package name.abuchen.portfolio.datatransfer.pdf.fintechgroupbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class FinTechGroupBankPDFExtractorTest
{
    FinTechGroupBankPDFExtractor extractor = new FinTechGroupBankPDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testFinTechSammelabrechnung01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005194062"), hasWkn("519406"), hasTicker(null), //
                        hasName("BAYWA AG VINK.NA. O.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE0008402215"), hasWkn("840221"), hasTicker(null), //
                        hasName("HANN.RUECK SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-01-28T12:50"), hasShares(150.00), //
                        hasSource("FinTechSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 678984193"), //
                        hasAmount("EUR", 5893.10), hasGrossValue("EUR", 5887.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90 + 1.00 + 1.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2014-01-28T12:58"), hasShares(100.00), //
                        hasSource("FinTechSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 678985130"), //
                        hasAmount("EUR", 5954.80), hasGrossValue("EUR", 5948.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90 + 1.00 + 1.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2014-01-28T12:58"), hasShares(100.00), //
                        hasSource("FinTechSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 678985130"), //
                        hasAmount("EUR", 5943.00), hasGrossValue("EUR", 5948.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90 + 1.00 + 1.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2014-01-28T12:58"), hasShares(100.00), //
                        hasSource("FinTechSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 678985130"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechSammelabrechnung02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000CQ0U7Z4"), hasWkn("CQ0U7Z"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL19 MGA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CY1SEV5"), hasWkn("CY1SEV"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL19 NVD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CY49224"), hasWkn("CY4922"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL19 AZ5"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CQ0J2E5"), hasWkn("CQ0J2E"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL19 XIX"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CQ0L8X8"), hasWkn("CQ0L8X"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL19 IUI1"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000TD53H52"), hasWkn("TD53H5"), hasTicker(null), //
                        hasName("HSBC T+B CALL19 HDI"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000DM6DCB5"), hasWkn("DM6DCB"), hasTicker(null), //
                        hasName("DEUT.BANK CALL19 MSF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CQ0LAL1"), hasWkn("CQ0LAL"), hasTicker(null), //
                        hasName("CITI.GL.M. CALL19 MCP"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000TD53FP5"), hasWkn("TD53FP"), hasTicker(null), //
                        hasName("HSBC T+B CALL19 FB2A"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000TD752U9"), hasWkn("TD752U"), hasTicker(null), //
                        hasName("HSBC T+B CALL19 TL0"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T14:41"), hasShares(4550.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301138113"), //
                        hasAmount("EUR", 3008.90), hasGrossValue("EUR", 3003.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T14:46"), hasShares(745.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301140879"), //
                        hasAmount("EUR", 3000.80), hasGrossValue("EUR", 2994.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T14:46"), hasShares(4100.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301141388"), //
                        hasAmount("EUR", 3039.90), hasGrossValue("EUR", 3034.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T14:48"), hasShares(2870.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301141655"), //
                        hasAmount("EUR", 3019.40), hasGrossValue("EUR", 3013.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T14:51"), hasShares(4300.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301143554"), //
                        hasAmount("EUR", 3015.90), hasGrossValue("EUR", 3010.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T14:53"), hasShares(2050.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301143813"), //
                        hasAmount("EUR", 3019.40), hasGrossValue("EUR", 3013.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T15:09"), hasShares(2470.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301160198"), //
                        hasAmount("EUR", 2992.60), hasGrossValue("EUR", 2988.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T15:30"), hasShares(2280.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301175761"), //
                        hasAmount("EUR", 3015.50), hasGrossValue("EUR", 3009.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T15:31"), hasShares(1145.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301175892"), //
                        hasAmount("EUR", 3017.25), hasGrossValue("EUR", 3011.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-01T15:52"), hasShares(1247.00), //
                        hasSource("FinTechSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 1301188569"), //
                        hasAmount("EUR", 5417.88), hasGrossValue("EUR", 5411.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testFinTechSammelabrechnung03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung03.txt"), errors);

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
                        hasIsin("DE000A1MECS1"), hasWkn("A1MECS"), hasTicker(null), //
                        hasName("SOURCE PHY.MRKT.ETC00 XAU"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-01-09T15:00"), hasShares(0.025361), //
                        hasSource("FinTechSammelabrechnung03.txt"), //
                        hasNote("Transaktion-Nr.: 1344625752"), //
                        hasAmount("EUR", 2.72), hasGrossValue("EUR", 2.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechSammelabrechnung04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung04.txt"), errors);

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
                        hasIsin("DE0001234567"), hasWkn("DS5WKN"), hasTicker(null), //
                        hasName("DEUT.BANK CALL20 BBB"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-13T16:20"), hasShares(2000.00), //
                        hasSource("FinTechSammelabrechnung04.txt"), //
                        hasNote("Transaktion-Nr.: 1234567895"), //
                        hasAmount("EUR", 1023.90), hasGrossValue("EUR", 1020.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));
    }

    @Test
    public void testFinTechSammelabrechnung05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VN4LAU4"), hasWkn("VN4LAU"), hasTicker(null), //
                        hasName("VONT.FINL PR CALL17 DAX"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000VN547F8"), hasWkn("VN547F"), hasTicker(null), //
                        hasName("VONT.FINL PR PUT17 DAX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-01-02T13:15"), hasShares(1750.00), //
                        hasSource("FinTechSammelabrechnung05.txt"), //
                        hasNote("Transaktion-Nr.: 1147218952"), //
                        hasAmount("EUR", 1036.40), hasGrossValue("EUR", 1032.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2017-01-02T14:55"), hasShares(1250.00), //
                        hasSource("FinTechSammelabrechnung05.txt"), //
                        hasNote("Transaktion-Nr.: 1147259184"), //
                        hasAmount("EUR", 1003.90), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));

        assertThat(results, hasItem(sale( //
                        hasDate("2017-01-02T16:00"), hasShares(1750.00), //
                        hasSource("FinTechSammelabrechnung05.txt"), //
                        hasNote("Transaktion-Nr.: 1147293642"), //
                        hasAmount("EUR", 1232.40), hasGrossValue("EUR", 1312.50), //
                        hasTaxes("EUR", 76.20), hasFees("EUR", 3.90))));

        assertThat(results, hasItem(sale( //
                        hasDate("2017-01-02T16:07"), hasShares(1250.00), //
                        hasSource("FinTechSammelabrechnung05.txt"), //
                        hasNote("Transaktion-Nr.: 1147294899"), //
                        hasAmount("EUR", 844.10), hasGrossValue("EUR", 850.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-01-02T16:07"), hasShares(1250.00), //
                        hasSource("FinTechSammelabrechnung05.txt"), //
                        hasNote("Transaktion-Nr.: 1147294899"), //
                        hasAmount("EUR", 44.72), hasGrossValue("EUR", 44.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechSammelabrechnung06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung06.txt"), errors);

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
                        hasIsin("DE000SKWM021"), hasWkn("SKWM02"), hasTicker(null), //
                        hasName("SKW STAHL-METAL.HLDG.NA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-09-08T08:32"), hasShares(460.00), //
                        hasSource("FinTechSammelabrechnung06.txt"), //
                        hasNote("Transaktion-Nr.: 1087224318"), //
                        hasAmount("EUR", 1253.15), hasGrossValue("EUR", 1265.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 6.85))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2016-09-08T08:32"), hasShares(460.00), //
                        hasSource("FinTechSammelabrechnung06.txt"), //
                        hasNote("Transaktion-Nr.: 1087224318"), //
                        hasAmount("EUR", 463.04), hasGrossValue("EUR", 463.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechSammelabrechnung07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechSammelabrechnung07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VN4LAU4"), hasWkn("VN4LAU"), hasTicker(null), //
                        hasName("VONT.FINL PR CALL17 DAX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-01-02T13:15"), hasShares(1750.00), //
                        hasSource("FinTechSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 1147218956"), //
                        hasAmount("EUR", 1036.40), hasGrossValue("EUR", 1032.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VN547F8"), hasWkn("VN547F"), hasTicker(null), //
                        hasName("VONT.FINL PR PUT17 DAX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-01-02T14:55"), hasShares(1250.00), //
                        hasSource("FinTechSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 1147259186"), //
                        hasAmount("EUR", 1003.90), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));

        assertThat(results, hasItem(sale( //
                        hasDate("2017-01-02T16:00"), hasShares(1750.00), //
                        hasSource("FinTechSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 1147293640"), //
                        hasAmount("EUR", 1232.40), hasGrossValue("EUR", 1312.50), //
                        hasTaxes("EUR", 76.20), hasFees("EUR", 3.90))));

        assertThat(results, hasItem(sale( //
                        hasDate("2017-01-02T16:07"), hasShares(1250.00), //
                        hasSource("FinTechSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 1147294897"), //
                        hasAmount("EUR", 844.10), hasGrossValue("EUR", 850.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-01-02T16:07"), hasShares(1250.00), //
                        hasSource("FinTechSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 1147294897"), //
                        hasAmount("EUR", 44.72), hasGrossValue("EUR", 44.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testbiwAGKauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGKauf01.txt"), errors);

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
                        hasIsin("LU0392495023"), hasWkn("ETF114"), hasTicker(null), //
                        hasName("C.S.-MSCI PACIF.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-12-03T13:59"), hasShares(10.00), //
                        hasSource("biwAGKauf01.txt"), //
                        hasNote("Transaktion-Nr.: 999999999"), //
                        hasAmount("EUR", 50.30), hasGrossValue("EUR", 44.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testbiwAGKauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGKauf02.txt"), errors);

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
                        hasIsin("LU0378438732"), hasWkn("ETF001"), hasTicker(null), //
                        hasName("COMST.-DAX TR UCITS ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-08-01T00:00"), hasShares(2.460378), //
                        hasSource("biwAGKauf02.txt"), //
                        hasNote("Transaktion-Nr.: 1071613216"), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testbiwAGWertpapierEingang01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGWertpapierEingang01.txt"), errors);

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
                        hasIsin("DE000US9RGR9"), hasWkn(null), hasTicker(null), //
                        hasName("UBS AG LONDON 14/16 RWE"), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-11-24T00:00"), hasShares(250.00), //
                        hasSource("biwAGWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 952921288"), //
                        hasAmount("EUR", 7517.50), hasGrossValue("EUR", 7517.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testbiwAGWertpapierAusgang01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGWertpapierAusgang01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000CM35Z36"), hasWkn(null), hasTicker(null), //
                        hasName("COMMERZBANK INLINE09EO/SF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2009-12-29"), hasShares(650.00), //
                        hasSource("biwAGWertpapierAusgang01.txt"), //
                        hasNote("Transaktion-Nr.: 203036888"), //
                        hasAmount("EUR", 0.65), hasGrossValue("EUR", 0.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2009-12-29"), hasShares(650.00), //
                        hasSource("biwAGWertpapierAusgang01.txt"), //
                        hasNote("Transaktion-Nr.: 203036888"), //
                        hasAmount("EUR", 382.12), hasGrossValue("EUR", 382.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SG1JXM7"), hasWkn(null), hasTicker(null), //
                        hasName("SG EFF. INLINE09 DAX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2009-12-29"), hasShares(13.00), //
                        hasSource("biwAGWertpapierAusgang01.txt"), //
                        hasNote("Transaktion-Nr.: 203037029"), //
                        hasAmount("EUR", 130.00), hasGrossValue("EUR", 130.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testbiwAGKontoauszug01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2014-11-12"), //
                        hasSource("biwAGKontoauszug01.txt"), //
                        hasNote("Gebühr Kapitaltransaktion Ausland ISIN12345678"), //
                        hasAmount("EUR", 4.56), hasGrossValue("EUR", 4.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2014-12-31"), //
                        hasSource("biwAGKontoauszug01.txt"), //
                        hasNote("Zinsabschluss 01.10.2014 - 31.12.2014"), //
                        hasAmount("EUR", 7.89), hasGrossValue("EUR", 7.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testbiwAGKontoauszug02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGKontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2011-02-10"), hasAmount("EUR", 1300.00), //
                        hasSource("biwAGKontoauszug02.txt"), hasNote("CASH / 0/377366"))));
    }

    @Test
    public void testbiwAGKontoauszug03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "biwAGKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2013-01-28"), //
                        hasSource("biwAGKontoauszug03.txt"), //
                        hasNote("flatex trader 2.0 Basis"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2013-02-14"), //
                        hasSource("biwAGKontoauszug03.txt"), //
                        hasNote("Gebühr Kapitaltransaktion Ausland US0378331005"), //
                        hasAmount("EUR", 1.50), hasGrossValue("EUR", 1.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2013-03-01"), //
                        hasSource("biwAGKontoauszug03.txt"), //
                        hasNote("flatex trader 2.0 Basis"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2013-03-28"), //
                        hasSource("biwAGKontoauszug03.txt"), //
                        hasNote("flatex trader 2.0 Basis"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2013-03-31"), hasShares(0.00), //
                                        hasSource("biwAGKontoauszug03.txt"), //
                                        hasNote("Zinsabschluss 01.01.2013 - 31.03.2013"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFinTechKauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf01.txt"), errors);

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
                        hasIsin("IE00B2QWCY14"), hasWkn("A0Q1YY"), hasTicker(null), //
                        hasName("ISHSIII-S+P SM.CAP600 DLD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-12-15T00:00"), hasShares(19.334524), //
                        hasSource("FinTechKauf01.txt"), //
                        hasNote("Transaktion-Nr.: 1137201681"), //
                        hasAmount("EUR", 1050.00), hasGrossValue("EUR", 1050.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf02.txt"), errors);

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
                        hasIsin("LU0392494992"), hasWkn("ETF113"), hasTicker(null), //
                        hasName("C.-MSCI NO.AM.TRN U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-16T00:00"), hasShares(13.268957), //
                        hasSource("FinTechKauf02.txt"), //
                        hasNote("Transaktion-Nr.: 1234211246"), //
                        hasAmount("EUR", 800.00), hasGrossValue("EUR", 800.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKauf03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf03.txt"), errors);

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
                        hasIsin("LU0328475792"), hasWkn("DBX1A7"), hasTicker(null), //
                        hasName("DB X-TR.S.E.600U.E.(DR)1C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-15T00:00"), hasShares(5.082011), //
                        hasSource("FinTechKauf03.txt"), //
                        hasNote("Transaktion-Nr.: 1233799247"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKauf04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf04.txt"), errors);

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
                        hasIsin("IE00B2NPKV68"), hasWkn("A0NECU"), hasTicker(null), //
                        hasName("ISHSII-JPM DL EM BD DLDIS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-15T00:00"), hasShares(9.703363), //
                        hasSource("FinTechKauf04.txt"), //
                        hasNote("Transaktion-Nr.: 1234387912"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 999.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));
    }

    @Test
    public void testFinTechKauf05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf05.txt"), errors);

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
                        hasIsin("IE00B3S5XW04"), hasWkn("A1JJTP"), hasTicker(null), //
                        hasName("SPDR BARC.EO.GOV.BD ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-01-09T15:00"), hasShares(0.099044), //
                        hasSource("FinTechKauf05.txt"), //
                        hasNote("Transaktion-Nr.: 1344974056"), //
                        hasAmount("EUR", 6.16), hasGrossValue("EUR", 6.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKauf06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf06.txt"), errors);

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
                        hasIsin("LU0274211480"), hasWkn("DBX1DA"), hasTicker(null), //
                        hasName("DB X-TRACK.DAX ETF(DR)1C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-01-02T00:00"), hasShares(7.979324), //
                        hasSource("FinTechKauf06.txt"), //
                        hasNote("Transaktion-Nr.: 1342424242"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKauf07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf07.txt"), errors);

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
                        hasIsin("IE00B6YX5D40"), hasWkn("A1JKS0"), hasTicker(null), //
                        hasName("SPDR S+P US DIV.ARIST.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-01-02T00:00"), hasShares(22.973458), //
                        hasSource("FinTechKauf07.txt"), //
                        hasNote("Transaktion-Nr.: 1340886542"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 998.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testFinTechKauf08()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf08.txt"), errors);

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
                        hasIsin("LU0635178014"), hasWkn("ETF127"), hasTicker(null), //
                        hasName("COMS.-MSCI EM.M.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-03T00:00"), hasShares(1.43414), //
                        hasSource("FinTechKauf08.txt"), //
                        hasNote("Transaktion-Nr.: 1555928306"), //
                        hasAmount("EUR", 52.50), hasGrossValue("EUR", 52.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKauf09()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf09.txt"), errors);

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
                        hasIsin("IE00BF2B0K52"), hasWkn("A2DTF1"), hasTicker(null), //
                        hasName("FRAN.LIB.Q EM EQ.UC.DLA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-17T17:52"), hasShares(61.00), //
                        hasSource("FinTechKauf09.txt"), //
                        hasNote("Transaktion-Nr.: 1111111111"), //
                        hasAmount("EUR", 1279.55), hasGrossValue("EUR", 1270.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.71))));
    }

    @Test
    public void testFinTechKauf10()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKauf10.txt"), errors);

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-04T00:00"), hasShares(8.205431), //
                        hasSource("FinTechKauf10.txt"), //
                        hasNote("Transaktion-Nr.: 1321692761"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-12-04T00:00"), hasShares(8.205431), //
                        hasSource("FinTechKauf10.txt"), //
                        hasNote("Transaktion-Nr.: 1321692761"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKaufStorno01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKaufStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0392494562"), //
                        hasWkn("ETF110"), //
                        hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2018-01-04"), //
                                        hasSource("FinTechKaufStorno01.txt"), //
                                        hasNote("Transaktion-Nr.: 1350807964"), //
                                        hasAmount("EUR", 400), //
                                        hasGrossValue("EUR", 400)))));
    }

    @Test
    public void testFinTechVerkauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechVerkauf01.txt"), errors);

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
                        hasIsin("DE000US9RGR9"), hasWkn("US9RGR"), hasTicker(null), //
                        hasName("UBS AG LONDON 14/16 RWE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-01-22T16:14"), hasShares(250.00), //
                        hasSource("FinTechVerkauf01.txt"), //
                        hasNote("Transaktion-Nr.: 980001189 | Zinsbetrag 9.264,06 EUR"), //
                        hasAmount("EUR", 16508.16), hasGrossValue("EUR", 16514.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testFinTechVerkauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechVerkauf02.txt"), errors);

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
                        hasIsin("DE0009807008"), hasWkn("980700"), hasTicker(null), //
                        hasName("GRUNDBESITZ EUROPA RC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-07-04T14:23"), hasShares(121.00), //
                        hasSource("FinTechVerkauf02.txt"), //
                        hasNote("Transaktion-Nr.: 1242877942"), //
                        hasAmount("EUR", 4840.15), hasGrossValue("EUR", 4846.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testFinTechVerkauf03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechVerkauf03.txt"), errors);

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
                        hasIsin("IE00B53HP851"), hasWkn("A0YEDM"), hasTicker(null), //
                        hasName("ISHSVII-FTSE 100 LS ACC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-01-09T15:00"), hasShares(0.007229), //
                        hasSource("FinTechVerkauf03.txt"), //
                        hasNote("Transaktion-Nr.: 1344971210"), //
                        hasAmount("EUR", 0.95), hasGrossValue("EUR", 0.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechVerkauf04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechVerkauf04.txt"), errors);

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
                        hasIsin("IE00BKWQ0D84"), hasWkn("A1191N"), hasTicker(null), //
                        hasName("SSGA S.E.E.II-M.EU.CON.S."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-02-06T00:00"), hasShares(0.089051), //
                        hasSource("FinTechVerkauf04.txt"), //
                        hasNote("Transaktion-Nr.: 1574141471"), //
                        hasAmount("EUR", 9.48), hasGrossValue("EUR", 15.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testFinTechDividende01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende01.txt"), errors);

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
                        hasIsin("DE0008402215"), hasWkn("840221"), hasTicker(null), //
                        hasName("HANN.RUECK SE NA O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-05-08T00:00"), hasExDate("2014-05-08"), //
                        hasShares(360.00), //
                        hasSource("FinTechDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 716759781"), //
                        hasAmount("EUR", 795.15), hasGrossValue("EUR", 1080.00), //
                        hasTaxes("EUR", 284.85), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechDividende02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende02.txt"), errors);

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
                        hasIsin("DE1234567890"), hasWkn("AB1234"), hasTicker(null), //
                        hasName("ISH.FOOBAR 12345666 x.EFT"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-01-15T00:00"), hasExDate("2014-01-15"), //
                        hasShares(99.00), //
                        hasSource("FinTechDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 111111111"), //
                        hasAmount("EUR", 55.55), hasGrossValue("EUR", 77.77), //
                        hasTaxes("EUR", 22.22), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechDividende03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende03.txt"), errors);

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
                        hasIsin("DE0006335003"), hasWkn("633500"), hasTicker(null), //
                        hasName("KRONES AG O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-06-23T00:00"), hasExDate("2017-06-21"), //
                        hasShares(15.00), //
                        hasSource("FinTechDividende03.txt"), //
                        hasNote("Transaktion-Nr.: 1236644834"), //
                        hasAmount("EUR", 17.13), hasGrossValue("EUR", 23.25), //
                        hasTaxes("EUR", 6.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechDividende04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende04.txt"), errors);

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
                        hasIsin("US8552441094"), hasWkn("884437"), hasTicker(null), //
                        hasName("STARBUCKS CORP."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-08-25T00:00"), hasExDate("2017-08-08"), //
                        hasShares(105.00), //
                        hasSource("FinTechDividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.45), hasGrossValue("EUR", 22.23), //
                        hasForexGrossValue("USD", 26.25), //
                        hasTaxes("EUR", 1.11 + (7.88 / 1.1808)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechDividende04WithSecurityInUSD()
    {
        var security = new Security("STARBUCKS CORP.", "EUR");
        security.setIsin("US8552441094");
        security.setWkn("884437");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-08-25T00:00"), hasExDate("2017-08-08"), //
                        hasShares(105.00), //
                        hasSource("FinTechDividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.45), hasGrossValue("EUR", 22.23), //
                        hasTaxes("EUR", 1.11 + (7.88 / 1.1808)), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFinTechDividende05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende05.txt"), errors);

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
                        hasIsin("GB00B03MLX29"), hasWkn("A0D94M"), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELL A EO-07"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-12-20T00:00"), hasExDate("2017-11-16"), //
                        hasShares(180.00), //
                        hasSource("FinTechDividende05.txt"), //
                        hasNote("Transaktion-Nr.: 0000000000"), //
                        hasAmount("EUR", 60.97), hasGrossValue("EUR", 71.73), //
                        hasTaxes("EUR", 10.76), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechDividende06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechDividende06.txt"), errors);

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
                        hasIsin("DE1234567890"), hasWkn("AB1234"), hasTicker(null), //
                        hasName("ISH.FOOBAR 12345666 x.EFT"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-04-28T00:00"), hasExDate("2016-04-28"), //
                        hasShares(10.00), //
                        hasSource("FinTechDividende06.txt"), //
                        hasNote("Transaktion-Nr.: 111111111"), //
                        hasAmount("EUR", 73.75), hasGrossValue("EUR", 73.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierAusgang01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierAusgang01.txt"),
                        errors);

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
                        hasIsin("DE000CM31SV9"), hasWkn(null), hasTicker(null), //
                        hasName("COMMERZBANK INLINE09EO/SF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2009-12-02T00:00"), hasShares(325.00), //
                        hasSource("FinTechWertpapierAusgang01.txt"), //
                        hasNote("Transaktion-Nr.: 197409035"), //
                        hasAmount("EUR", 2867.88), hasGrossValue("EUR", 3250.00), //
                        hasTaxes("EUR", 382.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierAusgang02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierAusgang02.txt"),
                        errors);

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
                        hasIsin("DE000CK1Q3N7"), hasWkn(null), hasTicker(null), //
                        hasName("COMMERZBANK INLINE11EO/SF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2011-07-18T00:00"), hasShares(200.00), //
                        hasSource("FinTechWertpapierAusgang02.txt"), //
                        hasNote("Transaktion-Nr.: 376762270"), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierAusgang03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierAusgang03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000CB81KN1"), hasWkn(null), hasTicker(null), //
                        hasName("COMMERZBANK PUT10 EOLS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CM3C8A3"), hasWkn(null), hasTicker(null), //
                        hasName("COMMERZBANK CALL10 EO/DL"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CM3C896"), hasWkn(null), hasTicker(null), //
                        hasName("COMMERZBANK CALL10 EO/DL"), //
                        hasCurrencyCode("EUR"))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2010-03-16T00:00"), hasShares(2000.00), //
                        hasSource("FinTechWertpapierAusgang03.txt"), //
                        hasNote("Transaktion-Nr.: 223770199"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2010-03-16T00:00"), hasShares(1250.00), //
                        hasSource("FinTechWertpapierAusgang03.txt"), //
                        hasNote("Transaktion-Nr.: 223770243"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2010-03-16T00:00"), hasShares(750.00), //
                        hasSource("FinTechWertpapierAusgang03.txt"), //
                        hasNote("Transaktion-Nr.: 223770249"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierAusgang04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierAusgang04.txt"),
                        errors);

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
                        hasIsin("DE000SG0WRD3"), hasWkn("SG0WRD"), hasTicker(null), //
                        hasName("SG EFF. TURBOL ZS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-09-28T00:00"), hasShares(83.00), //
                        hasSource("FinTechWertpapierAusgang04.txt"), //
                        hasNote("Transaktionsnummer: 921414163"), //
                        hasAmount("EUR", 111.22), hasGrossValue("EUR", 111.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierAusgang05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierAusgang05.txt"),
                        errors);

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
                        hasIsin("CH0585795898"), hasWkn("UE5KPQ"), hasTicker(null), //
                        hasName("UBS LDN CALL21 SQ3"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-25T00:00"), hasShares(400.00), //
                        hasSource("FinTechWertpapierAusgang05.txt"), //
                        hasNote("Transaktion-Nr.: 1234567890"), //
                        hasAmount("EUR", 305.20), hasGrossValue("EUR", 305.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-06-25T00:00"), hasShares(400.00), //
                        hasSource("FinTechWertpapierAusgang05.txt"), //
                        hasNote("Transaktion-Nr.: 1234567890"), //
                        hasAmount("EUR", 88.53), hasGrossValue("EUR", 88.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierAusgang06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierAusgang06.txt"),
                        errors);

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
                        hasIsin("LU0392494562"), hasWkn(null), hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0444605645"), hasWkn(null), hasTicker(null), //
                        hasName("C-IBO.E.L.S.D.O.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-18T00:00"), hasShares(310.00), //
                        hasSource("FinTechWertpapierAusgang06.txt"), //
                        hasNote("Transaktion-Nr.: 9876543211"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-18T00:00"), hasShares(118.00), //
                        hasSource("FinTechWertpapierAusgang06.txt"), //
                        hasNote("Transaktion-Nr.: 9876543210"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechWertpapierEingang01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechWertpapierEingang01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(19L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474503"), hasWkn(null), hasTicker(null), //
                        hasName("DEKAFONDS CF"), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-02-16T00:00"), hasShares(0.052), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461796"), //
                        hasAmount("EUR", 5.50), hasGrossValue("EUR", 5.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-02-20T00:00"), hasShares(0.003), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461797"), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-03-16T00:00"), hasShares(0.432), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461798"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-04-15T00:00"), hasShares(0.424), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461799"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-05-15T00:00"), hasShares(0.446), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461800"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-06-15T00:00"), hasShares(0.467), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461801"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-07-15T00:00"), hasShares(0.447), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461802"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-08-17T00:00"), hasShares(0.462), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461803"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-09-15T00:00"), hasShares(0.504), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461804"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-10-15T00:00"), hasShares(0.504), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461805"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-11-16T00:00"), hasShares(0.474), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461806"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2015-12-15T00:00"), hasShares(0.49), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461807"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-01-15T00:00"), hasShares(0.525), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461808"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-02-15T00:00"), hasShares(0.551), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461809"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-02-19T00:00"), hasShares(0.117), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461810"), //
                        hasAmount("EUR", 10.12), hasGrossValue("EUR", 10.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-03-15T00:00"), hasShares(0.523), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461811"), //
                        hasAmount("EUR", 49.98), hasGrossValue("EUR", 49.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-04-15T00:00"), hasShares(0.517), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461812"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-05-17T00:00"), hasShares(0.521), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461813"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2016-06-15T00:00"), hasShares(0.541), //
                        hasSource("FinTechWertpapierEingang01.txt"), //
                        hasNote("Transaktion-Nr.: 1127461814"), //
                        hasAmount("EUR", 49.99), hasGrossValue("EUR", 49.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKontoauszug01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-01-29T00:00"), hasAmount("EUR", 1100.00), //
                        hasSource("FinTechKontoauszug01.txt"), hasNote("Überweisung"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2016-03-31T00:00"), hasShares(0.00), //
                                        hasSource("FinTechKontoauszug01.txt"), //
                                        hasNote("Zinsabschluss 01.01.2016 - 31.03.2016"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFinTechKontoauszug02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-01-26"), hasAmount("EUR", 15000.00), //
                        hasSource("FinTechKontoauszug02.txt"), hasNote("Überweisung"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2016-03-31"), hasShares(0.00), //
                                        hasSource("FinTechKontoauszug02.txt"), //
                                        hasNote("Zinsabschluss 01.01.2016 - 31.03.2016"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFinTechKontoauszug03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2016-12-31T00:00"), hasAmount("EUR", 4.94), //
                        hasSource("FinTechKontoauszug03.txt"), hasNote("Steuertopfoptimierung 2016"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2016-09-30T00:00"), hasShares(0.00), //
                                        hasSource("FinTechKontoauszug03.txt"), //
                                        hasNote("Zinsabschluss 01.07.2016 - 30.09.2016"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2016-09-30T00:00"), hasShares(0.00), //
                                        hasSource("FinTechKontoauszug03.txt"), //
                                        hasNote("Zinsabschluss 01.07.2016 - 30.09.2016"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFinTechKontoauszug04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2010-10-01T00:00"), hasAmount("EUR", 2000.00), //
                        hasSource("FinTechKontoauszug04.txt"), hasNote("EINZAHLUNG 4 FLATEX / 0/16765097"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2010-09-30T00:00"), hasShares(0.00), //
                                        hasSource("FinTechKontoauszug04.txt"), //
                                        hasNote("Zinsabschluss 01.07.2010 - 30.09.2010"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2010-12-31T00:00"), //
                        hasSource("FinTechKontoauszug04.txt"), //
                        hasNote("Zinsabschluss 01.10.2010 - 31.12.2010"), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFinTechKontoauszug05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2017-03-31"), //
                        hasSource("FinTechKontoauszug05.txt"), //
                        hasNote("Zinsabschluss 01.01.2017 - 31.03.2017"), //
                        hasAmount("EUR", 0.48), hasGrossValue("EUR", 0.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-14"), hasAmount("EUR", 3500.00), //
                        hasSource("FinTechKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-05-18"), hasAmount("EUR", 2500.00), //
                        hasSource("FinTechKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-04-07"), hasAmount("EUR", 2250.00), //
                        hasSource("FinTechKontoauszug05.txt"), hasNote("/REC/FC:MAX"))));

    }

    @Test
    public void testFinTechKontoauszug06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FinTechKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2018-09-30"), //
                        hasSource("FinTechKontoauszug06.txt"), //
                        hasNote("Zinsabschluss 01.07.2018 - 30.09.2018"), //
                        hasAmount("EUR", 1.59), hasGrossValue("EUR", 1.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-10-08"), hasAmount("EUR", 11350.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-10-25"), hasAmount("EUR", 6000.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-11-01"), hasAmount("EUR", 6000.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-11-15"), hasAmount("EUR", 3000.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-06"), hasAmount("EUR", 12.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Prämie"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-07"), hasAmount("EUR", 6.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Prämie"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-10"), hasAmount("EUR", 2.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Prämie"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-11"), hasAmount("EUR", 8.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Prämie"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-12"), hasAmount("EUR", 6.00), //
                        hasSource("FinTechKontoauszug06.txt"), hasNote("Prämie"))));
    }

    @Test
    public void testFlatExKauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExKauf01.txt"), errors);

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
                        hasIsin("IE00BKM4GZ66"), hasWkn("A111X9"), hasTicker(null), //
                        hasName("IS C.MSCI EMIMI U.ETF DLA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-10T17:30"), hasShares(29.00), //
                        hasSource("FlatExKauf01.txt"), //
                        hasNote("Transaktion-Nr.: 1609519682"), //
                        hasAmount("EUR", 760.09), hasGrossValue("EUR", 751.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.51))));
    }

    @Test
    public void testFlatExKauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExKauf02.txt"), errors);

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
                        hasIsin("US0382221051"), hasWkn("865177"), hasTicker(null), //
                        hasName("APPLIED MATERIALS INC."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-01T21:59"), hasShares(66.00), //
                        hasSource("FlatExKauf02.txt"), //
                        hasNote("Transaktion-Nr.: 2008664208"), //
                        hasAmount("EUR", 3437.43), hasGrossValue("EUR", 3430.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.85))));
    }

    @Test
    public void testFlatExVerkauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExVerkauf01.txt"), errors);

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
                        hasIsin("IE00B41RYL63"), hasWkn("A1JJTM"), hasTicker(null), //
                        hasName("SPDR BL.BA.EO AG.BD U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-06-20T09:08"), hasShares(151.00), //
                        hasSource("FlatExVerkauf01.txt"), //
                        hasNote("Transaktion-Nr.: 1234140149"), //
                        hasAmount("EUR", 9529.81), hasGrossValue("EUR", 9538.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.51))));
    }

    @Test
    public void testFlatExVerkauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExVerkauf02.txt"), errors);

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
                        hasIsin("IE00B3WJKG14"), hasWkn("A142N1"), hasTicker(null), //
                        hasName("ISHSV-S+500INF.T.SECT.DLA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-19T09:22"), hasShares(425.00), //
                        hasSource("FlatExVerkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5681.04), hasGrossValue("EUR", 5999.30), //
                        hasTaxes("EUR", 305.85), hasFees("EUR", 9.90 + 2.51))));
    }

    @Test
    public void testFlatExVerkauf03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExVerkauf03.txt"), errors);

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
                        hasIsin("DE0009848119"), hasWkn("984811"), hasTicker(null), //
                        hasName("DWS TOP DIVIDENDE LD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-10T17:55"), hasShares(91.00), //
                        hasSource("FlatExVerkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10746.30), hasGrossValue("EUR", 10756.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-11-10T17:55"), hasShares(91.00), //
                        hasSource("FlatExVerkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 18.87), hasGrossValue("EUR", 18.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExVerkauf04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExVerkauf04.txt"), errors);

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
                        hasIsin("CA05156X1087"), hasWkn("A12GS7"), hasTicker(null), //
                        hasName("AURORA CANNABIS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-22T00:00"), hasShares(0.50), //
                        hasSource("FlatExVerkauf04.txt"), //
                        hasNote("Spitzenregulierung in CA05156X1087 | Transaktions-Nr. 1942669999"), //
                        hasAmount("EUR", 5.86), hasGrossValue("EUR", 5.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExVorabpauschale01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExVorabpauschale01.txt"), errors);

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
                        hasIsin("IE00BKM4GZ66"), hasWkn("A111X9"), hasTicker(null), //
                        hasName("ISHS MSCI EM USD-AC"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-11T00:00"), hasShares(476.00), //
                        hasSource("FlatExVorabpauschale01.txt"), //
                        hasNote("Transaktion-Nr.: 1776319005"), //
                        hasAmount("EUR", 4.69), hasGrossValue("EUR", 4.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExVorabpauschale02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExVorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("ISHS CR WD USD-AC"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2020-01-11T00:00"), hasShares(1.741300), //
                                        hasSource("FlatExVorabpauschale02.txt"), //
                                        hasNote("Transaktion-Nr.: 1222222222"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDividende01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende01.txt"), errors);

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
                        hasIsin("IE00B945VV12"), hasWkn("A1T8FS"), hasTicker(null), //
                        hasName("VANG.FTSE DEV.EU.UETF EOD"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-04-10T00:00"), hasExDate("2019-03-28"), //
                        hasShares(197.00), //
                        hasSource("FlatExDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 1234567890"), //
                        hasAmount("EUR", 36.07), hasGrossValue("EUR", 36.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDividende02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende02.txt"), errors);

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
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT    DL-,00000625"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-12T00:00"), hasExDate("2019-11-20"), //
                        hasShares(15.00), //
                        hasSource("FlatExDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 1757281127"), //
                        hasAmount("EUR", 4.98), hasGrossValue("EUR", 6.87), //
                        hasForexGrossValue("USD", 7.65), //
                        hasTaxes("EUR", 0.86 + (1.15 / 1.1137)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDividende02WithSecurityInEUR()
    {
        var security = new Security("MICROSOFT    DL-,00000625", "EUR");
        security.setIsin("US5949181045");
        security.setWkn("870747");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-12T00:00"), hasExDate("2019-11-20"), //
                        hasShares(15.00), //
                        hasSource("FlatExDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 1757281127"), //
                        hasAmount("EUR", 4.98), hasGrossValue("EUR", 6.87), //
                        hasTaxes("EUR", 0.86 + (1.15 / 1.1137)), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDividende03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende03.txt"), errors);

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
                        hasIsin("IE00B8GKDB10"), hasWkn("A1T8FV"), hasTicker(null), //
                        hasName("VA.FTSE A.W.H.D.Y.UETFDLD"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasExDate("2020-06-11"), //
                        hasShares(31.89), //
                        hasSource("FlatExDividende03.txt"), //
                        hasNote("Transaktion-Nr.: 2222222222"), //
                        hasAmount("EUR", 11.42), hasGrossValue("EUR", 11.42), //
                        hasForexGrossValue("USD", 12.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDividende03WithSecurityInEUR()
    {
        var security = new Security("VA.FTSE A.W.H.D.Y.UETFDLD", "EUR");
        security.setIsin("IE00B8GKDB10");
        security.setWkn("A1T8FV");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-24T00:00"), hasExDate("2020-06-11"), //
                        hasShares(31.89), //
                        hasSource("FlatExDividende03.txt"), //
                        hasNote("Transaktion-Nr.: 2222222222"), //
                        hasAmount("EUR", 11.42), hasGrossValue("EUR", 11.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDividende04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende04.txt"), errors);

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
                        hasIsin("US46284V1017"), hasWkn("A14MS9"), hasTicker(null), //
                        hasName("IRON MOUNTAIN (NEW)DL-,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T00:00"), hasExDate("2020-09-14"), //
                        hasShares(197.00), //
                        hasSource("FlatExDividende04.txt"), //
                        hasNote("Transaktion-Nr.: 2041157988"), //
                        hasAmount("EUR", 75.30), hasGrossValue("EUR", 103.87), //
                        hasForexGrossValue("USD", 121.84), //
                        hasTaxes("EUR", 12.99 + (18.28 / 1.173)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDividende04WithSecurityInEUR()
    {
        var security = new Security("IRON MOUNTAIN (NEW)DL-,01", "EUR");
        security.setIsin("US46284V1017");
        security.setWkn("A14MS9");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T00:00"), hasExDate("2020-09-14"), //
                        hasShares(197.00), //
                        hasSource("FlatExDividende04.txt"), //
                        hasNote("Transaktion-Nr.: 2041157988"), //
                        hasAmount("EUR", 75.30), hasGrossValue("EUR", 103.87), //
                        hasTaxes("EUR", 12.99 + (18.28 / 1.173)), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDividende05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende05.txt"), errors);

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
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT    DL-,00000625"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-11T00:00"), hasExDate("2020-05-20"), //
                        hasShares(50.00), //
                        hasSource("FlatExDividende05.txt"), //
                        hasNote("Transaktion-Nr. : 1111111111"), //
                        hasAmount("EUR", 16.73), hasGrossValue("EUR", 22.47), //
                        hasForexGrossValue("USD", 25.50), //
                        hasTaxes("EUR", 2.37 + (3.82 / 1.1348)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDividende05WithSecurityInEUR()
    {
        var security = new Security("MICROSOFT    DL-,00000625", "EUR");
        security.setIsin("US5949181045");
        security.setWkn("870747");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-11T00:00"), hasExDate("2020-05-20"), //
                        hasShares(50.00), //
                        hasSource("FlatExDividende05.txt"), //
                        hasNote("Transaktion-Nr. : 1111111111"), //
                        hasAmount("EUR", 16.73), hasGrossValue("EUR", 22.47), //
                        hasTaxes("EUR", 2.37 + (3.82 / 1.1348)), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDividende06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende06.txt"), errors);

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
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT    DL-,00000625"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-11T00:00"), hasExDate("2020-05-20"), //
                        hasShares(50.00), //
                        hasSource("FlatExDividende06.txt"), //
                        hasNote("Transaktion-Nr. : 1111111111"), //
                        hasAmount("USD", 18.99), hasGrossValue("USD", 25.50), //
                        hasTaxes("USD", (2.37 * 1.1348) + 3.82), hasFees("USD", 0.00))));
    }

    @Test
    public void testFlatExDividende07WithNegativeAmount()
    {
        /***
         * This test is a dividend transaction with negative amount. If we have
         * a negative amount and no gross reinvestment, we first book the
         * dividends received and then the tax charge Taxes must be paid.
         */
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDividende07.txt"), errors);

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
                        hasIsin("LU0386882277"), hasWkn("A0RLJD"), hasTicker(null), //
                        hasName("PICTET-GL.MEGAT.SEL.P EO"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-21T00:00"), hasShares(10.00), //
                        hasSource("FlatExDividende07.txt"), //
                        hasNote("Transaktion-Nr.: 1784953069 | Bruttothesaurierung 23,19 EUR"), //
                        hasAmount("EUR", 8.26), hasGrossValue("EUR", 8.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExStockDividende01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExStockDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121147"), hasWkn("867025"), hasTicker(null), //
                        hasName("FAURECIA EU INH      EO 7"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150001Q9"), hasWkn("A2QL01"), hasTicker(null), //
                        hasName("STELLANTIS BR RG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-04-01"), hasExDate(null), //
                        hasShares(178.00), //
                        hasSource("FlatExStockDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 2289444861"), //
                        hasAmount("EUR", 135.00), hasGrossValue("EUR", 135.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(3.00), //
                        hasSource("FlatExStockDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 2289444861"), //
                        hasAmount("EUR", 135.00), hasGrossValue("EUR", 135.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-04-01"), hasShares(178.00), //
                        hasSource("FlatExStockDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 2289444861"), //
                        hasAmount("EUR", 37.54), hasGrossValue("EUR", 37.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf01.txt"), errors);

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
                        hasIsin("XS2198879145"), hasWkn("A3E444"), hasTicker(null), //
                        hasName("FRAPORT AG 20/27"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-27T17:34"), hasShares(10.00), //
                        hasSource("FlatExDegiroKauf01.txt"), //
                        hasNote("Transaktion-Nr.: 1225591278 | Zinsbetrag 6,25 EUR"), //
                        hasAmount("EUR", 1704.15), hasGrossValue("EUR", 1696.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00 + 5.90))));
    }

    @Test
    public void testFlatExDegiroKauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf02.txt"), errors);

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
                        hasIsin("US912810SQ22"), hasWkn("A281P1"), hasTicker(null), //
                        hasName("USA 20/40"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-04T09:53"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf02.txt"), //
                        hasNote("Transaktion-Nr.: 3409315621 | Zinsbetrag 1,25 EUR"), //
                        hasAmount("EUR", 1138.15), hasGrossValue("EUR", 1127.54), //
                        hasForexGrossValue("USD", 1215.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 4.71))));
    }

    @Test
    public void testFlatExDegiroKauf02WithSecurityInEUR()
    {
        var security = new Security("Great Eagle Holdings Ltd. Registered Shares HD -,50", "EUR");
        security.setIsin("US912810SQ22");
        security.setWkn("A281P1");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf02.txt"), errors);

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
                        hasDate("2023-09-04T09:53"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf02.txt"), //
                        hasNote("Transaktion-Nr.: 3409315621 | Zinsbetrag 1,25 EUR"), //
                        hasAmount("EUR", 1138.15), hasGrossValue("EUR", 1127.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 4.71))));
    }

    @Test
    public void testFlatExDegiroKauf03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf03.txt"), errors);

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
                        hasIsin("US912810TB44"), hasWkn("A3KYSD"), hasTicker(null), //
                        hasName("USA 21/51"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-18T15:19"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf03.txt"), //
                        hasNote("Transaktion-Nr.: 3527408249 | Zinsbetrag 3,31 EUR"), //
                        hasAmount("EUR", 1172.56 + 0.87), hasGrossValue("EUR", 1162.83), //
                        hasForexGrossValue("USD", 1268.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 4.70))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-12-18T15:19"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf03.txt"), //
                        hasNote("Transaktion-Nr.: 3527408249"), //
                        hasAmount("EUR", 0.87), hasGrossValue("EUR", 0.87), //
                        hasForexGrossValue("USD", 0.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKauf03WithSecurityInEUR()
    {
        var security = new Security("USA 21/51", "EUR");
        security.setIsin("US912810TB44");
        security.setWkn("A3KYSD");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-18T15:19"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf03.txt"), //
                        hasNote("Transaktion-Nr.: 3527408249 | Zinsbetrag 3,31 EUR"), //
                        hasAmount("EUR", 1172.56 + 0.87), hasGrossValue("EUR", 1162.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 4.70))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-12-18T15:19"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf03.txt"), //
                        hasNote("Transaktion-Nr.: 3527408249"), //
                        hasAmount("EUR", 0.87), hasGrossValue("EUR", 0.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKauf04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf04.txt"), errors);

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
                        hasIsin("IE000F6G1DE0"), hasWkn("A3DJQH"), hasTicker(null), //
                        hasName("ISHARES  CORP BOND 1-5YR"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-13T09:04"), hasShares(1520.00), //
                        hasSource("FlatExDegiroKauf04.txt"), //
                        hasNote("Transaktion-Nr.: 5419071589"), //
                        hasAmount("EUR", 7734.01), hasGrossValue("EUR", 7721.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 2.51))));
    }

    @Test
    public void testFlatExDegiroKauf05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf05.txt"), errors);

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
                        hasIsin("IE00BJ5JNY98"), hasWkn("A2PHCC"), hasTicker(null), //
                        hasName("ISHARES MSCI WLD INFO TEC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-16T21:57"), hasShares(2065.00), //
                        hasSource("FlatExDegiroKauf05.txt"), //
                        hasNote("Transaktion-Nr.: 4076340401"), //
                        hasAmount("EUR", 28009.30), hasGrossValue("EUR", 28001.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));
    }

    @Test
    public void testFlatExDegiroKauf06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf06.txt"), errors);

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("ISHARES STOXX EUROPE 600"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-17T00:00"), hasShares(1.291528), //
                        hasSource("FlatExDegiroKauf06.txt"), //
                        hasNote("Transaktion-Nr.: 4338026845"), //
                        hasAmount("EUR", 69.82), hasGrossValue("EUR", 69.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKauf07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf07.txt"), errors);

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
                        hasIsin("AT0000A0VRQ6"), hasWkn("A1G6UV"), hasTicker(null), //
                        hasName("AUSTRIA 12/44 MTN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-23T10:00"), hasShares(20.00), //
                        hasSource("FlatExDegiroKauf07.txt"), //
                        hasNote("Transaktion-Nr.: 1234567890 | Zinsbetrag 33,14 EUR"), //
                        hasAmount("EUR", 1902.31), hasGrossValue("EUR", 1901.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.07))));
    }

    @Test
    public void testFlatExDegiroKauf08()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf08.txt"), errors);

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
                        hasIsin("IE00BM67HT60"), hasWkn("A113FM"), hasTicker(null), //
                        hasName("XTRACKERS MSCI WLD INFORM"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-30T10:24"), hasShares(10.00), //
                        hasSource("FlatExDegiroKauf08.txt"), //
                        hasNote("Transaktion-Nr.: 4665986542"), //
                        hasAmount("EUR", 1014.81), hasGrossValue("EUR", 1006.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.51))));
    }

    @Test
    public void testFlatExDegiroKauf09()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf09.txt"), errors);

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
                        hasIsin("IE00B3WJKG14"), hasWkn("A142N1"), hasTicker(null), //
                        hasName("ISHARES S&P 500 INFO TECH"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T16:24"), hasShares(13.948947), //
                        hasSource("FlatExDegiroKauf09.txt"), //
                        hasNote("Transaktion-Nr.: 4685418678"), //
                        hasAmount("EUR", 501.50), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testFlatExDegiroKauf10()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf10.txt"), errors);

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
                        hasIsin("DE0008019001"), hasWkn("801900"), hasTicker(null), //
                        hasName("DEUTSCHE PFANDBRIEFBANK A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29T10:09"), hasShares(500.00), //
                        hasSource("FlatExDegiroKauf10.txt"), //
                        hasNote("Transaktion-Nr.: 1754597838"), //
                        hasAmount("EUR", 2092.90), hasGrossValue("EUR", 2085.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));
    }

    @Test
    public void testFlatExDegiroKauf11()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf11.txt"), errors);

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
                        hasIsin("IE0003Z9E2Y3"), hasWkn("A3C7FZ"), hasTicker(null), //
                        hasName("GLOBAL X COPPER MINERS ET"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-30T17:30"), hasShares(172.00), //
                        hasSource("FlatExDegiroKauf11.txt"), //
                        hasNote("Transaktion-Nr.: 35693739674"), //
                        hasAmount("EUR", 1668.90), hasGrossValue("EUR", 1665.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.90 + 2.00))));
    }

    @Test
    public void testFlatExDegiroKauf12()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKauf12.txt"), errors);

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
                        hasIsin("IE00BG0SKF03"), hasWkn("A2JJAQ"), hasTicker(null), //
                        hasName("ISHARES EDGE MSCI EM VALU"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-02-02T16:04"), hasShares(4.445934), //
                        hasSource("FlatExDegiroKauf12.txt"), //
                        hasNote("Transaktion-Nr.: 4758701150"), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 298.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testCryptoKauf01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroCryptoKauf01.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-30T18:55"), hasShares(0.00014), //
                        hasSource("FlatExDegiroCryptoKauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.70), hasGrossValue("EUR", 10.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));
    }

    @Test
    public void testFlatExDegiroVerkauf01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf01.txt"), errors);

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
                        hasIsin("IE00BQ3D6V05"), hasWkn("A12GPB"), hasTicker(null), //
                        hasName("COMGEST GROWTH ASIA"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-05-12T00:00"), hasShares(80.00), //
                        hasSource("FlatExDegiroVerkauf01.txt"), //
                        hasNote("Transaktion-Nr.: 2831966689"), //
                        hasAmount("EUR", 4199.73), hasGrossValue("EUR", 4216.61), //
                        hasForexGrossValue("USD", 4406.40), //
                        hasTaxes("EUR", 16.88), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf01WithSecurityInUSD()
    {
        var security = new Security("COMGEST GROWTH ASIA", "EUR");
        security.setIsin("IE00BQ3D6V05");
        security.setWkn("A12GPB");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf01.txt"), errors);

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
                        hasDate("2022-05-12T00:00"), hasShares(80.00), //
                        hasSource("FlatExDegiroVerkauf01.txt"), //
                        hasNote("Transaktion-Nr.: 2831966689"), //
                        hasAmount("EUR", 4199.73), hasGrossValue("EUR", 4216.61), //
                        hasTaxes("EUR", 16.88), hasFees("EUR", 0.00))));

        // check currency compatibility
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDegiroVerkauf02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf02.txt"), errors);

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
                        hasIsin("IE00BQ3D6V05"), hasWkn("A12GPB"), hasTicker(null), //
                        hasName("COMGEST GROWTH ASIA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-05-25T00:00"), hasShares(0.385884), //
                        hasSource("FlatExDegiroVerkauf02.txt"), //
                        hasNote("Transaktion-Nr.: 3333333333"), //
                        hasAmount("EUR", 3.48), hasGrossValue("EUR", 3.51), //
                        hasTaxes("EUR", 0.03), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-05-25T00:00"), hasShares(0.385884), //
                        hasSource("FlatExDegiroVerkauf02.txt"), //
                        hasNote("Transaktion-Nr.: 3333333333"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf03.txt"), errors);

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
                        hasIsin("NL0000009538"), hasWkn("940602"), hasTicker(null), //
                        hasName("ROY.PHILIPS BR RG"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-05-23T00:00"), hasShares(0.384510), //
                        hasSource("FlatExDegiroVerkauf03.txt"), //
                        hasNote("Spitzenregulierung in NL0000009538 | Transaktions-Nr. 3291805526"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BHZPJ569"), hasWkn("A2PCB4"), hasTicker(null), //
                        hasName("ISHARES MSCI WORLD ESG EN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-10-04T00:00"), hasShares(0.370302), //
                        hasSource("FlatExDegiroVerkauf04.txt"), //
                        hasNote("Transaktion-Nr.: 12345678942"), //
                        hasAmount("EUR", 3.10), hasGrossValue("EUR", 3.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-10-04T00:00"), hasShares(0.370302), //
                        hasSource("FlatExDegiroVerkauf04.txt"), //
                        hasNote("Transaktion-Nr.: 12345678942"), //
                        hasAmount("EUR", 1.14), hasGrossValue("EUR", 1.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-10-04T00:00"), hasShares(0.370302), //
                        hasSource("FlatExDegiroVerkauf04.txt"), //
                        hasNote("Transaktion-Nr.: 12345678942"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf05.txt"), errors);

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
                        hasIsin("US88579Y1010"), hasWkn("851745"), hasTicker(null), //
                        hasName("3M RG"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-10T00:00"), hasShares(0.500000), //
                        hasSource("FlatExDegiroVerkauf05.txt"), //
                        hasNote("Spitzenregulierung in US88579Y1010 | Transaktions-Nr. 3727350393"), //
                        hasAmount("EUR", 30.58), hasGrossValue("EUR", 30.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf06.txt"), errors);

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
                        hasIsin("IE00B4L5Y983"), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("ISHARES CORE MSCI WORLD E"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-16T00:00"), hasShares(0.105442), //
                        hasSource("FlatExDegiroVerkauf06.txt"), //
                        hasNote("Transaktion-Nr.: 4076392428"), //
                        hasAmount("EUR", 5.25), hasGrossValue("EUR", 11.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-01-16T00:00"), hasShares(0.105442), //
                        hasSource("FlatExDegiroVerkauf06.txt"), //
                        hasNote("Transaktion-Nr.: 4076392428"), //
                        hasAmount("EUR", 1.08), hasGrossValue("EUR", 1.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf07.txt"), errors);

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
                        hasIsin("IE00BD1F4N50"), hasWkn("A2AP36"), hasTicker(null), //
                        hasName("ISHARES EDGE MSCI USA MOM"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-02T11:14"), hasShares(4400.00), //
                        hasSource("FlatExDegiroVerkauf07.txt"), //
                        hasNote("Transaktion-Nr.: 5623076367"), //
                        hasAmount("EUR", 63704.10), hasGrossValue("EUR", 63712.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf08()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf08.txt"), errors);

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
                        hasIsin("DE0005190003"), hasWkn("519000"), hasTicker(null), //
                        hasName("BAYERISCHE MOTOREN WERKE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-22T16:52"), hasShares(200.00), //
                        hasSource("FlatExDegiroVerkauf08.txt"), //
                        hasNote("Transaktion-Nr.: 4732792019"), //
                        hasAmount("EUR", 17500.12), hasGrossValue("EUR", 17524.00), //
                        hasTaxes("EUR", 15.15 + 0.83), hasFees("EUR", 5.90 + 2.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf09()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf09.txt"), errors);

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
                        hasIsin("AT0000APOST4"), hasWkn("A0JML5"), hasTicker(null), //
                        hasName("OESTERREICHISCHE POST AG"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-09T10:49"), hasShares(111.00), //
                        hasSource("FlatExDegiroVerkauf09.txt"), //
                        hasNote("Transaktion-Nr.: 4768516515"), //
                        hasAmount("EUR", 11111.11), hasGrossValue("EUR", 11234.36), //
                        hasTaxes("EUR", 111.11), hasFees("EUR", 11.10 + 1.04))));
    }

    @Test
    public void testFlatExDegiroVerkauf10()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf10.txt"), errors);

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
                        hasIsin("US7479066000"), hasWkn("A40M9N"), hasTicker(null), //
                        hasName("QUANTUM CORP."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-08-14T15:30"), hasShares(60.00), //
                        hasSource("FlatExDegiroVerkauf10.txt"), //
                        hasNote("Transaktion-Nr.: 5164291571"), //
                        hasAmount("EUR", 1326.94 - 58.02), hasGrossValue("EUR", 1274.85), //
                        hasForexGrossValue("USD", 1480.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.03))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-08-14T15:30"), hasShares(60.00), //
                        hasSource("FlatExDegiroVerkauf10.txt"), //
                        hasNote("Transaktion-Nr.: 5164291571"), //
                        hasAmount("EUR", 58.02), hasGrossValue("EUR", 58.02), //
                        hasForexGrossValue("USD", 67.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroVerkauf10WithSecurityInEUR()
    {
        var security = new Security("QUANTUM CORP.", "EUR");
        security.setIsin("US7479066000");
        security.setWkn("A40M9N");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVerkauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-08-14T15:30"), hasShares(60.00), //
                        hasSource("FlatExDegiroVerkauf10.txt"), //
                        hasNote("Transaktion-Nr.: 5164291571"), //
                        hasAmount("EUR", 1326.94 - 58.02), hasGrossValue("EUR", 1274.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.03))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-08-14T15:30"), hasShares(60.00), //
                        hasSource("FlatExDegiroVerkauf10.txt"), //
                        hasNote("Transaktion-Nr.: 5164291571"), //
                        hasAmount("EUR", 58.02), hasGrossValue("EUR", 58.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoVerkauf01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroCryptoVerkauf01.txt"),
                        errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-01T17:16"), hasShares(0.00014), //
                        hasSource("FlatExDegiroCryptoVerkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.94), hasGrossValue("EUR", 10.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));
    }

    @Test
    public void testFlatExDegiroSammelabrechnungCrypto01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnungCrypto01.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ETH"), //
                        hasName("Ethereum"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-21T08:08"), hasShares(0.0325), //
                        hasSource("FlatExDegiroSammelabrechnungCrypto01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3190.11), hasGrossValue("EUR", 3174.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.87))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-21T16:36"), hasShares(0.10), //
                        hasSource("FlatExDegiroSammelabrechnungCrypto01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 371.84), hasGrossValue("EUR", 369.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.85))));
    }

    @Test
    public void testFlatExDegiroDividende01WithNegativeAmount()
    {
        /***
         * This test is a dividend transaction with negative amount. If we have
         * a negative amount and no gross reinvestment, we first book the
         * dividends received and then the tax charge Taxes must be paid.
         */
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende01.txt"), errors);

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
                        hasIsin("IE00BFY0GT14"), hasWkn("A2N6CW"), hasTicker(null), //
                        hasName("SPDR MSCI WORLD ETF"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-10-08T00:00"), hasShares(168.90), //
                        hasSource("FlatExDegiroDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 123456789 | Bruttothesaurierung 78,81 USD"), //
                        hasAmount("EUR", 15.24), hasGrossValue("EUR", 15.24), //
                        hasForexGrossValue("USD", 17.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende01WithNegativeAmountAndSecurityInEUR()
    {
        /***
         * This test is a dividend transaction with negative amount. If we have
         * a negative amount and no gross reinvestment, we first book the
         * dividends received and then the tax charge Taxes must be paid.
         */
        var security = new Security("SPDR MSCI WORLD ETF", "EUR");
        security.setIsin("IE00BFY0GT14");
        security.setWkn("A2N6CW");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-10-08T00:00"), hasShares(168.90), //
                        hasSource("FlatExDegiroDividende01.txt"), //
                        hasNote("Transaktion-Nr.: 123456789 | Bruttothesaurierung 78,81 USD"), //
                        hasAmount("EUR", 15.24), hasGrossValue("EUR", 15.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDegiroDividende02WithNegativeAmount()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK1PV551"), hasWkn("A1XEY2"), hasTicker(null), //
                        hasName("XTRACKERS MSCI WORLD ETF"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-02T00:00"), hasExDate("2022-02-17"), //
                        hasShares(162.19), //
                        hasSource("FlatExDegiroDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 1234567891"), //
                        hasAmount("EUR", 31.21), hasGrossValue("EUR", 31.21), //
                        hasForexGrossValue("USD", 34.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-03-02T00:00"), hasShares(162.19), //
                        hasSource("FlatExDegiroDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 1234567891 | Bruttoausschüttung 34,66 USD"), //
                        hasAmount("EUR", 99.39), hasGrossValue("EUR", 99.39), //
                        hasForexGrossValue("USD", 110.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende02WithNegativeAmountAndSecurityInEUR()
    {
        var security = new Security("XTRACKERS MSCI WORLD ETF", "EUR");
        security.setIsin("IE00BK1PV551");
        security.setWkn("A1XEY2");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-02T00:00"), hasExDate("2022-02-17"), //
                        hasShares(162.19), //
                        hasSource("FlatExDegiroDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 1234567891"), //
                        hasAmount("EUR", 31.21), hasGrossValue("EUR", 31.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-03-02T00:00"), hasShares(162.19), //
                        hasSource("FlatExDegiroDividende02.txt"), //
                        hasNote("Transaktion-Nr.: 1234567891 | Bruttoausschüttung 34,66 USD"), //
                        hasAmount("EUR", 99.39), hasGrossValue("EUR", 99.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDegiroDividende03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende03.txt"), errors);

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
                        hasIsin("US09075V1026"), hasWkn("A2PSR2"), hasTicker(null), //
                        hasName("BIONTECH SE SPON. ADRS 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-17T00:00"), hasExDate("2022-06-02"), //
                        hasShares(5.00), //
                        hasSource("FlatExDegiroDividende03.txt"), //
                        hasNote("Transaktion-Nr. : 2877924522"), //
                        hasAmount("EUR", 7.37), hasGrossValue("EUR", 10.14), //
                        hasForexGrossValue("USD", 10.66), //
                        hasTaxes("EUR", 2.81 / 1.051700), hasFees("EUR", 0.10 / 1.051700))));
    }

    @Test
    public void testFlatExDegiroDividende03WithSecurityInEUR()
    {
        var security = new Security("BIONTECH SE SPON. ADRS 1", "EUR");
        security.setIsin("US09075V1026");
        security.setWkn("A2PSR2");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-17T00:00"), hasExDate("2022-06-02"), //
                        hasShares(5.00), //
                        hasSource("FlatExDegiroDividende03.txt"), //
                        hasNote("Transaktion-Nr. : 2877924522"), //
                        hasAmount("EUR", 7.37), hasGrossValue("EUR", 10.14), //
                        hasTaxes("EUR", 2.81 / 1.051700), hasFees("EUR", 0.10 / 1.051700))));

        // check currency compatibility
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDegiroDividende04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende04.txt"), errors);

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
                        hasIsin("US09075V1026"), hasWkn("A2PSR2"), hasTicker(null), //
                        hasName("BIONTECH SE SPON. ADRS 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-17T00:00"), hasExDate("2022-06-02"), //
                        hasShares(10.00), //
                        hasSource("FlatExDegiroDividende04.txt"), //
                        hasNote("Transaktion-Nr. : 2877924406"), //
                        hasAmount("USD", 15.49), hasGrossValue("USD", 21.32), //
                        hasTaxes("USD", 5.63), hasFees("USD", 0.20))));
    }

    @Test
    public void testFlatExDegiroDividende05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende05.txt"), errors);

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
                        hasIsin("IE00BL25JP72"), hasWkn("A1103G"), hasTicker(null), //
                        hasName("X(IE)-MSCI WRLD MOM. 1CDL"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-14"), hasShares(248.34), //
                        hasSource("FlatExDegiroDividende05.txt"), //
                        hasNote("Transaktion-Nr.: 0123456789 | Bruttothesaurierung 32,86 USD"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasForexGrossValue("USD", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende05WithSecurityInEUR()
    {
        var security = new Security("X(IE)-MSCI WRLD MOM. 1CDL", "EUR");
        security.setIsin("IE00BL25JP72");
        security.setWkn("A1103G");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-14"), hasShares(248.34), //
                        hasSource("FlatExDegiroDividende05.txt"), //
                        hasNote("Transaktion-Nr.: 0123456789 | Bruttothesaurierung 32,86 USD"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende06.txt"), errors);

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
                        hasIsin("IE00BL25JL35"), hasWkn("A1103D"), hasTicker(null), //
                        hasName("X(IE)-MSCI WRLD QUAL.1CDL"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-14"), hasShares(239.96), //
                        hasSource("FlatExDegiroDividende06.txt"), //
                        hasNote("Transaktion-Nr.: 6685264591 | Bruttothesaurierung 26,25 USD"), //
                        hasAmount("EUR", 0.28), hasGrossValue("EUR", 0.28), //
                        hasForexGrossValue("USD", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende06WithSecurityInEUR()
    {
        var security = new Security("X(IE)-MSCI WRLD QUAL.1CDL", "EUR");
        security.setIsin("IE00BL25JL35");
        security.setWkn("A1103D");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-14"), hasShares(239.96), //
                        hasSource("FlatExDegiroDividende06.txt"), //
                        hasNote("Transaktion-Nr.: 6685264591 | Bruttothesaurierung 26,25 USD"), //
                        hasAmount("EUR", 0.28), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende07.txt"), errors);

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
                        hasIsin("IE00BL25JM42"), hasWkn("A1103E"), hasTicker(null), //
                        hasName("X(IE)-MSCI WORLD VAL.1CDL"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-14"), hasShares(115.00), //
                        hasSource("FlatExDegiroDividende07.txt"), //
                        hasNote("Transaktion-Nr.: 6040257022 | Bruttothesaurierung 51,78 USD"), //
                        hasAmount("EUR", 10.65), hasGrossValue("EUR", 10.65), //
                        hasForexGrossValue("USD", 11.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende07WithSecurityInEUR()
    {
        var security = new Security("X(IE)-MSCI WORLD VAL.1CDL", "EUR");
        security.setIsin("IE00BL25JM42");
        security.setWkn("A1103E");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2023-07-14"), hasShares(115.00), //
                        hasSource("FlatExDegiroDividende07.txt"), //
                        hasNote("Transaktion-Nr.: 6040257022 | Bruttothesaurierung 51,78 USD"), //
                        hasAmount("EUR", 10.65), hasGrossValue("EUR", 10.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende08()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende08.txt"), errors);

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
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT    DL-,00000625"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-22T00:00"), hasExDate("2023-03-13"), //
                        hasShares(96.00), //
                        hasSource("FlatExDegiroDividende08.txt"), hasNote("Transaktion-Nr.: 2222222222"), //
                        hasAmount("USD", 244.72), hasGrossValue("USD", 244.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende09()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT    DL-,00000625"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-25T00:00"), hasExDate("2022-06-10"), //
                        hasShares(117.82), //
                        hasSource("FlatExDegiroDividende09.txt"), hasNote("Transaktion-Nr.: 2222222222"), //
                        hasAmount("CHF", 188.93), hasGrossValue("CHF", 231.71), //
                        hasTaxes("CHF", 42.78), hasFees("CHF", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende10()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende10.txt"), errors);

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
                        hasIsin("US89114QCB23"), hasWkn("A2RY26"), hasTicker(null), //
                        hasName("TORON.DOM.BK 19/24 MTN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-11T00:00"), hasExDate("2023-09-11"), //
                        hasShares(100.00), //
                        hasSource("FlatExDegiroDividende10.txt"), //
                        hasNote("Transaktion-Nr.: 3415691892"), //
                        hasAmount("EUR", 109.86), hasGrossValue("EUR", 151.53), //
                        hasForexGrossValue("USD", 162.50), //
                        hasTaxes("EUR", 41.67), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende10WithSecurityInEUR()
    {
        var security = new Security("TORON.DOM.BK 19/24 MTN", "EUR");
        security.setIsin("US89114QCB23");
        security.setWkn("A2RY26");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-11T00:00"), hasExDate("2023-09-11"), //
                        hasShares(100.00), //
                        hasSource("FlatExDegiroDividende10.txt"), //
                        hasNote("Transaktion-Nr.: 3415691892"), //
                        hasAmount("EUR", 109.86), hasGrossValue("EUR", 151.53), //
                        hasTaxes("EUR", 41.67), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende11()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007236101"), hasWkn("D69671218"), hasTicker(null), //
                        hasName("Siemens Share"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        dividend( //
                                        hasDate("2024-02-13T00:00"), hasExDate("2024-02-09"), //
                                        hasShares(641.745), //
                                        hasSource("FlatExDegiroDividende11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 2484.43), hasGrossValue("EUR", 3016.20), //
                                        hasTaxes("EUR", 504.06 + 27.71), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroDividende12()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende12.txt"), errors);

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
                        hasIsin("US92826C8394"), hasWkn("A0NC7B"), hasTicker(null), //
                        hasName("VISA INC."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T00:00"), hasExDate("2025-11-12"), //
                        hasShares(10.00), //
                        hasSource("FlatExDegiroDividende12.txt"), //
                        hasNote("Transaktion-Nr. : 4620331518"), //
                        hasAmount("EUR", 4.31), hasGrossValue("EUR", 5.79), //
                        hasForexGrossValue("USD", 6.70), //
                        hasTaxes("EUR", 1.48), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende12WithSecurityInEUR()
    {
        var security = new Security("VISA INC.", "EUR");
        security.setIsin("US92826C8394");
        security.setWkn("A0NC7B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T00:00"), hasExDate("2025-11-12"), //
                        hasShares(10.00), //
                        hasSource("FlatExDegiroDividende12.txt"), //
                        hasNote("Transaktion-Nr. : 4620331518"), //
                        hasAmount("EUR", 4.31), hasGrossValue("EUR", 5.79), //
                        hasTaxes("EUR", 1.48), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende13()
    {
        // The document does not contain an exchange rate, it is derived from
        // the amounts: (33,00 USD - 4,95 USD - 24,57 USD) / 2,99 EUR
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende13.txt"), errors);

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
                        hasIsin("US8326964058"), hasWkn("633835"), hasTicker(null), //
                        hasName("J.M. SMUCKER CO."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T00:00"), hasExDate("2025-11-14"), //
                        hasShares(30.00), //
                        hasSource("FlatExDegiroDividende13.txt"), //
                        hasNote("Transaktion-Nr. : 6155515228"), //
                        hasAmount("USD", 24.57), hasGrossValue("USD", 33.00), //
                        hasTaxes("USD", 4.95 + 3.48), hasFees("USD", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende14()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende14.txt"), errors);

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
                        hasIsin("US0341641035"), hasWkn("920678"), hasTicker(null), //
                        hasName("ANDERSONS INC., THE"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-23"), hasExDate("2026-01-02"), //
                        hasShares(100.00), //
                        hasSource("FlatExDegiroDividende14.txt"), //
                        hasNote("Transaktion-Nr. : 4741404453"), //
                        hasAmount("EUR", 12.68), hasGrossValue("EUR", 17.03), //
                        hasTaxes("EUR", 4.35), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende15()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende15.txt"), errors);

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
                        hasIsin("US7561091049"), hasWkn("899744"), hasTicker(null), //
                        hasName("REALTY INCOME CORP."), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-09"), hasShares(75.00), //
                        hasSource("FlatExDegiroDividende15.txt"), //
                        hasNote("Transaktion-Nr.: 4061359393 | Bruttothesaurierung 400,58 USD"), //
                        hasAmount("EUR", 107.10), hasGrossValue("EUR", 107.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividende16()
    {
        // The document does not contain an exchange rate, it is derived from
        // the amounts: (2,61 USD - 0,39 USD - 1,95 USD) / 0,23 EUR
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividende16.txt"), errors);

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
                        hasIsin("US57636Q1040"), hasWkn("A0F602"), hasTicker(null), //
                        hasName("MASTERCARD INC. A"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-02-09T00:00"), hasExDate("2026-01-09"), //
                        hasShares(3.00), //
                        hasSource("FlatExDegiroDividende16.txt"), //
                        hasNote("Transaktion-Nr. : 4773716165"), //
                        hasAmount("USD", 1.95), hasGrossValue("USD", 2.61), //
                        hasTaxes("USD", 0.39 + 0.27), hasFees("USD", 0.00))));
    }

    @Test
    public void testFlatExDegiroDividendeStorno01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividendeStorno01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("ISHARES STOXX EUROPE 600"), hasCurrencyCode("EUR"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2025-01-02T00:00"), hasExDate("2024-12-16"), //
                                        hasSource("FlatExDegiroDividendeStorno01.txt"), //
                                        hasNote("Transaktion-Nr.: 1011111111"), //
                                        hasAmount("EUR", 19.99), hasGrossValue("EUR", 19.99), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroDividendeReinvestGebuehren01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "FlatExDegiroDividendeReinvestGebuehren01.txt"), errors);

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
                        hasIsin("GB0002374006"), hasWkn("851247"), hasTicker(null), //
                        hasName("DIAGEO PLC"), //
                        hasCurrencyCode("EUR"))));

        // check fee after dividende reinvest transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-11-07T00:00"), hasShares(1.00), //
                        hasSource("FlatExDegiroDividendeReinvestGebuehren01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.37), hasGrossValue("EUR", 0.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroFusion01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroFusion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1861134382"), hasWkn("A2JSDA"), hasTicker(null), //
                        hasName("AM IS M W SP UEDCC"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2024-01-23T00:00"), hasShares(101.910692), //
                                        hasSource("FlatExDegiroFusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 82.85), hasGrossValue("EUR", 82.85), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroKapitalerhoehung01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKapitalerhoehung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn("A1CX3T"), hasTicker(null), //
                        hasName("TESLA RG"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2022-08-29T00:00"), hasShares(6.00), //
                                        hasSource("FlatExDegiroKapitalerhoehung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroKapitalherabsetzung01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKapitalherabsetzung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1175448666"), hasWkn("A3DHHH"), hasTicker(null), //
                        hasName("STRAUMANN HLDG RG"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2024-04-18T00:00"), hasShares(10.00), //
                                        hasSource("FlatExDegiroKapitalherabsetzung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 4.13), hasGrossValue("EUR", 4.13), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroWertpapiertausch01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroWertpapiertausch01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA05156X8843"), hasWkn("A2P4EC"), hasTicker(null), //
                        hasName("AURORA CANNABIS RG"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2020-05-13T00:00"), hasShares(37.00), //
                                        hasSource("FlatExDegiroWertpapiertausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroWertpapiertausch02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroWertpapiertausch02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("SE0018538068"), hasWkn("A3D3A1"), hasTicker(null), //
                        hasName("MGI RG-A"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2023-01-04T00:00"), hasShares(185.00), //
                                        hasSource("FlatExDegiroWertpapiertausch02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExSammelabrechnung01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExSammelabrechnung01.txt"), errors);

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
                        hasIsin("CA03765K1049"), hasWkn("A12HM0"), hasTicker(null), //
                        hasName("APHRIA INC."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-04-09T16:52"), hasShares(540.00), //
                        hasSource("FlatExSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 123456789"), //
                        hasAmount("EUR", 4416.52), hasGrossValue("EUR", 4573.80), //
                        hasTaxes("EUR", 148.87), hasFees("EUR", 5.90 + 2.51))));
    }

    @Test
    public void testFlatExSammelabrechnung02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExSammelabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4581401001"), hasWkn("855681"), hasTicker(null), //
                        hasName("INTEL CORP.       DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-30T18:09"), hasShares(100.00), //
                        hasSource("FlatExSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 2101694078"), //
                        hasAmount("USD", 4773.36), hasGrossValue("USD", 4780.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 7.03 + 0.11))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-30T18:09"), hasShares(20.00), //
                        hasSource("FlatExSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 2101694102"), //
                        hasAmount("USD", 955.98), hasGrossValue("USD", 956.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.02))));
    }

    @Test
    public void testFlatExSammelabrechnung02WithSecurityInEUR()
    {
        var security = new Security("INTEL CORP.       DL-,001", "EUR");
        security.setIsin("US4581401001");
        security.setWkn("855681");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExSammelabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4581401001"), hasWkn("855681"), hasTicker(null), //
                        hasName("INTEL CORP.       DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-30T18:09"), hasShares(100.00), //
                        hasSource("FlatExSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 2101694078"), //
                        hasAmount("USD", 4773.36), hasGrossValue("USD", 4780.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 7.03 + 0.11))));

        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-30T18:09"), hasShares(20.00), //
                        hasSource("FlatExSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 2101694102"), //
                        hasAmount("USD", 955.98), hasGrossValue("USD", 956.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.02))));
    }

    @Test
    public void testFlatExSammelabrechnung03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExSammelabrechnung03.txt"), errors);

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
                        hasIsin("DE000A0S9GB0"), hasWkn("A0S9GB"), hasTicker(null), //
                        hasName("DT.BOERSE COM. XETRA-GOLD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("KYG9830T1067"), hasWkn("A2JNY1"), hasTicker(null), //
                        hasName("XIAOMI CORP. CL.B"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-25T09:04"), hasShares(38.00), //
                        hasSource("FlatExSammelabrechnung03.txt"), //
                        hasNote("Transaktion-Nr.: 0000000000"), //
                        hasAmount("EUR", 1558.05), hasGrossValue("EUR", 1564.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.80 + 2.38))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-25T09:09"), hasShares(1470.00), //
                        hasSource("FlatExSammelabrechnung03.txt"), //
                        hasNote("Transaktion-Nr.: 0000000000"), //
                        hasAmount("EUR", 1581.52), hasGrossValue("EUR", 1572.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.80 + 5.55))));
    }

    @Test
    public void testFlatExSammelabrechnungDevisen01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExSammelabrechnungDevisen01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2021-03-05T00:00"), hasAmount("EUR", 1840.17), //
                                        hasSource("FlatExSammelabrechnungDevisen01.txt"), //
                                        hasNote("Auftrag Nr. 5122608575")))));
    }

    @Test
    public void testFlatExKontoauszug01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-07"), hasAmount("EUR", 250.00), //
                        hasSource("FlatExKontoauszug01.txt"), hasNote("Überweisung"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-07-20"), //
                        hasSource("FlatExKontoauszug01.txt"), //
                        hasNote("Depotgebühren 01.04.2020 - 30.04.2020"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2020-06-30"), //
                        hasSource("FlatExKontoauszug01.txt"), //
                        hasNote("Zinsabschluss 01.04.2020 - 30.06.2020"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExKontoauszug02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-19"), hasAmount("EUR", 50.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Lastschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-19"), hasAmount("EUR", 50.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Lastschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-19"), hasAmount("EUR", 50.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Lastschrift"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-19"), hasAmount("EUR", 400.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(removal(hasDate("2019-11-19"), hasAmount("EUR", 53.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("R-Transaktion"))));

        assertThat(results, hasItem(removal(hasDate("2019-11-19"), hasAmount("EUR", 53.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("R-Transaktion"))));

        assertThat(results, hasItem(removal(hasDate("2019-11-19"), hasAmount("EUR", 53.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("R-Transaktion"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-25"), hasAmount("EUR", 50.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-25"), hasAmount("EUR", 150.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-25"), hasAmount("EUR", 159.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2019-12-10"), hasAmount("EUR", 160.00), //
                        hasSource("FlatExKontoauszug02.txt"), hasNote("Überweisung"))));

        // check interest transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2019-12-31"), //
                        hasSource("FlatExKontoauszug02.txt"), //
                        hasNote("Zinsabschluss 01.10.2019 - 31.12.2019"), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExKontoauszug03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-07"), hasAmount("EUR", 250.00), //
                        hasSource("FlatExKontoauszug03.txt"), hasNote("Überweisung"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-07-20"), //
                        hasSource("FlatExKontoauszug03.txt"), //
                        hasNote("Depotgebühren 01.04.2020 - 30.04.2020"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2020-06-30"), //
                        hasSource("FlatExKontoauszug03.txt"), //
                        hasNote("Zinsabschluss 01.04.2020 - 30.06.2020"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExKontoauszug04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2019-06-30"), //
                        hasSource("FlatExKontoauszug04.txt"), //
                        hasNote("Zinsabschluss 01.04.2019 - 30.06.2019"), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-07-02"), hasAmount("EUR", 495.05), //
                        hasSource("FlatExKontoauszug04.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-08-01"), //
                        hasSource("FlatExKontoauszug04.txt"), //
                        hasNote("ZINSPILOT Auszahlung FIMBank p.l.c."), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-08-16"), //
                        hasSource("FlatExKontoauszug04.txt"), //
                        hasNote("ZINSPILOT Auszahlung FIMBank p.l.c."), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-09-02"), //
                        hasSource("FlatExKontoauszug04.txt"), //
                        hasNote("ZINSPILOT Auszahlung FIMBank p.l.c."), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-09-16"), //
                        hasSource("FlatExKontoauszug04.txt"), //
                        hasNote("ZINSPILOT Auszahlung FIMBank p.l.c."), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(3L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-08T00:00"), hasAmount("EUR", 200.00), //
                        hasSource("FlatExDegiroKontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-10T00:00"), hasAmount("EUR", 200.00), //
                        hasSource("FlatExDegiroKontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-08T00:00"), hasAmount("EUR", 200.00), //
                        hasSource("FlatExDegiroKontoauszug01.txt"), hasNote("Überweisung"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2021-07-19T00:00"), hasShares(0.00), //
                                        hasSource("FlatExDegiroKontoauszug01.txt"), //
                                        hasNote("Depotgebühren 01.04.2021 - 30.04.2021"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2021-07-19T00:00"), hasShares(0.00), //
                                        hasSource("FlatExDegiroKontoauszug01.txt"), //
                                        hasNote("Depotgebühren 01.05.2021 - 31.05.2021"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2021-07-19T00:00"), hasShares(0.00), //
                                        hasSource("FlatExDegiroKontoauszug01.txt"), //
                                        hasNote("Depotgebühren 01.06.2021 - 30.06.2021"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-06-30T00:00"), //
                        hasSource("FlatExDegiroKontoauszug01.txt"), //
                        hasNote("Zinsabschluss 01.04.2021 - 30.06.2021"), //
                        hasAmount("EUR", 2.73), hasGrossValue("EUR", 2.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(24L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-04-04"), hasAmount("EUR", 2000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(removal(hasDate("2022-04-06"), hasAmount("EUR", 1800.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-04-20"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-04-26"), hasAmount("EUR", 1250.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-04-29"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-02"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-03"), hasAmount("EUR", 1500.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-03"), hasAmount("EUR", 600.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-10"), hasAmount("EUR", 2500.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-13"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-17"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-27"), hasAmount("EUR", 1500.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-31"), hasAmount("EUR", 600.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-06-02"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-06-14"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-06-17"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        assertThat(results, hasItem(deposit(hasDate("2022-07-01"), hasAmount("EUR", 600.00), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), hasNote("Überweisung"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-03-04"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Depotservicegebühr US09075V1026"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-04-19"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Depotgebühren 01.01.2022 - 31.01.2022"), //
                        hasAmount("EUR", 10.84), hasGrossValue("EUR", 10.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-04-19"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Depotgebühren 01.02.2022 - 28.02.2022"), //
                        hasAmount("EUR", 9.99), hasGrossValue("EUR", 9.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-04-19"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Depotgebühren 01.03.2022 - 31.03.2022"), //
                        hasAmount("EUR", 9.85), hasGrossValue("EUR", 9.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-04-22"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Depotservicegebühr US47215P1066"), //
                        hasAmount("EUR", 1.34), hasGrossValue("EUR", 1.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-06-21"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Gebühr Tax Voucher WKN A0NFN3"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-03-31"), //
                        hasSource("FlatExDegiroKontoauszug02.txt"), //
                        hasNote("Zinsabschluss 01.01.2022 - 31.03.2022"), //
                        hasAmount("EUR", 1.16), hasGrossValue("EUR", 1.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-03-31"), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), //
                        hasNote("Zinsabschluss 01.01.2021 - 31.03.2021"), //
                        hasAmount("EUR", 5.60), hasGrossValue("EUR", 5.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-06"), hasAmount("EUR", 300.00), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-26"), hasAmount("EUR", 500.00), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-04"), hasAmount("EUR", 300.00), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-06-02"), hasAmount("EUR", 300.00), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-04-19"), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), //
                        hasNote("Depotgebühren 01.01.2021 - 31.01.2021"), //
                        hasAmount("EUR", 3.17), hasGrossValue("EUR", 3.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-04-19"), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), //
                        hasNote("Depotgebühren 01.02.2021 - 28.02.2021"), //
                        hasAmount("EUR", 3.13), hasGrossValue("EUR", 3.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-04-19"), //
                        hasSource("FlatExDegiroKontoauszug03.txt"), //
                        hasNote("Depotgebühren 01.03.2021 - 31.03.2021"), //
                        hasAmount("EUR", 3.25), hasGrossValue("EUR", 3.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        interest( //
                                        hasDate("2023-09-30"), //
                                        hasSource("FlatExDegiroKontoauszug04.txt"), //
                                        hasNote("Zinsabschluss"), //
                                        hasAmount("EUR", 0.68), hasGrossValue("EUR", 0.68), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroKontoauszug05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-10-25"), //
                        hasSource("FlatExDegiroKontoauszug05.txt"), //
                        hasNote("Gebühr Kapitaltransaktion Ausland US17275R1023"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-11-09"), hasAmount("EUR", 10000.00), //
                        hasSource("FlatExDegiroKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-11-10"), hasAmount("EUR", 10000.00), //
                        hasSource("FlatExDegiroKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-12-01"), //
                        hasSource("FlatExDegiroKontoauszug05.txt"), //
                        hasNote("Gebühr Kapitaltransaktion Ausland JP3756600007"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2024-01-02"), hasShares(0.00), //
                                        hasSource("FlatExDegiroKontoauszug06.txt"), //
                                        hasNote("Zinsabschluss 01.10.2023 - 31.12.2023"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-15T00:00"), hasShares(0.00), //
                        hasSource("FlatExDegiroKontoauszug06.txt"), //
                        hasNote("Steuerkorrektur aufgrund FSA-Thematik"), //
                        hasAmount("EUR", 22.98), hasGrossValue("EUR", 22.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-13"), hasAmount("EUR", 1.88), //
                        hasSource("FlatExDegiroKontoauszug07.txt"), hasNote("Gutschrift aus Kulanz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-17"), hasAmount("EUR", 1.88), //
                        hasSource("FlatExDegiroKontoauszug07.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testFlatExDegiroKontoauszug08()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-25"), //
                        hasSource("FlatExDegiroKontoauszug08.txt"), //
                        hasNote("Bearbeitungsgebühr - Erträgnisaufstellung"), //
                        hasAmount("EUR", 15.90), hasGrossValue("EUR", 15.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-30"), //
                        hasSource("FlatExDegiroKontoauszug08.txt"), //
                        hasNote("Portokosten - Versand Kundenformular"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug09()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2025-09-30"), hasShares(0.00), //
                                        hasSource("FlatExDegiroKontoauszug09.txt"), //
                                        hasNote("Zinsabschluss 01.07.2025 - 30.09.2025"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-03"), hasAmount("EUR", 150.00), //
                        hasSource("FlatExDegiroKontoauszug09.txt"), hasNote("LI787233965724704495"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-05"), hasAmount("EUR", 150.00), //
                        hasSource("FlatExDegiroKontoauszug09.txt"), hasNote("fs939468989892221962"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-12-03"), hasAmount("EUR", 150.00), //
                        hasSource("FlatExDegiroKontoauszug09.txt"), hasNote("dM239204144852486335"))));
    }

    @Test
    public void testFlatExDegiroKontoauszug10()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert cancellation transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2025-09-30"), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), //
                        hasNote("Zinsabschluss 01.07.2025 - 30.09.2025"), //
                        hasAmount("EUR", 3.39), hasGrossValue("EUR", 3.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-01"), hasAmount("EUR", 3000.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("Mn132692519750748439"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-07"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("Iz840881600557395584"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-08"), hasAmount("EUR", 500.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("If031173443553589854"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-09"), hasAmount("EUR", 550.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("eS100457973588945924"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-06"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("at281887082404416719"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-27"), hasAmount("EUR", 4000.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("kH263779613641675977"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-12-09"), hasAmount("EUR", 1000.00), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), hasNote("vZ423074479485097902"))));

        // assert cancellation transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2025-12-31"), //
                        hasSource("FlatExDegiroKontoauszug10.txt"), //
                        hasNote("Zinsabschluss 01.10.2025 - 31.12.2025"), //
                        hasAmount("EUR", 0.23), hasGrossValue("EUR", 0.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDegiroKontoauszug11()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroKontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2026-03-31"), //
                                        hasSource("FlatExDegiroKontoauszug11.txt"), //
                                        hasNote("Zinsabschluss 01.01.2026 - 31.03.2026"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-17"), hasAmount("EUR", 250.00), //
                        hasSource("FlatExDegiroKontoauszug11.txt"), hasNote("Er752174888515904584"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-17"), hasAmount("EUR", 150.00), //
                        hasSource("FlatExDegiroKontoauszug11.txt"), hasNote("Go232073202152738067"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-19"), hasAmount("EUR", 250.00), //
                        hasSource("FlatExDegiroKontoauszug11.txt"), hasNote("eJ978910244859480755"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-19"), hasAmount("EUR", 1800.00), //
                        hasSource("FlatExDegiroKontoauszug11.txt"), hasNote("Wd391247229262193081"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-06-10"), hasAmount("EUR", 8000.00), //
                        hasSource("FlatExDegiroKontoauszug11.txt"), hasNote("XC023307750674965771"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-06-17"), hasAmount("EUR", 250.00), //
                        hasSource("FlatExDegiroKontoauszug11.txt"), hasNote("Oc723230964070762008"))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung01.txt"),
                        errors);

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
                        hasIsin("US88339J1051"), hasWkn("A2ARCV"), hasTicker(null), //
                        hasName("THE TRA.DESK A DL-,000001"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US17275R1023"), hasWkn("878841"), hasTicker(null), //
                        hasName("CISCO SYSTEMS    DL-,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-09T17:37"), hasShares(3.00), //
                        hasSource("FlatExDeGiroSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 229"), //
                        hasAmount("EUR", 1737.50), hasGrossValue("EUR", 1745.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-09T17:40"), hasShares(41.00), //
                        hasSource("FlatExDeGiroSammelabrechnung01.txt"), //
                        hasNote("Transaktion-Nr.: 229"), //
                        hasAmount("EUR", 1796.12), hasGrossValue("EUR", 1788.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung02()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung02.txt"),
                        errors);

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
                        hasIsin("US2561631068"), hasWkn("A2JHLZ"), hasTicker(null), //
                        hasName("DOCUSIGN INC    DL-,0001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-09T14:14"), hasShares(25.00), //
                        hasSource("FlatExDeGiroSammelabrechnung02.txt"), //
                        hasNote("Transaktion-Nr.: 2592937917"), //
                        hasAmount("EUR", 3456.41), hasGrossValue("EUR", 3448.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.51))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung03()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung03.txt"),
                        errors);

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
                        hasIsin("DE000MA5GEG8"), hasWkn("MA5GEG"), hasTicker(null), //
                        hasName("MS CI.I. CALL23 ABL"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-11-07T17:21"), hasShares(1190.00), //
                        hasSource("FlatExDeGiroSammelabrechnung03.txt"), //
                        hasNote("Transaktion-Nr.: 2512347917"), //
                        hasAmount("EUR", 1.19), hasGrossValue("EUR", 1.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-11-07T17:21"), hasShares(1190.00), //
                        hasSource("FlatExDeGiroSammelabrechnung03.txt"), //
                        hasNote("Transaktion-Nr.: 2512347917"), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung04()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung04.txt"),
                        errors);

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
                        hasIsin("US88579Y1010"), hasWkn("851745"), hasTicker(null), //
                        hasName("3M CO.             DL-,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-30T15:30"), hasShares(5.00), //
                        hasSource("FlatExDeGiroSammelabrechnung04.txt"), //
                        hasNote("Transaktion-Nr.: 3157457617"), //
                        hasAmount("EUR", 534.00), hasGrossValue("EUR", 528.10), //
                        hasForexGrossValue("USD", 572.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung04WithSecurityInEUR()
    {
        var security = new Security("3M CO.             DL-,01", "EUR");
        security.setIsin("US88579Y1010");
        security.setWkn("851745");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung04.txt"),
                        errors);

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
                        hasDate("2023-01-30T15:30"), hasShares(5.00), //
                        hasSource("FlatExDeGiroSammelabrechnung04.txt"), //
                        hasNote("Transaktion-Nr.: 3157457617"), //
                        hasAmount("EUR", 534.00), hasGrossValue("EUR", 528.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));

        // check currency compatibility
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung05()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung05.txt"),
                        errors);

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
                        hasIsin("DE000VK9A2S8"), hasWkn("VK9A2S"), hasTicker(null), //
                        hasName("VONT.FINL PR MINIL"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-22T13:04"), hasShares(140.00), //
                        hasSource("FlatExDeGiroSammelabrechnung05.txt"), //
                        hasNote("Transaktion-Nr.: 4485191452"), //
                        hasAmount("EUR", 1480.90), hasGrossValue("EUR", 1477.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.90))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung06()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("JE00B2NFTL95"), hasWkn("A0V6Z0"), hasTicker(null), //
                        hasName("WITR COM.SEC.Z08/UN.IDX"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("XS2852999775"), hasWkn("A4AH1M"), hasTicker(null), //
                        hasName("LEVERAGE SHARES GOLD+ ETP"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-29T08:31"), hasShares(9.00), //
                        hasSource("FlatExDeGiroSammelabrechnung06.txt"), //
                        hasNote("Transaktion-Nr.: 4636047412"), //
                        hasAmount("EUR", 1683.96), hasGrossValue("EUR", 1742.67), //
                        hasTaxes("EUR", 50.07), hasFees("EUR", 5.90 + 2.74))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-29T08:51"), hasShares(150.00), //
                        hasSource("FlatExDeGiroSammelabrechnung06.txt"), //
                        hasNote("Transaktion-Nr.: 4666192991"), //
                        hasAmount("EUR", 2033.04), hasGrossValue("EUR", 2024.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.74))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung07()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroSammelabrechnung07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(12L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(25));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US00130H1059"), hasWkn("882177"), hasTicker(null), //
                        hasName("AES CORP., THE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA05156V1022"), hasWkn("A1W7D4"), hasTicker(null), //
                        hasName("AURINIA PHARMACEUTICALS I"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US11135F1012"), hasWkn("A2JG9Z"), hasTicker(null), //
                        hasName("BROADCOM INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US14174T1079"), hasWkn("A11398"), hasTicker(null), //
                        hasName("CARETRUST REIT INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5324571083"), hasWkn("858560"), hasTicker(null), //
                        hasName("ELI LILLY AND COMPANY"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US92556V1061"), hasWkn("A2QAME"), hasTicker(null), //
                        hasName("VIATRIS INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US83601L1026"), hasWkn("A2QHA5"), hasTicker(null), //
                        hasName("SOTERA HEALTH COMPANY"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US29357K1034"), hasWkn("A12D51"), hasTicker(null), //
                        hasName("ENOVA INTERNATIONAL INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("KYG6683N1034"), hasWkn("A3C82G"), hasTicker(null), //
                        hasName("NU HOLDINGS LTD. A"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US3453708600"), hasWkn("502391"), hasTicker(null), //
                        hasName("FORD MOTOR CO."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US4062161017"), hasWkn("853986"), hasTicker(null), //
                        hasName("HALLIBURTON CO."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0038705702"), hasWkn("889328"), hasTicker(null), //
                        hasName("MILLICOM INTL CELLULAR S."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02T15:30"), hasShares(120.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682588850"), //
                        hasAmount("EUR", 1460.05), hasGrossValue("EUR", 1476.00), //
                        hasTaxes("EUR", 9.31), hasFees("EUR", 5.90 + 0.74))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02T15:30"), hasShares(105.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682592903"), //
                        hasAmount("EUR", 1414.31 - 9.31), hasGrossValue("EUR", 1422.61 - 9.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-02T15:30"), hasShares(105.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682592903"), //
                        hasAmount("EUR", 9.31), hasGrossValue("EUR", 9.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( ////
                        hasDate("2026-01-02T15:30"), hasShares(5.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682595695"), //
                        hasAmount("EUR", 1500.11), hasGrossValue("EUR", 1506.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02T15:32"), hasShares(45.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682601474"), //
                        hasAmount("EUR", 1368.70), hasGrossValue("EUR", 1377.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02T15:30"), hasShares(120.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682588850"), //
                        hasAmount("EUR", 1460.05), hasGrossValue("EUR", 1476.00), //
                        hasTaxes("EUR", 9.31), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02T15:32"), hasShares(2.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682602407"), //
                        hasAmount("EUR", 1801.36), hasGrossValue("EUR", 1808.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02T15:32"), hasShares(160.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682603713"), //
                        hasAmount("EUR", 1649.87), hasGrossValue("EUR", 1689.60), //
                        hasTaxes("EUR", 33.09), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T15:34"), hasShares(95.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682606959"), //
                        hasAmount("EUR", 1450.88), hasGrossValue("EUR", 1442.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T15:34"), hasShares(10.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682608626"), //
                        hasAmount("EUR", 1388.30), hasGrossValue("EUR", 1380.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T15:35"), hasShares(100.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682610434"), //
                        hasAmount("EUR", 1433.90), hasGrossValue("EUR", 1425.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T15:35"), hasShares(130.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682612776"), //
                        hasAmount("EUR", 1460.82), hasGrossValue("EUR", 1454.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T15:35"), hasShares(60.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682613610"), //
                        hasAmount("EUR", 1455.64), hasGrossValue("EUR", 1449.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-02T15:36"), hasShares(30.00), //
                        hasSource("FlatExDeGiroSammelabrechnung07.txt"), //
                        hasNote("Transaktion-Nr.: 4682615368"), //
                        hasAmount("EUR", 1454.30), hasGrossValue("EUR", 1446.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung08()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnung08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("CA0084741085"), hasWkn("860325"), hasTicker(null), //
                        hasName("AGNICO EAGLE MINES LTD."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA01921D2041"), hasWkn("A417BV"), hasTicker(null), //
                        hasName("ALLIED GOLD CORP."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasWkn("A14Y6F"), hasTicker(null), //
                        hasName("ALPHABET INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US29357K1034"), hasWkn("A12D51"), hasTicker(null), //
                        hasName("ENOVA INTERNATIONAL INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US3453708600"), hasWkn("502391"), hasTicker(null), //
                        hasName("FORD MOTOR CO."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US4062161017"), hasWkn("853986"), hasTicker(null), //
                        hasName("HALLIBURTON CO."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US4432011082"), hasWkn("A2PZ2D"), hasTicker(null), //
                        hasName("HOWMET AEROSPACE INC."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("KYG6683N1034"), hasWkn("A3C82G"), hasTicker(null), //
                        hasName("NU HOLDINGS LTD. A"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US83601L1026"), hasWkn("A2QHA5"), hasTicker(null), //
                        hasName("SOTERA HEALTH COMPANY"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US9581021055"), hasWkn("863060"), hasTicker(null), //
                        hasName("WESTERN DIGITAL CORP."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:36"), hasShares(10.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745481480"), //
                        hasAmount("EUR", 1701.96), hasGrossValue("EUR", 1810.50), //
                        hasTaxes("EUR", 100.24), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:36"), hasShares(75.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745482455"), //
                        hasAmount("EUR", 1823.20), hasGrossValue("EUR", 1980.00), //
                        hasTaxes("EUR", 148.50), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:36"), hasShares(5.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745483413"), //
                        hasAmount("EUR", 1399.20), hasGrossValue("EUR", 1406.25), //
                        hasTaxes("EUR", 0.41), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:37"), hasShares(10.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745483647"), //
                        hasAmount("EUR", 1281.70), hasGrossValue("EUR", 1290.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.40))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-27T15:37"), hasShares(10.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745483647"), //
                        hasAmount("EUR", 24.75), hasGrossValue("EUR", 24.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:37"), hasShares(130.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745484810"), //
                        hasAmount("EUR", 1470.91), hasGrossValue("EUR", 1486.42), //
                        hasTaxes("EUR", 8.87), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:37"), hasShares(60.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745485584"), //
                        hasAmount("EUR", 1652.03), hasGrossValue("EUR", 1738.20), //
                        hasTaxes("EUR", 79.53), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:37"), hasShares(8.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745486146"), //
                        hasAmount("EUR", 1437.77), hasGrossValue("EUR", 1452.00), //
                        hasTaxes("EUR", 7.59), hasFees("EUR", 5.90 + 0.74))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:38"), hasShares(100.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745486582"), //
                        hasAmount("EUR", 1489.65), hasGrossValue("EUR", 1525.40), //
                        hasTaxes("EUR", 27.45), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:38"), hasShares(95.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745487431"), //
                        hasAmount("EUR", 1455.97), hasGrossValue("EUR", 1472.50), //
                        hasTaxes("EUR", 8.23), hasFees("EUR", 5.90 + 2.40))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27T15:38"), hasShares(10.00), //
                        hasSource("FlatExDegiroSammelabrechnung08.txt"), //
                        hasNote("Transaktion-Nr.: 4745488464"), //
                        hasAmount("EUR", 1901.16), hasGrossValue("EUR", 2048.00), //
                        hasTaxes("EUR", 140.20), hasFees("EUR", 5.90 + 0.74))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung09()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnung09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US30303M1027"), hasWkn("A1JWVX"), hasTicker(null), //
                        hasName("META PLATFORMS INC. A"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000A28M8D0"), hasWkn("A28M8D"), hasTicker(null), //
                        hasName("VANECK BITCOIN ETN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA8277191059"), hasWkn("A408EQ"), hasTicker(null), //
                        hasName("SILVER47 EXPLORATION CORP"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-14T09:56"), hasShares(80.00), //
                        hasSource("FlatExDegiroSammelabrechnung09.txt"), //
                        hasNote("Transaktion-Nr.: 4713456485"), //
                        hasAmount("EUR", 3458.42), hasGrossValue("EUR", 3451.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.85))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-14T18:00"), hasShares(27.00), //
                        hasSource("FlatExDegiroSammelabrechnung09.txt"), //
                        hasNote("Transaktion-Nr.: 4714408398"), //
                        hasAmount("EUR", 14302.75), hasGrossValue("EUR", 14323.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.90 + 0.85))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-14T18:00"), hasShares(27.00), //
                        hasSource("FlatExDegiroSammelabrechnung09.txt"), //
                        hasNote("Transaktion-Nr.: 4714408398"), //
                        hasAmount("EUR", 230.67), hasGrossValue("EUR", 230.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-14T18:11"), hasShares(608.00), //
                        hasSource("FlatExDegiroSammelabrechnung09.txt"), //
                        hasNote("Transaktion-Nr.: 4714426142"), //
                        hasAmount("EUR", 361.05), hasGrossValue("EUR", 352.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.51))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung10()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnung10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT CORP."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-02-03T15:31"), hasShares(5.00), //
                        hasSource("FlatExDegiroSammelabrechnung10.txt"), //
                        hasNote("Transaktion-Nr.: 4761452746"), //
                        hasAmount("EUR", 1780.75), hasGrossValue("EUR", 1774.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.85))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung11()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnung11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US84615Q1031"), hasWkn("A42D4F"), hasTicker(null), //
                        hasName("SPACE EXPLORATION TECHS."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-12T00:00"), hasShares(10.00), //
                        hasSource("FlatExDegiroSammelabrechnung11.txt"), //
                        hasNote("Transaktion-Nr.: 5028417943"), //
                        hasAmount("EUR", 1166.61), hasGrossValue("EUR", 1166.61), //
                        hasForexGrossValue("USD", 1350.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-06-12T18:02"), hasShares(10.00), //
                        hasSource("FlatExDegiroSammelabrechnung11.txt"), //
                        hasNote("Transaktion-Nr.: 5029322308"), //
                        hasAmount("EUR", 1319.57), hasGrossValue("EUR", 1386.40), //
                        hasForexGrossValue("USD", 1604.34), //
                        hasTaxes("EUR", 51.93 + 2.85 + 4.15), hasFees("EUR", 5.90 + 2.00))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung12()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnung12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US0152711091"), hasWkn("907179"), hasTicker(null), //
                        hasName("ALEXANDRIA REAL EST. EQU."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0389231087"), hasWkn("A0CAPU"), hasTicker(null), //
                        hasName("ARBOR REALTY TRUST INC."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US92936U1097"), hasWkn("A1J5SB"), hasTicker(null), //
                        hasName("W.P. CAREY INC."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-09-29T15:53"), hasShares(40.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244678367"), //
                        hasAmount("EUR", 1937.30 - 230.20), hasGrossValue("EUR", 1715.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-09-29T15:58"), hasShares(272.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689219"), //
                        hasAmount("EUR", 1545.15 - 605.20), hasGrossValue("EUR", 945.87), //
                        hasForexGrossValue("USD", 1077.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.02))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-09-29T15:58"), hasShares(28.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689238"), //
                        hasAmount("EUR", 161.59 - 64.22), hasGrossValue("EUR", 97.37), //
                        hasForexGrossValue("USD", 110.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-09-29T18:42"), hasShares(20.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244967808"), //
                        hasAmount("EUR", 1167.90), hasGrossValue("EUR", 1160.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 2.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-09-29T15:53"), hasShares(40.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244678367"), //
                        hasAmount("EUR", 230.20), hasGrossValue("EUR", 230.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-09-29T15:58"), hasShares(272.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689219"), //
                        hasAmount("EUR", 605.20), hasGrossValue("EUR", 605.20), //
                        hasForexGrossValue("USD", 689.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-09-29T15:58"), hasShares(28.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689238"), //
                        hasAmount("EUR", 64.22), hasGrossValue("EUR", 64.22), //
                        hasForexGrossValue("USD", 73.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDeGiroSammelabrechnung12WithSecurityInEUR()
    {
        var security = new Security("ARBOR REALTY TRUST INC.", "EUR");
        security.setIsin("US0389231087");
        security.setWkn("A0CAPU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FinTechGroupBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroSammelabrechnung12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-09-29T15:58"), hasShares(272.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689219"), //
                        hasAmount("EUR", 1545.15 - 605.20), hasGrossValue("EUR", 945.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90 + 0.02))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-09-29T15:58"), hasShares(28.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689238"), //
                        hasAmount("EUR", 161.59 - 64.22), hasGrossValue("EUR", 97.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-09-29T15:58"), hasShares(272.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689219"), //
                        hasAmount("EUR", 605.20), hasGrossValue("EUR", 605.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-09-29T15:58"), hasShares(28.00), //
                        hasSource("FlatExDegiroSammelabrechnung12.txt"), //
                        hasNote("Transaktion-Nr.: 5244689238"), //
                        hasAmount("EUR", 64.22), hasGrossValue("EUR", 64.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFlatExDeGiroDepotServiceGebuehr01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDeGiroDepotServiceGebuehr01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        fee( //
                                        hasDate("2023-03-23"), //
                                        hasSource("FlatExDeGiroDepotServiceGebuehr01.txt"), //
                                        hasNote("Depotservicegebühr US09075V1026"), //
                                        hasAmount("EUR", 0.18), hasGrossValue("EUR", 0.18), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFlatExDegiroVorabpauschale01()
    {
        var extractor = new FinTechGroupBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FlatExDegiroVorabpauschale01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0009909999"), hasWkn("A39999"), hasTicker(null), //
                        hasName("AM S&P XXXXXXX"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2024-01-29T00:00"), hasShares(265.851), //
                                        hasSource("FlatExDegiroVorabpauschale01.txt"), //
                                        hasNote("Transaktion-Nr.: 3583072052"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
