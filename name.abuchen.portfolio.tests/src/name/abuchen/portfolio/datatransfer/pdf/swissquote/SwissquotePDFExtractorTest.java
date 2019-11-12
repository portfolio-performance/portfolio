package name.abuchen.portfolio.datatransfer.pdf.swissquote;

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
import name.abuchen.portfolio.datatransfer.pdf.SwissquotePDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SwissquotePDFExtractorTest {
    
    @Test
    public void testKauf01() {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getCurrencyCode(), is("USD"));
        assertThat(security.getName(), is("APPLE ORD"));

        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                is(Money.of("USD", Values.Amount.factorize(2900.6))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                is(Money.of("USD", Values.Amount.factorize(0.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                is(Money.of("USD", Values.Amount.factorize(4.75))));
    }

    @Test
    public void testKauf02() {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("CH0001752309"));
        assertThat(security.getCurrencyCode(), is("CHF"));
        assertThat(security.getName(), is("FISCHER N"));

        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-15T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                is(Money.of("CHF", Values.Amount.factorize(2747.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                is(Money.of("CHF", Values.Amount.factorize(31.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                is(Money.of("CHF", Values.Amount.factorize(2.05))));
    }

    @Test
    public void testKauf03() {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DK0010268606"));
        assertThat(security.getCurrencyCode(), is("DKK"));
        assertThat(security.getName(), is("VESTAS WIND SYSTEMS ORD"));

        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-07-14T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                is(Money.of("CHF", Values.Amount.factorize(5650.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                is(Money.of("CHF", Values.Amount.factorize(39.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                is(Money.of("CHF", Values.Amount.factorize(8.40))));
    }

    @Test
    public void testVerkauf01() {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("CH0363463438"));
        assertThat(security.getCurrencyCode(), is("CHF"));
        assertThat(security.getName(), is("IDORSIA N"));

        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(322)));
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-02-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                is(Money.of("CHF", Values.Amount.factorize(8198.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                is(Money.of("CHF", Values.Amount.factorize(31.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                is(Money.of("CHF", Values.Amount.factorize(6.20))));
    }

    @Test
    public void testDividende01()
    {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US41753F1093"));
        assertThat(security.getName(), is("HARVEST CAPITAL CREDIT ORD"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                is(Money.of("USD", Values.Amount.factorize(19.60))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(350)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-19T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("USD", Values.Amount.factorize(8.40))));
        assertThat(transaction.getGrossValue(), is(Money.of("USD", Values.Amount.factorize(28.00))));
    }

    @Test
    public void testDividende02()
    {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CH0025751329"));
        assertThat(security.getName(), is("LOGITECH N"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                is(Money.of("CHF", Values.Amount.factorize(118.62))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(250)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-18T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(63.88))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(182.50))));
    }
    
    @Test
    public void testCapitalGain()
    {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteKapitalgewinn.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CH0371153492"));
        assertThat(security.getName(), is("Landis+Gyr N"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                is(Money.of("CHF", Values.Amount.factorize(107.10))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(34)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-27T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(107.10))));
    }

    @Test
    public void testGebuehr()
    {
        Client client = new Client();

        SwissquotePDFExtractor extractor = new SwissquotePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SwissquoteGebuehr.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getMonetaryAmount(),
                is(Money.of("CHF", Values.Amount.factorize(28.55))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-30T00:00")));
    }
}