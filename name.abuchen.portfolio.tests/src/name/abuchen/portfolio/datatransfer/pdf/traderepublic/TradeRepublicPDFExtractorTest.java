package name.abuchen.portfolio.datatransfer.pdf.traderepublic;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeed;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFeedProperty;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.datatransfer.pdf.TradeRepublicPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class TradeRepublicPDFExtractorTest
{
    TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client())
    {
        @Override
        protected List<SecuritySearchProvider> lookupCryptoProvider()
        {
            return TestCoinSearchProvider.cryptoProvider();
        }
    };

    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-13T12:14")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Order: dead-beef | Ausführung: ab12-c3de"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(25.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(24.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US08862E1091"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Beyond Meat Inc. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-13T13:59")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Order: 1234-abcd | Ausführung: a1b2-3456"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(59.19))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(58.19))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88160R1014"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Tesla Inc. Registered Shares DL-,001"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-06-17T12:27")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Order: fd98-0283 | Ausführung: 51cb-50a8"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(193.08))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(192.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007472060"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Wirecard AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-05T15:16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Order: a1b2-3456 | Ausführung: 1234-56bb"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(485.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(484.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7561091049"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Realty Income Corp. Registered Shares DL 1"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-21T15:57")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Order: xxxx-xxxx | Ausführung: xxxx-xxxx"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1396.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1395.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88579Y1010"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("3M Co. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-02-15T17:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(976.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(975.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BASF SE Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-05-02T21:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertThat(entry.getNote(), is("Order: 01f6-b7cc | Ausführung: a952-e304"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(95.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(94.69))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB0007980591"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BP PLC Registered Shares DL -,25"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-07-14T16:48")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(855.90))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(854.90))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HS15WQ0"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC Trinkaus & Burkhardt GmbH X-TUP O.End DAX/XDAX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-27T16:17"), hasShares(1000.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Order: 39f0-5191 | Ausführung: f728-6a96"), //
                        hasAmount("EUR", 941.00), hasGrossValue("EUR", 940.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HS15WQ0"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC Trinkaus & Burkhardt GmbH X-TUP O.End DAX/XDAX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-27T14:05"), hasShares(1000.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Order: 64b4-82d5 | Ausführung: c4f5-afae"), //
                        hasAmount("EUR", 941.00), hasGrossValue("EUR", 940.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HG0HJ17"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC Trinkaus & Burkhardt GmbH TurboC O.End Chevron"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-12T19:41"), hasShares(1000.00), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Order: c141-b5b3 | Ausführung: 4019-2100"), //
                        hasAmount("EUR", 1611.00), hasGrossValue("EUR", 1610.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-09T00:00"), hasShares(0.09351), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Round Up: 42c2-50a7 | Ausführung: 8f44-fdf5"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1829220216"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi MSCI AC World UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-04T00:00"), hasShares(0.032743), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Saveback: B2C4-n64q | Ausführung: 5437-f7f5"), //
                        hasAmount("EUR", 13.78), hasGrossValue("EUR", 13.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0001141802"), hasWkn(null), hasTicker(null), //
                        hasName("Bundesrep.Deutschland Bundesobl.Ser.180 v.2019(24)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-26T15:53"), hasShares(10.19), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Order: 20f0-0026 | Ausführung: 761d-19e4"), //
                        hasAmount("EUR", 1000.13), hasGrossValue("EUR", 999.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A0E9W5"), hasWkn(null), hasTicker(null), //
                        hasName("Kontron"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-27T08:15"), hasShares(23.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Order: 0e9a-3i77 | Ausführung: b53c-j787"), //
                        hasAmount("EUR", 481.70), hasGrossValue("EUR", 480.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US86800U3023"), hasWkn(null), hasTicker(null), //
                        hasName("Super Micro Computer"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-23T21:56"), hasShares(10.00), //
                        hasSource("Kauf16.txt"), //
                        hasNote("Order: 9eb1-48df | Ausführung: bdc4-0272"), //
                        hasAmount("EUR", 421.60), hasGrossValue("EUR", 420.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3YLTY66"), hasWkn(null), hasTicker(null), //
                        hasName("MSCI All Country World Investable Market Unhedged USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-07-02T00:00"), hasShares(0.047644), //
                        hasSource("Kauf17.txt"), //
                        hasNote("Sparplan: 85ea-l3Nv | Ausführung: 0073-f716"), //
                        hasAmount("EUR", 13.21), hasGrossValue("EUR", 10.57), //
                        hasTaxes("EUR", 2.64), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("FTSE All-World USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-04T00:00"), hasShares(0.000074), //
                        hasSource("Kauf18.txt"), //
                        hasNote("Sparplan: 3dbf-1e4b | Ausführung: 3837-baeb"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf19()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A1XML2"), hasWkn(null), hasTicker(null), //
                        hasName("Anleihe Sept. 2117"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-11T12:06"), hasShares(40.00), //
                        hasSource("Kauf19.txt"), //
                        hasNote("Auftrag: 6630-2b3d | Ausführung: 1369-4eb2 | Stückzinsen 75,25 EUR"), //
                        hasAmount("EUR", 2476.25), hasGrossValue("EUR", 2475.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf20()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US68389XCV55"), hasWkn(null), hasTicker(null), //
                        hasName("Sept. 2064"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-15T17:48"), hasShares(12.5351), //
                        hasSource("Kauf20.txt"), //
                        hasNote("Auftrag: 38ac-eee6 | Ausführung: 3a9c-1967 | Stückzinsen 3,30 EUR"), //
                        hasAmount("EUR", 1001.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf21()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("XS2800678224"), hasWkn(null), hasTicker(null), //
                        hasName("Aug. 2029"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-15T12:28"), hasShares(0.0421), //
                        hasSource("Kauf21.txt"), //
                        hasNote("Auftrag: 31cf-3652 | Ausführung: 0720-8c9c | Stückzinsen 0,11 EUR"), //
                        hasAmount("EUR", 4.27), hasGrossValue("EUR", 4.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSecurityBuy01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SQ6QKU9"), hasWkn(null), hasTicker(null), //
                        hasName("Société Générale Effekten GmbH MiniL O.End"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-28T11:13"), hasShares(100.00), //
                        hasSource("Buy01.txt"), //
                        hasNote("Order: 10VG-16T0 | Execution: 4A66-g597"), //
                        hasAmount("EUR", 87.40), hasGrossValue("EUR", 86.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681038243"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-Amundi NASDAQ-100 Namens-Anteile C Cap.EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-02T00:00"), hasShares(0.016033), //
                        hasSource("Buy02.txt"), //
                        hasNote("Saveback: 3765-0e2e | Execution: ab98-f9b9"), //
                        hasAmount("EUR", 2.97), hasGrossValue("EUR", 2.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCompra01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A1ML7J1"), hasWkn(null), hasTicker(null), //
                        hasName("Vonovia SE "), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-01T11:56"), hasShares(0.781379), //
                        hasSource("Compra01.txt"), //
                        hasNote("Orden: 1b03-784c | Ejecución: d4e7-9ecc"), //
                        hasAmount("EUR", 18.80), hasGrossValue("EUR", 18.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSecurityAcquisto01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Acquisto01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3281404601"), hasWkn(null), hasTicker(null), //
                        hasName("zjBAM Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-01T10:46"), hasShares(125.00), //
                        hasSource("Acquisto01.txt"), //
                        hasNote("Ordine: cY43-6m6l | Esecuzione: V711-7789"), //
                        hasAmount("EUR", 3719.75), hasGrossValue("EUR", 3718.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testSecurityAcquisto02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Acquisto02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BWT3KN65"), hasWkn(null), hasTicker(null), //
                        hasName("Factor MSCI USA Quality ESG EUR Hedged (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-03T00:00"), hasShares(0.170055), //
                        hasSource("Acquisto02.txt"), //
                        hasNote("Saveback: b05b-DL1N | Esecuzione: 1bfa-7328"), //
                        hasAmount("EUR", 7.15), hasGrossValue("EUR", 7.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSecurityAcquisto03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Acquisto03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BWT3KN65"), hasWkn(null), hasTicker(null), //
                        hasName("Factor MSCI USA Quality ESG EUR Hedged (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-17T00:00"), hasShares(0.100058), //
                        hasSource("Acquisto03.txt"), //
                        hasNote("Accumulo: d81c-7o2A | Esecuzione: 2bQ1-76v5"), //
                        hasAmount("EUR", 4.28), hasGrossValue("EUR", 4.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoKauf01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ETH"), //
                        hasName("Ethereum"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-03T12:32"), hasShares(0.0878), //
                        hasSource("CryptoKauf01.txt"), //
                        hasNote("Order: 2dc3-a410 | Ausführung: ce15-0e37"), //
                        hasAmount("EUR", 300.87), hasGrossValue("EUR", 299.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testCryptoKauf02()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-16T00:00"), hasShares(0.000983), //
                        hasSource("CryptoKauf02.txt"), //
                        hasNote("Sparplan: y646-a753 | Ausführung: K7Y2-2e37"), //
                        hasAmount("EUR", 24.99), hasGrossValue("EUR", 24.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoKauf03()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-06T21:10"), hasShares(0.0026), //
                        hasSource("CryptoKauf03.txt"), //
                        hasNote("Order: e010-35b5 | Ausführung: fc83-aeb4"), //
                        hasAmount("EUR", 125.21), hasGrossValue("EUR", 124.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testCryptoKauf04()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf04.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-26T14:47"), hasShares(0.005), //
                        hasSource("CryptoKauf04.txt"), //
                        hasNote("Order: c960-9918 | Ausführung: 41b0-5376"), //
                        hasAmount("EUR", 273.73), hasGrossValue("EUR", 272.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testCryptoKauf05()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf05.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ETH"), //
                        hasName("Ethereum"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-29T18:00"), hasShares(0.11), //
                        hasSource("CryptoKauf05.txt"), //
                        hasNote("Order: 619b-2d8c | Ausführung: 2ce7-e8a4"), //
                        hasAmount("EUR", 428.59), hasGrossValue("EUR", 427.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testCryptoKauf06()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf06.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T00:00"), hasShares(0.000254), //
                        hasSource("CryptoKauf06.txt"), //
                        hasNote("Saveback: 9eed-7c4d | Ausführung: 4e58-c971"), //
                        hasAmount("EUR", 13.71), hasGrossValue("EUR", 13.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoKauf07()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf07.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-03T00:00"), hasShares(0.000262), //
                        hasSource("CryptoKauf07.txt"), //
                        hasNote("Auftrag: 53fd-8pY9 | Ausführung: ab5c-e715"), //
                        hasAmount("EUR", 24.98), hasGrossValue("EUR", 24.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoKauf08()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf08.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XRP"), //
                        hasName("XRP"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "xrp"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-24T00:00"), hasShares(4.305443), //
                        hasSource("CryptoKauf08.txt"), //
                        hasNote("Sparplan: 6a76-2917 | Ausführung: 5d58-8434"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoKauf09()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf09.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-17T00:00"), hasShares(0.000294), //
                        hasSource("CryptoKauf09.txt"), //
                        hasNote("Sparplan: fab1-67af | Ausführung: 7c98-a11f"), //
                        hasAmount("EUR", 24.99), hasGrossValue("EUR", 24.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoVerkauf01()
    {
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ETH"), //
                        hasName("Ethereum"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "ethereum"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-10T16:11"), hasShares(0.1), //
                        hasSource("CryptoVerkauf01.txt"), //
                        hasNote("Order: 25a4-7fba | Ausführung: dd26-52b9"), //
                        hasAmount("EUR", 261.85), hasGrossValue("EUR", 262.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testAchat01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88032Q1094"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Tencent Holdings Ltd. Reg.Sh.(unsp.ADRs)/1 HD -,0001"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-17T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3773)));
        assertThat(entry.getSource(), is("Achat01.txt"));
        assertThat(entry.getNote(), is("Programmé : eea2-4c8b | Exécution : cee1-2d00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(20.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(20.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testAchat02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-10T17:33"), hasShares(1.00), //
                        hasSource("Achat02.txt"), //
                        hasNote("Ordre : 69da-1c6f | Exécution : 6f72-063d"), //
                        hasAmount("EUR", 157.18), hasGrossValue("EUR", 156.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testAchat03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-10T17:33"), hasShares(0.92086), //
                        hasSource("Achat03.txt"), //
                        hasNote("Ordre : 7c28-5194 | Exécution : f671-36d8"), //
                        hasAmount("EUR", 143.82), hasGrossValue("EUR", 143.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAchat04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker(null), //
                        hasName("Tesla Inc. Registered Shares DL-,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-24T17:10"), hasShares(0.1127), //
                        hasSource("Achat04.txt"), //
                        hasNote("Ordre : f023-fccb | Exécution : d275-d0b1"), //
                        hasAmount("EUR", 18.81), hasGrossValue("EUR", 18.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAchat05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0846707026"), hasWkn(null), hasTicker(null), //
                        hasName("Berkshire Hathaway (B)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-17T00:00"), hasShares(0.216943), //
                        hasSource("Achat05.txt"), //
                        hasNote("Plan de D'épargne: 78c5-4592 | Exécution : 7d14-148e"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAchat06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006452907"), hasWkn(null), hasTicker(null), //
                        hasName("Nemetschek"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-17T00:00"), hasShares(0.487012), //
                        hasSource("Achat06.txt"), //
                        hasNote("Plan de D'épargne: 685f-452d | Exécution : c847-5f66"), //
                        hasAmount("EUR", 60.00), hasGrossValue("EUR", 60.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(299.96))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(4.20))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-13T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(123.45))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(123.45))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(123.45))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-04-01"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-04-15"), hasAmount("EUR", 690.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-04-30"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-14"), hasAmount("EUR", 690.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-31"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-14"), hasAmount("EUR", 690.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-01"), hasAmount("EUR", 11111.11), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-04-01"), hasAmount("EUR", 0.01), //
                                        hasSource("Kontoauszug05.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-05-01"), hasAmount("EUR", 0.01), //
                                        hasSource("Kontoauszug05.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-06-01"), hasAmount("EUR", 0.01), //
                                        hasSource("Kontoauszug05.txt"), hasNote(null)))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(20L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 147.34), //
                                        hasSource("Kontoauszug06.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-02"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-04-03"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Trade Republic Card"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-04"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-07"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug06.txt"), hasNote("Backerei XAXRs 798"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-07"), hasAmount("EUR", 70.60), //
                        hasSource("Kontoauszug06.txt"), hasNote("EDEKA AxrLcb"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-08"), hasAmount("EUR", 1.80), //
                        hasSource("Kontoauszug06.txt"), hasNote("EBFwhg LfId nxUb"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-13"), hasAmount("EUR", 2.50), //
                        hasSource("Kontoauszug06.txt"), hasNote("vCvfyqNr hOBYv 798"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-13"), hasAmount("EUR", 74.33), //
                        hasSource("Kontoauszug06.txt"), hasNote("EDEKA hyIMEN"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-14"), hasAmount("EUR", 10.05), //
                        hasSource("Kontoauszug06.txt"), hasNote("REWE uGLyXYQ CCG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-15"), hasAmount("EUR", 61.09), //
                        hasSource("Kontoauszug06.txt"), hasNote("TOTAL SERVICE STATION"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-19"), hasAmount("EUR", 108.83), //
                        hasSource("Kontoauszug06.txt"), hasNote("wfAHGwkQxJztVgB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-20"), hasAmount("EUR", 45.80), //
                        hasSource("Kontoauszug06.txt"), hasNote("EDEKA jyLXSG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-20"), hasAmount("EUR", 53.37), //
                        hasSource("Kontoauszug06.txt"), hasNote("TOTAL SERVICE STATION"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-25"), hasAmount("EUR", 42.84), //
                        hasSource("Kontoauszug06.txt"), hasNote("KLEINTIERPRAXIS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-25"), hasAmount("EUR", 33.50), //
                        hasSource("Kontoauszug06.txt"), hasNote("RHBhHIp CyO UAuuE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-27"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("27964 lYtWjRAJd NA - jH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-27"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("wHzU Er sEH jTaaEGJm tMrm"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-27"), hasAmount("EUR", 28.67), //
                        hasSource("Kontoauszug06.txt"), hasNote("TOTAL SERVICE STATION"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-27"), hasAmount("EUR", 33.46), //
                        hasSource("Kontoauszug06.txt"), hasNote("EDEKA jLcXlR"))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(35L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(35));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 53.89), //
                                        hasSource("Kontoauszug07.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-02"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-10"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("TooGoodT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-10"), hasAmount("EUR", 12.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("BURGER"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-12"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-13"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("TooGoodT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-14"), hasAmount("EUR", 39.86), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-14"), hasAmount("EUR", 0.08), //
                        hasSource("Kontoauszug07.txt"), hasNote("Visa Geld zurueck Aktion"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-16"), hasAmount("EUR", 18.34), //
                        hasSource("Kontoauszug07.txt"), hasNote("ALIEXPRESS.COM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-16"), hasAmount("EUR", 17.53), //
                        hasSource("Kontoauszug07.txt"), hasNote("ALIEXPRESS.COM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-16"), hasAmount("EUR", 19.40), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-16"), hasAmount("EUR", 0.08), //
                        hasSource("Kontoauszug07.txt"), hasNote("Visa Geld zurueck Aktion"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-17"), hasAmount("EUR", 18.34), //
                        hasSource("Kontoauszug07.txt"), hasNote("ALIEXPRESS.COM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-17"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("TooGoodT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-17"), hasAmount("EUR", 20.40), //
                        hasSource("Kontoauszug07.txt"), hasNote("Hornbach Baumarkt AG FIL."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-19"), hasAmount("EUR", 7.99), //
                        hasSource("Kontoauszug07.txt"), hasNote("AMZN Mktp DE*HD9KW0JT4"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-19"), hasAmount("EUR", 0.37), //
                        hasSource("Kontoauszug07.txt"), hasNote("Visa Geld zurueck Aktion"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-19"), hasAmount("EUR", 0.35), //
                        hasSource("Kontoauszug07.txt"), hasNote("Visa Geld zurueck Aktion"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-20"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("TooGoodT"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-20"), hasAmount("EUR", 0.08), //
                        hasSource("Kontoauszug07.txt"), hasNote("Visa Geld zurueck Aktion"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-21"), hasAmount("EUR", 36.26), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-21"), hasAmount("EUR", 17.99), //
                        hasSource("Kontoauszug07.txt"), hasNote("NETFLIX.COM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-22"), hasAmount("EUR", 51.70), //
                        hasSource("Kontoauszug07.txt"), hasNote("VORWERK CO. KG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-22"), hasAmount("EUR", 10.90), //
                        hasSource("Kontoauszug07.txt"), hasNote("eBay O*99-99999-99999"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-22"), hasAmount("EUR", 7.59), //
                        hasSource("Kontoauszug07.txt"), hasNote("eBay O*99-99999-99999"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-23"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("TooGoodT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-23"), hasAmount("EUR", 6.08), //
                        hasSource("Kontoauszug07.txt"), hasNote("Rossmann 1234"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-24"), hasAmount("EUR", 18.51), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-25"), hasAmount("EUR", 43.99), //
                        hasSource("Kontoauszug07.txt"), hasNote("AMZN Mktp DE*H545J2OX7"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-25"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("TooGoodT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-26"), hasAmount("EUR", 15.49), //
                        hasSource("Kontoauszug07.txt"), hasNote("DHL*YKUNQRDBB99U"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-27"), hasAmount("EUR", 18.09), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-29"), hasAmount("EUR", 7.59), //
                        hasSource("Kontoauszug07.txt"), hasNote("eBay"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-30"), hasAmount("EUR", 25.97), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-30"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(15L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(4L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-02"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-10-02"), hasAmount("EUR", 3.81), //
                                        hasSource("Kontoauszug08.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-25"), hasAmount("EUR", 1800.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-30"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-30"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-11-01"), hasAmount("EUR", 13.68), //
                                        hasSource("Kontoauszug08.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-11-29"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-11-30"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-12-01"), hasAmount("EUR", 19.29), //
                                        hasSource("Kontoauszug08.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-14"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-14"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-27"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-12-29"), hasAmount("EUR", 25.50), //
                                        hasSource("Kontoauszug08.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-02"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-30"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));
    }

    @Test
    public void testKontoauszug09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-05-01"), hasAmount("EUR", 58.71), //
                                        hasSource("Kontoauszug09.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-01"), hasAmount("EUR", 69.99), //
                        hasSource("Kontoauszug09.txt"), hasNote("DETLEV LOUIS MOTORRAD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-02"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("PAYPAL *VODAFONE"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-02"), hasAmount("EUR", 4.94), //
                        hasSource("Kontoauszug09.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-03"), hasAmount("EUR", 40.31), //
                        hasSource("Kontoauszug09.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-03"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("TooGoodT"))));
    }

    @Test
    public void testKontoauszug10()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 172.23), //
                                        hasSource("Kontoauszug10.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-01"), hasAmount("EUR", 172.23), //
                        hasSource("Kontoauszug10.txt"), hasNote("PayOut to transit"))));
    }

    @Test
    public void testKontoauszug11()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-02-01"), hasAmount("EUR", 33.37), //
                                        hasSource("Kontoauszug11.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-02-19"), hasAmount("EUR", 78.17), //
                        hasSource("Kontoauszug11.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-01"), hasAmount("EUR", 6000.00), //
                        hasSource("Kontoauszug11.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-03-01"), hasAmount("EUR", 12.47), //
                                        hasSource("Kontoauszug11.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 7.68), //
                                        hasSource("Kontoauszug11.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-04"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug11.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-28"), hasAmount("EUR", 2.40), //
                        hasSource("Kontoauszug11.txt"), hasNote("Hornbach Baumarkt AG FIL."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-28"), hasAmount("EUR", 13.95), //
                        hasSource("Kontoauszug11.txt"), hasNote("Hornbach Baumarkt AG FIL."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-28"), hasAmount("EUR", 9.48), //
                        hasSource("Kontoauszug11.txt"), hasNote("Hornbach Baumarkt AG FIL."))));
    }

    @Test
    public void testKontoauszug12()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 30.15), //
                                        hasSource("Kontoauszug12.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-02"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-03"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-11"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-16"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Einzahlung akzeptiert: DE5987654321 auf DE12334567658"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-23"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-29"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-05-01"), hasAmount("EUR", 48.63), //
                                        hasSource("Kontoauszug12.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-03"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-06"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-13"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 3600.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Einzahlung akzeptiert: DE5987654321 auf DE12334567658"))));
    }

    @Test
    public void testKontoauszug13()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-02"), hasAmount("EUR", 5361.15), //
                        hasSource("Kontoauszug13.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-06"), hasAmount("EUR", 505.50), //
                        hasSource("Kontoauszug13.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-07"), hasAmount("EUR", 1164.38), //
                        hasSource("Kontoauszug13.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-15"), hasAmount("EUR", 29994.37), //
                        hasSource("Kontoauszug13.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-20"), hasAmount("EUR", 18085.60), //
                        hasSource("Kontoauszug13.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-27"), hasAmount("EUR", 26.59), //
                        hasSource("Kontoauszug13.txt"), hasNote("kost.ate"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-28"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("gNMNvajnz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-28"), hasAmount("EUR", 7.48), //
                        hasSource("Kontoauszug13.txt"), hasNote("tttbC jHeBx"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-28"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("CSXaP"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-29"), hasAmount("EUR", 26.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("WhW XwD XAUS & XZD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-30"), hasAmount("EUR", 2.24), //
                        hasSource("Kontoauszug13.txt"), hasNote("QLSQ Fil. 4249"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-31"), hasAmount("EUR", 28.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("ykSkDv*IZkapF McAh glw"))));
    }

    @Test
    public void testKontoauszug14()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-03"), hasAmount("EUR", 5.18), //
                        hasSource("Kontoauszug14.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-09"), hasAmount("EUR", 33.21), //
                        hasSource("Kontoauszug14.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-14"), hasAmount("EUR", 6500.00), //
                        hasSource("Kontoauszug14.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-06-14"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug14.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-06-14"), hasAmount("EUR", 80.62), //
                        hasSource("Kontoauszug14.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug15()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-06-01"), hasAmount("EUR", 123.98), //
                                        hasSource("Kontoauszug15.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-05"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-10"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("Einzahlung akzeptiert: DE17500105175434601138 auf DE30501108019083804737"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-18"), hasAmount("EUR", 8000.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-06-27"), hasAmount("EUR", 216.56), //
                        hasSource("Kontoauszug15.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-06-27"), hasAmount("EUR", 111.69), //
                        hasSource("Kontoauszug15.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug16()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-26"), hasAmount("EUR", 22000.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Outgoing transfer for Vorname Nachname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-27"), hasAmount("EUR", 3.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("PARKEN FLUGHAFENSTUTTG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-27"), hasAmount("EUR", 26.30), //
                        hasSource("Kontoauszug16.txt"), hasNote("AMZN Mktp DE*000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-30"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Incoming transfer from Vorname Nachname"))));
    }

    @Test
    public void testKontoauszug17()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-01"), hasAmount("EUR", 14.64),
                        hasSource("Kontoauszug17.txt"), hasNote("KAUFLAND STUTTGART MUEHLH"))));
    }

    @Test
    public void testKontoauszug18()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-02"), hasAmount("EUR", 1200.00),
                        hasSource("Kontoauszug18.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        assertThat(results, hasItem(removal(hasDate("2024-08-19"), hasAmount("EUR", 900.00),
                        hasSource("Kontoauszug18.txt"), hasNote("Outgoing transfer for Vorname Nachname"))));

        assertThat(results, hasItem(deposit(hasDate("2024-08-29"), hasAmount("EUR", 2500.00),
                        hasSource("Kontoauszug18.txt"), hasNote("Incoming transfer from Vorname Nachname"))));
    }

    @Test
    public void testKontoauszug19()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-01"), hasAmount("EUR", 250.00),
                        hasSource("Kontoauszug19.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-07-01"), hasAmount("EUR", 54.83), //
                                        hasSource("Kontoauszug19.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-03"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-15"), hasAmount("EUR", 6000.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Einzahlung akzeptiert: DE12345678901234567789 auf DE23456789012345678901"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-22"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Outgoing transfer for EMYRMzk QpSHhzd"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-23"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Incoming transfer from KLEslAT zxcWeqg"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-29"), hasAmount("EUR", 600.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Outgoing transfer for TtJeOuZ sAkseHU"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-08-01"), hasAmount("EUR", 74.47), //
                                        hasSource("Kontoauszug19.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-08-02"), hasAmount("EUR", 6.54), //
                        hasSource("Kontoauszug19.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-02"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Outgoing transfer for RBTzIlt ZDKKNDt"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-12"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Outgoing transfer for zpQkUUg EJrrpLo"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-13"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Incoming transfer from bfNFczb vazFwnd"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-14"), hasAmount("EUR", 1800.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Incoming transfer from gnBKCWX yxkIHfg"))));
    }

    @Test
    public void testKontoauszug20()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-20"), hasAmount("EUR", 889.77), //
                        hasSource("Kontoauszug20.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-26"), hasAmount("EUR", 24.95),
                        hasSource("Kontoauszug20.txt"), hasNote("Hornbach Baumarkt AG FIL."))));
    }

    @Test
    public void testKontoauszug21()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(24L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(6L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 114.96), //
                                        hasSource("Kontoauszug21.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-04-16"), hasAmount("EUR", 5.00), hasSource("Kontoauszug21.txt"),
                        hasNote("Trade Republic Card"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-23"), hasAmount("EUR", 37.82),
                        hasSource("Kontoauszug21.txt"), hasNote("ALDI SAGT DANKE"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-05-01"), hasAmount("EUR", 111.50), //
                                        hasSource("Kontoauszug21.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-06-01"), hasAmount("EUR", 115.41), //
                                        hasSource("Kontoauszug21.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-14"), hasAmount("EUR", 250.00),
                        hasSource("Kontoauszug21.txt"), hasNote("Incoming transfer from ELGcUHm aGUVVCu Ts. zVweVvRT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-15"), hasAmount("EUR", 32.52),
                        hasSource("Kontoauszug21.txt"), hasNote("Sebert's Hausschlachtewar"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-15"), hasAmount("EUR", 10.00),
                        hasSource("Kontoauszug21.txt"), hasNote("APOTHEKE DR ANSCHUETZ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-29"), hasAmount("EUR", 108.05),
                        hasSource("Kontoauszug21.txt"), hasNote("REWE Riethmueller oHG"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-07-01"), hasAmount("EUR", 107.80), //
                                        hasSource("Kontoauszug21.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-05"), hasAmount("EUR", 89.43),
                        hasSource("Kontoauszug21.txt"), hasNote("SB-TANK 9868"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-14"), hasAmount("EUR", 12.34),
                        hasSource("Kontoauszug21.txt"), hasNote("Outgoing transfer for BvNENli XcObMce Jv. lIzWULKt"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-15"), hasAmount("EUR", 12.50),
                        hasSource("Kontoauszug21.txt"), hasNote("APOTHEKE DR ANSCHUETZ"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-22"), hasAmount("EUR", 80000.00),
                        hasSource("Kontoauszug21.txt"), hasNote("Incoming transfer from KySavdk xMfQpFe zn. iSrubVAU"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-24"), hasAmount("EUR", 10.70),
                        hasSource("Kontoauszug21.txt"), hasNote("Neckarbruecke"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-08-01"), hasAmount("EUR", 168.75), //
                                        hasSource("Kontoauszug21.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-09"), hasAmount("EUR", 5.63),
                        hasSource("Kontoauszug21.txt"), hasNote("APOTHEKE DR ANSCHUETZ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-13"), hasAmount("EUR", 12.34),
                        hasSource("Kontoauszug21.txt"), hasNote("Outgoing transfer for rCyGuWY fhxOEBW gA. HBFOfBLD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-14"), hasAmount("EUR", 80000.00),
                        hasSource("Kontoauszug21.txt"), hasNote("Outgoing transfer for AmaJIZH jLWGtPe Kb. WGfgHavT"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-09-01"), hasAmount("EUR", 193.06), //
                                        hasSource("Kontoauszug21.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-02"), hasAmount("EUR", 7.50),
                        hasSource("Kontoauszug21.txt"), hasNote("APOTHEKE DR ANSCHUETZ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-21"), hasAmount("EUR", 7.10),
                        hasSource("Kontoauszug21.txt"), hasNote("Frankfurt (Main) Hbf"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-25"), hasAmount("EUR", 148.22),
                        hasSource("Kontoauszug21.txt"), hasNote("REWE Riethmueller oHG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-26"), hasAmount("EUR", 5.00),
                        hasSource("Kontoauszug21.txt"), hasNote("APOTHEKE DR ANSCHUETZ"))));
    }

    @Test
    public void testKontoauszug22()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-03-30"), hasAmount("EUR", 4000.00),
                        hasSource("Kontoauszug22.txt"), hasNote("Accepted PayIn:DE74500400480142038900 to DE30110101008889827581"))));
    }

    @Test
    public void testKontoauszug23()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-12-01"), hasAmount("EUR", 150.28), //
                                        hasSource("Kontoauszug23.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-02"), hasAmount("EUR", 39.94),
                        hasSource("Kontoauszug23.txt"), hasNote("xoSEqCGkmh eqt STATION"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-02"), hasAmount("EUR", 500.00),
                        hasSource("Kontoauszug23.txt"), hasNote("Incoming transfer from FFzotalY jzkOA VDDIVAlJ-BFlsJNLDaTKa"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-02"), hasAmount("EUR", 7.24),
                        hasSource("Kontoauszug23.txt"), hasNote("Your Saveback payment"))));
    }

    @Test
    public void testKontoauszug24()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-29"), hasAmount("EUR", 18.94),
                        hasSource("Kontoauszug24.txt"), hasNote("Rossmann 000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 3000.00),
                        hasSource("Kontoauszug24.txt"), hasNote("Outgoing transfer for Vorname Nachname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-31"), hasAmount("EUR", 18.85),
                        hasSource("Kontoauszug24.txt"), hasNote("Lidl sagt Danke"))));
    }

    @Test
    public void testKontoauszug25()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-23"), hasAmount("EUR", 500.00),
                        hasSource("Kontoauszug25.txt"), hasNote("Vorname Nachname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-24"), hasAmount("EUR", 34.14),
                        hasSource("Kontoauszug25.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-24"), hasAmount("EUR", 359.37),
                        hasSource("Kontoauszug25.txt"), hasNote("Möbel Heidenreich GmbH"))));
    }

    @Test
    public void testKontoauszug26()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(21L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-11-01"), hasAmount("EUR", 104.13), //
                                        hasSource("Kontoauszug26.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-04"), hasAmount("EUR", 200.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-11"), hasAmount("EUR", 121.50),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-11"), hasAmount("EUR", 2.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-11"), hasAmount("EUR", 3.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-18"), hasAmount("EUR", 4000.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-18"), hasAmount("EUR", 150.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-19"), hasAmount("EUR", 50.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-20"), hasAmount("EUR", 8000.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-21"), hasAmount("EUR", 984.72),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-22"), hasAmount("EUR", 100.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-26"), hasAmount("EUR", 50.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-12-01"), hasAmount("EUR", 112.33), //
                                        hasSource("Kontoauszug26.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-02"), hasAmount("EUR", 750.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-10"), hasAmount("EUR", 50.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-13"), hasAmount("EUR", 3400.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Incoming transfer from name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-18"), hasAmount("EUR", 300.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-20"), hasAmount("EUR", 50.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-22"), hasAmount("EUR", 5800.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-22"), hasAmount("EUR", 36000.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-26"), hasAmount("EUR", 65.00),
                        hasSource("Kontoauszug26.txt"), hasNote("Outgoing transfer for name surname"))));
    }

    @Test
    public void testKontoauszug27()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(55L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(55));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-01"), hasAmount("EUR", 6.70),
                        hasSource("Kontoauszug27.txt"), hasNote("HubNsrxbIO HbYYXkTJHpUamm"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-01"), hasAmount("EUR", 13.32),
                        hasSource("Kontoauszug27.txt"), hasNote("eSInlgr XWAnJB DuQwySEEKY"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-01"), hasAmount("EUR", 6.60),
                        hasSource("Kontoauszug27.txt"), hasNote("rpyl rs UFBh phbAM FFB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-01"), hasAmount("EUR", 41.59),
                        hasSource("Kontoauszug27.txt"), hasNote("GDS-goHlc WyTr"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-01"), hasAmount("EUR", 55.45),
                        hasSource("Kontoauszug27.txt"), hasNote("WBecp FBgFVeE"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-12-01"), hasAmount("EUR", 15.77), //
                                        hasSource("Kontoauszug27.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-02"), hasAmount("EUR", 4.75),
                        hasSource("Kontoauszug27.txt"), hasNote("licfLzsOq xzwQtZL"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-02"), hasAmount("EUR", 9.45),
                        hasSource("Kontoauszug27.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-03"), hasAmount("EUR", 46.64),
                        hasSource("Kontoauszug27.txt"), hasNote("hFZmjl uehnzaSz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-03"), hasAmount("EUR", 14.50),
                        hasSource("Kontoauszug27.txt"), hasNote("cAnLVQI ZjXL"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-08"), hasAmount("EUR", 9.00),
                        hasSource("Kontoauszug27.txt"), hasNote("epGjyYMZfp tpjIaWheSWVfbo"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-08"), hasAmount("EUR", 37.30),
                        hasSource("Kontoauszug27.txt"), hasNote("guMz PQNi"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-08"), hasAmount("EUR", 98.54),
                        hasSource("Kontoauszug27.txt"), hasNote("kfv Xztr"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-09"), hasAmount("EUR", 520.00),
                        hasSource("Kontoauszug27.txt"), hasNote("Incoming transfer from QLVxyD JfJJlK"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-11"), hasAmount("EUR", 100.00),
                        hasSource("Kontoauszug27.txt"), hasNote("EmIExmHsD uuxPydbjQz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-13"), hasAmount("EUR", 3.19),
                        hasSource("Kontoauszug27.txt"), hasNote("pRIfK JyMISAgH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-13"), hasAmount("EUR", 4.87),
                        hasSource("Kontoauszug27.txt"), hasNote("pMqnD JSQBaiCPxQex"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-13"), hasAmount("EUR", 13.50),
                        hasSource("Kontoauszug27.txt"), hasNote("jnsEkeJV rtUOS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-14"), hasAmount("EUR", 36.18),
                        hasSource("Kontoauszug27.txt"), hasNote("EFFASFSFAS.DSA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-14"), hasAmount("EUR", 240.00),
                        hasSource("Kontoauszug27.txt"), hasNote("IVsrlEj MhwZqDpU doQHXUcJ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-15"), hasAmount("EUR", 36.42),
                        hasSource("Kontoauszug27.txt"), hasNote("nIfj uqenPoyA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-15"), hasAmount("EUR", 2.80),
                        hasSource("Kontoauszug27.txt"), hasNote("IxVflBvruR yLXMJvHzDtPplW"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-15"), hasAmount("EUR", 89.65),
                        hasSource("Kontoauszug27.txt"), hasNote("ytbMV zcMiqTW"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-16"), hasAmount("EUR", 12.99),
                        hasSource("Kontoauszug27.txt"), hasNote("sfsdfsd.ww*DS23424234"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-19"), hasAmount("EUR", 22.98),
                        hasSource("Kontoauszug27.txt"), hasNote("Vtob PgVu DE*DI5NV9OJ5"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-19"), hasAmount("EUR", 55.73),
                        hasSource("Kontoauszug27.txt"), hasNote("UDZr coFY"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-20"), hasAmount("EUR", 3.38),
                        hasSource("Kontoauszug27.txt"), hasNote("HDnl RstW"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-20"), hasAmount("EUR", 2.10),
                        hasSource("Kontoauszug27.txt"), hasNote("PuraVKczLStpt FLPk DFG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-20"), hasAmount("EUR", 400.00),
                        hasSource("Kontoauszug27.txt"), hasNote("yhCAHu SntXVf"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-21"), hasAmount("EUR", 10.49),
                        hasSource("Kontoauszug27.txt"), hasNote("FDSFDAFSASF.FSA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-21"), hasAmount("EUR", 34.40),
                        hasSource("Kontoauszug27.txt"), hasNote("ktEH & fqvS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-21"), hasAmount("EUR", 200.00),
                        hasSource("Kontoauszug27.txt"), hasNote("*FOhJADgX QQnE AG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-21"), hasAmount("EUR", 9.99),
                        hasSource("Kontoauszug27.txt"), hasNote("JDPr NlRbC&sTL wnIjzXt"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-22"), hasAmount("EUR", 2.80),
                        hasSource("Kontoauszug27.txt"), hasNote("bKavFEEKTX nrxWZnaymunkcP"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-22"), hasAmount("EUR", 400.00),
                        hasSource("Kontoauszug27.txt"), hasNote("rjSDlg UFstUb"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-23"), hasAmount("EUR", 11.14),
                        hasSource("Kontoauszug27.txt"), hasNote("wmUhh fxerrhfP"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-24"), hasAmount("EUR", 3.30),
                        hasSource("Kontoauszug27.txt"), hasNote("QPlRVNVpw LyaBrfT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-25"), hasAmount("EUR", 24.56),
                        hasSource("Kontoauszug27.txt"), hasNote("yGOG mOxj"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-25"), hasAmount("EUR", 4.00),
                        hasSource("Kontoauszug27.txt"), hasNote("urIbuRlwLGmeB wVBT GMB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-26"), hasAmount("EUR", 5.14),
                        hasSource("Kontoauszug27.txt"), hasNote("BACKBLAZE INC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-27"), hasAmount("EUR", 99.61),
                        hasSource("Kontoauszug27.txt"), hasNote("Nfw skVO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-28"), hasAmount("EUR", 238.10),
                        hasSource("Kontoauszug27.txt"), hasNote("ID pncliPcD kQWV"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-28"), hasAmount("EUR", 23.96),
                        hasSource("Kontoauszug27.txt"), hasNote("EcEuL kCFygIASpTyN"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-28"), hasAmount("EUR", 1.80),
                        hasSource("Kontoauszug27.txt"), hasNote("FhvyLMRXE bZxnvCz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-29"), hasAmount("EUR", 5.70),
                        hasSource("Kontoauszug27.txt"), hasNote("jxrMtJgSqC bVTOLlpPCmahbU"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-29"), hasAmount("EUR", 19.99),
                        hasSource("Kontoauszug27.txt"), hasNote("F.D.V. cbdMLKSmT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 38.77),
                        hasSource("Kontoauszug27.txt"), hasNote("sVwi Gflp DE*DF53535"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 5.10),
                        hasSource("Kontoauszug27.txt"), hasNote("obOIB *FLGH PANyDq Da"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 3.30),
                        hasSource("Kontoauszug27.txt"), hasNote("BVBcatJdS 184"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 1.59),
                        hasSource("Kontoauszug27.txt"), hasNote("vfhv Fil. 3779"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-31"), hasAmount("EUR", 12.49),
                        hasSource("Kontoauszug27.txt"), hasNote("Bmh*VwwypVb KCclsISCP Nor"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-31"), hasAmount("EUR", 76.50),
                        hasSource("Kontoauszug27.txt"), hasNote("dAGljO ujZvTfUWOOP"))));
    }

    @Test
    public void testKontoauszug28()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-26"), hasAmount("EUR", 5.36),
                        hasSource("Kontoauszug28.txt"), hasNote("BACKBLAZE INC"))));
    }

    @Test
    public void testKontoauszug29()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-04"), hasAmount("EUR", 1200.00),
                        hasSource("Kontoauszug29.txt"), hasNote("Einzahlung akzeptiert: DE00000000000000000000 auf DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-17"), hasAmount("EUR", 187.96),
                        hasSource("Kontoauszug29.txt"), hasNote("Sepa Direct Debit transfer to Stadt Wohnort"))));
    }

    @Test
    public void testKontoauszug30()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(61L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(61));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-01-01"), hasAmount("EUR", 9.07), //
                                        hasSource("Kontoauszug30.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-02"), hasAmount("EUR", 222.22),
                        hasSource("Kontoauszug30.txt"), hasNote("qo xzWAzeVDSUbhaMSprBw"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-02"), hasAmount("EUR", 8.64),
                        hasSource("Kontoauszug30.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-03"), hasAmount("EUR", 35.00),
                        hasSource("Kontoauszug30.txt"), hasNote("PAYPAL Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-03"), hasAmount("EUR", 20.34),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-03"), hasAmount("EUR", 25.09),
                        hasSource("Kontoauszug30.txt"), hasNote("Netto Marken-Discount"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-03"), hasAmount("EUR", 62.39),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym 168"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-03"), hasAmount("EUR", 11.41),
                        hasSource("Kontoauszug30.txt"), hasNote("SONDERPREIS BAUMARKT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-10"), hasAmount("EUR", 5.98),
                        hasSource("Kontoauszug30.txt"), hasNote("ALDI SUED"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-10"), hasAmount("EUR", 25.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-12"), hasAmount("EUR", 73.91),
                        hasSource("Kontoauszug30.txt"), hasNote("KAUFLAND Anonym 4850"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-13"), hasAmount("EUR", 8.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-13"), hasAmount("EUR", 4.67),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-14"), hasAmount("EUR", 20.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-14"), hasAmount("EUR", 3.10),
                        hasSource("Kontoauszug30.txt"), hasNote("DM-Drogerie Markt"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-15"), hasAmount("EUR", 400.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Incoming transfer from Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-16"), hasAmount("EUR", 767.90),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-18"), hasAmount("EUR", 17.28),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-18"), hasAmount("EUR", 28.03),
                        hasSource("Kontoauszug30.txt"), hasNote("Netto Marken-Discount"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-20"), hasAmount("EUR", 4.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-23"), hasAmount("EUR", 65.69),
                        hasSource("Kontoauszug30.txt"), hasNote("Netto Marken-Discount"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-24"), hasAmount("EUR", 25.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-26"), hasAmount("EUR", 200.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-28"), hasAmount("EUR", 52.84),
                        hasSource("Kontoauszug30.txt"), hasNote("Penny Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-28"), hasAmount("EUR", 28.95),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-28"), hasAmount("EUR", 20.00),
                        hasSource("Kontoauszug30.txt"), hasNote("GLS*0G7FAFB9"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-28"), hasAmount("EUR", 8.58),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-29"), hasAmount("EUR", 16.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym Restaur"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 2.80),
                        hasSource("Kontoauszug30.txt"), hasNote("eBay Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 4.49),
                        hasSource("Kontoauszug30.txt"), hasNote("eBay Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 500.00),
                        hasSource("Kontoauszug30.txt"), hasNote("")))); //TODO

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 5.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Shop Apotheke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-30"), hasAmount("EUR", 1500.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-31"), hasAmount("EUR", 2500.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-02-01"), hasAmount("EUR", 5.67), //
                                        hasSource("Kontoauszug30.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-01"), hasAmount("EUR", 25.32),
                        hasSource("Kontoauszug30.txt"), hasNote("Netto Marken-Discount"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-03"), hasAmount("EUR", 13.42),
                        hasSource("Kontoauszug30.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-04"), hasAmount("EUR", 3.65),
                        hasSource("Kontoauszug30.txt"), hasNote("Netto Marken-Discount"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-04"), hasAmount("EUR", 2.19),
                        hasSource("Kontoauszug30.txt"), hasNote("Netto Marken-Discount"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-06"), hasAmount("EUR", 20.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-07"), hasAmount("EUR", 22.28),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-11"), hasAmount("EUR", 9.49),
                        hasSource("Kontoauszug30.txt"), hasNote("NORDSEE GMBH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-11"), hasAmount("EUR", 450.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-12"), hasAmount("EUR", 20.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-13"), hasAmount("EUR", 260.95),
                        hasSource("Kontoauszug30.txt"), hasNote("BestSecret 2131544279"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-14"), hasAmount("EUR", 35.65),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-17"), hasAmount("EUR", 500.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Incoming transfer from bMCHytMkX bVvms"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-19"), hasAmount("EUR", 4.51),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-19"), hasAmount("EUR", 5.20),
                        hasSource("Kontoauszug30.txt"), hasNote("DM-Drogerie Markt"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-20"), hasAmount("EUR", 50.98),
                        hasSource("Kontoauszug30.txt"), hasNote("M and M Direct Limited"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-21"), hasAmount("EUR", 20.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anonym Anonym"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-21"), hasAmount("EUR", 391.76),
                        hasSource("Kontoauszug30.txt"), hasNote("Amazon.de*Anonym"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-21"), hasAmount("EUR", 400.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Incoming transfer from gIzfkDZbc gsOiG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-22"), hasAmount("EUR", 17.35),
                        hasSource("Kontoauszug30.txt"), hasNote("DM-Drogerie Markt"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-22"), hasAmount("EUR", 126.07),
                        hasSource("Kontoauszug30.txt"), hasNote("rZWeDfE 168"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-23"), hasAmount("EUR", 26.70),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-25"), hasAmount("EUR", 82.98),
                        hasSource("Kontoauszug30.txt"), hasNote("BestSecret 2131544279"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-25"), hasAmount("EUR", 60.00),
                        hasSource("Kontoauszug30.txt"), hasNote("Anony-E-NCDMJeoa tsVr"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-27"), hasAmount("EUR", 20.00),
                        hasSource("Kontoauszug30.txt"), hasNote("zryFpdTXMhjkx N/O"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-28"), hasAmount("EUR", 31.66),
                        hasSource("Kontoauszug30.txt"), hasNote("Lidl sagt Danke"))));
    }

    @Test
    public void testKontoauszug31()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(15L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-02-01"), hasAmount("EUR", 14.02), //
                                        hasSource("Kontoauszug31.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-02"), hasAmount("EUR", 10.00),
                        hasSource("Kontoauszug31.txt"), hasNote("XKCGtEUond mrCSfYTiNqeYko"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-02"), hasAmount("EUR", 45.47),
                        hasSource("Kontoauszug31.txt"), hasNote("eKYU JDAu"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-03"), hasAmount("EUR", 2000.00),
                        hasSource("Kontoauszug31.txt"), hasNote("Incoming transfer from hwPQmk Lnqlne"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-03"), hasAmount("EUR", 500.00),
                        hasSource("Kontoauszug31.txt"), hasNote("ORLJJB CjFgCR"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-02-03"), hasAmount("EUR", 7.41),
                        hasSource("Kontoauszug31.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-06"), hasAmount("EUR", 50.00),
                        hasSource("Kontoauszug31.txt"), hasNote("IJg iaMHdAtsmcz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-07"), hasAmount("EUR", 16.90),
                        hasSource("Kontoauszug31.txt"), hasNote("ssdfgdsg.dg*dfgdfgd"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-07"), hasAmount("EUR", 5.00),
                        hasSource("Kontoauszug31.txt"), hasNote("slH*PHJv AGWQDEib"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-08"), hasAmount("EUR", 14.64),
                        hasSource("Kontoauszug31.txt"), hasNote("NxZoZ FPEBxaxYHxEv"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-09"), hasAmount("EUR", 3.00),
                        hasSource("Kontoauszug31.txt"), hasNote("BdSuGjcvjM JtiqCrHrewhNHO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-09"), hasAmount("EUR", 32.93),
                        hasSource("Kontoauszug31.txt"), hasNote("vwJx BYlo"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-11"), hasAmount("EUR", 25.95),
                        hasSource("Kontoauszug31.txt"), hasNote("mUd*rAFw bzXrShpE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-28"), hasAmount("EUR", 24.43),
                        hasSource("Kontoauszug31.txt"), hasNote("KNhf dsoW"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-28"), hasAmount("EUR", 100.00),
                        hasSource("Kontoauszug31.txt"), hasNote("JVtffogi FIlx qd XdIEcrG"))));
    }

    @Test
    public void testKontoauszug32()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug32.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-03-18"), hasAmount("EUR", 7.33),
                        hasSource("Kontoauszug32.txt"), hasNote("Sepa Direct Debit transfer to Vodafone West GmbH"))));
    }

    @Test
    public void testKontoauszug33()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug33.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-06-07"), hasAmount("EUR", 42.89),
                        hasSource("Kontoauszug33.txt"), hasNote("_BAZG VIA WebShop"))));

        assertThat(results, hasItem(removal(hasDate("2025-06-07"), hasAmount("EUR", 0.93),
                        hasSource("Kontoauszug33.txt"), hasNote("GENVOICE.COM"))));

        assertThat(results, hasItem(removal(hasDate("2025-06-08"), hasAmount("EUR", 26.18),
                        hasSource("Kontoauszug33.txt"), hasNote("Schloss Laufen Rheinfall"))));

        assertThat(results, hasItem(removal(hasDate("2025-06-11"), hasAmount("EUR", 21.84),
                        hasSource("Kontoauszug33.txt"), hasNote("Lidl sagt Danke"))));
    }

    @Test
    public void testKontoauszug34()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug34.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-08"), hasAmount("EUR", 313.42),
                        hasSource("Kontoauszug34.txt"), hasNote("Vorname Nachname"))));

        assertThat(results, hasItem(removal(hasDate("2025-06-08"), hasAmount("EUR", 10.68),
                        hasSource("Kontoauszug34.txt"), hasNote("Schloss Laufen Rheinfall"))));

        assertThat(results, hasItem(removal(hasDate("2025-06-08"), hasAmount("EUR", 9.62),
                        hasSource("Kontoauszug34.txt"), hasNote("Schloss Laufen Rheinfall"))));

        assertThat(results, hasItem(removal(hasDate("2025-06-29"), hasAmount("EUR", 12.90),
                        hasSource("Kontoauszug34.txt"), hasNote("TROELSCH GMBH BAECKERE"))));
    }

    @Test
    public void testKontoauszug35()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug35.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-11-01"), hasAmount("EUR", 0.55), //
                                        hasSource("Kontoauszug35.txt"), hasNote(null)))));

        assertThat(results, hasItem(deposit(hasDate("2025-11-03"), hasAmount("EUR", 0.01),
                        hasSource("Kontoauszug35.txt"), hasNote("Your Kindergeld bonus"))));
    }

    @Test
    public void testKontoauszug36()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug36.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction

        assertThat(results, hasItem(deposit(hasDate("2025-11-21"), hasAmount("EUR", 58.36),
                        hasSource("Kontoauszug36.txt"), hasNote("Crypto one percent bonus compensation for orderId: 0a000a0a-a000-000a-a000-00a000a0a0a0"))));

        assertThat(results, hasItem(deposit(hasDate("2025-11-26"), hasAmount("EUR", 93.99),
                        hasSource("Kontoauszug36.txt"), hasNote("Crypto one percent bonus compensation for orderId: 0a00000a-a000-000a-a000-00a000a0a0a0"))));

        assertThat(results, hasItem(deposit(hasDate("2025-11-28"), hasAmount("EUR", 2000.00),
                        hasSource("Kontoauszug36.txt"), hasNote("Incoming transfer from Vorname Nachname (DE00000000000000000000)"))));
    }

    @Test
    public void testKontoauszug37()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug37.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction

        assertThat(results, hasItem(deposit(hasDate("2025-12-01"), hasAmount("EUR", 4.71),
                        hasSource("Kontoauszug37.txt"), hasNote("Cash reward allocation"))));


        assertThat(results, hasItem(removal(hasDate("2025-12-31"), hasAmount("EUR", 1800.00),
                        hasSource("Kontoauszug37.txt"), hasNote("Outgoing transfer for Vorname Nachname (DE00000000000000000000)"))));
    }

    @Test
    public void testKontoauszug38()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug38.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction

        assertThat(results, hasItem(deposit(hasDate("2025-12-01"), hasAmount("EUR", 0.06),
                        hasSource("Kontoauszug38.txt"), hasNote("Cash reward allocation"))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-12-01"), hasAmount("EUR", 1.17), //
                                        hasSource("Kontoauszug38.txt"), hasNote(null)))));
    }

    @Test
    public void testEstrattoContoRiassuntivo01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoContoRiassuntivo01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-09-01"), hasAmount("EUR", 1.75), //
                                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-09-01"), hasAmount("EUR", 3000.00),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("Incoming transfer from eraE IQRKrwr"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-07"), hasAmount("EUR", 29.44),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("TODIS SUPERMERCATO TODIS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-07"), hasAmount("EUR", 24.00),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("SUMUP *GUSTO - SRLS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-09"), hasAmount("EUR", 1.70),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("C.G.I. C-O ENEL EGEO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-10"), hasAmount("EUR", 2.20),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("C.G.I. C-O ENEL EGEO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-14"), hasAmount("EUR", 18.00),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("2000 EVENTI RISTORAZIONE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-14"), hasAmount("EUR", 7.00),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("PARAD ICE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-15"), hasAmount("EUR", 34.20),
                        hasSource("EstrattoContoRiassuntivo01.txt"), hasNote("Glovo 14SEP NT5MYVDKR"))));
    }

    @Test
    public void testReleveDeCompte01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-07-01"), hasAmount("EUR", 1.24), //
                                        hasSource("ReleveDeCompte01.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-01"), hasAmount("EUR", 500.00), //
                        hasSource("ReleveDeCompte01.txt"), hasNote("Paiement accepté: DE98200411330722961000 à DE21502109007019521081"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-28"), hasAmount("EUR", 100.00), //
                        hasSource("ReleveDeCompte01.txt"), hasNote("Apple Pay Top up"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-29"), hasAmount("EUR", 500.00), //
                        hasSource("ReleveDeCompte01.txt"), hasNote("Paiement accepté: DE98200411330722961000 à DE21502109007019521081"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-08-01"), hasAmount("EUR", 1.30), //
                                        hasSource("ReleveDeCompte01.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-01"), hasAmount("EUR", 500.00), //
                        hasSource("ReleveDeCompte01.txt"), hasNote(null))));
    }

    @Test
    public void testReleveDeCompte02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-09-01"), hasAmount("EUR", 0.28), //
                                        hasSource("ReleveDeCompte02.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-02"), hasAmount("EUR", 100.00), //
                        hasSource("ReleveDeCompte02.txt"), hasNote("Paiement accepté: FR7634047446300402044310454 à DE55402154005012458754"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-24"), hasAmount("EUR", 50.00), //
                        hasSource("ReleveDeCompte02.txt"), hasNote("Paiement accepté: FR7634047446300402044310454 à DE55402154005012458754"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-28"), hasAmount("EUR", 10.00), //
                        hasSource("ReleveDeCompte02.txt"), hasNote("Card Top up with ****7853"))));
    }

    @Test
    public void testReleveDeCompte03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReleveDeCompte03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-12"), hasAmount("EUR", 100.00), //
                        hasSource("ReleveDeCompte03.txt"), hasNote("Paiement accepté: FR7640047144300402054310442 à DE55402154005012458754"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-16"), hasAmount("EUR", 25.57), //
                        hasSource("ReleveDeCompte03.txt"), hasNote("Remboursement de votre cadeau"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-11-01"), hasAmount("EUR", 0.18), //
                                        hasSource("ReleveDeCompte03.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-12-01"), hasAmount("EUR", 0.18), //
                                        hasSource("ReleveDeCompte03.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2023-12-29"), hasAmount("EUR", 0.05), //
                                        hasSource("ReleveDeCompte03.txt"), hasNote(null)))));
    }

    @Test
    public void testTransaccionesDeCuenta01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(110L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(110));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-05-01"), hasAmount("EUR", 26.13), //
                                        hasSource("TransaccionesDeCuenta01.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-02"), hasAmount("EUR", 2600.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-02"), hasAmount("EUR", 30.99), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WATSON RESTAURANTS"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-02"), hasAmount("EUR", 3.89), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-03"), hasAmount("EUR", 100.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CALLE BLESA 38"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-03"), hasAmount("EUR", 100.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CALLE BLESA 38"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-03"), hasAmount("EUR", 8000.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-04"), hasAmount("EUR", 23.10), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("KOSKA TAVERNA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-05"), hasAmount("EUR", 49.90), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Amazon Prime*HG5HI9V74"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-05"), hasAmount("EUR", 19.36), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("SABADELL - GRAN VIA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-07"), hasAmount("EUR", 14.44), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("AMAZON* 404-7152465-80"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-08"), hasAmount("EUR", 12.88), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("AMAZON* 404-0339054-87"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-08"), hasAmount("EUR", 5.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("BOCAARTE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-09"), hasAmount("EUR", 274.15), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-09"), hasAmount("EUR", 7.04), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("AMAZON* 404-4939695-24"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-11"), hasAmount("EUR", 105.64), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-12"), hasAmount("EUR", 300.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CALLE BLESA 38"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-12"), hasAmount("EUR", 200.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CALLE BLESA 38"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-12"), hasAmount("EUR", 300.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CALLE BLESA 38"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-12"), hasAmount("EUR", 18.33), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *CABIFY"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-12"), hasAmount("EUR", 192.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-14"), hasAmount("EUR", 15.16), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("McDonalds 72400528"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-14"), hasAmount("EUR", 25.77), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *ALIPAY EUR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-14"), hasAmount("EUR", 8.24), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("AMAZON* 404-2912865-61"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-16"), hasAmount("EUR", 148.96), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("AMAZON* 404-7202126-78"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-16"), hasAmount("EUR", 29.49), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WWW.AMAZON* 404-625052"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-20"), hasAmount("EUR", 6.81), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("BACKBLAZE INC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-21"), hasAmount("EUR", 2.92), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WAYLET BENEFICIOS REPSOL"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-24"), hasAmount("EUR", 14.15), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("MERCADONA CAN ROSELL"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-24"), hasAmount("EUR", 61.23), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WETACA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-24"), hasAmount("EUR", 4.50), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("RESTAURANT LA BORDA 2000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-25"), hasAmount("EUR", 4.66), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *ALIPAY EUR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-28"), hasAmount("EUR", 8.98), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WWW.AMAZON* 404-464344"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-29"), hasAmount("EUR", 3.98), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WAYLET BENEFICIOS REPSOL"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-29"), hasAmount("EUR", 2500.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-31"), hasAmount("EUR", 70.89), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WETACA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-31"), hasAmount("EUR", 35.86), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Nori - Sushi Cocktail"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-01"), hasAmount("EUR", 6.49), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("WETACA"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-06-01"), hasAmount("EUR", 32.61), //
                                        hasSource("TransaccionesDeCuenta01.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-01"), hasAmount("EUR", 20.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-02"), hasAmount("EUR", 182.90), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *HENNESMAURI"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-03"), hasAmount("EUR", 2212.33), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-03"), hasAmount("EUR", 8.57), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-04"), hasAmount("EUR", 13.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-04"), hasAmount("EUR", 3.50), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("TURIN LABORATORIO DI GELA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-05"), hasAmount("EUR", 17.80), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("RUSTIC BASEMENT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-06"), hasAmount("EUR", 46.64), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("DELICATESSEN ARGENTINA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-06"), hasAmount("EUR", 3.50), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("TURIN LABORATORIO DI GELA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-07"), hasAmount("EUR", 10.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-07"), hasAmount("EUR", 13.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-11"), hasAmount("EUR", 9.99), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-11"), hasAmount("EUR", 25.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-11"), hasAmount("EUR", 0.60), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("D PARKING SOCIEDAD CIVIL"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-12"), hasAmount("EUR", 50.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-12"), hasAmount("EUR", 34.99), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Gympass"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-13"), hasAmount("EUR", 180.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-14"), hasAmount("EUR", 29.97), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *ALIPAY EUR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-15"), hasAmount("EUR", 15.59), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("BARCELONA - PARALELO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-15"), hasAmount("EUR", 440.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CASHZONE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-15"), hasAmount("EUR", 450.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CASHZONE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-17"), hasAmount("EUR", 101.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("RESTAURANT CAN FARELL, S."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-18"), hasAmount("EUR", 250.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-18"), hasAmount("EUR", 2.50), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("TURIN LABORATORIO DI GELA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-18"), hasAmount("EUR", 75.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-19"), hasAmount("EUR", 3.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("HOTEL MARIVELLA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-19"), hasAmount("EUR", 22.60), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("DE PAULA HAMBURGUESERIA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-19"), hasAmount("EUR", 8.90), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("CAF. RTE. EL CISNE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-20"), hasAmount("EUR", 30.99), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAGATELIA SLU"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-20"), hasAmount("EUR", 10.20), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-20"), hasAmount("EUR", 97.85), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("SUSHITA GREEN"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-20"), hasAmount("EUR", 1.80), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PARKING MORALEJA GREEN"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-21"), hasAmount("EUR", 6.68), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *OCTOPUSELEC OC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-21"), hasAmount("EUR", 18.87), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-21"), hasAmount("EUR", 4.10), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("ALBISA QUETI"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-21"), hasAmount("EUR", 70.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("ROBERTO BESTEIRO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-21"), hasAmount("EUR", 5.60), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("APARCAMIENTO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-22"), hasAmount("EUR", 13.70), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("BODEGA CUZCO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-22"), hasAmount("EUR", 43.95), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("ALBISA QUETI"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-22"), hasAmount("EUR", 7.63), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("BACKBLAZE INC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-23"), hasAmount("EUR", 23.78), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-23"), hasAmount("EUR", 6.70), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("HELADERIA MENTA Y LIMON"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-24"), hasAmount("EUR", 1.30), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("POPPY PARK SALAMANCA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-24"), hasAmount("EUR", 19.10), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("LOS CAPRICHOS DE MENESES"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-25"), hasAmount("EUR", 1050.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-25"), hasAmount("EUR", 29.50), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("MONTEFER"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-25"), hasAmount("EUR", 10.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("DANIA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-25"), hasAmount("EUR", 3500.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-26"), hasAmount("EUR", 5.45), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PARKING COLON PZA MAYOR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-28"), hasAmount("EUR", 158.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("HAYQUEESTAR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-29"), hasAmount("EUR", 23.68), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-29"), hasAmount("EUR", 13.96), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("LECLERC SALAMANCA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-29"), hasAmount("EUR", 17.70), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("RTE. MONTERO CASA COMIDAS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-29"), hasAmount("EUR", 8.20), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("EL CARACOL DEL BIERZO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-29"), hasAmount("EUR", 17.30), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("OXLO A LO CANALLA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-30"), hasAmount("EUR", 16.32), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-07-01"), hasAmount("EUR", 34.97), //
                                        hasSource("TransaccionesDeCuenta01.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-01"), hasAmount("EUR", 16.72), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-01"), hasAmount("EUR", 28.40), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("BONAREA TORREFARRERA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-02"), hasAmount("EUR", 12.71), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("PAYPAL *OCTOPUSELEC OC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-02"), hasAmount("EUR", 34.60), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("FICUS&PERSICA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-02"), hasAmount("EUR", 10.46), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-03"), hasAmount("EUR", 30.00), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-03"), hasAmount("EUR", 138.50), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("RINCON TIO GERONIMO"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-04"), hasAmount("EUR", 44.40), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("AREA 103"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-04"), hasAmount("EUR", 275.34), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Ingreso aceptado: ES00000000000000000000000 a DE00000000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-06"), hasAmount("EUR", 41.31), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("ELIE PEIXETERS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-06"), hasAmount("EUR", 4.35), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("FRUITES I VERDURES MINYON"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-06"), hasAmount("EUR", 42.55), //
                        hasSource("TransaccionesDeCuenta01.txt"), hasNote("Revolut**9033*"))));
    }

    @Test
    public void testTransaccionesDeCuenta02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(255L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(9L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(255));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-02"), hasAmount("EUR", 2250.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Ingreso aceptado: ES0000000000000000000000 a DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-26"), hasAmount("EUR", 3800.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Ingreso aceptado: ES0000000000000000000000 a DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-02-01"), hasAmount("EUR", 900.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-02-01"), hasAmount("EUR", 10.07), //
                                        hasSource("TransaccionesDeCuenta02.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-02-08"), hasAmount("EUR", 300.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-02-18"), hasAmount("EUR", 1072.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-03-01"), hasAmount("EUR", 14.48), //
                                        hasSource("TransaccionesDeCuenta02.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-05"), hasAmount("EUR", 5809.91), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-20"), hasAmount("EUR", 1400.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Ingreso aceptado: ES0000000000000000000000 a DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-26"), hasAmount("EUR", 5000.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Ingreso aceptado: ES0000000000000000000000 a DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-04-01"), hasAmount("EUR", 7.79), //
                                        hasSource("TransaccionesDeCuenta02.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-04-03"), hasAmount("EUR", 5.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Trade Republic Card"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-08"), hasAmount("EUR", 75.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("E.S.BALLUS BERGA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-08"), hasAmount("EUR", 9.96), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Reembolso por tu regalo"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-11"), hasAmount("EUR", 8.95), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("FARMACIA IGNACIO JANE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-13"), hasAmount("EUR", 9.60), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PAYPAL *BITWARDEN"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-15"), hasAmount("EUR", 111.70), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("CASTELL DE ROSANES"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-16"), hasAmount("EUR", 34.99), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PAYPAL *WALLAPOP WALLA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-17"), hasAmount("EUR", 2700.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Ingreso aceptado: ES0000000000000000000000 a DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-18"), hasAmount("EUR", 2600.00), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PayOut to transit"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-20"), hasAmount("EUR", 7.16), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("BACKBLAZE INC"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-23"), hasAmount("EUR", 10.91), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("PAYPAL *COLEGIODERE"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-24"), hasAmount("EUR", 1612.41), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("Ingreso aceptado: ES0000000000000000000000 a DE00000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-20"), hasAmount("EUR", 6.81), //
                        hasSource("TransaccionesDeCuenta02.txt"), hasNote("BACKBLAZE INC"))));
    }

    @Test
    public void testTransaccionesDeCuenta03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(19L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-12-02"), hasAmount("EUR", 113.59), //
                                        hasSource("TransaccionesDeCuenta03.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-02"), hasAmount("EUR", 15.00), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-11"), hasAmount("EUR", 9.99), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-11"), hasAmount("EUR", 32.00), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("PERRUQUERIA ANGEL MANCEBO"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-12"), hasAmount("EUR", 54.56), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("PAYPAL *ZALANDOSE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-13"), hasAmount("EUR", 9.00), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("TAXI LLIC. 570"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-22"), hasAmount("EUR", 54.06), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("RESTAURANTE ELIM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-23"), hasAmount("EUR", 1100.00), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("Revolut**3808*"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-27"), hasAmount("EUR", 5900.00), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("Ingreso aceptado: ES1235830002290012888722 a JP99672109888010680123"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-28"), hasAmount("EUR", 132.35), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("MONTEFER"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-29"), hasAmount("EUR", 9.20), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("Tesla Spain, S.L."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-29"), hasAmount("EUR", 0.20), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("APARCAMIENTO PALENCIA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 28.08), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("IBERDROLA SMART MOBILITY"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 95.00), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("MESON DE LA DOLORES"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 19.70), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("FARMACIA CENTRAL"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 12.85), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("WWW.ENDESAX.COM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 9.60), //
                        hasSource("TransaccionesDeCuenta03.txt"), hasNote("WAYLET"))));
    }

    @Test
    public void testTransaccionesDeCuenta04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-11-01"), hasAmount("EUR", 124.66), //
                                        hasSource("TransaccionesDeCuenta04.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2024-12-02"), hasAmount("EUR", 113.59), //
                                        hasSource("TransaccionesDeCuenta04.txt"), hasNote(null)))));

    }

    @Test
    public void testTransaccionesDeCuenta05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-03-01"), hasAmount("EUR", 2.29), //
                                        hasSource("TransaccionesDeCuenta05.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-03-02"), hasAmount("EUR", 1.00), //
                        hasSource("TransaccionesDeCuenta05.txt"), hasNote("HrhUNwjjL WMXjOiKn LL"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-03"), hasAmount("EUR", 1425.00), //
                        hasSource("TransaccionesDeCuenta05.txt"), hasNote("Ingreso aceptado: PX12345678901234567890 a PX12345678901234567890"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-03"), hasAmount("EUR", 2.69), //
                        hasSource("TransaccionesDeCuenta05.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-03-22"), hasAmount("EUR", 51.00), //
                        hasSource("TransaccionesDeCuenta05.txt"), hasNote("PayOut to transit"))));
    }

    @Test
    public void testTransaccionesDeCuenta06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-02-01"), hasAmount("EUR", 115.90), //
                                        hasSource("TransaccionesDeCuenta06.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-01"), hasAmount("EUR", 20.00), //
                        hasSource("TransaccionesDeCuenta06.txt"), hasNote("LA MARMOTTE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-01"), hasAmount("EUR", 49.50), //
                        hasSource("TransaccionesDeCuenta06.txt"), hasNote("R A S L"))));
    }

    @Test
    public void testTransaccionesDeCuenta07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-06-01"), hasAmount("EUR", 28.91), //
                                        hasSource("TransaccionesDeCuenta07.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-19"), hasAmount("EUR", 10000.00), //
                        hasSource("TransaccionesDeCuenta07.txt"), hasNote("Incoming transfer from YHjZyNS IQufQTD wPaldIXA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-23"), hasAmount("EUR", 15000.00), //
                        hasSource("TransaccionesDeCuenta07.txt"), hasNote("Incoming transfer from lMsasPJ reIAvsN mAFbBROD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-06-27"), hasAmount("EUR", 10.71), //
                        hasSource("TransaccionesDeCuenta07.txt"), hasNote("WWW.AMAZON.* 646JH4Q44"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-06-27"), hasAmount("EUR", 50.00), //
                        hasSource("TransaccionesDeCuenta07.txt"), hasNote("PaKJZfD qMtbDX QuKXLV"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-06-30"), hasAmount("EUR", 5.98), //
                        hasSource("TransaccionesDeCuenta07.txt"), hasNote("CLAREL"))));
    }

    @Test
    public void testTransaccionesDeCuenta08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-08-01"), hasAmount("EUR", 47.64), //
                                        hasSource("TransaccionesDeCuenta08.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-01"), hasAmount("EUR", 119.75), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("hiVhai"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-02"), hasAmount("EUR", 7.94), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("COOP. IcaSem lLoww"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-02"), hasAmount("EUR", 20.00), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("JtwYTd FKqXQ bEudwBb"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-03"), hasAmount("EUR", 20.00), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("Incoming transfer from vyZLCu nmjitnt LwTujL"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-04"), hasAmount("EUR", 10.38), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-09"), hasAmount("EUR", 51.04), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("MwjqMZ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-15"), hasAmount("EUR", 56.82), //
                        hasSource("TransaccionesDeCuenta08.txt"), hasNote("APPLUS YBRMgv uEYJ MaMGLK"))));
    }

    @Test
    public void testAccountStatementSummary01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatementSummary01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-20"), hasAmount("EUR", 100.00), //
                        hasSource("AccountStatementSummary01.txt"), hasNote("Google Pay Top up"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-24"), hasAmount("EUR", 100.00), //
                        hasSource("AccountStatementSummary01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-24"), hasAmount("EUR", 10.01), //
                        hasSource("AccountStatementSummary01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-26"), hasAmount("EUR", 15.99), //
                        hasSource("AccountStatementSummary01.txt"), hasNote(null))));
    }

    @Test
    public void testAccountStatementSummary02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatementSummary02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest(hasDate("2025-11-01"), hasAmount("EUR", 53.33), //
                                        hasSource("AccountStatementSummary02.txt"), hasNote(null)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-02"), hasAmount("EUR", 23.53), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("ALIEXPRESS.COM"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-02"), hasAmount("EUR", 19.90), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("PAYPAL *URBANSPORTS"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-03"), hasAmount("EUR", 20.00), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("FUNDACION TIERRA DE HOMBRES"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-03"), hasAmount("EUR", 108.74), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("CAL MAJORAL"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-03"), hasAmount("EUR", 10.12), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("Your Saveback payment"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-06"), hasAmount("EUR", 6.99), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("PAYPAL *ITUNESAPPST APPLE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-07"), hasAmount("EUR", 13.90), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("3.OCIO 2025"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-08"), hasAmount("EUR", 6.99), //
                        hasSource("AccountStatementSummary02.txt"), hasNote("PAYPAL *ITUNESAPPST APPLE"))));
    }

    @Test
    public void testTransaccionesDeCuenta09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TransaccionesDeCuenta09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-22"), hasAmount("EUR", 1000.00), //
                        hasSource("TransaccionesDeCuenta09.txt"), hasNote("eBxhX eBxhX eBxhX"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-26"), hasAmount("EUR", 200.00), //
                        hasSource("TransaccionesDeCuenta09.txt"), hasNote("1234567812345673456444 eBxhX eBxhX"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-09-29"), hasAmount("EUR", 315.00), //
                        hasSource("TransaccionesDeCuenta09.txt"), hasNote("1234567812345673456444 eBxhX eBxhX"))));
    }

    @Test
    public void testSteuerabrechnung01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Steuerabrechnung01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(3.75 + 0.21 + 0.30))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(3.75 + 0.21 + 0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSteuerabrechnung02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HS33QJ0"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC Trinkaus & Burkhardt GmbH TurboC O.End salesfor"), //
                        hasCurrencyCode("EUR"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-04-11T00:00"), hasShares(400.00), //
                        hasSource("Steuerabrechnung02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.21), hasGrossValue("EUR", 2.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerabrechnung03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerabrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("iShares plc - iShares Core MSCI EM IMI UCITS ETF USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-09-01T00:00"), hasShares(0.00), //
                        hasSource("Steuerabrechnung03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.65), hasGrossValue("EUR", 5.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerabrechnung04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerabrechnung04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B652H904"), hasWkn(null), hasTicker(null), //
                        hasName("iShares V plc - iShares EM Dividend UCITS ETF USD (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-10-09T00:00"), hasShares(0.00), //
                        hasSource("Steuerabrechnung04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.96), hasGrossValue("EUR", 0.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteueroptimierung01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steueroptimierung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-07-18T00:00"), //
                        hasSource("Steueroptimierung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 776.13), hasGrossValue("EUR", 776.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteueroptimierung02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steueroptimierung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-07-30T00:00"), //
                        hasSource("Steueroptimierung02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 20.72), hasGrossValue("EUR", 20.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteueroptimierung03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steueroptimierung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-09-28T00:00"), //
                        hasSource("Steueroptimierung03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerkorrektur01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerkorrektur01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US92936U1097"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("W.P. Carey Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-11-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(38.4597)));
        assertThat(transaction.getSource(), is("Steuerkorrektur01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(6.30))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(6.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(6.94))));
    }

    @Test
    public void testStornoSteuerkorrektur01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "StornoSteuerkorrektur01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92936U1097"), hasWkn(null), hasTicker(null), //
                        hasName("W.P. Carey Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2023-11-02T00:00"), hasShares(38.4597), //
                                        hasSource("StornoSteuerkorrektur01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 6.30), hasGrossValue("EUR", 6.30), //
                                        hasForexGrossValue("USD", 6.94), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testStornoSteuerkorrektur01WithSecurityInUSD()
    {
        var security = new Security("W.P. Carey Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US92936U1097");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "StornoSteuerkorrektur01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2023-11-02T00:00"), hasShares(38.4597), //
                                        hasSource("StornoSteuerkorrektur01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 6.30), hasGrossValue("EUR", 6.30), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            var c = new CheckCurrenciesAction();
                                            var account = new Account();
                                            account.setCurrencyCode("EUR");
                                            var s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AU000000CUV3"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Clinuvel Pharmaceuticals Ltd. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-06-18T17:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Order: 55a8-39ad | Ausführung: 051a-e65e"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1792.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1825.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(30.63 + 1.68))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000TR95XU9"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG Call 15.12.21 DAX 14000"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-10T11:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(500)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Order: 1234-1234 | Ausführung: 1234-1234"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(3594.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(3595.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-10T11:42")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getSource(), is("Verkauf02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(21.63))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(21.63))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007100000"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Daimler AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-21T09:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Order: c17d-baea | Ausführung: 6415-fd77"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1199.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-21T09:30")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
        assertThat(transaction.getSource(), is("Verkauf03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(139.58))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(139.58))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M62Q58"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-12T12:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0068)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Order: 36de-8883 | Ausführung: f439-3735"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(0.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.29))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A3H2333"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HAMBORNER REIT AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-26T11:44")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3632)));
        assertThat(entry.getSource(), is("Verkauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(3.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A3H23V7"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Epigenomics AG Inhaber-Bezugsrechte"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-25T11:32")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16)));
        assertThat(entry.getSource(), is("Verkauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(0.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.34))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check fee transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T11:32")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getSource(), is("Verkauf06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(1.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(1.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0006070006"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HOCHTIEF AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-23T21:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9)));
        assertThat(entry.getSource(), is("Verkauf07.txt"));
        assertThat(entry.getNote(), is("Order: 5a93-03d1 | Ausführung: b11c-12c4"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(623.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(624.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-23T21:10")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(9)));
        assertThat(transaction.getSource(), is("Verkauf07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(4.56 + 0.25))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(4.56 + 0.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US6700024010"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Novavax Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-03T16:49")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Verkauf08.txt"));
        assertThat(entry.getNote(), is("Order: 886a-ffff | Ausführung: dcd2-ffff"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(301.88))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(302.88))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-03T16:49")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(2)));
        assertThat(transaction.getSource(), is("Verkauf08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(12.65 + 0.69))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(12.65 + 0.69))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("Coca-Cola"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-10T12:35"), hasShares(0.286887), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Order: a00a-0000 | Ausführung: 000a-aa00"), //
                        hasAmount("EUR", 17.00), hasGrossValue("EUR", 17.07), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HG6HZ94"), hasWkn(null), hasTicker(null), //
                        hasName("Long @49.99 $ Monster Beverage Open End Turbo"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-11T16:00"), hasShares(1000.00), //
                        hasSource("Verkauf10.txt"), //
                        hasNote("Order: 55e1-4f5a | Ausführung: b02a-06c8"), //
                        hasAmount("EUR", 149.00), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010755611"), hasWkn(null), hasTicker(null), //
                        hasName("Lev MSCI USA EUR (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        sale( //
                                        hasDate("2025-01-17T16:05"), hasShares(200.00), //
                                        hasSource("Verkauf11.txt"), //
                                        hasNote("Order: 0085-7d2e | Ausführung: e13e-6ec7"), //
                                        hasAmount("EUR", 165.42), hasGrossValue("EUR", 330.84), //
                                        hasTaxes("EUR", 165.42), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SQ6QKU9"), hasWkn(null), hasTicker(null), //
                        hasName("Société Générale Effekten GmbH MiniL O.End"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-05-02T18:18"), hasShares(490.00), //
                        hasSource("Sell01.txt"), //
                        hasNote("Order: 1778-101b | Execution: eiX5-F5D7"), //
                        hasAmount("EUR", 433.14), hasGrossValue("EUR", 434.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testVenta01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Venta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008019001"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Pfandbriefbank AG "), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-15T16:15"), hasShares(100.00), //
                        hasSource("Venta01.txt"), //
                        hasNote("Orden: 4152-cdd0 | Ejecución: f46e-e2ae"), //
                        hasAmount("EUR", 387.20), hasGrossValue("EUR", 388.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testVente01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vente01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4TW784"), hasWkn(null), hasTicker(null), //
                        hasName("WisdomTree Multi Ass.Iss.PLC ETP 30.11.62 3X Short Daily"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-16T16:41"), hasShares(0.477676), //
                        hasSource("Vente01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 53.70), hasGrossValue("EUR", 53.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRepayment01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Repayment01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SQ728J8"), hasWkn(null), hasTicker(null), //
                        hasName("Société Générale Effekten GmbH MiniL O.End"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-06T00:00"), hasShares(500.00), //
                        hasSource("Repayment01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 35.00), hasGrossValue("EUR", 35.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTilgung01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tilgung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000TT22GS8"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG TurboC O.End Linde"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(700)));
        assertThat(entry.getSource(), is("Tilgung01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(0.70))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(700)));
        assertThat(transaction.getSource(), is("Tilgung01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(29.24 + 1.61 + 2.34))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(29.24 + 1.61 + 2.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTilgung02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tilgung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE1234567891"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG TurboP O.End ShopApEu"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
        assertThat(entry.getSource(), is("Tilgung02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(0.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testAusbuchung01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ausbuchung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SU34220"), hasWkn(null), hasTicker(null), //
                        hasName("TUI AG Best Turbo"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-30T00:00"), hasShares(2053.00), //
                        hasSource("Ausbuchung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.05), hasGrossValue("EUR", 2.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendeStorno01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("Realty Income Corp. Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-02-26T00:00"), hasShares(200), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 30.17), hasGrossValue("EUR", 40.61), //
                                        hasForexGrossValue("USD", 45.10), //
                                        hasTaxes("EUR", ((6.81 / 1.1105) + 4.09 + 0.22)), hasFees("EUR", 0.00)))));

        // check tax refund transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2020-02-26T00:00"), hasShares(200), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.36), hasGrossValue("EUR", 0.36), //
                                        hasForexGrossValue("USD", 0.40), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno01WithSecurityInEUR()
    {
        var security = new Security("Realty Income Corp. Registered Shares DL 1", "EUR");
        security.setIsin("US7561091049");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-02-26T00:00"), hasShares(200), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 30.17), hasGrossValue("EUR", 40.61), //
                                        hasTaxes("EUR", ((6.81 / 1.1105) + 4.09 + 0.22)), hasFees("EUR", 0.00)))));

        // check tax refund transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2020-02-26T00:00"), hasShares(200), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.36), hasGrossValue("EUR", 0.36), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US36162J1060"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("GEO Group Inc., The Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check cancellation (Storno) transaction
        var cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(),
                        is(LocalDateTime.parse("2021-05-25T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(79)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DividendeStorno02.txt"));
        assertNull(((Transaction) cancellation.getSubject()).getNote());

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(29.66))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(34.90))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(5.24))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = ((Transaction) cancellation.getSubject()).getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(37.92))));
    }

    @Test
    public void testDividendeStorno03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL482"), hasWkn(null), hasTicker(null), //
                        hasName("Euro iSTOXX ex FIN"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2024-01-01T00:00"), hasShares(1.525759), //
                                        hasSource("DividendeStorno03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.25), hasGrossValue("EUR", 0.33), //
                                        hasTaxes("EUR", 0.08), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B652H904"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsV-EM Dividend UCITS ETF Registered Shares USD o.N."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(4.18))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(5.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.89 + 0.04))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(5.63))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("iShsV-EM Dividend UCITS ETF Registered Shares USD o.N.", "EUR");
        security.setIsin("IE00B652H904");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(4.18))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(5.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.89 + 0.04))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5949181045"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Microsoft Corp. Registered Shares DL-,00000625"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(3.10))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(4.16))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.62 + 0.42 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(4.60))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("Microsoft Corp. Registered Shares DL-,00000625", "EUR");
        security.setIsin("US5949181045");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(3.10))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(4.16))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.62 + 0.42 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US09247X1019"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Blackrock Inc. Reg. Shares Class A DL -,01"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.96))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.45 + 0.28 + 0.01 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(3.30))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("Blackrock Inc. Reg. Shares Class A DL -,01", "EUR");
        security.setIsin("US09247X1019");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.96))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.45 + 0.28 + 0.01 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UH1"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(1.63))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(1.63))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US8754651060"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Tanger Fact.Outlet Centrs Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(142)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(39.21))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(46.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(6.92))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(50.41))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("Tanger Fact.Outlet Centrs Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US8754651060");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(142)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(39.21))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(46.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(6.92))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00B03MLX29"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Royal Dutch Shell Reg. Shares Class A EO -,07"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(50.30))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(67.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(10.15 + 6.90 + 0.40))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(80.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("Royal Dutch Shell Reg. Shares Class A EO -,07", "EUR");
        security.setIsin("GB00B03MLX29");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(50.30))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(67.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(10.15 + 6.90 + 0.40))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US9314271084"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Walgreens Boots Alliance Inc. Reg. Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.3272)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(1.09))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(1.56))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        var security = new Security("Walgreens Boots Alliance Inc. Reg. Shares DL -,01", "EUR");
        security.setIsin("US9314271084");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.3272)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(1.09))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1FZS574"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsII-MSCI Turkey UCITS ETF Registered Shs USD (Dist) o.N."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(124.0903)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(24.61))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(33.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(8.36 + 0.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(41.04))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        var security = new Security("iShsII-MSCI Turkey UCITS ETF Registered Shs USD (Dist) o.N.", "EUR");
        security.setIsin("IE00B1FZS574");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(124.0903)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(24.61))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(33.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(8.36 + 0.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005785604"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Fresenius SE & Co. KGaA Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4.3521)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.83))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(3.83))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.95 + 0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FI0009005961"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Stora Enso Oyj Reg. Shares Cl.R EO 1,70"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(3.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(1.13 + 0.37 + 0.02 + 0.03))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FI0009005987"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UPM Kymmene Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
        assertThat(transaction.getSource(), is("Dividende11.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(45.50))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(65.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(19.50))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00BH4HKS39"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vodafone Group PLC Registered Shares DL 0,2095238"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(699)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(31.43))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(31.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(26.80))));

        // check for the reinvestment of dividends
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getPortfolioTransaction().getSource(), is("Dividende12.txt"));
        assertNull(entry.getPortfolioTransaction().getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        var security = new Security("Vodafone Group PLC Registered Shares DL 0,2095238", "EUR");
        security.setIsin("GB00BH4HKS39");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(699)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(31.43))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(31.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check for the reinvestment of dividends
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getPortfolioTransaction().getSource(), is("Dividende12.txt"));
        assertNull(entry.getPortfolioTransaction().getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKBF6H24"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIII-Core MSCI World U.ETF Reg. Shares EUR Hgd (Dis) o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende13.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(1.92))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.35))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.41 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7960542030"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Samsung SDI Co. Ltd. Reg.Shs(Sp.GDRs 144A)/4 SW5000"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6.214744)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.76))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(1.23 / 1.0985))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.27 / 1.0958))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.12 / 1.0958))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(1.23))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        var security = new Security("Samsung SDI Co. Ltd. Reg.Shs(Sp.GDRs 144A)/4 SW5000", "EUR");
        security.setIsin("US7960542030");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6.214744)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.76))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(1.23 / 1.0985))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.27 / 1.0958))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.12 / 1.0958))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK1PV551"), hasWkn(null), hasTicker(null), //
                        hasName("MSCI World USD (Dist)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-07T00:00"), hasShares(123.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 99.99), hasGrossValue("EUR", 112.21), //
                        hasForexGrossValue("USD", 122.24), //
                        hasTaxes("EUR", 11.11 + 1.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        var security = new Security("MSCI World USD (Dist)", "EUR");
        security.setIsin("IE00BK1PV551");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-07T00:00"), hasShares(123.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 99.99), hasGrossValue("EUR", 112.21), //
                        hasTaxes("EUR", 11.11 + 1.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7181721090"), hasWkn(null), hasTicker(null), //
                        hasName("Philip Morris Internat. Inc. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-10T00:00"), hasShares(55), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 63.31), hasGrossValue("EUR", 63.31), //
                        hasForexGrossValue("USD", 69.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-01-10T00:00"), hasShares(55), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.43), hasGrossValue("EUR", 5.43), //
                        hasForexGrossValue("USD", 5.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16WithSecurityInEUR()
    {
        var security = new Security("Philip Morris Internat. Inc. Registered Shares o.N.", "EUR");
        security.setIsin("US7181721090");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-10T00:00"), hasShares(55), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 63.31), hasGrossValue("EUR", 63.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-01-10T00:00"), hasShares(55), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.43), hasGrossValue("EUR", 5.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-05-16T00:00"), hasShares(1.92086), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.38), hasGrossValue("EUR", 0.44), //
                        hasForexGrossValue("USD", 0.48), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("Apple Inc. Registered Shares o.N.", "EUR");
        security.setIsin("US0378331005");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-05-16T00:00"), hasShares(1.92086), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.38), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011683594"), hasWkn(null), hasTicker(null), //
                        hasName("Developed Markets Dividend Leaders EUR (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-12T00:00"), hasShares(90.537929), //
                        hasSource("Dividende18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 66.95), hasGrossValue("EUR", 78.77), //
                        hasTaxes("EUR", 11.82), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende19()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000PAH0038"), hasWkn(null), hasTicker(null), //
                        hasName("Porsche Holding"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(45.671650), //
                        hasSource("Dividende19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 116.92), hasGrossValue("EUR", 130.98), //
                        hasTaxes("EUR", 13.33 + 0.73), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-06-14T00:00"), hasShares(45.671650), //
                        hasSource("Dividende19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.33 + 0.73), hasGrossValue("EUR", 13.33 + 0.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("Realty Income"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(23.908066), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.97), hasGrossValue("EUR", 5.84), //
                        hasForexGrossValue("USD", 6.28), //
                        hasTaxes("EUR", 0.87), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20WithSecurityInEUR()
    {
        var security = new Security("Realty Income", "EUR");
        security.setIsin("US7561091049");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(23.908066), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.97), hasGrossValue("EUR", 5.84), //
                        hasTaxes("EUR", 0.87), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL482"), hasWkn(null), hasTicker(null), //
                        hasName("Euro iSTOXX ex FIN Dividend+ EUR (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-10T00:00"), hasShares(206.651869), //
                        hasSource("Dividende21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 79.76), hasGrossValue("EUR", 99.19), //
                        hasTaxes("EUR", 16.98 + 1.52 + 0.93), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende22()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007231334"), hasWkn(null), hasTicker(null), //
                        hasName("Sixt (Vz)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-17T00:00"), hasShares(1.552443), //
                        hasSource("Dividende22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.09), hasGrossValue("EUR", 6.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US91324P1021"), hasWkn(null), hasTicker(null), //
                        hasName("UnitedHealth"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-25T00:00"), hasShares(12.00), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.97), hasGrossValue("EUR", 23.49), //
                        hasForexGrossValue("USD", 25.20), //
                        hasTaxes("EUR", 3.78 / 1.073), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23WithSecurityInEUR()
    {
        var security = new Security("UnitedHealth", "EUR");
        security.setIsin("US91324P1021");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-25T00:00"), hasShares(12.00), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.97), hasGrossValue("EUR", 23.49), //
                        hasTaxes("EUR", 3.78 / 1.073), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK1PV551"), hasWkn(null), hasTicker(null), //
                        hasName("MSCI World USD (Dist)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-07T00:00"), hasShares(110.661875), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.44), hasGrossValue("EUR", 36.10), //
                        hasForexGrossValue("USD", 39.33), //
                        hasTaxes("EUR", 6.32 + 0.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24WithSecurityInEUR()
    {
        var security = new Security("MSCI World USD (Dist)", "EUR");
        security.setIsin("IE00BK1PV551");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-07T00:00"), hasShares(110.661875), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.44), hasGrossValue("EUR", 36.10), //
                        hasTaxes("EUR", 6.32 + 0.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende25()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000PAH0038"), hasWkn(null), hasTicker(null), //
                        hasName("Porsche Holding"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(45.671650), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 116.92), hasGrossValue("EUR", 130.98), //
                        hasTaxes("EUR", 13.33 + 0.73), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-06-14T00:00"), hasShares(45.671650), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.06), hasGrossValue("EUR", 14.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende26()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1875395870"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers Nikkei 225 Inhaber-Ant. 2D EURH o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-01T00:00"), hasShares(1.580925), //
                        hasSource("Dividende26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.51), hasGrossValue("EUR", 0.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende27()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("Microsoft"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-13T00:00"), hasShares(0.561914), //
                        hasSource("Dividende27.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.33), hasGrossValue("EUR", 0.39), //
                        hasForexGrossValue("USD", 0.42), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende27WithSecurityInEUR()
    {
        var security = new Security("Microsoft", "EUR");
        security.setIsin("US5949181045");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-13T00:00"), hasShares(0.561914), //
                        hasSource("Dividende27.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.33), hasGrossValue("EUR", 0.39), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US67066G1040"), hasWkn(null), hasTicker(null), //
                        hasName("NVIDIA"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-10-03T00:00"), hasShares(32.00), //
                        hasSource("Dividende28.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.29), //
                        hasForexGrossValue("USD", 0.32), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende28WithSecurityInEUR()
    {
        var security = new Security("NVIDIA", "EUR");
        security.setIsin("US67066G1040");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-10-03T00:00"), hasShares(32.00), //
                        hasSource("Dividende28.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.29), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US67066G1040"), hasWkn(null), hasTicker(null), //
                        hasName("NVIDIA"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-03T00:00"), hasShares(4.171585), //
                        hasSource("Dividende29.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.03), //
                        hasForexGrossValue("USD", 0.04), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende29WithSecurityInEUR()
    {
        var security = new Security("NVIDIA", "EUR");
        security.setIsin("US67066G1040");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-03T00:00"), hasShares(4.171585), //
                        hasSource("Dividende29.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(0.0929), //
                        hasSource("Dividend01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasForexGrossValue("USD", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend01WithSecurityInEUR()
    {
        var security = new Security("Apple Inc. Registered Shares o.N.", "EUR");
        security.setIsin("US0378331005");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(0.0929), //
                        hasSource("Dividend01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6092071058"), hasWkn(null), hasTicker(null), //
                        hasName("Mondelez"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-12T00:00"), hasShares(2.00), //
                        hasSource("Dividend02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.78), //
                        hasForexGrossValue("USD", 0.85), //
                        hasTaxes("EUR", 0.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend02WithSecurityInEUR()
    {
        var security = new Security("Mondelez", "EUR");
        security.setIsin("US6092071058");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-12T00:00"), hasShares(2.00), //
                        hasSource("Dividend02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.78), //
                        hasTaxes("EUR", 0.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("Core Euro Corp Bond EUR (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-31T00:00"), hasShares(43.00), //
                        hasSource("Dividend03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 90.28), hasGrossValue("EUR", 90.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("Realty Income"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(15.00), //
                        hasSource("Dividend04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.11), hasGrossValue("EUR", 3.66), //
                        hasForexGrossValue("USD", 3.94), //
                        hasTaxes("EUR", 0.55), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend04WithSecurityInEUR()
    {
        var security = new Security("Realty Income", "EUR");
        security.setIsin("US7561091049");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(15.00), //
                        hasSource("Dividend04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.11), hasGrossValue("EUR", 3.66), //
                        hasTaxes("EUR", 0.55), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9256521090"), hasWkn(null), hasTicker(null), //
                        hasName("Vici Properties"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-03T00:00"), hasShares(30.00), //
                        hasSource("Dividend05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.86), hasGrossValue("EUR", 11.60), //
                        hasForexGrossValue("USD", 12.45), //
                        hasTaxes("EUR", 1.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend05WithSecurityInEUR()
    {
        var security = new Security("Vici Properties", "EUR");
        security.setIsin("US9256521090");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-03T00:00"), hasShares(30.00), //
                        hasSource("Dividend05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.86), hasGrossValue("EUR", 11.60), //
                        hasTaxes("EUR", 1.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("Realty Income"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-15T00:00"), hasShares(15.00), //
                        hasSource("Dividend06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.09), hasGrossValue("EUR", 3.63), //
                        hasForexGrossValue("USD", 3.95), //
                        hasTaxes("EUR", 0.54), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend06WithSecurityInEUR()
    {
        var security = new Security("Realty Income", "EUR");
        security.setIsin("US7561091049");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-15T00:00"), hasShares(15.00), //
                        hasSource("Dividend06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.09), hasGrossValue("EUR", 3.63), //
                        hasTaxes("EUR", 0.54), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9256521090"), hasWkn(null), hasTicker(null), //
                        hasName("Vici Properties"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-03T00:00"), hasShares(30.00), //
                        hasSource("Dividend07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.86), hasGrossValue("EUR", 11.60), //
                        hasForexGrossValue("USD", 12.45), //
                        hasTaxes("EUR", 1.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend07WithSecurityInEUR()
    {
        var security = new Security("Vici Properties", "EUR");
        security.setIsin("US9256521090");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-03T00:00"), hasShares(30.00), //
                        hasSource("Dividend07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.86), hasGrossValue("EUR", 11.60), //
                        hasTaxes("EUR", 1.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker(null), //
                        hasName("Alphabet (A)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-17T00:00"), hasShares(2.00), //
                        hasSource("Dividend08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.37), //
                        hasForexGrossValue("USD", 0.40), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend08WithSecurityInEUR()
    {
        var security = new Security("Alphabet (A)", "EUR");
        security.setIsin("US02079K3059");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-17T00:00"), hasShares(2.00), //
                        hasSource("Dividend08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.37), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn(null), hasTicker(null), //
                        hasName("Realty Income"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(15.00), //
                        hasSource("Dividend09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.11), hasGrossValue("EUR", 3.66), //
                        hasForexGrossValue("USD", 3.94), //
                        hasTaxes("EUR", 0.55), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend09WithSecurityInEUR()
    {
        var security = new Security("Realty Income", "EUR");
        security.setIsin("US7561091049");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-14T00:00"), hasShares(15.00), //
                        hasSource("Dividend09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.11), hasGrossValue("EUR", 3.66), //
                        hasTaxes("EUR", 0.55), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendo01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendo01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA29250N1050"), hasWkn(null), hasTicker(null), //
                        hasName("Enbridge Inc. Registered Shares o.N."), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-01T00:00"), hasShares(20.971565), //
                        hasSource("Dividendo01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.56), hasGrossValue("EUR", 12.74), //
                        hasForexGrossValue("CAD", 18.61), //
                        hasTaxes("EUR", (4.65 / 1.46055)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendo01WithSecurityInEUR()
    {
        var security = new Security("Enbridge Inc. Registered Shares o.N.", "EUR");
        security.setIsin("CA29250N1050");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendo01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-01T00:00"), hasShares(20.971565), //
                        hasSource("Dividendo01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.56), hasGrossValue("EUR", 12.74), //
                        hasTaxes("EUR", (4.65 / 1.46055)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDistribuzione01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Distribuzione01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE001A0F8UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-17T00:00"), hasShares(23.632607), //
                        hasSource("Distribuzione01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.64), hasGrossValue("EUR", 16.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKaptialreduktion01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalreduktion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CA0679011084"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Barrick Gold Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check capital reduction transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.4226)));
        assertThat(transaction.getSource(), is("Kapitalreduktion01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.71))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.97))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.25 + 0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(1.18))));
    }

    @Test
    public void testKaptialreduktion01WithSecurityInEUR()
    {
        var security = new Security("Barrick Gold Corp. Registered Shares o.N.", "EUR");
        security.setIsin("CA0679011084");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalreduktion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.4226)));
        assertThat(transaction.getSource(), is("Kapitalreduktion01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.71))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.97))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.25 + 0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testKapitalerhoehungGegenBar01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KapitalerhoehungGegenBar01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D6554"), hasWkn(null), hasTicker(null), //
                        hasName("Nordex SE Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2021-07-02T00:00"), hasShares(130.00), //
                                        hasSource("KapitalerhoehungGegenBar01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDepotuebertragEingehend()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotuebertragEingehend.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000KBX1006"), hasWkn(null), hasTicker(null), //
                        hasName("Knorr-Bremse AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2022-11-08T00:00"), hasShares(13.00), //
                                        hasSource("DepotuebertragEingehend.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSpinOff01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SpinOff01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US64110Y1082"), hasWkn(null), hasTicker(null), //
                        hasName("Net Lease Office Properties Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2023-11-03T00:00"), hasShares(2.564), //
                                        hasSource("SpinOff01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtausch01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A3E5CX4"), hasWkn(null), hasTicker(null), //
                        hasName("Nordex SE Inhaber-Bezugsrechte"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2021-07-20T00:00"), hasShares(129.25), //
                                        hasSource("Umtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtausch02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D6554"), hasWkn(null), hasTicker(null), //
                        hasName("Nordex SE Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2021-07-20T00:00"), hasShares(47.00), //
                                        hasSource("Umtausch02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(173.3905)));
        assertThat(transaction.getSource(), is("Vorabpauschale01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.30 + 0.02))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.30 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010315770"), hasWkn(null), hasTicker(null), //
                        hasName("MUF-Amundi MSCI World II U.E. Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2024-01-15T00:00"), hasShares(5.598), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Vorabpauschale 1,73 EUR"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B40B8R38"), hasWkn(null), hasTicker(null), //
                        hasName("S&P 500 Consumer Staples USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-24T00:00"), hasShares(591.791634), //
                        hasSource("Vorabpauschale03.txt"), //
                        hasNote("Vorabpauschale 45,40 EUR"), //
                        hasAmount("EUR", 11.97), hasGrossValue("EUR", 11.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("Core MSCI World USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-29T00:00"), hasShares(10.00), //
                        hasSource("Vorabpauschale04.txt"), //
                        hasNote("Vorabpauschale 9,23 EUR"), //
                        hasAmount("EUR", 2.43), hasGrossValue("EUR", 2.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0533033667"), hasWkn(null), hasTicker(null), //
                        hasName("MSCI World Information Tech EUR (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-28T00:00"), hasShares(42.056174), //
                        hasSource("Vorabpauschale05.txt"), //
                        hasNote("Vorabpauschale 140,55 EUR"), //
                        hasAmount("EUR", 37.07), hasGrossValue("EUR", 37.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.4534)));
        assertThat(entry.getSource(), is("Sparplan01.txt"));
        assertThat(entry.getNote(), is("Sparplan: 123-123 | Ausführung: ff2f-1254"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0744"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.DJ Asia Pa.S.D.30 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.9261)));
        assertThat(entry.getSource(), is("Sparplan02.txt"));
        assertThat(entry.getNote(), is("Sparplan: xxxx-xxxx | Ausführung: xxxx-xxxx"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0031442068"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs Core S&P 500 UC.ETF USDD Registered Shares USD (Dist)oN"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.5520)));
        assertThat(entry.getSource(), is("Sparplan03.txt"));
        assertThat(entry.getNote(), is("Sparplan: 77c8-1b0c | Ausführung: 8eac-25ab"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(150.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(150.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0000121014"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LVMH Moët Henn. L. Vuitton SE Actions Port. (C.R.) EO 0,3"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.0028)));
        assertThat(entry.getSource(), is("Sparplan04.txt"));
        assertThat(entry.getNote(), is("Sparplan: 98a0-1d15 | Ausführung: 3609-6874"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1003.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(3.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("NL0010273215"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ASML Holding N.V. Aandelen op naam EO -,09"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-04-19T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0358)));
        assertThat(entry.getSource(), is("Sparplan05.txt"));
        assertThat(entry.getNote(), is("Sparplan: af97-c4b1 | Ausführung: fc69-6fc3"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(19.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(19.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0846707026"), hasWkn(null), hasTicker(null), //
                        hasName("Berkshire Hathaway Inc. Reg.Shares B New DL -,00333"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-16T00:00"), hasShares(0.3367), //
                        hasSource("Sparplan06.txt"), //
                        hasNote("Savings plan: 7687-2574 | Execution: e083-506a"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0846707026"), hasWkn(null), hasTicker(null), //
                        hasName("Berkshire Hathaway Inc. Reg.Shares B New DL -,00333"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-02T00:00"), hasShares(0.081967), //
                        hasSource("Sparplan07.txt"), //
                        hasNote("Savings plan: dbc9-ad4d | Execution: d008-0f58"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFMXXD54"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard S&P 500 UCITS ETF Reg. Shs USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-02T00:00"), hasShares(0.336491), //
                        hasSource("Sparplan08.txt"), //
                        hasNote("Savings plan: 6af7-5be3 | Execution: 8c26-15e1"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011794037"), hasWkn(null), hasTicker(null), //
                        hasName("Ahold Delhaize"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-10T00:00"), hasShares(0.106382), //
                        hasSource("Sparplan09.txt"), //
                        hasNote("Sparplan: 6c05-cb18 | Ausführung: d149-71d0"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan10()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn(null), hasTicker(null), //
                        hasName("Novo-Nordisk (B)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-02T00:00"), hasShares(0.235139), //
                        hasSource("Sparplan10.txt"), //
                        hasNote("Sparplan: 914b-b475 | Ausführung: f514-277b"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan11()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0000130809"), hasWkn(null), hasTicker(null), //
                        hasName("Société Générale"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-04T00:00"), hasShares(0.920979), //
                        hasSource("Sparplan11.txt"), //
                        hasNote("Sparplan: abcd-1234 | Ausführung: 1234-abcd"), //
                        hasAmount("EUR", 25.08), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.08), hasFees("EUR", 0.00))));
    }

    @Test
    public void testPianoDinvestimento01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PianoDinvestimento01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BWT3KN65"), hasWkn(null), hasTicker(null), //
                        hasName("Factor MSCI USA Quality ESG EUR Hedged (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-16T00:00"), hasShares(6.236285), //
                        hasSource("PianoDinvestimento01.txt"), //
                        hasNote("Piano D'Investimenton: d9I1-588y | Esecuzione: b61b-9U71"), //
                        hasAmount("EUR", 270.00), hasGrossValue("EUR", 270.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testPlanDeInvestion01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PlanDeInvestion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF "), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-02T00:00"), hasShares(2.245424), //
                        hasSource("PlanDeInvestion01.txt"), //
                        hasNote("Plan de Invesión: 21ef-595a | Ejecución: ff4d-982a"), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testPlanDeInvestion02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PlanDeInvestion02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000716YHJ7"), hasWkn(null), hasTicker(null), //
                        hasName("FTSE All-World USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24T00:00"), hasShares(25.007144), //
                        hasSource("PlanDeInvestion02.txt"), //
                        hasNote("Plan de Invesión: 865f-4af2 | Ejecución: 98d3-987f"), //
                        hasAmount("EUR", 175.00), hasGrossValue("EUR", 175.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFusion01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US83088V1026"), hasWkn(null), hasTicker(null), //
                        hasName("Slack Technologies Inc. Registered Shs Cl.A o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US79466L3024"), hasWkn(null), hasTicker(null), //
                        hasName("salesforce.com Inc. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2021-07-27T00:00"), hasShares(10.00), //
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2021-07-27T00:00"), hasShares(0.776), //
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSplit01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Split01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasWkn(null), hasTicker(null), //
                        hasName("Amazon.com Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorSplitTransactionsNotSupported, //
                        inboundDelivery( //
                                        hasDate("2022-06-04T00:00"), hasShares(17.4743), //
                                        hasSource("Split01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testZwangsuebernahme01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zwangsuebernahme01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010785967"), hasWkn(null), hasTicker(null), //
                        hasName("Quantafuel AS Navne-Aksjer NK -,01"), //
                        hasCurrencyCode("NOK"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-12T00:00"), hasShares(17.00), //
                        hasSource("Zwangsuebernahme01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.54), hasGrossValue("EUR", 9.54), //
                        hasForexGrossValue("NOK", 108.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZwangsuebernahme01WithSecurityInEUR()
    {
        var security = new Security("Quantafuel AS Navne-Aksjer NK -,01", "EUR");
        security.setIsin("NO0010785967");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zwangsuebernahme01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-12T00:00"), hasShares(17.00), //
                        hasSource("Zwangsuebernahme01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.54), hasGrossValue("EUR", 9.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerlicherUmtausch01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerlicherUmtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010754135"), hasWkn(null), hasTicker(null), //
                        hasName("AMUN.GOV.BD EO BR.IG 1-3 U.ETF Actions au Porteur o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2023-12-07T00:00"), hasShares(50.00), //
                                        hasSource("SteuerlicherUmtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSteuerlicherUmtausch02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerlicherUmtausch02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYX.EO Gov.Bd 1-3Y(DR)U.E. Nam.-An. Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2023-12-07T00:00"), hasShares(67.541), //
                                        hasSource("SteuerlicherUmtausch02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVergleichsverfahren01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vergleichsverfahren01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00BH0P3Z91"), hasWkn(null), hasTicker(null), //
                        hasName("BHP Group PLC Registered Shares DL -,50"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("AU000000BHP4"), hasWkn(null), hasTicker(null), //
                        hasName("BHP Group Ltd. Registered Shares DL -,50"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2022-01-27T00:00"), hasShares(8.00), //
                                        hasSource("Vergleichsverfahren01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2022-01-27T00:00"), hasShares(8.00), //
                                        hasSource("Vergleichsverfahren01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testTitelumtausch01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Titelumtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("Shell PLC Reg. Shares Class A EO -,07"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BP6MXD84"), hasWkn(null), hasTicker(null), //
                        hasName("Shell PLC Reg. Shares Class EO -,07"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2022-02-01T00:00"), hasShares(25.00), //
                                        hasSource("Titelumtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2022-02-01T00:00"), hasShares(25.00), //
                                        hasSource("Titelumtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testEinzahlungsabrechnung01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlungsabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-01-07T00:00"), //
                        hasSource("Einzahlungsabrechnung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 49.00), hasGrossValue("EUR", 49.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testEinzahlungsabrechnung02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlungsabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-09-01T00:00"), //
                        hasSource("Einzahlungsabrechnung02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testEinzahlungsabrechnung03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlungsabrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-03-01T00:00"), //
                        hasSource("Einzahlungsabrechnung03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testReglementDuVersement01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ReglementDuVersement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2023-05-24T00:00"), //
                        hasSource("ReglementDuVersement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-01T00:00"), //
                        hasSource("Zinsabrechnung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.88), hasGrossValue("EUR", 0.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-02-01T00:00"), //
                        hasSource("Zinsabrechnung02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.58), hasGrossValue("EUR", 2.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-11-01T00:00"), //
                        hasSource("Zinsabrechnung03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 112.40), hasGrossValue("EUR", 152.67), //
                        hasTaxes("EUR", 38.17 + 2.10), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung04()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01T00:00"), //
                        hasSource("Zinsabrechnung04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 118.17), hasGrossValue("EUR", 160.50), //
                        hasTaxes("EUR", 40.13 + 2.20), hasFees("EUR", 0.00))));

        // check dividend interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01T00:00"), //
                        hasSource("Zinsabrechnung04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.77), hasGrossValue("EUR", 3.76), //
                        hasTaxes("EUR", 0.94 + 0.05), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung05()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-06-01T00:00"), //
                        hasSource("Zinsabrechnung05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.28), hasGrossValue("EUR", 34.33), //
                        hasTaxes("EUR", 8.58 + 0.47), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung06()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-10-01T00:00"), //
                        hasSource("Zinsabrechnung06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 74.95), hasGrossValue("EUR", 101.79), //
                        hasTaxes("EUR", 25.45 + 1.39), hasFees("EUR", 0.00))));

        // check dividend interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-10-01T00:00"), //
                        hasSource("Zinsabrechnung06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.84), hasGrossValue("EUR", 16.08), //
                        hasTaxes("EUR", 4.02 + 0.22), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung07()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01T00:00"), //
                        hasSource("Zinsabrechnung07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 21.02 + 52.06), hasGrossValue("EUR", 21.02 + 52.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung08()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-01-01T00:00"), //
                        hasSource("Zinsabrechnung08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.36), hasGrossValue("EUR", 45.31), //
                        hasTaxes("EUR", 11.33 + 0.62), hasFees("EUR", 0.00))));

        // check dividend interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-01-01T00:00"), //
                        hasSource("Zinsabrechnung08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 66.10), hasGrossValue("EUR", 89.78), //
                        hasTaxes("EUR", 22.45 + 1.23), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung09()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-03-07T00:00"), //
                        hasSource("Zinsabrechnung09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 97.32), hasGrossValue("EUR", 135.14), //
                        hasTaxes("EUR", 33.04 + 2.97 + 1.81), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRapportDInterets01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "RapportDInterets01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-02-01T00:00"), //
                        hasSource("RapportDInterets01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung10()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-12-01T00:00"), //
                        hasSource("Zinsabrechnung10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 28.88), hasGrossValue("EUR", 38.51), //
                        hasTaxes("EUR", 9.63), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRapportDInterets02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "RapportDInterets02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-02-01T00:00"), //
                        hasSource("RapportDInterets02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.98), //
                        hasTaxes("EUR", 0.16 + 0.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRescontoInteressiMaturati01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "RescontoInteressiMaturati01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-06-01T00:00"), //
                        hasSource("RescontoInteressiMaturati01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testInterestInvoice01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InterestInvoice01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-10-02T00:00"), //
                        hasSource("InterestInvoice01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.47), hasGrossValue("EUR", 1.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testInterestInvoice02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InterestInvoice02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");
        assertThat(errors, empty());

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01T00:00"), //
                        hasSource("InterestInvoice02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.10 + 11.56), hasGrossValue("EUR", 7.10 + 11.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSynthese01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Synthese01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-04-19T00:00"), //
                        hasSource("Synthese01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUeberweisungsdetails01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ueberweisungsdetails01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-01"), hasAmount("EUR", 6400.00),
                        hasSource("Ueberweisungsdetails01.txt"), hasNote("SEPA Überweisung"))));
    }

    @Test
    public void testUeberweisungsdetails02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ueberweisungsdetails02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-21"), hasAmount("EUR", 1000.00),
                        hasSource("Ueberweisungsdetails02.txt"), hasNote("SEPA Überweisung"))));
    }

    @Test
    public void testExAnte01()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ExAnte01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CNE100000296"), hasWkn(null), hasTicker(null), //
                        hasName("BYD"), //
                        hasCurrencyCode("EUR"))));

        // check interest transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-08T00:00"), hasShares(12.00), //
                        hasSource("ExAnte01.txt"), //
                        hasNote("Auftrag: 089ebe91"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testExAnte02()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ExAnte02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CNE100000296"), hasWkn(null), hasTicker(null), //
                        hasName("BYD"), //
                        hasCurrencyCode("EUR"))));

        // check interest transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-03T00:00"), hasShares(11.00), //
                        hasSource("ExAnte02.txt"), //
                        hasNote("Auftrag: 14010452"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testExAnte03()
    {
        var extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ExAnte03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("FTSE All-World USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check interest transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-11-03T00:00"), hasShares(0.851192), //
                        hasSource("ExAnte03.txt"), //
                        hasNote("Auftrag: Np7t6v68"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
