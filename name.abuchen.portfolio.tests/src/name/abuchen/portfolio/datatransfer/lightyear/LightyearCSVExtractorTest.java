package name.abuchen.portfolio.datatransfer.lightyear;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.TestExtractorHelper;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class LightyearCSVExtractorTest
{
    private static final String CSV_HEADER = "Date,Reference,Ticker,ISIN,Type,Quantity,CCY,Price/share,Gross Amount,FX Rate,Fee,Net Amt.,Tax Amt.";

    private List<Item> extract(Client client, List<Exception> errors, String... lines) throws IOException
    {
        return extractWithHeader(client, CSV_HEADER, errors, lines);
    }

    private List<Item> extractWithHeader(Client client, String header, List<Exception> errors, String... lines)
                    throws IOException
    {
        var csv = header + "\n" + String.join("\n", lines);
        var file = File.createTempFile("lightyear_test", ".csv");
        file.deleteOnExit();
        Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);

        return new LightyearCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file), errors);
    }

    @Test
    public void testLightyearExport01() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var filename = "LightyearExport01.csv";

        var results = TestExtractorHelper.runExtractor(new LightyearCSVExtractor(client), client, getClass(), filename,
                        errors);

        assertThat(errors, empty());

        // 5 Buy/Sell (3 BUY, 2 SELL)
        assertThat(countBuySell(results), is(5L));

        // 10 Account transactions (2 Dividends, 2 Distributions, 1 Deposit, 2
        // Withdrawals, 1 Interest, 1 Reward, 1 Conversion Fee)
        assertThat(countAccountTransactions(results), is(10L));

        // 1 Currency conversion transfer
        assertThat(countAccountTransfers(results), is(1L));

        // 16 transactions and 5 securities created by the import
        assertThat(results.size(), is(21));

        // Dividend GOOG
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-16T06:02:35"), //
                        hasAmount("USD", 11.98), //
                        hasTaxes("USD", 2.11), //
                        hasSecurity(hasTicker("GOOG"), hasIsin("US02079K1079")))));

        // Dividend V
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-02T06:16:10"), //
                        hasAmount("USD", 1.14), //
                        hasTaxes("USD", 0.20), //
                        hasSecurity(hasTicker("V"), hasIsin("US92826C8394")))));

        // MMF Distribution BRICEKSP (Fee column parsed as tax)
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T13:33:55"), //
                        hasAmount("EUR", 6.83), //
                        hasTaxes("EUR", 0.36), //
                        hasSecurity(hasTicker("BRICEKSP"), hasIsin("IE000GWTNRJ7")))));

        // MMF Distribution ICSUSSDP (Fee column parsed as tax)
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T13:33:55"), //
                        hasAmount("USD", 21.17), //
                        hasTaxes("USD", 0.81), //
                        hasSecurity(hasTicker("ICSUSSDP"), hasIsin("IE00B44BQ083")))));

        // Reward
        assertThat(results, hasItem(interest( //
                        hasDate("2025-10-02T17:19:06"), //
                        hasAmount("EUR", 0.05))));

        // Uninvested cash interest
        assertThat(results, hasItem(interest( //
                        hasDate("2025-10-01T03:18:28"), //
                        hasAmount("USD", 0.02))));

        // Deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-04-07T16:04:53"), //
                        hasAmount("EUR", 100.00))));

        // Withdrawals
        assertThat(results, hasItem(removal( //
                        hasDate("2025-07-30T09:01:34"), //
                        hasAmount("USD", 300.00))));
        assertThat(results, hasItem(removal( //
                        hasDate("2025-07-25T07:45:01"), //
                        hasAmount("USD", 1.00))));

        // AAPL Buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-15T10:30:00"), //
                        hasShares(10.00), //
                        hasAmount("USD", 1855.00), //
                        hasSecurity(hasTicker("AAPL"), hasIsin("US0378331005")))));

        // AAPL Sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-20T14:22:11"), //
                        hasShares(10.00), //
                        hasAmount("USD", 2103.00), //
                        hasSecurity(hasTicker("AAPL"), hasIsin("US0378331005")))));

        // Conversion: Transfer out from EUR, in to USD, and fee
        assertThat(results, hasItem(outboundCash( //
                        hasDate("2025-04-07T19:09:30"), //
                        hasAmount("EUR", 59.03), //
                        hasForexGrossValue("USD", 64.50))));
        assertThat(results, hasItem(inboundCash( //
                        hasDate("2025-04-07T19:09:30"), //
                        hasAmount("USD", 64.50))));
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-07T19:09:30"), //
                        hasAmount("EUR", 0.21))));

        new AssertImportActions().check(results, "EUR", "USD");
    }

    @Test
    public void testDistributionMissingIsinUsesTickerFallback() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "01/12/2025 13:33:55,IN-0000000005,BRICEKSP,,Distribution,,EUR,,7.19,,0.36,6.83,0.00");

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T13:33:55"), //
                        hasAmount("EUR", 6.83), //
                        hasTaxes("EUR", 0.36), //
                        hasSecurity(hasTicker("BRICEKSP"), hasIsin("IE000GWTNRJ7")))));
    }

    @Test
    public void testIsoDates() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "2025-04-07 16:04:53,DT-0000000013,,,Deposit,,EUR,,100.00,,0.00,100.00,",
                        "2025-03-15 10:30:00,OR-0000000014,AAPL,US0378331005,Buy,5.0,USD,180.00,900.00,,0.00,900.00,");

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(results, hasItem(deposit(hasDate("2025-04-07T16:04:53"), hasAmount("EUR", 100.00))));
        assertThat(results,
                        hasItem(purchase(hasDate("2025-03-15T10:30:00"), hasShares(5.00), hasAmount("USD", 900.00))));
    }

    @Test
    public void testUnsupportedFileReportsError() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extractWithHeader(client, "Some,Other,Header,Columns", errors, "1,2,3,4");

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testBlankCurrencyDoesNotAffectSecurityLookup() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "15/03/2025 10:30:00,OR-01,AAPL,US0378331005,Buy,10.00,,185.50,1855.00,,0.00,1855.00,",
                        "16/03/2025 10:30:00,OR-02,AAPL,US0378331005,Buy,10.00,USD,185.50,1855.00,,0.00,1855.00,");

        assertThat(errors.size(), is(1));
        assertThat(countBuySell(results), is(1L));
        assertThat(results.get(0).getSecurity().getCurrencyCode(), is("USD"));
    }

    @Test
    public void testQuotedFieldsAndHeaders() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extractWithHeader(client,
                        "\"Date\",\"Reference\",\"Ticker\",\"ISIN\",\"Type\",\"Quantity\",\"CCY\",\"Price/share\",\"Gross Amount\",\"FX Rate\",\"Fee\",\"Net Amt.\",\"Tax Amt.\"",
                        errors,
                        "\"2025-04-07 16:04:53\",\"DT-0000000013\",\"\",\"\",\"Deposit\",\"\",\"EUR\",\"\",\"100.00\",\"\",\"0.00\",\"100.00\",");

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results, hasItem(deposit(hasDate("2025-04-07T16:04:53"), hasAmount("EUR", 100.00))));
    }

    @Test
    public void testMissingRequiredFieldsThrowErrors() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "15/03/2025 10:30:00,OR-01,AAPL,US0378331005,Buy,,USD,185.50,,,0.00,,",
                        "07/04/2025 16:04:53,DT-01,,,Deposit,,EUR,,,,0.00,,");

        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(errors.size(), is(2));
    }

    @Test
    public void testCashTransactionsWithMissingCurrencyAreRejected() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "2025-04-07 16:04:53,DT-01,,,Deposit,,,,,,0.00,100.00,",
                        "2025-04-08 16:04:53,WL-01,,,Withdrawal,,,,,,0.00,-25.00,",
                        "2025-04-09 16:04:53,IN-01,,,Interest,,,,,,0.00,1.00,0.10",
                        "2025-04-10 16:04:53,RW-01,,,Reward,,,,,,0.00,0.50,");

        assertThat(results, empty());
        assertThat(errors.size(), is(4));
        for (var error : errors)
            assertThat(error.getMessage().contains("Missing currency"), is(true));
    }

    @Test
    public void testIncompleteAndMalformedConversionsAreRejectedAtomically() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "2025-04-07 19:09:30,CN-INCOMPLETE,EUR,,Conversion,,EUR,,-10.00,,0.00,-10.00,",
                        "2025-04-08 19:09:30,CN-AMOUNT,EUR,,Conversion,,EUR,,not-a-number,,0.00,-10.00,",
                        "2025-04-08 19:09:30,CN-AMOUNT,USD,,Conversion,,USD,,11.00,0.90,0.00,11.00,",
                        "2025-04-09 19:09:30,CN-FEE,EUR,,Conversion,,EUR,,-10.00,,not-a-number,-10.00,",
                        "2025-04-09 19:09:30,CN-FEE,USD,,Conversion,,USD,,10.00,1.00,0.00,10.00,",
                        "2025-04-10 19:09:30,CN-NO-FX,EUR,,Conversion,,EUR,,-59.03,,0.00,-59.03,",
                        "2025-04-10 19:09:30,CN-NO-FX,USD,,Conversion,,USD,,64.50,,0.00,64.50,",
                        "2025-04-11 16:04:53,DT-VALID,,,Deposit,,EUR,,100.00,,0.00,100.00,");

        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results, hasItem(deposit(hasDate("2025-04-11T16:04:53"), hasAmount("EUR", 100.00))));
        assertThat(errors.size(), is(4));
    }

    @Test
    public void testForexGrossValueOnCurrencyMismatch() throws IOException
    {
        var client = new Client();
        // Existing security in EUR
        var sec = new Security("Apple Inc", "EUR");
        sec.setIsin("US0378331005");
        sec.setTickerSymbol("AAPL");
        client.addSecurity(sec);

        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "15/03/2025 10:30:00,OR-01,AAPL,US0378331005,Buy,10.00,USD,185.50,1855.00,0.92,0.00,1855.00,");

        assertThat(errors, empty());
        assertThat(countBuySell(results), is(1L));
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-15T10:30:00"), //
                        hasShares(10.00), //
                        hasAmount("USD", 1855.00), //
                        hasForexGrossValue("EUR", 1706.60))));
    }

    @Test
    public void testForexCurrencyMismatchWithoutRateIsReported() throws IOException
    {
        var client = new Client();
        var apple = new Security("Apple Inc", "EUR");
        apple.setIsin("US0378331005");
        apple.setTickerSymbol("AAPL");
        client.addSecurity(apple);

        var alphabet = new Security("Alphabet Inc", "EUR");
        alphabet.setIsin("US02079K1079");
        alphabet.setTickerSymbol("GOOG");
        client.addSecurity(alphabet);

        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "15/03/2025 10:30:00,OR-01,AAPL,US0378331005,Buy,10.00,USD,185.50,1855.00,,0.00,1855.00,",
                        "16/12/2025 06:02:35,IN-01,GOOG,US02079K1079,Dividend,52.0,USD,,14.09,,0.00,11.98,2.11");

        assertThat(results, empty());
        assertThat(errors.size(), is(2));
        assertThat(errors.get(0).getMessage().contains("Missing FX Rate"), is(true));
        assertThat(errors.get(1).getMessage().contains("Missing FX Rate"), is(true));
    }

    @Test
    public void testForexGrossValueWithFee() throws IOException
    {
        var client = new Client();
        var sec = new Security("Apple Inc", "EUR");
        sec.setIsin("US0378331005");
        sec.setTickerSymbol("AAPL");
        client.addSecurity(sec);

        // Buy with a $5 fee: gross = 1855, net = 1860 (gross + fee),
        // totalAmount = 1860
        // GROSS_VALUE must use the gross value (1855), not totalAmount (1860)
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "15/03/2025 10:30:00,OR-01,AAPL,US0378331005,Buy,10.00,USD,185.50,1855.00,0.92,5.00,1860.00,");

        assertThat(errors, empty());
        assertThat(countBuySell(results), is(1L));
        // grossValue = totalAmount - fee = 1860 - 5 = 1855
        // forexGrossValue = 1855 * 0.92 = 1706.60
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-15T10:30:00"), //
                        hasShares(10.00), //
                        hasAmount("USD", 1860.00), //
                        hasForexGrossValue("EUR", 1706.60))));
    }

    @Test
    public void testForexDividendWithTax() throws IOException
    {
        var client = new Client();
        var sec = new Security("Alphabet Inc", "EUR");
        sec.setIsin("US02079K1079");
        sec.setTickerSymbol("GOOG");
        client.addSecurity(sec);

        // Dividend with tax: gross = 14.09, tax = 2.11, net = 11.98
        // GROSS_VALUE must use grossValue (net + tax = 14.09), not netAmount
        // (11.98)
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "16/12/2025 06:02:35,IN-01,GOOG,US02079K1079,Dividend,52.0,USD,,14.09,0.92,0.00,11.98,2.11");

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        // grossValue = netAmount + tax = 11.98 + 2.11 = 14.09
        // forexGrossValue = 14.09 * 0.92 = 12.9628 -> 12.96
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-16T06:02:35"), //
                        hasAmount("USD", 11.98), //
                        hasTaxes("USD", 2.11), //
                        hasForexGrossValue("EUR", 12.96))));
    }

    @Test
    public void testMissingSecurityInformationThrowsError() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extract(client, errors,
                        "15/03/2025 10:30:00,OR-01,,,Buy,10.00,USD,185.50,1855.00,,0.00,1855.00,");

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage().contains("Missing security name, ISIN, and ticker"), is(true));
        assertThat(client.getSecurities(), empty());
    }

    @Test
    public void testUtf8BomHandling() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = extractWithHeader(client, "\uFEFF" + CSV_HEADER, errors,
                        "15/03/2025 10:30:00,OR-01,AAPL,US0378331005,Buy,10.00,USD,185.50,1855.00,,0.00,1855.00,");

        assertThat(errors, empty());
        assertThat(countBuySell(results), is(1L));
    }
}
