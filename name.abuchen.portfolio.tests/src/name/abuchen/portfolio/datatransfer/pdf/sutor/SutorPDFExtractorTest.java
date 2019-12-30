package name.abuchen.portfolio.datatransfer.pdf.sutor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SutorPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SutorPDFExtractorTest
{

    @Test
    public void testBuyTransactions() throws IOException
    {
        SutorPDFExtractor extractor = new SutorPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "sutor_umsaetze_pdf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertNull(security.getIsin());
        assertThat(security.getName(), is("iShares Core MSCI Emerging Markets"));

        // check buy transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 8_35L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3308)));
    }

}
