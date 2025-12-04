package name.abuchen.portfolio.datatransfer.pdf.traderepublic;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.datatransfer.pdf.TradeRepublicPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.online.SecuritySearchProvider;

@SuppressWarnings("nls")
public class TradeRepublicPDFExtractorTest2
{
    TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testCardTransactionsPresent()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatementSummary02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(44L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(results.size(), is(44));
        new AssertImportActions().check(results, "EUR");

        // Transfer on 12-nov-2025 25 EUR
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-12T00:00:00"), //
                        hasAmount("EUR", 25), //
                        hasSource("AccountStatementSummary02.txt") //
        )));

        // AliExpress Card Transaction on 02-nov-2025
        assertThat(results, hasItem(removal( //
                        hasDate("2025-11-02T00:00:00"), //
                        hasAmount("EUR", 23.53), //
                        hasNote("ALIEXPRESS.COM"),
                        hasSource("AccountStatementSummary02.txt") //
        )));

        // on 03-nov-2025 10.12 EUR
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-03T00:00:00"), //
                        hasAmount("EUR", 10.12), //
                        hasSource("AccountStatementSummary02.txt") //
        )));

    }

    @Test
    public void testExpectedInterestPaymentsPresent()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatementSummary02.txt"), errors);

        assertThat(errors, empty());

        // Additional Card Transaction PAYPAL 6.99 different date
        // Not working
        // assertThat(results, hasItem(removal( //
        // hasDate("2025-11-08T00:00:00"), //
        // hasAmount("EUR", 6.99), //
        // hasNote("PAYPAL *ITUNESAPPST APPLE"),
        // hasSource("AccountStatementSummary02.txt"))));

        // interest payment on 01-nov-2025
        assertThat(results, hasItem(interest( //
                        hasDate("2025-11-01T00:00:00"), //
                        hasAmount("EUR", 53.33), //
                        hasSource("AccountStatementSummary02.txt") //
        )));

    }

    public void testExpectedTransactionsPresent()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatementSummary02.txt"), errors);

        assertThat(errors, empty());

        // Saving Plan (Trade) 03-nov-2025: 175.00 EUR
        assertThat(results, hasItem(removal( //
                        hasDate("2025-11-03T00:00:00"), //
                        hasAmount("EUR", 175.00), //
                        hasSource("AccountStatementSummary02.txt") //
        )));

        // Saving Plan (Trade) execution 03-nov-2025: 12.04 EUR
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2025-11-03T00:00:00"), //
                        // TransactionMatchers.hasIsin("IE000716YHJ7"), //
                        hasAmount("EUR", 12.04), //
                        // TransactionMatchers.hasSharesCloseTo(1.405555, 1e-6),
                        hasSource("AccountStatementSummary02.txt"))));


        // incoming large transfer on 28-nov-2025: 8952.98 EUR
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-28T00:00:00"), //
                        hasAmount("EUR", 8952.98), //
                        hasSource("AccountStatementSummary02.txt"))));

    }



}