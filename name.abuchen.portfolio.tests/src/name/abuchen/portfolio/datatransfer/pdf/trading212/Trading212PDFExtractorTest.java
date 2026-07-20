package name.abuchen.portfolio.datatransfer.pdf.trading212;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.Trading212PDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class Trading212PDFExtractorTest
{
    @Test
    public void testAktivitaetsauszug01()
    {
        var extractor = new Trading212PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aktivitaetsauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(18));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker("VWCE"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker("EUNL"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:04:56"), hasShares(0.03398586), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501427897"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:05:14"), hasShares(0.04437147), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501429617"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:08:12"), hasShares(1.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501431724"), //
                        hasAmount("EUR", 112.67), hasGrossValue("EUR", 112.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:08:38"), hasShares(1.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501432006"), //
                        hasAmount("EUR", 112.67), hasGrossValue("EUR", 112.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:11:08"), hasShares(2.21906621), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501433363"), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:11:10"), hasShares(1.6999864), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501433415"), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-06T08:12:29"), hasShares(1.73397226), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44466988302"), //
                        hasAmount("EUR", 254.93), hasGrossValue("EUR", 254.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-06T08:12:29"), hasShares(2.26343768), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44466988303"), //
                        hasAmount("EUR", 254.91), hasGrossValue("EUR", 254.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-06T08:12:56"), hasShares(2.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501435029"), //
                        hasAmount("EUR", 225.24), hasGrossValue("EUR", 225.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:22:26"), hasShares(2.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501443398"), //
                        hasAmount("EUR", 225.13), hasGrossValue("EUR", 225.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:23:06"), hasShares(1.11076553), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501443474"), //
                        hasAmount("EUR", 125.00), hasGrossValue("EUR", 125.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-06T08:23:08"), hasShares(0.85068735), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501443481"), //
                        hasAmount("EUR", 125.00), hasGrossValue("EUR", 125.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transactions
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-06T08:12:29"), hasShares(0.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44466988302"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-06T08:12:29"), hasShares(0.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44466988303"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-06T08:12:56"), hasShares(0.00), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44501435029"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-06T02:10:08"), //
                        hasSource("Aktivitaetsauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.35), hasGrossValue("EUR", 1.83), //
                        hasTaxes("EUR", 0.48), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAktivitaetsauszug02()
    {
        var extractor = new Trading212PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aktivitaetsauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker("VWCE"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker("EUNL"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T08:05:03"), hasShares(0.03377465), //
                        hasSource("Aktivitaetsauszug02.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44514761133"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T08:05:24"), hasShares(0.04406062), //
                        hasSource("Aktivitaetsauszug02.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44514762366"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-07T11:34:48"), hasShares(1.00035279), //
                        hasSource("Aktivitaetsauszug02.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44551706884"), //
                        hasAmount("EUR", 113.28), hasGrossValue("EUR", 113.42), //
                        hasTaxes("EUR", 0.14), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-07T11:34:49"), hasShares(0.76659456), //
                        hasSource("Aktivitaetsauszug02.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44551706892"), //
                        hasAmount("EUR", 113.41), hasGrossValue("EUR", 113.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check removal transactions
        assertThat(results, hasItem(removal(hasDate("2026-01-07T01:32:07"), hasAmount("EUR", 57.35), //
                        hasSource("Aktivitaetsauszug02.txt"), hasNote("Kartenkauf"))));

        assertThat(results, hasItem(removal(hasDate("2026-01-07T01:32:07"), hasAmount("EUR", 199.90), //
                        hasSource("Aktivitaetsauszug02.txt"), hasNote("Kartenkauf"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-07T02:10:10"), //
                        hasSource("Aktivitaetsauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.32), hasGrossValue("EUR", 1.79), //
                        hasTaxes("EUR", 0.47), hasFees("EUR", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-07T11:30:16"), hasAmount("EUR", 22.33), //
                        hasSource("Aktivitaetsauszug02.txt"), hasNote("JP Morgan Bankeinzahlung"))));
    }

    @Test
    public void testAktivitaetsauszug03()
    {
        var extractor = new Trading212PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aktivitaetsauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(22L));
        assertThat(countBuySell(results), is(22L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(48));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US20030N1019"), hasWkn(null), hasTicker("CTP2"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasWkn(null), hasTicker("ASML"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn(null), hasTicker("NKE"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3VVMM84"), hasWkn(null), hasTicker("VFEM"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn(null), hasTicker("PRG"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US00123Q1040"), hasWkn(null), hasTicker("4OQ1"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker("CCC3"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US94106L1098"), hasWkn(null), hasTicker("UWS"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker("MSF"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US02209S1033"), hasWkn(null), hasTicker("PHM7"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE000S9YS762"), hasWkn(null), hasTicker("LIN"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B8GKDB10"), hasWkn(null), hasTicker("VHYL"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BP6MXD84"), hasWkn(null), hasTicker("SHELL"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker("RY6"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55S42"), hasWkn(null), hasTicker("VERX"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn(null), hasTicker("NOV"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB0007188757"), hasWkn(null), hasTicker("RIO1"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5801351017"), hasWkn(null), hasTicker("MDO"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE0000QLH0G6"), hasWkn(null), hasTicker("FTWD"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0304201033"), hasWkn(null), hasTicker("AWC"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA29250N1050"), hasWkn(null), hasTicker("EN3"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1491231015"), hasWkn(null), hasTicker("CAT1"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:41"), hasShares(0.04070231), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850736"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:41"), hasShares(0.00092744), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850742"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:42"), hasShares(0.01810436), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850750"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:43"), hasShares(0.01522388), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850758"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:43"), hasShares(0.00827653), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850764"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:44"), hasShares(0.10393315), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850770"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:44"), hasShares(0.01696042), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850777"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:44"), hasShares(0.00543304), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850783"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:44"), hasShares(0.00249694), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850791"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:45"), hasShares(0.02034303), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850797"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:46"), hasShares(0.00268562), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850811"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:47"), hasShares(0.03549851), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850817"), //
                        hasAmount("EUR", 2.55), hasGrossValue("EUR", 2.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:47"), hasShares(0.03286082), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850823"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:47"), hasShares(0.02020602), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850829"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:48"), hasShares(0.02148499), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850839"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:48"), hasShares(0.01969111), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850845"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:49"), hasShares(0.01424779), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850851"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:49"), hasShares(0.00387832), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850857"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:45:50"), hasShares(0.35971223), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853850864"), //
                        hasAmount("EUR", 2.55), hasGrossValue("EUR", 2.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:46:00"), hasShares(0.00906666), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853851006"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:46:02"), hasShares(0.02579013), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853851023"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13T13:47:23"), hasShares(0.00186813), //
                        hasSource("Aktivitaetsauszug03.txt"), //
                        hasNote("Auftrags-ID-Nr.: 44853851247"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check deposit transactions
        assertThat(results, hasItem(deposit(hasDate("2026-01-12T22:15:23"), hasAmount("EUR", 0.39), //
                        hasSource("Aktivitaetsauszug03.txt"), hasNote("JP Morgan Bankeinzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2026-01-13T12:30:09"), hasAmount("EUR", 5.16), //
                        hasSource("Aktivitaetsauszug03.txt"), hasNote("JP Morgan Bankeinzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2026-01-13T12:30:09"), hasAmount("EUR", 10.00), //
                        hasSource("Aktivitaetsauszug03.txt"), hasNote("JP Morgan Bankeinzahlung"))));

        assertThat(results, hasItem(deposit(hasDate("2026-01-13T13:45:09"), hasAmount("EUR", 5.00), //
                        hasSource("Aktivitaetsauszug03.txt"), hasNote("JP Morgan Bankeinzahlung"))));
    }
}
