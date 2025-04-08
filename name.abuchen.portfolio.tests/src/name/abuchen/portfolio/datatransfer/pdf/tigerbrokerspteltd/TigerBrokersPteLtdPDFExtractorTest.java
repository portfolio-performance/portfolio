package name.abuchen.portfolio.datatransfer.pdf.tigerbrokerspteltd;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TigerBrokersPteLtdPDFExtractor;
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
public class TigerBrokersPteLtdPDFExtractorTest
{
    @Test
    public void testAccountStatement01()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security1.getIsin());
        assertNull(security1.getWkn());
        assertThat(security1.getTickerSymbol(), is("QQQ"));
        assertThat(security1.getName(), is("Invesco QQQ Trust"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security2.getIsin());
        assertNull(security2.getWkn());
        assertThat(security2.getTickerSymbol(), is("VOO"));
        assertThat(security2.getName(), is("Vanguard S&P 500 ETF"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("VT"));
        assertThat(security3.getName(), is("Vanguard Total World Stock ETF"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-10T01:52:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(48)));
        assertThat(entry.getSource(), is("AccountStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(16072.54))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(16070.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.14 + 0.99 + 1.00 + 0.15))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-14T22:27:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("AccountStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1304.13))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1301.99))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.01 + 0.99 + 1.00 + 0.14))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-10T04:56:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(18)));
        assertThat(entry.getSource(), is("AccountStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7065.33))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7063.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.05 + 0.99 + 1.00 + 0.14))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-14T22:30:09")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getSource(), is("AccountStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2721.63))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2719.48))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.02 + 0.99 + 1.00 + 0.14))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-10T02:02:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(69)));
        assertThat(entry.getSource(), is("AccountStatement01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6681.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6678.99))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.21 + 0.99 + 1.00 + 0.15))));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(68)));
        assertThat(transaction.getSource(), is("AccountStatement01.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(12.43))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(17.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.32))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("AccountStatement01.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(24.04))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(34.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.30))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check Deposits & Withdrawals transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).skip(2).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(30000.00))));
        assertThat(transaction.getSource(), is("AccountStatement01.txt"));
        assertThat(transaction.getNote(), is("DR-3649942"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("AccountStatement01.txt"));
        assertThat(transaction.getNote(), is("DR-3791377"));
    }

    @Test
    public void testAccountStatement02()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security1.getIsin());
        assertNull(security1.getWkn());
        assertThat(security1.getTickerSymbol(), is("VT"));
        assertThat(security1.getName(), is("VANGUARD INTL EQUITY INDEX FUND INC TOTAL WORLD STK INDEX FUND ETF SHS"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security2.getIsin());
        assertNull(security2.getWkn());
        assertThat(security2.getTickerSymbol(), is("VOO"));
        assertThat(security2.getName(), is("Vanguard S&P 500 ETF"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("QQQ"));
        assertThat(security3.getName(), is("Invesco QQQ Trust"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-22T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(69)));
        assertThat(transaction.getSource(), is("AccountStatement02.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(30.82))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(44.03))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(13.21))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("AccountStatement02.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(29.25))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(41.79))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(12.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(52)));
        assertThat(transaction.getSource(), is("AccountStatement02.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(23.86))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(34.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.22))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testAccountStatement03()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("QQQ"));
        assertThat(security.getName(), is("Invesco QQQ Trust"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-01-06T03:33:08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("AccountStatement03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(264.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(262.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.99 + 1.00 + 0.16))));
    }

    @Test
    public void testAccountStatement04()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security1.getIsin());
        assertNull(security1.getWkn());
        assertThat(security1.getTickerSymbol(), is("QQQ"));
        assertThat(security1.getName(), is("Invesco QQQ Trust"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security2.getIsin());
        assertNull(security2.getWkn());
        assertThat(security2.getTickerSymbol(), is("VOO"));
        assertThat(security2.getName(), is("Vanguard S&P 500 ETF"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security3.getIsin());
        assertNull(security3.getWkn());
        assertThat(security3.getTickerSymbol(), is("VT"));
        assertThat(security3.getName(), is("Vanguard Total World Stock ETF"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(68)));
        assertThat(transaction.getSource(), is("AccountStatement04.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(12.43))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(17.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.32))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("AccountStatement04.txt"));
        assertThat(transaction.getNote(), is("Ordinary Dividend"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(24.04))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(34.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.30))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-10T01:52:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(48)));
        assertThat(entry.getSource(), is("AccountStatement04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(16072.54))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(16070.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.14 + 0.99 + 1.00 + 0.15))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-14T22:27:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("AccountStatement04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1304.13))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1301.99))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.01 + 0.99 + 1.00 + 0.14))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-10T04:56:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(18)));
        assertThat(entry.getSource(), is("AccountStatement04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7065.33))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7063.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.05 + 0.99 + 1.00 + 0.14))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-14T22:30:09")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getSource(), is("AccountStatement04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2721.63))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2719.48))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.02 + 0.99 + 1.00 + 0.14))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-10T02:02:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(69)));
        assertThat(entry.getSource(), is("AccountStatement04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6681.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6678.99))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.21 + 0.99 + 1.00 + 0.15))));
    }

    @Test
    public void testAccountStatement05()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VOO"), //
                        hasName("Vanguard S&P 500 ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QQQ"), //
                        hasName("Invesco QQQ Trust"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VT"), //
                        hasName("VANGUARD INTL EQUITY INDEX FUND INC TOTAL WORLD STK INDEX FUND ETF SHS"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-06T03:33:08"), hasShares(1), //
                        hasSource("AccountStatement05.txt"), hasNote(null), //
                        hasAmount("USD", 264.94), hasGrossValue("USD", 262.79), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.99 + 0.16 + 1.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-10-06T00:00"), hasShares(25), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 25.71), hasGrossValue("USD", 36.73), //
                        hasTaxes("USD", 11.02), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-01T00:00"), hasShares(52), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 18.88), hasGrossValue("USD", 26.97), //
                        hasTaxes("USD", 8.09), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-22T00:00"), hasShares(69), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 30.82), hasGrossValue("USD", 44.03), //
                        hasTaxes("USD", 13.21), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-26T00:00"), hasShares(25), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 29.25), hasGrossValue("USD", 41.79), //
                        hasTaxes("USD", 12.54), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-12-30T00:00"), hasShares(52), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 23.86), hasGrossValue("USD", 34.08), //
                        hasTaxes("USD", 10.22), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-23T00:00"), hasShares(68), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 13.78), hasGrossValue("USD", 19.68), //
                        hasTaxes("USD", 5.90), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-30T00:00"), hasShares(25), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 26.02), hasGrossValue("USD", 37.18), //
                        hasTaxes("USD", 11.16), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-02T00:00"), hasShares(53), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 17.52), hasGrossValue("USD", 25.03), //
                        hasTaxes("USD", 7.51), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-23T00:00"), hasShares(69), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 38.15), hasGrossValue("USD", 44.88), //
                        hasTaxes("USD", 6.73), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-05T00:00"), hasShares(25), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 33.49), hasGrossValue("USD", 39.40), //
                        hasTaxes("USD", 5.91), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-08-01T00:00"), hasShares(53), //
                        hasSource("AccountStatement05.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 22.70), hasGrossValue("USD", 26.71), //
                        hasTaxes("USD", 4.01), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement06()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VT"), //
                        hasName("VANGUARD INTL EQUITY INDEX FUND INC TOTAL WORLD STK INDEX FUND ETF SHS"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VOO"), //
                        hasName("Vanguard S&P 500 ETF"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-21T00:00"), hasShares(68), //
                        hasSource("AccountStatement06.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 23.78), hasGrossValue("USD", 27.98), //
                        hasTaxes("USD", 4.20), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-04T00:00"), hasShares(25), //
                        hasSource("AccountStatement06.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 31.71), hasGrossValue("USD", 37.31), //
                        hasTaxes("USD", 5.60), hasFees("USD", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2023-09-21"), hasAmount("USD", 12.29), //
                        hasSource("AccountStatement06.txt"), hasNote(null))));
    }

    @Test
    public void testAccountStatement07()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QQQ"), //
                        hasName("Invesco QQQ Trust-ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VOO"), //
                        hasName("Vanguard S&P 500 ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VT"), //
                        hasName("VANGUARD INTL EQUITY INDEX FUND INC TOTAL WORLD STK INDEX FUND ETF SHS"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-08T23:45:38"), hasShares(1), //
                        hasSource("AccountStatement07.txt"), hasNote(null), //
                        hasAmount("USD", 442.49), hasGrossValue("USD", 440.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.99))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-08T00:00"), hasShares(53), //
                        hasSource("AccountStatement07.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 36.41), hasGrossValue("USD", 42.84), //
                        hasTaxes("USD", 6.43), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-17T00:00"), hasShares(52), //
                        hasSource("AccountStatement07.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 9.73), hasGrossValue("USD", 11.45), //
                        hasTaxes("USD", 1.72), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-03-21T00:00"), hasShares(69), //
                        hasSource("AccountStatement07.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 24.70), hasGrossValue("USD", 29.06), //
                        hasTaxes("USD", 4.36), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-03-28T00:00"), hasShares(25), //
                        hasSource("AccountStatement07.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 32.78), hasGrossValue("USD", 38.57), //
                        hasTaxes("USD", 5.79), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement08()
    {
        TigerBrokersPteLtdPDFExtractor extractor = new TigerBrokersPteLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QQQ"), //
                        hasName("Invesco QQQ Trust"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VOO"), //
                        hasName("Vanguard S&P 500 ETF"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VT"), //
                        hasName("VANGUARD INTL EQUITY INDEX FUND INC TOTAL WORLD STK INDEX FUND ETF SHS"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-06T03:33:08"), hasShares(1), //
                        hasSource("AccountStatement08.txt"), hasNote(null), //
                        hasAmount("USD", 264.94), hasGrossValue("USD", 262.79), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.99 + 0.16 + 1.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-23T00:00"), hasShares(68), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 13.78), hasGrossValue("USD", 19.68), //
                        hasTaxes("USD", 5.90), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-30T00:00"), hasShares(25), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 26.02), hasGrossValue("USD", 37.18), //
                        hasTaxes("USD", 11.16), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-02T00:00"), hasShares(53), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 17.52), hasGrossValue("USD", 25.03), //
                        hasTaxes("USD", 7.51), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-23T00:00"), hasShares(69), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 38.15), hasGrossValue("USD", 44.88), //
                        hasTaxes("USD", 6.73), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-05T00:00"), hasShares(25), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 33.49), hasGrossValue("USD", 39.40), //
                        hasTaxes("USD", 5.91), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-08-01T00:00"), hasShares(53), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 22.70), hasGrossValue("USD", 26.71), //
                        hasTaxes("USD", 4.01), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-21T00:00"), hasShares(68), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 23.78), hasGrossValue("USD", 27.98), //
                        hasTaxes("USD", 4.20), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-04T00:00"), hasShares(25), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 31.71), hasGrossValue("USD", 37.31), //
                        hasTaxes("USD", 5.60), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-01T00:00"), hasShares(53), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 24.12), hasGrossValue("USD", 28.38), //
                        hasTaxes("USD", 4.26), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-22T00:00"), hasShares(69), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 46.97), hasGrossValue("USD", 55.26), //
                        hasTaxes("USD", 8.29), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-28T00:00"), hasShares(25), //
                        hasSource("AccountStatement08.txt"), hasNote("Ordinary Dividend"), //
                        hasAmount("USD", 38.28), hasGrossValue("USD", 45.03), //
                        hasTaxes("USD", 6.75), hasFees("USD", 0.00))));
    }
}
