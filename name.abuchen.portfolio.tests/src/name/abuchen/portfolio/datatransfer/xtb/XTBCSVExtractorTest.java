package name.abuchen.portfolio.datatransfer.xtb;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
        assertCashOperations(items);
    }

    private void assertCashOperations(List<Item> items)
    {
        assertThat(countBuySell(items), is(4L));
        assertThat(countAccountTransactions(items), is(8L));
        assertThat(items, hasItem(deposit(hasAmount("EUR", 100.00))));
        assertThat(items, hasItem(purchase(hasShares(2.00), hasAmount("EUR", 50.25))));
        assertThat(items, hasItem(purchase(hasShares(0.02), hasAmount("EUR", 0.56))));
        assertThat(items, hasItem(sale(hasShares(0.50), hasAmount("EUR", 24.75))));
        assertThat(items, hasItem(sale(hasShares(0.10), hasAmount("EUR", 5.00))));
        assertThat(items, hasItem(removal(hasAmount("EUR", 10.00))));
        assertThat(items, hasItem(dividend(hasAmount("EUR", 0.35))));
        assertThat(items, hasItem(taxes(hasAmount("EUR", 0.05))));
        assertThat(items, hasItem(taxes(hasAmount("EUR", 0.12))));
        assertThat(items, hasItem(interestCharge(hasAmount("EUR", 0.04))));
        assertThat(items, hasItem(interest(hasAmount("EUR", 0.03))));
        assertThat(items, hasItem(interest(hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.07),
                        hasTaxes("EUR", 0.01))));
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
        assertCashOperations(items);
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
        assertThat(items.size(), is(12));
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
}
