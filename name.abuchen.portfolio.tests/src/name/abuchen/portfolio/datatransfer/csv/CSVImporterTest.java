package name.abuchen.portfolio.datatransfer.csv;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.text.ParsePosition;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumMapFormat;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class CSVImporterTest
{
    @Test
    public void testEnumConstantNameIsAccepted()
    {
        var format = new EnumMapFormat<>(PortfolioTransaction.Type.class);

        assertThat(format.parseObject("TRANSFER_IN", new ParsePosition(0)), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(format.parseObject(PortfolioTransaction.Type.SELL.toString(), new ParsePosition(0)),
                        is(PortfolioTransaction.Type.SELL));
    }
}
