package name.abuchen.portfolio.datatransfer.csv;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParsePosition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumMapFormat;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class CSVImporterTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testEnumConstantNameIsAccepted()
    {
        var format = new EnumMapFormat<>(PortfolioTransaction.Type.class);

        assertThat(format.parseObject("TRANSFER_IN", new ParsePosition(0)), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(format.parseObject(PortfolioTransaction.Type.SELL.toString(), new ParsePosition(0)),
                        is(PortfolioTransaction.Type.SELL));
    }

    @Test
    public void testRowShorterThanHeaderIsRead() throws IOException
    {
        var file = folder.newFile("test.csv");
        Files.writeString(file.toPath(),
                        Messages.CSVColumn_Date + ";" + Messages.CSVColumn_Value + "\n2026-08-24\n2026-08-25;100.00\n");

        var importer = new CSVImporter(new Client(), file);
        importer.setDelimiter(';');
        importer.setEncoding(StandardCharsets.UTF_8);
        importer.processFile(true);

        assertThat(importer.getNonEmptyValues(importer.getColumns()[1]).findFirst().orElse(null), is("100.00"));
    }
}
