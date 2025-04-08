package name.abuchen.portfolio.datatransfer.pdf.wealthsimpleInvestmentsInc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.WealthsimpleInvestmentsIncPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class WealthsimpleInvestmentsIncPDFExtractorTest
{
    @Test
    public void testDepotStatement01()
    {
        Client client = new Client();

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(83));
        new AssertImportActions().check(results, "CAD");

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security1.getIsin());
        assertNull(security1.getWkn());
        assertThat(security1.getTickerSymbol(), is("ZFL"));
        assertThat(security1.getName(), is("BMO Long Federal Bond ETF"));
        assertThat(security1.getCurrencyCode(), is("CAD"));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security2.getIsin());
        assertNull(security2.getWkn());
        assertThat(security2.getTickerSymbol(), is("QTIP"));
        assertThat(security2.getName(), is("Mackenzie Financial Corp"));
        assertThat(security2.getCurrencyCode(), is("CAD"));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("ZAG"));
        assertThat(security3.getName(), is("BMO AGGREGATE BOND INDEX ETF"));
        assertThat(security3.getCurrencyCode(), is("CAD"));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security4.getIsin());
        assertNull(security4.getWkn());
        assertThat(security4.getTickerSymbol(), is("XSH"));
        assertThat(security4.getName(), is("iShares Core Canadian ST Corp"));
        assertThat(security4.getCurrencyCode(), is("CAD"));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security5.getIsin());
        assertNull(security5.getWkn());
        assertThat(security5.getTickerSymbol(), is("GLDM"));
        assertThat(security5.getName(), is("World Gold Trust"));
        assertThat(security5.getCurrencyCode(), is("CAD"));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security6.getIsin());
        assertNull(security6.getWkn());
        assertThat(security6.getTickerSymbol(), is("EEMV"));
        assertThat(security6.getName(), is("iShares MSCI Emerg Min Vol ETF"));
        assertThat(security6.getCurrencyCode(), is("CAD"));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security7.getIsin());
        assertNull(security7.getWkn());
        assertThat(security7.getTickerSymbol(), is("ACWV"));
        assertThat(security7.getName(), is("iShares Edge MSCI Min Vol Global ETF"));
        assertThat(security7.getCurrencyCode(), is("CAD"));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security8.getIsin());
        assertNull(security8.getWkn());
        assertThat(security8.getTickerSymbol(), is("VTI"));
        assertThat(security8.getName(), is("Vanguard Total Stock Market ETF"));
        assertThat(security8.getCurrencyCode(), is("CAD"));

        Security security9 = results.stream().filter(SecurityItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security9.getIsin());
        assertNull(security9.getWkn());
        assertThat(security9.getTickerSymbol(), is("XIC"));
        assertThat(security9.getName(), is("iShares Core S&P/TSX Capped Composite Index ETF"));
        assertThat(security9.getCurrencyCode(), is("CAD"));

        Security security10 = results.stream().filter(SecurityItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security10.getIsin());
        assertNull(security10.getWkn());
        assertThat(security10.getTickerSymbol(), is("XEF"));
        assertThat(security10.getName(), is("iShares Core MSCI EAFE IMI Index ETF"));
        assertThat(security10.getCurrencyCode(), is("CAD"));

        // check 1st cancellation (Storno) (without tickerSymbol) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2020-07-10T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(14.0853)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DepotStatement01.txt"));
        assertNull(((Transaction) cancellation.getSubject()).getNote());

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1.36))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1.36))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 2nd cancellation (Storno) (without tickerSymbol) transaction
        cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2020-06-09T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(14.3972)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DepotStatement01.txt"));
        assertNull(((Transaction) cancellation.getSubject()).getNote());

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(0.88))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(0.88))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 3rd cancellation (Storno) (without tickerSymbol) transaction
        cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .skip(2).findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2020-05-11T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(14.3972)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DepotStatement01.txt"));
        assertNull(((Transaction) cancellation.getSubject()).getNote());

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1.27))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1.27))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(41L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.7582)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(35.04))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(35.04))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.6794)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(13.82))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(13.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14.0853)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1521.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1521.49))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50.9493)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1055.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1055.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(88.9806)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1493.09))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1493.09))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(79.9364)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1582.74))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1582.74))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 7th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15.0926)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(376.84))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(376.84))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 8th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-08-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.5764)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(12.19))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(12.19))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 9th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.0702)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(22.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(22.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 10th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.7993)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 11th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.4814)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 12th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(11).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.8204)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 13th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.3852)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 14th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(13).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.7504)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 15th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(14).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9.2542)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 16th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(15).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3119)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 17th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(16).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.9669)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 18th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(17).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.72)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 19th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(18).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.9932)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 20th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(19).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.1164)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 21th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(20).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0232)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(2.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(2.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 22th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(21).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0357)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(2.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(2.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 23th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(22).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.6774)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(27.71))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(27.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 24th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(23).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.1217)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(64.15))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(64.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 25th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(24).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22.6219)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 26th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(25).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8.8192)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 27th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(26).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.591)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 27th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(26).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.591)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 28th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(27).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14.3972)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 29th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(28).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(39.0135)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 30th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(29).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(23.8851)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(498.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(498.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 31th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(30).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(93.8442)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 32th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(31).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(98.7605)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1992.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1992.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 1st dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.3610)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(8.55))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(9.83))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20.8583)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(25.40))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(29.21))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(3.81))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(148.3070)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(79.9364)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 5th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(148.3070)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 6th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(79.9364)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 7th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4.7706)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.29))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 8th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(147.6276)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(6.35))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.35))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 9th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20.4999)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.30))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 10th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(88.9806)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 11th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(96.6783)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.16))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.16))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 12th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(88.9806)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 13th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(96.1019)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.13))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 14th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(18)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(96.5148)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.86))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.86))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 15th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(19)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(105.9986)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 16th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(5.5910)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(5.32))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.12))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.80))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 17th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(39.0135)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 18th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(22)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23.8851)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 19th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(23)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.8424)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(11.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(12.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(1.65))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 20th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(22.6576)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(19.47))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(2.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 21th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(101.8822)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 22th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(26)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(95.5216)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 23th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(27)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(98.7605)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.25))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 24th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(28)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(93.8442)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.75))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check fee transaction
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(32).iterator();
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.42 - 0.26 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.25 - 0.09 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.33 - 0.09 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.15 - 0.08 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.34 - 0.09 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.30 - 0.51 + 0.19))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.46 - 0.49 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.51 - 0.47 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.08 - 0.42 + 0.18))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));
    }

    @Test
    public void testtestDepotStatement01WithAllSecuritiesInUSD()
    {
        Security security6 = new Security("iShares MSCI Emerg Min Vol ETF", CurrencyUnit.USD);
        security6.setTickerSymbol("EEMV");

        Security security7 = new Security("iShares Edge MSCI Min Vol Global ETF", CurrencyUnit.USD);
        security7.setTickerSymbol("ACWV");

        Security security8 = new Security("Vanguard Total Stock Market ETF", CurrencyUnit.USD);
        security8.setTickerSymbol("VTI");

        Client client = new Client();
        client.addSecurity(security6);
        client.addSecurity(security7);
        client.addSecurity(security8);

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode("CAD");

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(80));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security1.getIsin());
        assertNull(security1.getWkn());
        assertThat(security1.getTickerSymbol(), is("ZFL"));
        assertThat(security1.getName(), is("BMO Long Federal Bond ETF"));
        assertThat(security1.getCurrencyCode(), is("CAD"));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security2.getIsin());
        assertNull(security2.getWkn());
        assertThat(security2.getTickerSymbol(), is("QTIP"));
        assertThat(security2.getName(), is("Mackenzie Financial Corp"));
        assertThat(security2.getCurrencyCode(), is("CAD"));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("ZAG"));
        assertThat(security3.getName(), is("BMO AGGREGATE BOND INDEX ETF"));
        assertThat(security3.getCurrencyCode(), is("CAD"));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security4.getIsin());
        assertNull(security4.getWkn());
        assertThat(security4.getTickerSymbol(), is("XSH"));
        assertThat(security4.getName(), is("iShares Core Canadian ST Corp"));
        assertThat(security4.getCurrencyCode(), is("CAD"));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security5.getIsin());
        assertNull(security5.getWkn());
        assertThat(security5.getTickerSymbol(), is("GLDM"));
        assertThat(security5.getName(), is("World Gold Trust"));
        assertThat(security5.getCurrencyCode(), is("CAD"));

        Security security9 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security9.getIsin());
        assertNull(security9.getWkn());
        assertThat(security9.getTickerSymbol(), is("XIC"));
        assertThat(security9.getName(), is("iShares Core S&P/TSX Capped Composite Index ETF"));
        assertThat(security9.getCurrencyCode(), is("CAD"));

        Security security10 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security10.getIsin());
        assertNull(security10.getWkn());
        assertThat(security10.getTickerSymbol(), is("XEF"));
        assertThat(security10.getName(), is("iShares Core MSCI EAFE IMI Index ETF"));
        assertThat(security10.getCurrencyCode(), is("CAD"));

        // check 1st cancellation without ticker symbol transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));
        assertThat(cancellation.getSource(), is("DepotStatement01.txt"));

        // check 2nd cancellation without ticker symbol transaction
        cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));
        assertThat(cancellation.getSource(), is("DepotStatement01.txt"));

        // check 3rd cancellation without ticker symbol transaction
        cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .skip(2).findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));
        assertThat(cancellation.getSource(), is("DepotStatement01.txt"));

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(41L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.7582)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(35.04))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(35.04))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.6794)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(13.82))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(13.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14.0853)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1521.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1521.49))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50.9493)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1055.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1055.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(88.9806)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1493.09))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1493.09))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(79.9364)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1582.74))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1582.74))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 7th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15.0926)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(376.84))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(376.84))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 8th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-08-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.5764)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(12.19))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(12.19))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 9th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-03T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.0702)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(22.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(22.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 10th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.7993)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 11th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.4814)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 12th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(11).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.8204)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 13th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.3852)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 14th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(13).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.7504)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 15th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(14).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9.2542)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 16th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(15).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3119)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 17th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(16).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.9669)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 18th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(17).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.72)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 19th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(18).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.9932)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 20th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(19).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.1164)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 21th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(20).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0232)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(2.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(2.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 22th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(21).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0357)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(2.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(2.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 23th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(22).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.6774)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(27.71))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(27.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 24th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(23).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.1217)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(64.15))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(64.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 25th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(24).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22.6219)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 26th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(25).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8.8192)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 27th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(26).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.591)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 27th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(26).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.591)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 28th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(27).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14.3972)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 29th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(28).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(39.0135)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(996.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 30th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(29).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(23.8851)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(498.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(498.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 31th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(30).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(93.8442)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1494.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 32th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(31).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(98.7605)));
        assertThat(entry.getSource(), is("DepotStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1992.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1992.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 1st dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.3610)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(8.55))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(9.83))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7.60))));

        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20.8583)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(25.40))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(29.21))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(3.81))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(22.60))));

        s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(148.3070)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(79.9364)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 5th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(148.3070)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 6th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(79.9364)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 7th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4.7706)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.29))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.70))));

        s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 8th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(147.6276)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(6.35))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.35))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 9th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20.4999)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.30))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 10th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(88.9806)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 11th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(96.6783)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.16))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.16))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 12th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(88.9806)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 13th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(96.1019)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.13))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 14th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(18)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(96.5148)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.86))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.86))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 15th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(19)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(105.9986)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.56))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 16th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(5.5910)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(5.32))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(6.12))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.80))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.50))));

        s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 17th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(39.0135)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 18th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(22)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23.8851)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 19th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(23)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.8424)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(11.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(12.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(1.65))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.36))));

        s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 20th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(22.6576)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(19.47))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(2.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(14.38))));

        s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 21th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(101.8822)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 22th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(26)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(95.5216)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 23th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(27)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(98.7605)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.25))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 24th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(28)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(93.8442)));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.75))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check fee transaction
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(32).iterator();
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.42 - 0.26 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.25 - 0.09 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.33 - 0.09 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.15 - 0.08 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.34 - 0.09 + 0.21))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.30 - 0.51 + 0.19))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.46 - 0.49 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.51 - 0.47 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.08 - 0.42 + 0.18))));
        assertThat(transaction.getSource(), is("DepotStatement01.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));
    }

    @Test
    public void testDepotStatement02()
    {
        Client client = new Client();

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(28));
        new AssertImportActions().check(results, "CAD");

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security1.getIsin());
        assertNull(security1.getWkn());
        assertThat(security1.getTickerSymbol(), is("EEMV"));
        assertThat(security1.getName(), is("iShares MSCI Emerg Min Vol ETF"));
        assertThat(security1.getCurrencyCode(), is("CAD"));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security2.getIsin());
        assertNull(security2.getWkn());
        assertThat(security2.getTickerSymbol(), is("ACWV"));
        assertThat(security2.getName(), is("iShares Edge MSCI Min Vol Global ETF"));
        assertThat(security2.getCurrencyCode(), is("CAD"));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("VTI"));
        assertThat(security3.getName(), is("Vanguard Total Stock Market ETF"));
        assertThat(security3.getCurrencyCode(), is("CAD"));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security4.getIsin());
        assertNull(security4.getWkn());
        assertThat(security4.getTickerSymbol(), is("XIC"));
        assertThat(security4.getName(), is("iShares Core S&P/TSX Capped Composite Index ETF"));
        assertThat(security4.getCurrencyCode(), is("CAD"));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security5.getIsin());
        assertNull(security5.getWkn());
        assertThat(security5.getTickerSymbol(), is("XEF"));
        assertThat(security5.getName(), is("iShares Core MSCI EAFE IMI Index ETF"));
        assertThat(security5.getCurrencyCode(), is("CAD"));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security6.getIsin());
        assertNull(security6.getWkn());
        assertThat(security6.getTickerSymbol(), is("ZAG"));
        assertThat(security6.getName(), is("BMO AGGREGATE BOND INDEX ETF"));
        assertThat(security6.getCurrencyCode(), is("CAD"));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security7.getIsin());
        assertNull(security7.getWkn());
        assertThat(security7.getTickerSymbol(), is("QTIP"));
        assertThat(security7.getName(), is("Mackenzie Financial Corp"));
        assertThat(security7.getCurrencyCode(), is("CAD"));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security8.getIsin());
        assertNull(security8.getWkn());
        assertThat(security8.getTickerSymbol(), is("ZFL"));
        assertThat(security8.getName(), is("BMO Long Federal Bond ETF"));
        assertThat(security8.getCurrencyCode(), is("CAD"));

        // check 1st cancellation without ticker symbol transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));
        assertThat(cancellation.getSource(), is("DepotStatement02.txt"));

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(9L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.7993)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 2th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.4814)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 3th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.8204)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.3852)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.7504)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9.2542)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 7th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3119)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 8th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.9669)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 9th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.72)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 10th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.9932)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 11th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.1164)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 1th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(39.0135)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23.8851)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.8424)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(11.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(12.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(1.65))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(22.6576)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(19.47))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(2.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 5th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(101.8822)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 6th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(95.5216)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check fee transaction
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(8).iterator();
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.46 - 0.49 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));
    }

    @Test
    public void testtestDepotStatement02WithAllSecuritiesInUSD()
    {
        Security security1 = new Security("iShares MSCI Emerg Min Vol ETF", CurrencyUnit.USD);
        security1.setTickerSymbol("EEMV");

        Security security2 = new Security("iShares Edge MSCI Min Vol Global ETF", CurrencyUnit.USD);
        security2.setTickerSymbol("ACWV");

        Client client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode("CAD");

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(26));

        // check security
        Security security3 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("VTI"));
        assertThat(security3.getName(), is("Vanguard Total Stock Market ETF"));
        assertThat(security3.getCurrencyCode(), is("CAD"));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security4.getIsin());
        assertNull(security4.getWkn());
        assertThat(security4.getTickerSymbol(), is("XIC"));
        assertThat(security4.getName(), is("iShares Core S&P/TSX Capped Composite Index ETF"));
        assertThat(security4.getCurrencyCode(), is("CAD"));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security5.getIsin());
        assertNull(security5.getWkn());
        assertThat(security5.getTickerSymbol(), is("XEF"));
        assertThat(security5.getName(), is("iShares Core MSCI EAFE IMI Index ETF"));
        assertThat(security5.getCurrencyCode(), is("CAD"));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security6.getIsin());
        assertNull(security6.getWkn());
        assertThat(security6.getTickerSymbol(), is("ZAG"));
        assertThat(security6.getName(), is("BMO AGGREGATE BOND INDEX ETF"));
        assertThat(security6.getCurrencyCode(), is("CAD"));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security7.getIsin());
        assertNull(security7.getWkn());
        assertThat(security7.getTickerSymbol(), is("QTIP"));
        assertThat(security7.getName(), is("Mackenzie Financial Corp"));
        assertThat(security7.getCurrencyCode(), is("CAD"));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security8.getIsin());
        assertNull(security8.getWkn());
        assertThat(security8.getTickerSymbol(), is("ZFL"));
        assertThat(security8.getName(), is("BMO Long Federal Bond ETF"));
        assertThat(security8.getCurrencyCode(), is("CAD"));

        // check 1st cancellation without ticker symbol transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)")));
        assertThat(cancellation.getSource(), is("DepotStatement02.txt"));

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(9L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.7993)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(128.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 2th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.4814)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(57.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 3th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.8204)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(172.91))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.3852)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(82.43))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.7504)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(138.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9.2542)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(154.73))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 7th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3119)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(33.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 8th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10.9669)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(228.77))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 9th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.72)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(28.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 10th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.9932)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(16.39))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 11th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.1164)));
        assertThat(entry.getSource(), is("DepotStatement02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(84.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 1th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(39.0135)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(13.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23.8851)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(5.23))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.8424)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(11.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(12.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(1.65))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.36))));

        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(22.6576)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(16.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(19.47))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(2.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(14.38))));

        s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // check 5th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(101.8822)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(4.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check 6th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(95.5216)));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(3.82))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

        // check fee transaction
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(8).iterator();
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(4.46 - 0.49 + 0.20))));
        assertThat(transaction.getSource(), is("DepotStatement02.txt"));
        assertThat(transaction.getNote(), is("Management fee to Wealthsimple"));
    }
}
