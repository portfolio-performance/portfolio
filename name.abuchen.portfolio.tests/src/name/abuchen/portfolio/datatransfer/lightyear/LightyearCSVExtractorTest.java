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

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.TestExtractorHelper;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class LightyearCSVExtractorTest
{
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
        var csv = String.join("\n", //
                        "Date,Reference,Ticker,ISIN,Type,Quantity,CCY,Price/share,Gross Amount,FX Rate,Fee,Net Amt.,Tax Amt.",
                        "01/12/2025 13:33:55,IN-0000000005,BRICEKSP,,Distribution,,EUR,,7.19,,0.36,6.83,0.00");

        var file = File.createTempFile("lightyear_test", ".csv");
        file.deleteOnExit();
        Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = new LightyearCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file), errors);

        assertThat(errors, empty());
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-01T13:33:55"), //
                        hasAmount("EUR", 6.83), //
                        hasTaxes("EUR", 0.36), //
                        hasSecurity(hasTicker("BRICEKSP"), hasIsin("IE000GWTNRJ7")))));
    }

    @Test
    public void testSemicolonDelimiterAndIsoDates() throws IOException
    {
        var csv = String.join("\n", //
                        "Date;Reference;Ticker;ISIN;Type;Quantity;CCY;Price/share;Gross Amount;FX Rate;Fee;Net Amt.;Tax Amt.",
                        "2025-04-07 16:04:53;DT-0000000013;;;Deposit;;EUR;;100.00;;0.00;100.00;",
                        "2025-03-15 10:30:00;OR-0000000014;AAPL;US0378331005;Buy;5.0;USD;180.00;900.00;;0.00;900.00;");

        var file = File.createTempFile("lightyear_test_semicolon", ".csv");
        file.deleteOnExit();
        Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = new LightyearCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file), errors);

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
        var csv = "Some,Other,Header,Columns\n1,2,3,4\n";
        var file = File.createTempFile("invalid_lightyear", ".csv");
        file.deleteOnExit();
        Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var results = new LightyearCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }
}
