package name.abuchen.portfolio.datatransfer.pdf.robinhood;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
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
import name.abuchen.portfolio.datatransfer.pdf.RobinhoodPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class RobinhoodPDFExtractorTest
{
    @Test
    public void testAccountStatement01()
    {
        var extractor = new RobinhoodPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(25L));
        assertThat(countBuySell(results), is(56L));
        assertThat(countAccountTransactions(results), is(15L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(96));
        new AssertImportActions().check(results, "USD");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("922042775"), hasTicker("VEU"), //
                        hasName("Vanguard FTSE All-World ex-US ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("007903107"), hasTicker("AMD"), //
                        hasName("AMD"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("042068205"), hasTicker("ARM"), //
                        hasName("Arm Holdings plc"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("458140100"), hasTicker("INTC"), //
                        hasName("Intel"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("808524201"), hasTicker("SCHX"), //
                        hasName("Schwab US Large-Cap ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46625H100"), hasTicker("JPM"), //
                        hasName("JPMorgan Chase"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("81762P102"), hasTicker("NOW"), //
                        hasName("ServiceNow"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("594918104"), hasTicker("MSFT"), //
                        hasName("Microsoft"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("38141G104"), hasTicker("GS"), //
                        hasName("Goldman Sachs"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("770700102"), hasTicker("HOOD"), //
                        hasName("Robinhood Markets"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("02079K305"), hasTicker("GOOGL"), //
                        hasName("Alphabet Class A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("30303M102"), hasTicker("META"), //
                        hasName("Meta Platforms"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("68389X105"), hasTicker("ORCL"), //
                        hasName("Oracle"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("G5279N105"), hasTicker("KLAR"), //
                        hasName("Klarna Group"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11135F101"), hasTicker("AVGO"), //
                        hasName("Broadcom"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("N07059210"), hasTicker("ASML"), //
                        hasName("ASML Holding NV"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("G25457105"), hasTicker("CRDO"), //
                        hasName("Credo Technology Group"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("902252105"), hasTicker("TYL"), //
                        hasName("Tyler Technologies"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("PSKY"), //
                        hasName("PSKY"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("WMT"), //
                        hasName("WMT"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("TSM"), //
                        hasName("TSM"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("CRM"), //
                        hasName("CRM"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("MU"), //
                        hasName("MU"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("MPWR"), //
                        hasName("MPWR"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("MRVL"), //
                        hasName("MRVL"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-31"), hasShares(10.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 736.65), hasGrossValue("USD", 736.65), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-02"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 224.36), hasGrossValue("USD", 224.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-05"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 232.72), hasGrossValue("USD", 232.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-08"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 224.24), hasGrossValue("USD", 224.24), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-09"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 220.48), hasGrossValue("USD", 220.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-09"), hasShares(4.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 181.44), hasGrossValue("USD", 181.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-09"), hasShares(12.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 907.20), hasGrossValue("USD", 907.20), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-12"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 218.48), hasGrossValue("USD", 218.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-12"), hasShares(12.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 326.88), hasGrossValue("USD", 326.88), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 632.48), hasGrossValue("USD", 632.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 624.72), hasGrossValue("USD", 624.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 621.44), hasGrossValue("USD", 621.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-13"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 277.44), hasGrossValue("USD", 277.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-14"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 208.72), hasGrossValue("USD", 208.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-14"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 617.20), hasGrossValue("USD", 617.20), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-14"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 464.36), hasGrossValue("USD", 464.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 456.36), hasGrossValue("USD", 456.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 457.20), hasGrossValue("USD", 457.20), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 231.12), hasGrossValue("USD", 231.12), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 234.60), hasGrossValue("USD", 234.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 472.50), hasGrossValue("USD", 472.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 959.48), hasGrossValue("USD", 959.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-15"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 100.48), hasGrossValue("USD", 100.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 107.36), hasGrossValue("USD", 107.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 238.48), hasGrossValue("USD", 238.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 321.60), hasGrossValue("USD", 321.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 318.72), hasGrossValue("USD", 318.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 604.24), hasGrossValue("USD", 604.24), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 454.24), hasGrossValue("USD", 454.24), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 184.12), hasGrossValue("USD", 184.12), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 365.44), hasGrossValue("USD", 365.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 180.60), hasGrossValue("USD", 180.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 103.36), hasGrossValue("USD", 103.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(15.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 404.40), hasGrossValue("USD", 404.40), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(9.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 242.64), hasGrossValue("USD", 242.64), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 152.72), hasGrossValue("USD", 152.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 152.00), hasGrossValue("USD", 152.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-20"), hasShares(8.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 607.68), hasGrossValue("USD", 607.68), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-21"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 244.96), hasGrossValue("USD", 244.96), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-21"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 105.44), hasGrossValue("USD", 105.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-21"), hasShares(4.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 108.48), hasGrossValue("USD", 108.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-21"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 353.44), hasGrossValue("USD", 353.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-21"), hasShares(24.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 640.80), hasGrossValue("USD", 640.80), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-22"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 254.73), hasGrossValue("USD", 254.73), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-23"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 528.96), hasGrossValue("USD", 528.96), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-23"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 317.60), hasGrossValue("USD", 317.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-23"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 597.20), hasGrossValue("USD", 597.20), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 324.00), hasGrossValue("USD", 324.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-01-27"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1469.36), hasGrossValue("USD", 1469.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-28"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 255.92), hasGrossValue("USD", 255.92), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 327.36), hasGrossValue("USD", 327.36), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 248.72), hasGrossValue("USD", 248.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 434.72), hasGrossValue("USD", 434.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 100.72), hasGrossValue("USD", 100.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 396.72), hasGrossValue("USD", 396.72), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-29"), hasShares(12.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 937.44), hasGrossValue("USD", 937.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-02"), hasExDate(null), hasShares(43.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 2.15), hasGrossValue("USD", 2.15), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-05"), hasExDate(null), hasShares(27.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 6.35), hasGrossValue("USD", 6.35), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-08"), hasExDate(null), hasShares(115.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 72.26), hasGrossValue("USD", 91.47), //
                        hasTaxes("USD", 19.21), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-08"), hasExDate(null), hasShares(24.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 9.98), hasGrossValue("USD", 9.98), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-14"), hasExDate(null), hasShares(5.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.58), hasGrossValue("USD", 0.58), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-15"), hasExDate(null), hasShares(3.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4.68), hasGrossValue("USD", 4.68), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-23"), hasExDate(null), hasShares(18.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 9.00), hasGrossValue("USD", 9.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-29"), hasExDate(null), hasShares(14.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.84), hasGrossValue("USD", 0.84), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-02"), hasAmount("USD", 7500.00), //
                        hasSource("AccountStatement01.txt"), hasNote("Transfer from Brokerage to Traditional IRA"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-02"), hasAmount("USD", 7500.00), //
                        hasSource("AccountStatement01.txt"), hasNote("ACH Deposit"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-12"), hasAmount("USD", 70.00), //
                        hasSource("AccountStatement01.txt"), hasNote("Cash back from Robinhood Credit Card"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-13"), hasAmount("USD", 10000.00), //
                        hasSource("AccountStatement01.txt"), hasNote("ACH Withdrawal"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-30"), hasAmount("USD", 30.46), //
                        hasSource("AccountStatement01.txt"), hasNote("Cash back from Robinhood Credit Card"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-20"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 102.83), hasGrossValue("USD", 102.83), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-30"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 58.21), hasGrossValue("USD", 58.21), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
