package name.abuchen.portfolio.datatransfer.pdf.bbvaesbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BBVAesPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class BBVAesPDFExtractorTest
{
    @Test
    public void bbvaCompra()
    {
        BBVAesPDFExtractor extractor = new BBVAesPDFExtractor(new Client());
        
        List<Exception> errors = new ArrayList<>();
        
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "bbva-compra.txt"), errors);
        
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        // check buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4581401001"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.INTEL CORPORATION -USD-"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-20T16:50:47"), hasShares(100), //
                        hasSource("bbva-compra.txt"), //
                        hasAmount("EUR", 1854.38), // TODO check
                        hasGrossValue("EUR", 1826.25), // TODO Check
                        hasTaxes("EUR", 0.0), //
                        hasFees("EUR", 28.13))));
    }
    
    @Test
    public void bbvaVenta()
    {
        BBVAesPDFExtractor extractor = new BBVAesPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "bbva-venta.txt"), errors);
        
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0079031078"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.ADVANCED MICRO DEV."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-17T20:18:37"), hasShares(10), //
                        hasSource("bbva-venta.txt"), //
                        hasAmount("EUR", 1081.36), // TODO check
                        hasGrossValue("EUR", 1105.59), // TODO Check
                        hasTaxes("EUR", 0.0), //
                        hasFees("EUR", 24.23))));
    }
    
    @Test
    public void bbvaCompraFondo()
    {
        BBVAesPDFExtractor extractor = new BBVAesPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "bbva-compra-fondo.txt"), errors);
        
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("ES0113925038"), hasWkn(null), hasTicker(null), //
                        hasName("BBVA BOLSA IND. USA CUBIERTO FI"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-02T00:00"), hasShares(451.5550246), //
                        hasSource("bbva-compra-fondo.txt"), //
                        hasAmount("EUR", 15000.00), //
                        hasGrossValue("EUR", 15000.00), //
                        hasTaxes("EUR", 0.0), //
                        hasFees("EUR", 0.0))));
    }
    
    @Test
    public void bbvaVentaFondo()
    {
        BBVAesPDFExtractor extractor = new BBVAesPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "bbva-venta-fondo.txt"), errors);
        
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("ES0113925038"), hasWkn(null), hasTicker(null), //
                        hasName("BBVA BOLSA IND. USA CUBIERTO FI"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-05-28T00:00"), hasShares(483.2315919), //
                        hasSource("bbva-venta-fondo.txt"), //
                        hasAmount("EUR", 16000.00), //
                        hasGrossValue("EUR", 16000.00), //
                        hasTaxes("EUR", 0.0), //
                        hasFees("EUR", 0.00))));
    }
    
    @Test
    public void bbvaDividendos()
    {
        BBVAesPDFExtractor extractor = new BBVAesPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "bbva-dividendos.txt"), errors);
        
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US8299331004"), hasWkn(null), hasTicker(null), //
                        hasName("ACC.SIRIUS XM HOLDINGS INC"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-28T00:00"), hasShares(33), //
                        hasSource("bbva-dividendos.txt"), //
                        hasAmount("EUR", 3.57), //
                        hasGrossValue("EUR", 7.83), //
                        hasTaxes("EUR", 2.44), //
                        hasFees("EUR", 1.82))));
    }
    
}































