package name.abuchen.portfolio.datatransfer.pdf.justTrade;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.JustTradePDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class JustTradePDFExtractorTest
{

    private List<Item> results;

    @Before
    public void loadPDF()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "justtrade_kauf_pdf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
    }

    @Test
    public void testSecurities() throws IOException
    {
        List<Security> securities = results.stream().filter(i -> i instanceof SecurityItem)
                        .map(item -> item.getSecurity()).collect(Collectors.toList());

        assertThat(securities.size(), is(1));

        assertThat(securities.get(0).getName(), is((String) null));
        assertThat(securities.get(0).getIsin(), is("IE00B1FZS350"));
        assertThat(securities.get(0).getWkn(), is("A0LEW8"));
    }

    @Test
    public void testBuyTransactions() throws IOException
    {
        List<BuySellEntry> entries = getTransactionEntries("BUY");

        assertThat(entries.size(), is(1));

        validateTransaction(entries, 0, 1340_64L, "2020-01-02T00:00", 53);
    }

    private List<BuySellEntry> getTransactionEntries(String type)
    {
        return results.stream().filter(i -> i instanceof BuySellEntryItem).map(i -> (BuySellEntryItem) i)
                        .map(i -> (BuySellEntry) i.getSubject())
                        .filter(entry -> entry.getAccountTransaction().getType()
                                        .equals(AccountTransaction.Type.valueOf(type)))
                        .filter(entry -> entry.getPortfolioTransaction().getType()
                                        .equals(PortfolioTransaction.Type.valueOf(type)))
                        .collect(Collectors.toList());

    }

    private void validateTransaction(List<BuySellEntry> entries, int index, long amount, String dateTime, double shares)
    {
        assertThat(entries.get(index).getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, amount)));
        assertThat(entries.get(index).getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse(dateTime)));
        assertThat(entries.get(index).getPortfolioTransaction().getShares(), is(Values.Share.factorize(shares)));
    }
}
