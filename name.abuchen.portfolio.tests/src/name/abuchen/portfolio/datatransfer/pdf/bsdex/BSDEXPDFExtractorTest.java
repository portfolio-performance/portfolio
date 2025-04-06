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
    public void testCryptoKauf()
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
                        hasAmount("EUR", 99.2), hasGrossValue("EUR", 99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.2))));
    }
    
    @Test
    public void testCryptoKauf2()
    {
        BSDEXPDFExtractor extractor = new BSDEXPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf2.txt"), errors);

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
                        hasDate("2024-04-30T19:16:59"), hasShares(0.0001283), //
                        hasSource("Kauf2.txt"), //
                        hasAmount("EUR", 7.19), hasGrossValue("EUR", 7.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));
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
                        hasAmount("EUR", 37.72), hasGrossValue("EUR", 37.8), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));
    }
}
