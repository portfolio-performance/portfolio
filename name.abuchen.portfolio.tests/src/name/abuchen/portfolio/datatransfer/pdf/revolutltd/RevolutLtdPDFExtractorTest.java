package name.abuchen.portfolio.datatransfer.pdf.revolutltd;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RevolutLtdPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class RevolutLtdPDFExtractorTest
{
    @Test
    public void testWertpapierVerkauf01()
    {
        RevolutLtdPDFExtractor extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88160R1014"));
        assertThat(security.getTickerSymbol(), is("TSLA"));
        assertThat(security.getName(), is("Tesla"));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.1451261)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertNull(entry.getNote());
        
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1166.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1166.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.02))));
    }

    @Test
    public void testAccountStatement01()
    {
        RevolutLtdPDFExtractor extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(2L));

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-08T00:00")));
            assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(460.85))));
            assertThat(transaction.getSource(), is("AccountStatement01.txt"));
            assertNull(transaction.getNote());
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-15T00:00")));
            assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(204.15))));
            assertThat(transaction.getSource(), is("AccountStatement01.txt"));
            assertNull(transaction.getNote());
        }
    }
}