package name.abuchen.portfolio.datatransfer.pdf.questrade;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.QuestradePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class QuestradePDFExtractorTest
{

    @Test
    public void testDeposit01()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CAD");

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-04-09"), //
                        hasAmount("CAD", 10000.00), //
                        hasSource("Deposit01.txt"), //
                        hasNote("Contribution"))));
    }

    @Test
    public void testBuy01()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEQT.TO"), //
                        hasName("VANGUARD ALL-EQUITY ETF  PORTFOLIO"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-10"), hasShares(50.0), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 2046.50), hasGrossValue("CAD", 2046.50), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testBuy02()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEQT.TO"), //
                        hasName("VANGUARD ALL-EQUITY ETF|PORTFOLIO ETF"), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-16"), hasShares(29.0), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 974.50), hasGrossValue("CAD", 974.40), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.10))));
    }

    @Test
    public void testDividend01()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VEQT.TO"), //
                        hasName(null), //
                        hasCurrencyCode("CAD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-07"), hasShares(29.0), //
                        hasSource("Dividend01.txt"), //
                        hasNote("REC 12/30/24"), //
                        hasAmount("CAD", 20.69), hasGrossValue("CAD", 20.69), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

}
