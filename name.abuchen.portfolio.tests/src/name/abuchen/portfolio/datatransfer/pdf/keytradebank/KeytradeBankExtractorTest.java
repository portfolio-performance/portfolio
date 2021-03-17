package name.abuchen.portfolio.datatransfer.pdf.keytradebank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.pdf.KeytradeBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class KeytradeBankExtractorTest
{

    @Test
    public void testWertpapierKauf01()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU1781541179"));
        assertThat(security.getName(), is("LYXOR CORE WORLD"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry t = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(t.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(t.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-15T12:31:50")));
        assertThat(t.getPortfolioTransaction().getShares(), is(Values.Share.factorize(168)));
        assertThat(t.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1994.39)));
        assertThat(t.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), 
                        is(Money.of("EUR", Values.Amount.factorize(14.95))));
    }
}
