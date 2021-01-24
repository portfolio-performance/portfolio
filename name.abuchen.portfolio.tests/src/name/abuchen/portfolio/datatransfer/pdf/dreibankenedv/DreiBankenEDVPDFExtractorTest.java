package name.abuchen.portfolio.datatransfer.pdf.dreibankenedv;

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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DreiBankenEDVPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
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
public class DreiBankenEDVPDFExtractorTest
{

    @Test
    public void testWertpapierKauf01()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0675401409"));
        assertThat(security.getName(), is("Lyxor Emerg Market 2x Lev ETF Inhaber-Anteile I o.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(205.30)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-04T12:05:55")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.02))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVVerkauf01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007664039"));
        assertThat(security.getName(), is("VOLKSWAGEN AG VORZUGSAKTIEN O.ST. O.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(680.30)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-01-02T13:54:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX).getAmount(),
                        is(Values.Amount.factorize(3.37)));
    }

    @Test
    public void testAusschuettung01()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVDividende01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M63284"));
        assertThat(security.getName(), is("iShs Euro.Property Yield U.ETF Registered Shares EUR (Dist)oN"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.30))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-10T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.02))));
    }

    @Test
    public void testAusschuettung02()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVDividende02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(t.getSecurity().getName(), is("GILEAD SCIENCES INC. Registered Shares DL -,001"));
        assertThat(t.getSecurity().getIsin(), is("US3755581036"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.EUR));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-12-14T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(3)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.21))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25 + 0.21))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(t, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testAusschuettung03()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVDividende03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(t.getSecurity().getName(), is("OLD REPUBLIC INTL CORP. Registered Shares DL 1"));
        assertThat(t.getSecurity().getIsin(), is("US6802231042"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.EUR));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(7)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.16))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.58))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(t, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testAusschuettung04()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVDividende04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(t.getSecurity().getName(), is("Main Street Capital Corp. Registered Shares DL -,01"));
        assertThat(t.getSecurity().getIsin(), is("US56035L1044"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.EUR));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(6)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.73))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.28))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(t, account);
        assertThat(s, is(Status.OK_STATUS));
    }
    
    @Test
    public void testAusschuettung05()
    {
        DreiBankenEDVPDFExtractor extractor = new DreiBankenEDVPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "3BankenEDVDividende05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        AccountTransaction t = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (AccountTransaction) ((TransactionItem) i).getSubject()).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(t.getSecurity().getName(), is("Lyxor Emerg Market 2x Lev ETF Inhaber-Anteile I o.N."));
        assertThat(t.getSecurity().getIsin(), is("LU0675401409"));
        assertThat(t.getSecurity().getCurrencyCode(), is(CurrencyUnit.EUR));

        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-12-09T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(6)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.12 + 0.02))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(t, account);
        assertThat(s, is(Status.OK_STATUS));
    }

}
