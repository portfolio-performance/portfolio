package name.abuchen.portfolio.datatransfer.pdf.bankslm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
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
public class BankSLMPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getWkn(), is("472672"));
        assertThat(security.getName(), is("Nokia Corp Inhaber-Aktien"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-09-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(17000)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(92658.45))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(92031.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(138.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(489.15))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getWkn(), is("135186"));
        assertThat(security.getName(), is("Bank SLM AG Namen-Aktien nom CHF 100.00"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-06-21T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1481.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1480.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(1.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getWkn(), is("24476758"));
        assertThat(security.getName(), is("UBS Group AG Namen-Aktien nom CHF 0.10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-02-10T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3000)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(43412.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(43200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(32.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(3.50 + 176.20))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getWkn(), is("2489948"));
        assertThat(security.getName(), is("UBS AG Namen-Aktien nom CHF 0.10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-08-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7798)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(142359.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(142859.35))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(107.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(3.50 + 389.30))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getWkn(), is("472672"));
        assertThat(security.getName(), is("Nokia Corp Inhaber-Aktien"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-24T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11500)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(43180.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(43509.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(65.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(264.30))));
    }

    @Test
    public void testDividende01()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0001351862"));
        assertThat(security.getWkn(), is("135186"));
        assertThat(security.getName(), is("Bank SLM AG Namen-Aktien nom CHF 100.00"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(18.20))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of("CHF", Values.Amount.factorize(28.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of("CHF", Values.Amount.factorize(9.80))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        BankSLMPDFExtractor extractor = new BankSLMPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FI0009000681"));
        assertThat(security.getWkn(), is("472672"));
        assertThat(security.getName(), is("Nokia Corp Inhaber-Aktien"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-07-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(17000)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3094.00))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4420.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(884.00 + 442.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
}
