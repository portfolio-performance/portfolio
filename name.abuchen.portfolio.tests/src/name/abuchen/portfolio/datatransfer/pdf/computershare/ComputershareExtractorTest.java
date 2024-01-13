package name.abuchen.portfolio.datatransfer.pdf.computershare;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.pdf.ComputersharePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ComputershareExtractorTest
{

    @Test
    public void testDepotAuszugWithPurchase01()
    {
        final ComputersharePDFExtractor extractor = new ComputersharePDFExtractor(new Client());

        final List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        System.out.println(Arrays.toString(results.toArray()));
        assertThat(results.size(), is(10));

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getWkn(), is("023135106"));
        assertThat(security.getTickerSymbol(), is("AMZN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        // check buy transaction
        Extractor.Item item = (Extractor.Item) results.stream()
                        .filter(Extractor.BuySellEntryItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(item.getTypeInformation(), is(AccountTransaction.Type.BUY.toString()));

        assertThat(item.getDate(), is(LocalDateTime.parse("2023-01-31T00:00")));
        assertThat(item.getShares(), is(750000000L));
        assertThat(item.getSource(), is("Depotauszug01.txt"));

        assertThat(item.getAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(756.00))));
    }


}
