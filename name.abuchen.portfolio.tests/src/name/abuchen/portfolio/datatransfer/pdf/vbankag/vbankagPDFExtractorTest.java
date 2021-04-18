package name.abuchen.portfolio.datatransfer.pdf.vbankag;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
import name.abuchen.portfolio.datatransfer.pdf.VBankAGPDFExtractor;
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
public class vbankagPDFExtractorTest
{
    
    @Test
    public void testWertpapierKauf01()
    {
        VBankAGPDFExtractor extractor = new VBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0S9GB0"));
        assertThat(security.getWkn(), is("A0S9GB"));
        assertThat(security.getName(), is("Deut. Börse Commodities GmbH Xetra-Gold IHS 2007(09/Und)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-08T09:36:23")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11116.97))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11071.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(45.17))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        VBankAGPDFExtractor extractor = new VBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0070176184"));
        assertThat(security.getWkn(), is("986333"));
        assertThat(security.getName(), is("Alger-Alger Amer.Ass.Gwth Fd Bearer Shares A o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-03-10T14:21:25")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14480.11))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14450.11))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        VBankAGPDFExtractor extractor = new VBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007472060"));
        assertThat(security.getWkn(), is("747206"));
        assertThat(security.getName(), is("Wirecard AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-15T12:37:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(180)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26199.03))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26649.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(407.31))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(42.66))));
    }

    @Test
    public void testWertpapierDividende01()
    {
        VBankAGPDFExtractor extractor = new VBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0834815366"));
        assertThat(security.getWkn(), is("A1J4YZ"));
        assertThat(security.getName(), is("OptoFlex Inhaber-Ant. P o.N."));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(48.54))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(65.92))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.38))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
}
