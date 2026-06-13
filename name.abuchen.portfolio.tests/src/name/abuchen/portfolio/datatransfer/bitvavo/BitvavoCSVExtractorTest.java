package name.abuchen.portfolio.datatransfer.bitvavo;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.securityTransfer;
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

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.TestExtractorHelper;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BitvavoCSVExtractorTest
{
    @Test
    public void testKauf01() throws IOException
    {
        var client = new Client();
        var extractor = new BitvavoCSVExtractor(client);
        var errors = new ArrayList<Exception>();

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), "Kauf01.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("BTC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-15T14:23:11"), hasShares(0.005), //
                        hasSource("Kauf01.csv"), //
                        hasNote("a1b2c3d4-e5f6-7890-abcd-ef1234567890"), //
                        hasAmount("EUR", 200.50), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));
    }

    @Test
    public void testVerkauf01() throws IOException
    {
        var client = new Client();
        var extractor = new BitvavoCSVExtractor(client);
        var errors = new ArrayList<Exception>();

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), "Verkauf01.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("BTC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-16T09:45:00"), hasShares(0.005), //
                        hasSource("Verkauf01.csv"), //
                        hasNote("b2c3d4e5-f6a7-8901-bcde-f12345678901"), //
                        hasAmount("EUR", 204.49), hasGrossValue("EUR", 205.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));
    }

    @Test
    public void testEinzahlung01() throws IOException
    {
        var client = new Client();
        var extractor = new BitvavoCSVExtractor(client);
        var errors = new ArrayList<Exception>();

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), "Einzahlung01.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(deposit( //
                        hasDate("2025-01-10T08:00:00"), //
                        hasSource("Einzahlung01.csv"), //
                        hasNote("c3d4e5f6-a7b8-9012-cdef-123456789012 | DE11***22"), //
                        hasAmount("EUR", 1000.00))));
    }

    @Test
    public void testKryptoAuszahlung01() throws IOException
    {
        var client = new Client();
        var extractor = new BitvavoCSVExtractor(client);
        var errors = new ArrayList<Exception>();

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), "KryptoAuszahlung01.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("BTC"), //
                        hasCurrencyCode("EUR"))));

        // Network fee: sell 0.00001 BTC at EUR 0
        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-20T16:30:45"), hasShares(0.00001), //
                        hasSource("KryptoAuszahlung01.csv"), //
                        hasNote("d4e5f6a7-b8c9-0123-defa-234567890123 | bc1qtestaddress0000000000000000000000000000"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // Net transfer: 0.00999 BTC (Depotumbuchung ausgehend)
        assertThat(results, hasItem(securityTransfer( //
                        hasDate("2025-01-20T16:30:45"), hasShares(0.00999), //
                        hasAmount("EUR", 0.00))));
    }

    @Test
    public void testRabatt01() throws IOException
    {
        var client = new Client();
        var extractor = new BitvavoCSVExtractor(client);
        var errors = new ArrayList<Exception>();

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), "Rabatt01.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(feeRefund( //
                        hasDate("2025-01-15T14:23:12"), //
                        hasSource("Rabatt01.csv"), //
                        hasNote("e5f6a7b8-c9d0-1234-efab-345678901234"), //
                        hasAmount("EUR", 0.50))));
    }

    @Test
    public void testBonus01() throws IOException
    {
        var client = new Client();
        var extractor = new BitvavoCSVExtractor(client);
        var errors = new ArrayList<Exception>();

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), "Bonus01.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(deposit( //
                        hasDate("2025-01-05T10:00:00"), //
                        hasSource("Bonus01.csv"), //
                        hasNote("f6a7b8c9-d0e1-2345-fabc-456789012345"), //
                        hasAmount("EUR", 10.00))));
    }
}
