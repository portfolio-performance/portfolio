package name.abuchen.portfolio.datatransfer.pdf.postfinance;

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
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.PostfinancePDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class PostfinancePDFExtractorTest
{
    @Test
    public void testKauf01()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("NL0000009355"));
        assertThat(security.getCurrencyCode(), is("EUR"));
        assertThat(security.getName(), is("UNILEVER DUTCH CERT"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-27T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2850.24))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.58))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(4.26))));
    }

    @Test
    public void testKauf02()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("CH0025751329"));
        assertThat(security.getCurrencyCode(), is("CHF"));
        assertThat(security.getName(), is("LOGITECH N"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-10-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2998.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(26.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(2.25))));
    }
    
    @Test
    public void testKauf03()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000PAH0038"));
        assertThat(security.getCurrencyCode(), is("EUR"));
        assertThat(security.getName(), is("PORSCHE AUTOMOBIL HOLDING PRF"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2968.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(4.45))));
        
        Unit gross = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(gross.getAmount(), is(Money.of("CHF", Values.Amount.factorize(2964.05))));
        assertThat(gross.getForex(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2737.40))));
    }
    
    @Test
    public void testVerkauf01()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BD4TYL27"));
        assertThat(security.getCurrencyCode(), is("CHF"));
        assertThat(security.getName(), is("UBSETF MSCI USA hdg to CHF"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-24T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(7467.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(11.20))));
    }
    
    @Test
    public void testDividende01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("NL0000009355"));
        assertThat(security.getName(), is("UNILEVER DUTCH CERT"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(20.93))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(60)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-02T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(3.69))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(24.62))));
    }

    @Test
    public void testDividende02()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CH0032912732"));
        assertThat(security.getName(), is("UBS ETF CH - SLI CHF A"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(36.69))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(34)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-09-06T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(19.75))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(56.44))));
    }
    
    @Test
    public void testKapitalgewinn01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKapitalgewinn01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CH0019396990"));
        assertThat(security.getName(), is("YPSOMED HLDG"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(26.60))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(19)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-07-03T00:00")));
    }
    
    @Test
    public void testJahresgebuhr01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceJahresgebuhr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(90.00))));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-03T00:00")));
    }
}
