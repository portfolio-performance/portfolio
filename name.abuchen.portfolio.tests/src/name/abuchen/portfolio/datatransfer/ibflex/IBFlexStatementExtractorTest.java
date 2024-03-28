package name.abuchen.portfolio.datatransfer.ibflex;

import java.nio.file.Files;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorTest
{
    private Extractor.InputFile createTempFile(InputStream input) throws IOException
    {
        File tempFile = Files.createTempFile("IBFlexStatementExtractorTest", null).toFile();
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);

        IOUtils.copy(input, fos);
        return new Extractor.InputFile(tempFile);
    }

    @Test
    public void testThatExceptionIsAddedForNonFlexStatementDocuments() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream otherFile = getClass().getResourceAsStream("../pdf/comdirect/Dividende05.txt");
        Extractor.InputFile tempFile = createTempFile(otherFile);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        assertThat(results.isEmpty(), is(true));
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testIBFlexStatementFile01() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile01.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(5));
        assertThat(buySellTransactions.size(), is(15));
        assertThat(accountTransactions.size(), is(11));
        assertThat(results.size(), is(31));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("CA38501D2041"));
        assertThat(security1.getWkn(), is("80845553"));
        assertThat(security1.getTickerSymbol(), is("GCM.TO"));
        assertThat(security1.getName(), is("GRAN COLOMBIA GOLD CORP"));
        assertThat(security1.getCurrencyCode(), is("CAD"));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("CA91701P1053"));
        assertThat(security2.getWkn(), is("44924734"));
        assertThat(security2.getTickerSymbol(), is("UUU.TO"));
        assertThat(security2.getName(), is("URANIUM ONE INC."));
        assertThat(security2.getCurrencyCode(), is("CAD"));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is(""));
        assertThat(security3.getWkn(), is("277684800"));
        assertThat(security3.getTickerSymbol(), is("FB180921C00200000"));
        assertThat(security3.getName(), is("FB 21SEP18 200.0 C"));
        assertThat(security3.getCurrencyCode(), is("CAD"));
        assertThat(security3.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("CA91701P1TN3"));
        assertThat(security4.getWkn(), is("123720813"));
        assertThat(security4.getTickerSymbol(), is("UUU.TEN2"));
        assertThat(security4.getName(), is("UUU(CA91701P1053) TENDERED TO CA91701P1TN3 1 FOR 1 (UUU.TEN2, URANIUM ONE INC. - TENDER FOR CASH CAD, CA91701P1TN3)"));
        assertThat(security4.getCurrencyCode(), is("CAD"));
        assertThat(security4.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getIsin(), is("CA38501D5010"));
        assertThat(security5.getWkn(), is("129258970"));
        assertThat(security5.getTickerSymbol(), is("GCM"));
        assertThat(security5.getName(), is("GCM(CA38501D2041) SPLIT 1 FOR 25 (GCM, GRAN COLOMBIA GOLD CORP, CA38501D5010)"));
        assertThat(security5.getCurrencyCode(), is("CAD"));
        assertThat(security5.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(11L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-01-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("VIRTX/EBS (NP,L1) FOR DEC 2012"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-04-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(9.49))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("BALANCE OF MONTHLY MINIMUM FEE FOR MAR 2013"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-02-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(1.23))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("CAD DEBIT INT FOR JAN-2013"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-02-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(15.17))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("CAD DEBIT INT FOR JAN-2013"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-09-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.07))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("GCM.TO CASH DIVIDEND USD 0.6900000000 - US TAX"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.18))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("NP SECURITIES AND FUTURES VALUE BUNDLE FOR APR 2017"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.18))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("NP SECURITIES AND FUTURES VALUE BUNDLE FOR APR 2017"));

        // check delivery outbound (Auslieferung) transaction
        PortfolioTransaction transaction1 = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(7).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction1.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(transaction1.getDateTime(), is(LocalDateTime.parse("2013-03-05T00:00")));
        assertThat(transaction1.getShares(), is(Values.Share.factorize(12000)));
        assertNull(transaction1.getSource());
        assertThat(transaction1.getNote(), is("UUU(CA91701P1053) TENDERED TO CA91701P1TN3 1 FOR 1 (UUU, URANIUM ONE INC., CA91701P1053)"));

        assertThat(transaction1.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check delivery inbound (Einlieferung) transaction
        transaction1 = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(8).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction1.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(transaction1.getDateTime(), is(LocalDateTime.parse("2013-03-05T00:00")));
        assertThat(transaction1.getShares(), is(Values.Share.factorize(12000)));
        assertNull(transaction1.getSource());
        assertThat(transaction1.getNote(), is("UUU(CA91701P1053) TENDERED TO CA91701P1TN3 1 FOR 1 (UUU.TEN2, URANIUM ONE INC. - TENDER FOR CASH CAD, CA91701P1TN3)"));

        assertThat(transaction1.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check delivery inbound (Einlieferung) transaction
        transaction1 = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(9).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction1.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(transaction1.getDateTime(), is(LocalDateTime.parse("2013-06-18T00:00")));
        assertThat(transaction1.getShares(), is(Values.Share.factorize(480)));
        assertNull(transaction1.getSource());
        assertThat(transaction1.getNote(), is("GCM(CA38501D2041) SPLIT 1 FOR 25 (GCM, GRAN COLOMBIA GOLD CORP, CA38501D5010)"));

        assertThat(transaction1.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check delivery outbound (Auslieferung) transaction
        transaction1 = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(10).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction1.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(transaction1.getDateTime(), is(LocalDateTime.parse("2013-06-18T00:00")));
        assertThat(transaction1.getShares(), is(Values.Share.factorize(12000)));
        assertNull(transaction1.getSource());
        assertThat(transaction1.getNote(), is("GCM(CA38501D2041) SPLIT 1 FOR 25 (GCM.OLD, GRAN COLOMBIA GOLD CORP, CA38501D2041)"));

        assertThat(transaction1.getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction1.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-04-01T09:34:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5000)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 855937427 | Transaction-ID: 3452745495"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1356.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1350.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(6.75))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(0.27))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-04-01T09:34:13")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6000)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 855937873 | Transaction-ID: 3452746284"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1628.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1620.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(8.10))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(0.27))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-04-01T09:34:14")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 855938012 | Transaction-ID: 3452746368"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(271.35))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(270.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.35))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(0.27))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:12:36")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813598116 | Transaction-ID: 3277427053"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:12:48")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813598306 | Transaction-ID: 3277427256"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(464.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(462.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(2.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:13:51")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813599636 | Transaction-ID: 3277432110"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 7th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:14:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813599932 | Transaction-ID: 3277432289"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 8th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:14:17")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813600188 | Transaction-ID: 3277432430"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 9th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:14:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813600464 | Transaction-ID: 3277432563"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 10th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:14:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813600662 | Transaction-ID: 3277435157"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 11th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:14:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813600885 | Transaction-ID: 3277435310"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(232.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(231.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.31))));

        // check 12th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(11).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-03T09:32:48")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 813803887 | Transaction-ID: 3279084518"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(2574.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(2563.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(11.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.33))));

        // check 13th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-02-19T15:36:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10000)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 836265238 | Transaction-ID: 3370960179"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(27300.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(27200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.72))));

        // check 14th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(13).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-05-11T11:42:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 2107403097 | Transaction-ID: 9004794431"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(1390.90))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(1390.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.90))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(6.95))));

        // check 15th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(14).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-10-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(12000)));
        assertNull(entry.getSource());
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(34320.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(34320.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(2.86))));
    }

    @Test
    public void testIBFlexStatementFile02() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile02.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(8));
        assertThat(buySellTransactions.size(), is(9));
        assertThat(accountTransactions.size(), is(3));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is(""));
        assertThat(security1.getWkn(), is("272800"));
        assertThat(security1.getTickerSymbol(), is("ORCL"));
        assertThat(security1.getName(), is("ORACLE CORP"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is(""));
        assertThat(security2.getWkn(), is("268828466"));
        assertThat(security2.getTickerSymbol(), is("ORCL170915P00050000"));
        assertThat(security2.getName(), is("ORCL 15SEP17 50.0 P"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is(""));
        assertThat(security3.getWkn(), is("286599259"));
        assertThat(security3.getTickerSymbol(), is("ORCL171117C00050000"));
        assertThat(security3.getName(), is("ORCL 17NOV17 50.0 C"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security3.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is(""));
        assertThat(security4.getWkn(), is("311191362"));
        assertThat(security4.getTickerSymbol(), is("PAYC181116C00120000"));
        assertThat(security4.getName(), is("PAYC 16NOV18 120.0 C"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security4.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getIsin(), is("DE0005190003"));
        assertThat(security5.getWkn(), is("14094"));
        assertThat(security5.getTickerSymbol(), is("BMW.DE"));
        assertThat(security5.getName(), is("BAYERISCHE MOTOREN WERKE AG"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security5.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getIsin(), is("DE0005140008"));
        assertThat(security6.getWkn(), is("14121"));
        assertThat(security6.getTickerSymbol(), is("DBK.DE"));
        assertThat(security6.getName(), is("DEUTSCHE BANK AG-REGISTERED"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security6.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getIsin(), is("DE000A0EQ578"));
        assertThat(security7.getWkn(), is("43669257"));
        assertThat(security7.getTickerSymbol(), is("H5E.DE"));
        assertThat(security7.getName(), is("HELMA EIGENHEIMBAU AG"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security7.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security8.getIsin(), is("DE000BASF111"));
        assertThat(security8.getWkn(), is("77680640"));
        assertThat(security8.getTickerSymbol(), is("BAS"));
        assertThat(security8.getName(), is("BASF SE"));
        assertThat(security8.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security8.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-15T16:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1908991475"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4185.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4183.38))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.67))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(41.8338))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5000.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-07T14:50:36")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1902533101"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(41.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.91))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.4408))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(53.00))));

        // check 3th buy sell (Amount = 0,00) transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-15T16:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1908991474"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.00))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check cancellation (Storno) 3rd buy sell transaction
        BuySellEntry cancellation = (BuySellEntry) results.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(item -> item.getFailureMessage() != null) //
                        .findFirst().orElseThrow(IllegalArgumentException::new) //
                        .getSubject();

        assertThat(cancellation, is(not(nullValue())));

        assertThat(cancellation.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(cancellation.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(cancellation, is(not(nullValue())));

        assertThat(cancellation.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-15T16:20")));
        assertThat(cancellation.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(cancellation.getSource());
        assertThat(cancellation.getNote(), is("Trade-ID: 1908991474"));

        assertThat(cancellation.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(cancellation.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(cancellation.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(cancellation.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(cancellation.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.00))));

        grossValueUnit = cancellation.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-19T14:30:51")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1910911677"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(42.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(45.86))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.92))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.4586))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(55.01))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-05-11T11:45:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 2107408815 | Transaction-ID: 9004815263"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(578.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(577.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.54))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(5.7778))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(690.64))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-11-06T03:35:13")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(141)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 970510813 | Transaction-ID: 3937447227"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11573.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11557.77))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.18))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(81.97))));

        // check 7th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-08-05T04:34:09")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1622654675 | Transaction-ID: 6698911710"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(115.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.80))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(11.50))));

        // check 8th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-06-07T04:15:27")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1834141392 | Transaction-ID: 7684604106"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3357.72))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3351.92))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.80))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(41.899))));

        // check 9th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-12-08T03:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1452394455 | Transaction-ID: 5956040041"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7188.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7178.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.05))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(71.78))));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("ORCL(US68389X1054) CASH DIVIDEND 0.19000000 USD PER SHARE - US TAX"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.67))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.41))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(19.00))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).skip(1).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.58))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("US SECURITIES SNAPSHOT AND FUTURES VALUE FOR AUG 2017"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.58))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("US SECURITIES SNAPSHOT AND FUTURES VALUE FOR JUL 2017"));
    }

    @Test
    public void testIBFlexStatementFile03() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile03.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(0));
        assertThat(buySellTransactions.size(), is(0));
        assertThat(accountTransactions.size(), is(1));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.02))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: mytransactionidhere | USD IBKR MANAGED SECURITIES (SYEP) INTEREST FOR NOV-2022"));
    }

    @Test
    public void testIBFlexStatementFile04() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile04.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(0));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US0250727031"));
        assertThat(security.getWkn(), is("385086964"));
        assertThat(security.getTickerSymbol(), is("AVDE"));
        assertThat(security.getName(), is("AVANTIS INTERNATIONAL EQUITY"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-25T10:59:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1111111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(775.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(775.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.30))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CHF", Values.Quote.factorize(55.39285714))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(844.95))));
    }

    @Test
    public void testIBFlexStatementFile05() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile05.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(2));
        assertThat(accountTransactions.size(), is(2));
        assertThat(results.size(), is(6));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US88579Y1010"));
        assertThat(security1.getWkn(), is("13098504"));
        assertThat(security1.getTickerSymbol(), is("MMM.DE"));
        assertThat(security1.getName(), is("3M CO."));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US12514G1085"));
        assertThat(security2.getWkn(), is("130432552"));
        assertThat(security2.getTickerSymbol(), is("CDW"));
        assertThat(security2.getName(), is("CDW CORP/DE"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-02-09T11:19:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 2029054512 | Transaction-ID: 8626274484"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1275.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1269.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.80))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(181.35))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-29T12:44:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 3004185992 | Transaction-ID: 13346288726"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2870.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2865.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(114.60))));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-03-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(7)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 8765764573"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.52))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.52))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-10T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 13713058125"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.50))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testIBFlexStatementFile06() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile06.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(2));
        assertThat(accountTransactions.size(), is(2));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US88579Y1010"));
        assertThat(security1.getWkn(), is("13098504"));
        assertThat(security1.getTickerSymbol(), is("MMM.DE"));
        assertThat(security1.getName(), is("3M CO."));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US12514G1085"));
        assertThat(security2.getWkn(), is("130432552"));
        assertThat(security2.getTickerSymbol(), is("CDW"));
        assertThat(security2.getName(), is("CDW CORP/DE"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-02-09T11:19:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 2029054512 | Transaction-ID: 8626274484"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1275.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1269.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.80))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(181.35))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-29T12:44:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 3004185992 | Transaction-ID: 13346288726"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2433.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2429.72))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.24))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(97.1888))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2870.00))));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-03-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(7)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 8765764573"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.74))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.74))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-10T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 13713058125"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.04))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.04))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(9.50))));
    }

    @Test
    public void testIBFlexStatementFile07() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile07.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(0));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US8688612048"));
        assertThat(security.getWkn(), is("27346611"));
        assertThat(security.getTickerSymbol(), is("SGN.DE"));
        assertThat(security.getName(), is("SURGUTNEFTEGAS-SP ADR"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-10-30T09:16:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(120)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 2592275115 | Transaction-ID: 11501005"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(726.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(720.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(6.00))));
    }

    @Test
    public void testIBFlexStatementFile08() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile08.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(0));
        assertThat(accountTransactions.size(), is(2));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US8688612048"));
        assertThat(security.getWkn(), is("27346611"));
        assertThat(security.getTickerSymbol(), is("SGN"));
        assertThat(security.getName(), is("SGN(US8688612048) CASH DIVIDEND USD 0.088051 PER SHARE - RU TAX"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-18T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(120)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 13505500800 | Transaction-ID: 13505500801 | SGN(US8688612048) CASH DIVIDEND USD 0.088051 PER SHARE - RU TAX"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(8.98))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.58))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).skip(1).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        Item item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.67))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 13505500802 | SGN(US8688612048) CASH DIVIDEND USD 0.088051 PER SHARE - FEE"));
    }

    @Test
    public void testIBFlexStatementFile09() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile09.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(2));
        assertThat(accountTransactions.size(), is(0));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("KY30744W1070"));
        assertThat(security.getWkn(), is("517782617"));
        assertThat(security.getTickerSymbol(), is("F1F"));
        assertThat(security.getName(), is("FARFETCH LTD-CLASS A"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-02T06:26:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(450)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 464740365 | Transaction-ID: 1522126816"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2446.84))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2444.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.44))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(5.432))));

        // check cancellation (Storno) transaction
        BuySellEntry cancellation = (BuySellEntry) results.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(item -> item.getFailureMessage() != null) //
                        .findFirst().orElseThrow(IllegalArgumentException::new) //
                        .getSubject();

        assertThat(cancellation, is(not(nullValue())));

        assertThat(cancellation.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(cancellation.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(cancellation, is(not(nullValue())));

        assertThat(cancellation.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-02T06:26:11")));
        assertThat(cancellation.getPortfolioTransaction().getShares(), is(Values.Share.factorize(450)));
        assertNull(cancellation.getSource());
        assertThat(cancellation.getNote(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(cancellation.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2444.40))));
        assertThat(cancellation.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2444.40))));
        assertThat(cancellation.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(cancellation.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(cancellation.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(5.432))));
    }

    @Test
    public void testIBFlexStatementFile10() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile10.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(0));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US91680M1071"));
        assertThat(security.getWkn(), is("460492620"));
        assertThat(security.getTickerSymbol(), is("UPST"));
        assertThat(security.getName(), is("UPSTART HOLDINGS INC"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-21T18:05:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 116815359 | Transaction-ID: 415451625"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.81))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.31))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(13.25))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(140.83))));
    }

    @Test
    public void testIBFlexStatementFile11() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile11.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(0));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("74347B243"));
        assertThat(security.getWkn(), is("317467459"));
        assertThat(security.getTickerSymbol(), is("QID"));
        assertThat(security.getName(), is("PROSHARES ULTRASHORT QQQ"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-12-13T10:36:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));
        assertNull(entry.getSource());
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(344.56))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(344.39))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.17))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(24.59928571))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(350.17))));
    }

    @Test
    public void testIBFlexStatementFile12() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile12.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(4));
        assertThat(accountTransactions.size(), is(11));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("XXXXXCAD"));
        assertThat(security1.getWkn(), is("XXXXXCAD"));
        assertThat(security1.getTickerSymbol(), is("XXXXXCAD.TO"));
        assertThat(security1.getName(), is("XXXXXCAD"));
        assertThat(security1.getCurrencyCode(), is("CAD"));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("XXXXXUSD"));
        assertThat(security2.getWkn(), is("XXXXXUSD"));
        assertThat(security2.getTickerSymbol(), is("XXXXXUSD"));
        assertThat(security2.getName(), is("XXXXXUSD"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-15T15:32:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(400)));
        assertNull(entry.getSource());
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(848.65))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(845.90))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.75))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(2.11475))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(1234.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-02T09:30:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertNull(entry.getSource());
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5203.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5202.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.79))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(26.01075))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5814.00))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-08T15:59:16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(35)));
        assertNull(entry.getSource());
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4093.22))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4091.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.80))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(116.8977143))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4545.50))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-15T14:17:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
        assertNull(entry.getSource());
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1349.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1347.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.79))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(26.951))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1504.50))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(11L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.81))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXUSD"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.99))));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.49))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXUSD"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.54))));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.55))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXUSD"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.97))));

        // check 1st dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXCAD"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26.95))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.71))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.76))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(46.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXCAD"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.24))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(42.75))));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXUSD"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(43.51))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(51.19))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.68))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(57.19))));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.72))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.72))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(26.50))));

        // check transaction
        // get transactions
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(7).iterator();

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.34))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXUSD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.34))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("XXXXXUSD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(999.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.92))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("USD DEBIT INT FOR DEC-2019"));
    }

    @Test
    public void testIBFlexStatementFile14() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile14.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(3));
        assertThat(buySellTransactions.size(), is(6));
        assertThat(accountTransactions.size(), is(2));
        assertThat(results.size(), is(11));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("CH0237935652"));
        assertThat(security1.getWkn(), is("150029461"));
        assertThat(security1.getTickerSymbol(), is("CHSPI.SW"));
        assertThat(security1.getName(), is("ISHARES CORE SPI CH"));
        assertThat(security1.getCurrencyCode(), is("CHF"));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US9229087690"));
        assertThat(security2.getWkn(), is("12340041"));
        assertThat(security2.getTickerSymbol(), is("VTI"));
        assertThat(security2.getName(), is("VANGUARD TOTAL STOCK MKT ETF"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US9219097683"));
        assertThat(security3.getWkn(), is("83512168"));
        assertThat(security3.getTickerSymbol(), is("VXUS"));
        assertThat(security3.getName(), is("VANGUARD TOTAL INTL STOCK"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security3.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-10T07:15:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 5281530886 | Transaction-ID: 23009417807"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(275.74))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(272.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(3.42))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CHF", Values.Quote.factorize(136.16))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-27T04:51:23")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 5312944175 | Transaction-ID: 23147049711"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(280.18))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(276.76))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(3.42))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CHF", Values.Quote.factorize(138.38))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-09T15:22:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 5280765385 | Transaction-ID: 23003106266"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1024.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1024.59))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.35))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(204.918))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-27T11:06:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 5314164096 | Transaction-ID: 23150695041"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1005.57))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1005.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.37))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(201.04))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-09T15:23:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 5280769765 | Transaction-ID: 23003107589"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(278.04))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(277.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.34))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(55.54))));

        // check 6th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-27T11:07:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 5314170175 | Transaction-ID: 23150713945"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(271.24))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(270.89))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.35))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(54.178))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1500.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 22952664670 | CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1500.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 23145951086 | CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));
    }

    @Test
    public void testIBFlexStatementFile15() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile15.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(2));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0090865873"));
        assertThat(security.getWkn(), is("422810461"));
        assertThat(security.getTickerSymbol(), is("009086587"));
        assertThat(security.getName(), is("ABERDEEN STANDARD INV (LU) ABERDEEN STANDARD LIQUIDITY FUND (LUX) - EUR \"A2\"(EUR) ACC"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-12T10:19:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(23.342)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 129722031"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10004.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.95))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(428.412304))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));
    }

    @Test
    public void testIBFlexStatementFile16() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile16.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(1));
        assertThat(buySellTransactions.size(), is(2));
        assertThat(accountTransactions.size(), is(0));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88160R1014"));
        assertThat(security.getWkn(), is("76792991"));
        assertThat(security.getTickerSymbol(), is("TSLA"));
        assertThat(security.getName(), is("TESLA INC"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-24T12:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 0123456789"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(100.33))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.33))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(100.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-20T10:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 1234567890"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(199.70))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.30))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(200.00))));
    }

    @Test
    public void testIBFlexStatementFile17() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile17.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(0));
        assertThat(buySellTransactions.size(), is(0));
        assertThat(accountTransactions.size(), is(6));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(6L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.50))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 457125101 | R******09:NASDAQ (UTP TAPE C) LEVEL 1 FOR MAR 2023"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.50))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 457125105 | R******09:NYSE (CTA TAPE A) LEVEL 1 FOR MAR 2023"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.50))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 457125110 | R******09:OPRA NP L1 FOR MAR 2023"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.30))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 457155534 | Tax-Transaction-ID: 457125101 | r******09:NASDAQ (UTP Tape C) Level 1"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.30))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 457155535 | Tax-Transaction-ID: 457125105 | r******09:NYSE (CTA Tape A) Level 1"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.30))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 457155536 | Tax-Transaction-ID: 457125110 | r******09:OPRA NP L1"));
    }

    @Test
    public void testIBFlexStatementFile18() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile18.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(2));
        assertThat(accountTransactions.size(), is(4));
        assertThat(results.size(), is(8));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US30214U1025"));
        assertThat(security1.getWkn(), is("4730398"));
        assertThat(security1.getTickerSymbol(), is("EXPO"));
        assertThat(security1.getName(), is("EXPONENT INC"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US45168D1046"));
        assertThat(security2.getWkn(), is("270413"));
        assertThat(security2.getTickerSymbol(), is("IDXX"));
        assertThat(security2.getName(), is("IDEXX LABORATORIES INC"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-05-26T14:43:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 383581369 | Transaction-ID: 1160238864"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1978.14))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1977.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(89.87))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-05-26T14:51:14")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 383584912 | Transaction-ID: 1160245912"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1919.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1918.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.USD, Values.Quote.factorize(383.68))));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(22)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1216874520 | Transaction-ID: 1216874521 | EXPO(US30214U1025) CASH DIVIDEND USD 0.24 PER SHARE - US TAX"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.70))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.58))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(22)));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1385409476 | Transaction-ID: 1385409478 | EXPO(US30214U1025) CASH DIVIDEND USD 0.24 PER SHARE - US TAX"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.70))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.58))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).skip(2).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1112107901 | CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20000.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1147954315 | CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));
    }

    @Test
    public void testIBFlexStatementFile19() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile19.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(2));
        assertThat(accountTransactions.size(), is(3));
        assertThat(results.size(), is(7));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000HG51EM7"));
        assertThat(security1.getWkn(), is("580808642"));
        assertThat(security1.getTickerSymbol(), is("HG51EM"));
        assertThat(security1.getName(), is("AZO 17JAN24 2600 C"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security1.getFeed(), is(QuoteFeed.MANUAL));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000MB01YC2"));
        assertThat(security2.getWkn(), is("595368729"));
        assertThat(security2.getTickerSymbol(), is("MB01YC"));
        assertThat(security2.getName(), is("TXN 19JAN24 200 C"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security2.getFeed(), is(QuoteFeed.MANUAL));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-04T10:15:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(446)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 523119226 | Transaction-ID: 1779250008"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.58))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(994.58))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(2.23))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-03T10:32:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(862)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 522441859 | Transaction-ID: 1775854738"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1031.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1025.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.00))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(1.19))));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15000.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1528984216 | CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000.00))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1777346910 | CASH RECEIPTS / ELECTRONIC FUND TRANSFERS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));
        assertNull(transaction.getSource());
        assertThat(transaction.getNote(), is("Transaction-ID: 1648654096 | EUR CREDIT INT FOR JAN-2023"));
    }

    @Test
    public void testIBFlexStatementFile20() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile20.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(2));
        assertThat(results.size(), is(5));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US04010L1035"));
        assertThat(security1.getWkn(), is("31400554"));
        assertThat(security1.getTickerSymbol(), is("ARCC"));
        assertThat(security1.getName(), is("ARES CAPITAL CORP"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("CA13780R1091"));
        assertThat(security2.getWkn(), is("137828682"));
        assertThat(security2.getTickerSymbol(), is("EIT.UN"));
        assertThat(security2.getName(), is("EIT.UN(CA13780R1091) CASH DIVIDEND CAD 0.10 PER SHARE - CA TAX"));
        assertThat(security2.getCurrencyCode(), is("CAD"));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-01-23T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(157)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 486028469 | Transaction-ID: 1619968617"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2754.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2753.12))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.84))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(17.53579618))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2995.20))));

        // check 1st tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 486028469 | Transaction-ID: 1619968617"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.02))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(28.50))));

        // check 1st tax refund transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 486028469 | Transaction-ID: 1619968617"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.02))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(28.50))));
    }

    @Test
    public void testIBFlexStatementFile21() throws IOException
    {
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(new Client());

        InputStream activityStatement = getClass().getResourceAsStream("testIBFlexStatementFile21.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        List<Item> securityItems = results.stream().filter(SecurityItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance) //
                        .collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList());

        assertThat(errors, empty());
        assertThat(securityItems.size(), is(2));
        assertThat(buySellTransactions.size(), is(1));
        assertThat(accountTransactions.size(), is(1));
        assertThat(results.size(), is(4));

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000PE1MH84"));
        assertThat(security1.getWkn(), is("583904654"));
        assertThat(security1.getTickerSymbol(), is("PE1MH8.SG"));
        assertThat(security1.getName(), is("DAX 15DEC23 10500 0.0 P Capped Reverse Bonus auf den DAX"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security1.getFeed(), is(YahooFinanceQuoteFeed.ID));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US30214U1025"));
        assertThat(security2.getWkn(), is("4730398"));
        assertThat(security2.getTickerSymbol(), is("EXPO"));
        assertThat(security2.getName(), is("EXPO(US30214U1025) CASH DIVIDEND USD 0.24 PER SHARE - US TAX"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(security2.getFeed(), is(YahooFinanceQuoteFeed.ID));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-05-17T07:12:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 543995474 | Transaction-ID: 1872174881"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4807.44))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4800.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.43))));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(48.0001))));

        // check taxes transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertNull(transaction.getSource());
        assertThat(entry.getNote(), is("Trade-ID: 543995474 | Transaction-ID: 1872174881"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.58))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.58))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }
}
