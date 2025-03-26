package name.abuchen.portfolio.datatransfer.pdf.bsdex;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.BSDEXPDFExtractor;
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
public class BSDEXPDFExtractorTest
{
    @Test
    public void testWertpapierKauf()
    {
        BSDEXPDFExtractor extractor = new BSDEXPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf.txt"), errors);

        assertThat(errors, empty());
        System.out.println("Extraction errors: " + errors.size());
        for (Exception error : errors)
        {
            System.out.println("Error: " + error.getMessage());
            error.printStackTrace(); // Prints the stack trace for debugging
        }
        assertThat(results.size(), is(2));
        System.out.println("Extracted results: " + results.size());
        for (Item item : results)
        {
            System.out.println(item);
        }
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("BTC"));
        assertThat(security.getName(), is("Bitcoin")); // Or check for a specific name if applicable
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        
        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                      .orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2024-12-18T04:14:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.001)));
        assertThat(entry.getSource(), is("Kauf.txt"));
        
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(99.2))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(99))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.2))));
    }
    
    @Test
    public void testWertpapierVerkauf()
    {
        BSDEXPDFExtractor extractor = new BSDEXPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf.txt"), errors);

        assertThat(errors, empty());
        System.out.println("Extraction errors: " + errors.size());
        for (Exception error : errors)
        {
            System.out.println("Error: " + error.getMessage());
            error.printStackTrace(); // Prints the stack trace for debugging
        }
        assertThat(results.size(), is(2));
        System.out.println("Extracted results: " + results.size());
        for (Item item : results)
        {
            System.out.println(item);
        }
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getTickerSymbol(), is("XRP"));
        assertThat(security.getName(), is("XRP")); // Or check for a specific name if applicable
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        
        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                      .orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2024-12-17T18:07:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));
        assertThat(entry.getSource(), is("Verkauf.txt"));
        
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.72))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.8))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
    }
}
