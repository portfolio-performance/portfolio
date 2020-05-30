package name.abuchen.portfolio.datatransfer.pdf.targobank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TargobankPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TargobankPDFExtractorTest
{
    public void runWertpapierOrderTest(
                    String testCaseFilename,
                    int numberOfMatchingFiles,
                    String actualShareName,
                    String actualWkn,
                    String actualIsin,
                    Object actualPortfoioTransactionType,
                    Object actualAccoutTransactionType,
                    String actualDateTime,
                    double actualAmount,
                    String actualCurrency,
                    double actualShares
    )
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), testCaseFilename), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(numberOfMatchingFiles));

        Optional<Item> securityItem;
        Optional<Item> entryItem;

        // SecurityItem
        securityItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(securityItem.isPresent(), is(true));
        Security security = ((SecurityItem) securityItem.get()).getSecurity();
        assertThat(security.getName(), is(actualShareName));
        assertThat(security.getWkn(), is(actualWkn));
        assertThat(security.getIsin(), is(actualIsin));

        // BuySellEntryItem
        entryItem = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(entryItem.isPresent(), is(true));
        assertThat(entryItem.get().getSubject(), instanceOf(BuySellEntry.class));
        
        // BuySellEntry...
        BuySellEntry entry = (BuySellEntry) entryItem.get().getSubject();
        // ... has the correct type
        assertThat(entry.getPortfolioTransaction().getType(), is(actualPortfoioTransactionType));
        assertThat(entry.getAccountTransaction().getType(), is(actualAccoutTransactionType));
        // ... has the correct values
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse(actualDateTime)));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(actualAmount)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(actualCurrency, Values.Amount.factorize(0.0))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(actualShares)));
    }

    @Test
    public void testWertpapierKauf01()
    {
        String testCaseFilename = "Kauf (WPX007) TARGO 000000kontonummer am 2020-01-04.txt";
        int numberOfMatchingFiles = 2;
        String actualShareName = "FanCy shaRe. nAmE X0-X0";
        String actualWkn = "ABC123";
        String actualIsin = "DE0000ABC123";
        Object actualPortfolioTransactionType = PortfolioTransaction.Type.BUY;
        Object actualAccountTransactionType = AccountTransaction.Type.BUY;
        String actualDateTime = "2020-01-02T13:01:00";
        double actualAmount = 1008.91;
        String actualCurrency = "EUR";
        double actualShares = 987.654;
        runWertpapierOrderTest(testCaseFilename, numberOfMatchingFiles,
                        actualShareName, actualWkn, actualIsin, 
                        actualPortfolioTransactionType, actualAccountTransactionType,
                        actualDateTime, actualAmount, actualCurrency, actualShares);
    }
    
    @Test
    public void testWertpapierVerkauf01()
    {
        String testCaseFilename = "Verkauf (WPX010) TARGO 000000000101753165 am 2020-01-22.txt";
        int numberOfMatchingFiles = 2;
        String actualShareName = "an0tHer vERy FNcY NaMe";
        String actualWkn = "ZYX987";
        String actualIsin = "LU0000ZYX987";
        Object actualPortfolioTransactionType = PortfolioTransaction.Type.SELL;
        Object actualAccountTransactionType = AccountTransaction.Type.SELL;
        String actualDateTime = "2020-01-10T00:00:00";
        double actualAmount = 1239;
        String actualCurrency = "EUR";
        double actualShares = 10;
        runWertpapierOrderTest(testCaseFilename, numberOfMatchingFiles,
                        actualShareName, actualWkn, actualIsin, 
                        actualPortfolioTransactionType, actualAccountTransactionType,
                        actualDateTime, actualAmount, actualCurrency, actualShares);
    }
}
