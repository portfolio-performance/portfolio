package name.abuchen.portfolio.datatransfer.pdf.sutor;

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

    private List<Item> results;

    @Before
    public void loadPDF()
    {
        SutorPDFExtractor extractor = new SutorPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "sutor_umsaetze_pdf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(12));
    }

    @Test
    public void testSecurities() throws IOException
    {
        List<Security> securities = results.stream().filter(i -> i instanceof SecurityItem)
                        .map(item -> item.getSecurity()).collect(Collectors.toList());

        assertThat(securities.size(), is(3));
        securities.forEach(security -> assertNull(security.getIsin()));

        assertThat(securities.get(0).getName(), is("iShares Core MSCI Emerging Markets"));
        assertThat(securities.get(1).getName(), is("Lyxor Core Stoxx Europe 600 acc"));
        assertThat(securities.get(2).getName(), is("Dimensional European Value Fund"));
    }

    @Test
    public void testDeposits() throws IOException
    {
        List<TransactionItem> transactionItems = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (TransactionItem) i).collect(Collectors.toList());

        assertThat(transactionItems.size(), is(4));
        transactionItems.forEach(item -> assertThat(item.getAmount().getCurrencyCode(), is("EUR")));

        // direct deposit
        assertThat(transactionItems.get(0).getAmount().getAmount(), is(16042L));
        // administration fee
        assertThat(transactionItems.get(1).getAmount().getAmount(), is(1983L));
        // partial administration fee
        assertThat(transactionItems.get(2).getAmount().getAmount(), is(212L));
        // account management fees
        assertThat(transactionItems.get(3).getAmount().getAmount(), is(1350L));
    }

    @Test
    public void testBuyTransactions() throws IOException
    {
        List<BuySellEntry> entries = getTransactionEntries("BUY");

        assertThat(entries.size(), is(3));

        validateTransaction(entries, 0, 8_35L, "2019-07-04T00:00", 0.3308);
        validateTransaction(entries, 1, 242_09L, "2019-07-03T00:00", 1.5264);
        validateTransaction(entries, 2, 75_22L, "2019-07-02T00:00", 6.2631);
    }

    @Test
    public void testSellForFeePayments() throws IOException
    {
        List<BuySellEntry> entries = getTransactionEntries("SELL");

        assertThat(entries.size(), is(2));

        validateTransaction(entries, 0, 98L, "2019-07-05T00:00", 0.0387);
        validateTransaction(entries, 1, 4_78L, "2019-07-06T00:00", 0.0299);
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
