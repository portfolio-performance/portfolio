package name.abuchen.portfolio.datatransfer.csv.exporter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.util.TextUtil;

@SuppressWarnings("nls")
public class CSVExporterTest
{
    @Test
    public void testExportsExDateForAccountTransactions() throws Exception
    {
        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setDateTime(LocalDateTime.parse("2026-04-01T00:00"));
        transaction.setExDate(LocalDateTime.parse("2026-03-30T00:00"));
        transaction.setAmount(123_45);
        transaction.setCurrencyCode("EUR");

        var file = Files.createTempFile("csv-exporter-", ".csv");

        try
        {
            new CSVExporter().exportTransactions(file.toFile(), new ArrayList<>(List.of(transaction)));

            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.size(), is(2));

            var separator = Pattern.quote(String.valueOf(TextUtil.getListSeparatorChar()));
            var header = lines.get(0).split(separator, -1);
            var row = lines.get(1).split(separator, -1);

            var exDateColumn = IntStream.range(0, header.length)
                            .filter(i -> Messages.CSVColumn_ExDate.equals(header[i])).findFirst().orElseThrow();
            assertThat(row[exDateColumn], is("2026-03-30T00:00"));
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testLeavesExDateEmptyForPortfolioTransactions() throws Exception
    {
        var transaction = new PortfolioTransaction();
        transaction.setType(PortfolioTransaction.Type.BUY);
        transaction.setDateTime(LocalDateTime.parse("2026-04-01T00:00"));
        transaction.setAmount(123_45);
        transaction.setCurrencyCode("EUR");

        Path file = Files.createTempFile("csv-exporter-", ".csv");

        try
        {
            new CSVExporter().exportTransactions(file.toFile(), new ArrayList<>(List.of(transaction)));

            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.size(), is(2));

            var separator = Pattern.quote(String.valueOf(TextUtil.getListSeparatorChar()));
            var header = lines.get(0).split(separator, -1);
            var row = lines.get(1).split(separator, -1);

            var exDateColumn = IntStream.range(0, header.length)
                            .filter(i -> Messages.CSVColumn_ExDate.equals(header[i])).findFirst().orElseThrow();
            assertThat(row[exDateColumn], is(""));
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }
}
