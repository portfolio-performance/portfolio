package name.abuchen.portfolio.datatransfer.pdf.bsdex;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.BSDEXPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

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
        assertThat(security.getName(), is("Bitcoin"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));
        
        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-18T04:14:42"), hasShares(0.001), //
                        hasSource("Kauf.txt"), //
                        hasNote("ID: d54eb916-79c9-4eb2-b0ed-041dd43be6cc"), //
                        hasAmount("EUR", 99.2), hasGrossValue("EUR", 99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.2))));
        
//        // check buy sell transaction
//        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
//                      .orElseThrow(IllegalArgumentException::new).getSubject();
//        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
//        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
//
//        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2024-12-18T04:14:42")));
//        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.001)));
//        assertThat(entry.getSource(), is("Kauf.txt"));
//        
//        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(99.2))));
//        assertThat(entry.getPortfolioTransaction().getGrossValue(),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(99))));
//        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
//        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.2))));
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
            System.out.println("Results " + item);
        }
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        // check security
//        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
//                        .orElseThrow(IllegalArgumentException::new).getSecurity();
//        assertThat(security.getTickerSymbol(), is("XRP"));
//        assertThat(security.getName(), is("XRP")); // Or check for a specific name if applicable
//        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XRP"), //
                        hasName("XRP"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ripple"))));
        
        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-17T18:07:05"), hasShares(15), //
                        hasSource("Verkauf.txt"), //
                        hasNote("ID: 750968db-6059-481b-9dd4-f04544597f50"), //
                        hasAmount("EUR", 37.72), hasGrossValue("EUR", 37.8), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));
        
//        // check buy sell transaction
//        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
//                      .orElseThrow(IllegalArgumentException::new).getSubject();
//        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
//        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
//
//        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2024-12-17T18:07:05")));
//        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));
//        assertThat(entry.getSource(), is("Verkauf.txt"));
//        
//        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.72))));
//        assertThat(entry.getPortfolioTransaction().getGrossValue(),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.8))));
//        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
//        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
//            is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
    }
}
