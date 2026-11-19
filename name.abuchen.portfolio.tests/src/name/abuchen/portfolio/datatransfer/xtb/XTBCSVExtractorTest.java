package name.abuchen.portfolio.datatransfer.xtb;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.TestExtractorHelper;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class XTBCSVExtractorTest
{
    @Test
    public void testEightColumnExportLayout() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = TestExtractorHelper.runExtractor(new XTBCSVExtractor(client), client, getClass(),
                        "EUR_account_sample.csv", errors);

        assertThat(errors, empty());
        assertThat(countSecurities(items), is(1L));
        assertThat(items.size(), is(16));
        assertThat(items, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("EXAM.DE"), //
                        hasName("Example ETF"), //
                        hasCurrencyCode("EUR"))));
        assertCashOperations(items);
    }

    private void assertCashOperations(List<Item> items)
    {
        assertCashOperations(items, "EUR_account_sample.csv");
    }

    private void assertCashOperations(List<Item> items, String source)
    {
        assertThat(countBuySell(items), is(4L));
        assertThat(countAccountTransactions(items), is(11L));
        assertThat(countItemsWithFailureMessage(items), is(0L));
        assertThat(countSkippedItems(items), is(0L));

        assertThat(items, hasItem(deposit(hasDate("2025-01-02T09:00"), hasAmount("EUR", 100.00), //
                        hasSource(source), hasNote("Deposit (XTB ID: 1)"))));

        assertThat(items, hasItem(purchase( //
                        hasDate("2025-01-03T10:00"), hasShares(2.00), //
                        hasSource(source), //
                        hasNote("OPEN BUY 2 @ 25.125 (XTB ID: 2)"), //
                        hasAmount("EUR", 50.25), hasGrossValue("EUR", 50.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(purchase( //
                        hasDate("2025-01-03T11:00"), hasShares(0.02), //
                        hasSource(source), //
                        hasNote("OPEN BUY 0.02/3.02 @ 27.782 (XTB ID: 13)"), //
                        hasAmount("EUR", 0.56), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(sale( //
                        hasDate("2025-01-04T10:00"), hasShares(0.50), //
                        hasSource(source), //
                        hasNote("CLOSE BUY 0.5 @ 49.5 (XTB ID: 3)"), //
                        hasAmount("EUR", 24.75), hasGrossValue("EUR", 24.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(sale( //
                        hasDate("2025-01-04T11:00"), hasShares(0.10), //
                        hasSource(source), //
                        hasNote("CLOSE SELL 0.1/3.02 @ 50.00 (XTB ID: 14)"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(removal(hasDate("2025-01-05T09:00"), hasAmount("EUR", 10.00), //
                        hasSource(source), hasNote("Withdrawal (XTB ID: 4)"))));

        assertThat(items, hasItem(taxes( //
                        hasDate("2025-01-08T14:00"), hasShares(0.00), //
                        hasSecurity(hasName("Example ETF")), //
                        hasSource(source), //
                        hasNote("Financial transaction tax (XTB ID: 9)"), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(interestCharge( //
                        hasDate("2025-01-09T17:00"), not(hasSecurity()), //
                        hasSource(source), //
                        hasNote("ETHEREUM: Swap of position #100 (XTB ID: 10)"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(interest( //
                        hasDate("2025-01-09T17:05"), not(hasSecurity()), //
                        hasSource(source), //
                        hasNote("ETHEREUM: Swap of position #101 (XTB ID: 17)"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // the withholding tax booked at the same time becomes the tax of the
        // dividend; the number of shares is derived from 0.3500 per share
        assertThat(items, hasItem(dividend( //
                        hasDate("2025-01-10T10:00"), hasShares(1.00), //
                        hasSecurity(hasName("Example ETF")), //
                        hasSource(source), //
                        hasNote("EXAM.DE EUR 0.3500/ SHR (XTB ID: 15)"), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.35), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(interest( //
                        hasDate("2025-02-03T18:23:45"), not(hasSecurity()), //
                        hasSource(source), //
                        hasNote("Free-funds Interest 2025-01 (XTB ID: 11)"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.07), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(interestCharge( //
                        hasDate("2025-01-11T17:00"), not(hasSecurity()), //
                        hasSource(source), //
                        hasNote("ETHEREUM: Profit of position #102 (XTB ID: 18)"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(interestCharge( //
                        hasDate("2025-01-11T17:00"), not(hasSecurity()), //
                        hasSource(source), //
                        hasNote("ETHEREUM: Swap of position #102 (XTB ID: 19)"), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // the CFD shares the ticker with the real instrument, but its result
        // must not be booked as income of that security
        assertThat(items, hasItem(interest( //
                        hasDate("2025-01-12T15:00"), not(hasSecurity()), //
                        hasSource(source), //
                        hasNote("Example ETF: Profit of position #103 (XTB ID: 20)"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(items, hasItem(dividend( //
                        hasDate("2025-01-13T14:30"), hasShares(2.00), //
                        hasSecurity(hasName("Example ETF")), //
                        hasSource(source), //
                        hasNote("EXAM.DE EUR 0.4800/ SHR (XTB ID: 21)"), //
                        hasAmount("EUR", 0.96), hasGrossValue("EUR", 0.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        new AssertImportActions().check(items, "EUR");
    }

    @Test
    public void testTaxedInterestMatchesExistingTransaction() throws IOException
    {
        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = TestExtractorHelper.runExtractor(new XTBCSVExtractor(client), client, getClass(),
                        "EUR_account_sample.csv", errors);
        assertThat(errors, empty());

        var imported = items.stream().filter(item -> item instanceof TransactionItem)
                        .map(item -> (AccountTransaction) item.getSubject())
                        .filter(transaction -> transaction.getType() == AccountTransaction.Type.INTEREST
                                        && transaction.getUnitSum(Unit.Type.TAX).getAmount() > 0).findFirst()
                        .orElseThrow();

        var existing = new AccountTransaction();
        existing.setType(AccountTransaction.Type.INTEREST);
        existing.setDateTime(imported.getDateTime());
        existing.setCurrencyCode("EUR");
        existing.setAmount(6);
        existing.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 1)));
        var account = new Account();
        account.setCurrencyCode("EUR");
        account.addTransaction(existing);

        assertThat(new DetectDuplicatesAction(client).process(imported, account).getCode(), is(Code.WARNING));

        // Earlier XTB imports stored the gross interest and tax as separate transactions.
        existing.removeUnit(existing.getUnits().findFirst().orElseThrow());
        existing.setAmount(7);
        assertThat(new DetectDuplicatesAction(client).process(imported, account).getCode(), is(Code.WARNING));
    }

    @Test
    public void testTenColumnExportLayout() throws IOException
    {
        var resource = TestExtractorHelper.loadResourceAsInputFile(getClass(), "EUR_account_sample.csv");
        var content = toTenColumnLayout(Files.readString(resource.getFile().toPath()));
        var file = Files.createTempFile("xtb-ten-column-", ".csv");
        Files.writeString(file, content);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = new XTBCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file.toFile())
        {
            @Override
            public String getName()
            {
                return "EUR_ten_column_sample.csv";
            }
        }, errors);

        assertThat(errors, empty());
        assertCashOperations(items, "EUR_ten_column_sample.csv");
    }

    private String toTenColumnLayout(String content)
    {
        var lines = content.split("\\R");
        var converted = new StringBuilder();
        for (int index = 0; index < 4; index++)
            converted.append(lines[index]).append(";;\n");
        converted.append("Type;Instrument;Ticker;Category;Time;Amount;ID;Comment;Product;Position ID\n");

        for (int index = 5; index < lines.length; index++)
        {
            var columns = lines[index].split(";", -1);
            if ("Total".equals(columns[0]))
            {
                converted.append("Total;;;;;").append(columns[4]).append(";;;;\n");
            }
            else
            {
                converted.append(String.join(";", columns[0], columns[2], columns[1], "", columns[3], columns[4],
                                columns[5], columns[6], columns[7], "")).append('\n');
            }
        }
        return converted.toString();
    }

    @Test
    public void testWindows1250Encoding() throws IOException
    {
        var resource = TestExtractorHelper.loadResourceAsInputFile(getClass(), "EUR_account_sample.csv");
        var content = Files.readString(resource.getFile().toPath()).replace("Deposit;My Trades",
                        "Vklad odměny za pozvání přátel;My Trades");
        var file = Files.createTempFile("xtb-", ".csv");
        Files.writeString(file, content, Charset.forName("windows-1250"));

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = new XTBCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file.toFile())
        {
            @Override
            public String getName()
            {
                return "EUR_account_sample.csv";
            }
        }, errors);

        assertThat(errors, empty());
        assertThat(items.size(), is(15));
        assertThat(items, hasItem(deposit(hasNote("Vklad odměny za pozvání přátel (XTB ID: 1)"))));
    }

    @Test
    public void testUtf8ByteOrderMark() throws IOException
    {
        var resource = TestExtractorHelper.loadResourceAsInputFile(getClass(), "EUR_account_sample.csv");
        var content = "﻿" + Files.readString(resource.getFile().toPath());
        var file = Files.createTempFile("xtb-bom-", ".csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = new XTBCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file.toFile())
        {
            @Override
            public String getName()
            {
                return "EUR_account_sample.csv";
            }
        }, errors);

        assertThat(errors, empty());
        assertCashOperations(items);
    }

    @Test
    public void testCommaSeparatedFile() throws IOException
    {
        var resource = TestExtractorHelper.loadResourceAsInputFile(getClass(), "EUR_account_sample.csv");
        var content = toCommaSeparated(Files.readString(resource.getFile().toPath()));
        var file = Files.createTempFile("xtb-comma-", ".csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = new XTBCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file.toFile())
        {
            @Override
            public String getName()
            {
                return "EUR_account_sample.csv";
            }
        }, errors);

        assertThat(errors, empty());
        assertCashOperations(items);
    }

    /**
     * Converts the sample to the format of Excel with English regional
     * settings: "," as separator and "." as decimal separator.
     */
    private String toCommaSeparated(String content) throws IOException
    {
        var converted = new StringWriter();
        try (var parser = CSVFormat.DEFAULT.builder().setDelimiter(';').get().parse(new StringReader(content));
                        var printer = new CSVPrinter(converted,
                                        CSVFormat.DEFAULT.builder().setRecordSeparator('\n').get()))
        {
            var lineNumber = 0;
            for (var csvRecord : parser)
            {
                var values = csvRecord.toList();
                // the amount is the fifth column below the header
                if (lineNumber++ > 4)
                    values.set(4, values.get(4).replace(',', '.'));
                printer.printRecord(values);
            }
        }
        return converted.toString();
    }

    @Test
    public void testMalformedFileIsReportedAsError() throws IOException
    {
        var resource = TestExtractorHelper.loadResourceAsInputFile(getClass(), "EUR_account_sample.csv");
        // an unclosed quote makes the CSV parser fail while reading the
        // records
        var content = Files.readString(resource.getFile().toPath()).replace("Deposit;My Trades",
                        "\"Deposit;My Trades");
        var file = Files.createTempFile("xtb-malformed-", ".csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        var client = new Client();
        var errors = new ArrayList<Exception>();
        var items = new XTBCSVExtractor(client).extract(new SecurityCache(client), new InputFile(file.toFile())
        {
            @Override
            public String getName()
            {
                return "EUR_account_sample.csv";
            }
        }, errors);

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), instanceOf(IOException.class));
        assertThat(items, empty());
    }
}
