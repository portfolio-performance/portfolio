package name.abuchen.portfolio.datatransfer.pdf.postbank;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.PostbankPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class PostbankPDFExtractorTest
{
    @Test
    public void testDividende01()
    {
        Client client = new Client();

        PostbankPDFExtractor extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "postbank_dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US4781601046"));
        assertThat(security.getWkn(), is("853260"));
        assertThat(security.getName(), is("JOHNSON & JOHNSON  SHARES REGISTERED SHARES DL 1"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(8.64))));
        assertThat(t.getShares(), is(Values.Share.factorize(12)));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-02-22T00:00")));

        assertThat(t.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(10.17))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(1.53))));
    }
}
